/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.conference;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.sdp.SdpFactory;
import javax.servlet.sip.SipFactory;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.DisconnectEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.GenericMediaService;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.fake.MockMediaSession;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class ConferenceTest extends TestCase {

  Mockery mockery = new Mockery() {
    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  // JSR309 mock
  MsControlFactory msFactory = mockery.mock(MsControlFactory.class);

  MockMediaSession mediaSession = mockery.mock(MockMediaSession.class);

  MediaMixer mixer = mockery.mock(MediaMixer.class);

  // JSR289 mock
  SipFactory sipFactory = mockery.mock(SipFactory.class);

  SdpFactory sdpFactory = mockery.mock(SdpFactory.class);

  // Moho
  TestApp app = mockery.mock(TestApp.class);

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = new ApplicationContextImpl(app, msFactory, sipFactory, sdpFactory, "test", null, 2);

  MixerEndpoint address;

  ConferenceImpl mohoConference;

  Properties props = new Properties();

  protected void setUp() throws Exception {
    super.setUp();
    mockery.checking(new Expectations() {
      {
        // creation
        oneOf(msFactory).getProperties();
        will(returnValue(props));
      }
    });
    address = (MixerEndpoint) appContext.getEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * test join and unjoin();
   */
  public void testConferenceJoinAndUnjoin() {
    // create conference preparation.
    try {
      mockery.checking(new Expectations() {
        {
          // creation
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession).createMediaMixer(with(any(Configuration.class)), with(any(Parameters.class)));
          will(returnValue(mixer));

          oneOf(mixer).addListener(with(any(MediaEventListener.class)));
          will(returnValue(null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    final OutputCommand promptCommand = new OutputCommand(new TextToSpeechResource("welcome, password needed."));
    final InputCommand passCommand = new InputCommand(new SimpleGrammar("123"));
    final InputCommand exitCommand = new InputCommand(new SimpleGrammar("1#"));
    final OutputCommand exitAnnouncement = new OutputCommand(new TextToSpeechResource("bye."));

    // create conference.
    SimpleConferenceController controller = new SimpleConferenceController(promptCommand, passCommand, 3, exitCommand,
        exitAnnouncement);
    ConferenceManager manager = new ConferenceMangerImpl(appContext);
    mohoConference = (ConferenceImpl) manager.createConference(address, null, "test", 10, controller, null);

    // verify the creation result.
    assertTrue(mohoConference.getId().equals("test"));
    assertTrue(mohoConference.getMaxSeats() == 10);
    assertTrue(mohoConference.getOccupiedSeats() == 0);

    // mock the call
    final Call call = mockery.mock(Call.class);
    final NetworkConnection callNet = mockery.mock(NetworkConnection.class);
    final GenericMediaService mediaService = mockery.mock(GenericMediaService.class);
    final Prompt prompt = mockery.mock(Prompt.class);
    final Input input = mockery.mock(Input.class);
    final InputCompleteEvent event = mockery.mock(InputCompleteEvent.class);

    try {
      mockery.checking(new Expectations() {
        {
          allowing(call).getMediaObject();
          will(returnValue(callNet));

          allowing(call).getMediaService();
          will(returnValue(mediaService));

          allowing(call).getMediaService(with(any(Boolean.class)));
          will(returnValue(mediaService));

          oneOf(mediaService).prompt(with(same(promptCommand)), with(same(passCommand)), with(equal(3)));
          will(returnValue(prompt));

          allowing(prompt).getInput();
          will(returnValue(input));
          allowing(input).get();
          will(returnValue(event));
          allowing(event).hasMatch();
          will(returnValue(true));

          oneOf(call).addObservers(with(any(Observer.class)));
          oneOf(mediaService).input(exitCommand);

          // join
          oneOf(call).join(mohoConference, JoinType.BRIDGE, Direction.DUPLEX);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              mohoConference.addParticipant(call, JoinType.BRIDGE, Direction.DUPLEX, null);
              return new JointImpl(new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
                  new ArrayBlockingQueue<Runnable>(1)), new JointImpl.DummyJoinWorker(mohoConference, call));
            }
          });

          // unjoin
          oneOf(mixer).unjoin(callNet);
          oneOf(call).unjoin(mohoConference);

          oneOf(mediaService).output(with(same(exitAnnouncement)));

          oneOf(call).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // execute join.
    try {
      mohoConference.join(call, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (IllegalStateException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    catch (ExecutionException e) {
      e.printStackTrace();
    }

    // verify join result.
    assertTrue(mohoConference.getOccupiedSeats() == 1);
    assertTrue(mohoConference.getParticipants()[0] == call);

    // execute unjoin.
    mohoConference.unjoin(call);
    // verify unjoin result.
    assertTrue(mohoConference.getOccupiedSeats() == 0);
    assertTrue(mohoConference.getParticipants().length == 0);

    mockery.assertIsSatisfied();
  }

  /**
   * test password error.
   */
  public void testConferenceJoinFail() {
    // create conference preparation.
    try {
      mockery.checking(new Expectations() {
        {
          // creation
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession).createMediaMixer(with(any(Configuration.class)), with(any(Parameters.class)));
          will(returnValue(mixer));

          oneOf(mixer).addListener(with(any(MediaEventListener.class)));
          will(returnValue(null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    final OutputCommand promptCommand = new OutputCommand(new TextToSpeechResource("welcome, password needed."));
    final InputCommand passCommand = new InputCommand(new SimpleGrammar("123"));
    final InputCommand exitCommand = new InputCommand(new SimpleGrammar("1#"));
    final OutputCommand exitAnnouncement = new OutputCommand(new TextToSpeechResource("bye."));

    // create conference.
    SimpleConferenceController controller = new SimpleConferenceController(promptCommand, passCommand, 3, exitCommand,
        exitAnnouncement);
    ConferenceManager manager = new ConferenceMangerImpl(appContext);
    mohoConference = (ConferenceImpl) manager.createConference(address, null, "test", 10, controller, null);

    // verify the creation result.
    assertTrue(mohoConference.getId().equals("test"));
    assertTrue(mohoConference.getMaxSeats() == 10);
    assertTrue(mohoConference.getOccupiedSeats() == 0);

    // mock the call
    final Call call = mockery.mock(Call.class);
    final NetworkConnection callNet = mockery.mock(NetworkConnection.class);
    final GenericMediaService mediaService = mockery.mock(GenericMediaService.class);
    final Prompt prompt = mockery.mock(Prompt.class);
    final Input input = mockery.mock(Input.class);
    final InputCompleteEvent event = mockery.mock(InputCompleteEvent.class);

    try {
      mockery.checking(new Expectations() {
        {
          allowing(call).getMediaObject();
          will(returnValue(callNet));

          allowing(call).getMediaService();
          will(returnValue(mediaService));

          oneOf(mediaService).prompt(with(same(promptCommand)), with(same(passCommand)), with(equal(3)));
          will(returnValue(prompt));

          allowing(prompt).getInput();
          will(returnValue(input));
          allowing(input).get();
          will(returnValue(event));
          allowing(event).hasMatch();
          will(returnValue(false));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // execute join.
    try {
      mohoConference.join(call, JoinType.BRIDGE, Direction.DUPLEX).get();
      fail("don't catch the excpetion");
    }
    catch (Exception ex) {
      assertTrue(ex.getCause() instanceof ConferencePasswordNoMatchException);
    }

    // verify join result.
    assertTrue(mohoConference.getOccupiedSeats() == 0);
    assertTrue(mohoConference.getParticipants().length == 0);

    mockery.assertIsSatisfied();
  }

  interface TestApp extends Application {
    public void handleDisconnect(DisconnectEvent event);
  }
}
