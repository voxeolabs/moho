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

package com.voxeo.moho.media;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.RTC;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputPausedEvent;
import com.voxeo.moho.event.OutputResumedEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.event.RecordPausedEvent;
import com.voxeo.moho.event.RecordResumedEvent;
import com.voxeo.moho.event.RecordStartedEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.event.fake.MockEventSource;
import com.voxeo.moho.media.fake.MockMediaGroup;
import com.voxeo.moho.media.fake.MockMediaSession;
import com.voxeo.moho.media.fake.MockParameters;
import com.voxeo.moho.media.fake.MockPlayer;
import com.voxeo.moho.media.fake.MockRecorder;
import com.voxeo.moho.media.fake.MockSignalDetector;

public class GenericMediaServiceTest extends TestCase {
  Mockery mockery;

  // mock jsr289 object
  MockMediaSession session;

  NetworkConnection call;

  MockMediaGroup group;

  MockPlayer player;

  MockRecorder recorder;

  MockSignalDetector signalDetector;

  SignalGenerator signalGenerator;

  // mock moho object.
  MockEventSource parent;

  // testing object.
  GenericMediaService service;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mockery = new Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };

    // mock jsr289 object
    session = mockery.mock(MockMediaSession.class);

    call = mockery.mock(NetworkConnection.class);

    group = mockery.mock(MockMediaGroup.class);

    player = mockery.mock(MockPlayer.class);

    recorder = mockery.mock(MockRecorder.class);

    signalDetector = mockery.mock(MockSignalDetector.class);

    signalGenerator = mockery.mock(SignalGenerator.class);

    // mock moho object.
    parent = mockery.mock(MockEventSource.class);

    // creating GenericMediaService expectations.
    mockery.checking(new Expectations() {
      {
        oneOf(session).createMediaGroup(with(any(Configuration.class)));
        will(new Action() {
          @Override
          public void describeTo(final Description description) {
          }

          @Override
          public Object invoke(final Invocation invocation) throws Throwable {
            group.setMediaSession(session);
            return group;
          }
        });

        // oneOf(group).join(Joinable.Direction.DUPLEX, call);

        allowing(group).getPlayer();
        will(returnValue(player));

        allowing(group).getRecorder();
        will(returnValue(recorder));

        allowing(group).getSignalGenerator();
        will(returnValue(signalGenerator));

        allowing(group).getSignalDetector();
        will(returnValue(signalDetector));
      }
    });

    // create mediaservice
    service = (GenericMediaService) new GenericMediaServiceFactory().create(parent, session);
  }

  /**
   * @throws MalformedURLException
   */
  public void testRecordURLComplete() throws MalformedURLException {

    // prepare
    final URL url = new URL("http://TEST");

    // mock record start event.
    final RecorderEvent mediaEvent0 = mockery.mock(RecorderEvent.class, "mediaEvent0");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(RecorderEvent.STARTED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // mock record complete event.
    final RecorderEvent mediaEvent1 = mockery.mock(RecorderEvent.class);
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent1).getEventType();
          will(returnValue(RecorderEvent.RECORD_COMPLETED));

          allowing(mediaEvent1).getQualifier();
          will(returnValue(RecorderEvent.SILENCE));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // invoke record.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(recorder).record(url.toURI(), null, null);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = recorder.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent0);
                  }

                  try {
                    Thread.sleep(1000);
                  }
                  catch (final Exception ex) {
                    ex.printStackTrace();
                  }

                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent1);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    Recording recording = null;
    RecordCompleteEvent event = null;
    // excute
    try {
      recording = service.record(url);

      event = recording.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify
    assertTrue(event != null);
    assertTrue(event.getCause() == RecordCompleteEvent.Cause.SILENCE);
    // verify event is dispatched.
    assertTrue(parent.getReceivedEvents().get(0) instanceof RecordStartedEvent);
    assertTrue(parent.getReceivedEvents().get(1) instanceof RecordCompleteEvent);
    assertTrue(((RecordCompleteEvent) parent.getReceivedEvents().get(1)).getCause() == RecordCompleteEvent.Cause.SILENCE);
    assertTrue(recorder.listeners.size() == 0);
    mockery.assertIsSatisfied();
  }

  /**
   * @throws MalformedURLException
   */
  public void testRecordURL() throws MalformedURLException {

    // prepare.
    final URL url = new URL("http://TEST");

    // mock record start event.
    final RecorderEvent mediaEvent0 = mockery.mock(RecorderEvent.class, "mediaEvent0");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(RecorderEvent.STARTED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
    // mock record paused event.
    final RecorderEvent mediaEvent1 = mockery.mock(RecorderEvent.class, "mediaEvent1");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent1).getEventType();
          will(returnValue(RecorderEvent.PAUSED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
    // mock record resumed event.
    final RecorderEvent mediaEvent2 = mockery.mock(RecorderEvent.class, "mediaEvent2");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent2).getEventType();
          will(returnValue(RecorderEvent.RESUMED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
    // mock record stopped event.
    final RecorderEvent mediaEvent3 = mockery.mock(RecorderEvent.class, "mediaEvent3");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent3).getEventType();
          will(returnValue(RecorderEvent.RECORD_COMPLETED));

          allowing(mediaEvent3).getQualifier();
          will(returnValue(RecorderEvent.STOPPED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // invoke record.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(recorder).record(url.toURI(), null, null);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = recorder.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent0);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

          // invoke pause.
          oneOf(group).triggerAction(Recorder.PAUSE);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = recorder.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent1);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

          // invoke resume.
          oneOf(group).triggerAction(Recorder.RESUME);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = recorder.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent2);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

          // invoke stop.
          oneOf(group).triggerAction(Recorder.STOP);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = recorder.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<RecorderEvent>) listerner).onEvent(mediaEvent3);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    Recording recording = null;
    RecordCompleteEvent event = null;
    // execute
    try {
      recording = service.record(url);

      recording.pause();
      recording.resume();
      assertTrue(recorder.listeners.size() == 1);

      recording.stop();

      event = recording.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify
    assertTrue(event != null);
    assertTrue(event.getCause() == RecordCompleteEvent.Cause.CANCEL);
    // verify event is dispatched.
    assertTrue(parent.getReceivedEvents().get(0) instanceof RecordStartedEvent);
    assertTrue(parent.getReceivedEvents().get(1) instanceof RecordPausedEvent);
    assertTrue(parent.getReceivedEvents().get(2) instanceof RecordResumedEvent);
    assertTrue(parent.getReceivedEvents().get(3) instanceof RecordCompleteEvent);
    assertTrue(((RecordCompleteEvent) parent.getReceivedEvents().get(3)).getCause() == RecordCompleteEvent.Cause.CANCEL);
    assertTrue(recorder.listeners.size() == 0);
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  public void testOutputStringComplete() {

    // prepare.
    // mock play complete event.
    final PlayerEvent mediaEvent0 = mockery.mock(PlayerEvent.class, "mediaEvent0");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(PlayerEvent.PLAY_COMPLETED));

          allowing(mediaEvent0).getQualifier();
          will(returnValue(PlayerEvent.END_OF_PLAY_LIST));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    final MockParameters parameters = mockery.mock(MockParameters.class, "parameters");

    try {
      mockery.checking(new Expectations() {
        {
          // create parameters
          oneOf(group).createParameters();
          will(returnValue(parameters));

          // invoke player.play
          allowing(player).play(with(any(URI[].class)), with(new TypeSafeMatcher<RTC[]>() {
            @Override
            public boolean matchesSafely(final RTC[] item) {
              player.setRtcs(item);
              return true;
            }

            @Override
            public void describeTo(final Description description) {
            }

          }), with(same(parameters)));
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = player.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<PlayerEvent>) listerner).onEvent(mediaEvent0);
                  }

                }
              });
              th.start();
              return null;
            }
          });

        }
      });

      //

    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    final Output output = service.output("test");

    OutputCompleteEvent completEvent = null;
    try {
      completEvent = output.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify
    assertTrue(completEvent.getCause() == Cause.END);
    mockery.assertIsSatisfied();
  }

  /**
   * @throws MalformedURLException
   */
  public void testOutputString() throws MalformedURLException {

    // prepare.
    // mock record paused event.
    final PlayerEvent mediaEvent1 = mockery.mock(PlayerEvent.class, "mediaEvent1");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent1).getEventType();
          will(returnValue(PlayerEvent.PAUSED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
    // mock record resumed event.
    final PlayerEvent mediaEvent2 = mockery.mock(PlayerEvent.class, "mediaEvent2");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent2).getEventType();
          will(returnValue(PlayerEvent.RESUMED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
    // mock record stopped event.
    final PlayerEvent mediaEvent3 = mockery.mock(PlayerEvent.class, "mediaEvent3");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent3).getEventType();
          will(returnValue(PlayerEvent.PLAY_COMPLETED));

          allowing(mediaEvent3).getQualifier();
          will(returnValue(PlayerEvent.STOPPED));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    final MockParameters parameters = mockery.mock(MockParameters.class, "parameters");

    try {
      mockery.checking(new Expectations() {
        {
          // create parameters
          oneOf(group).createParameters();
          will(returnValue(parameters));

          // invoke play.
          allowing(player).play(with(any(URI[].class)), with(new TypeSafeMatcher<RTC[]>() {
            @Override
            public boolean matchesSafely(final RTC[] item) {
              player.setRtcs(item);
              return true;
            }

            @Override
            public void describeTo(final Description description) {
            }

          }), with(same(parameters)));

          // invoke pause.
          oneOf(group).triggerAction(Player.PAUSE);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = player.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<PlayerEvent>) listerner).onEvent(mediaEvent1);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

          // invoke resume.
          oneOf(group).triggerAction(Player.RESUME);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = player.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<PlayerEvent>) listerner).onEvent(mediaEvent2);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });

          // invoke stop.
          oneOf(group).triggerAction(Player.STOP);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = player.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<PlayerEvent>) listerner).onEvent(mediaEvent3);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    Output output = null;
    OutputCompleteEvent event = null;
    // execute
    try {
      output = service.output("test");

      output.pause();
      output.resume();
      assertTrue(player.listeners.size() == 1);

      output.stop();

      event = output.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify
    assertTrue(event != null);
    assertTrue(event.getCause() == OutputCompleteEvent.Cause.CANCEL);
    // verify event is dispatched.
    assertTrue(parent.getReceivedEvents().get(0) instanceof OutputPausedEvent);
    assertTrue(parent.getReceivedEvents().get(1) instanceof OutputResumedEvent);
    assertTrue(parent.getReceivedEvents().get(2) instanceof OutputCompleteEvent);
    assertTrue(((OutputCompleteEvent) parent.getReceivedEvents().get(2)).getCause() == OutputCompleteEvent.Cause.CANCEL);
    assertTrue(player.listeners.size() == 0);
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  public void testInputString() {

    // prepare.
    final SignalDetectorEvent mediaEvent0 = mockery.mock(SignalDetectorEvent.class, "mediaEvent0");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED));

          allowing(mediaEvent0).getQualifier();
          will(returnValue(SignalDetectorEvent.PATTERN_MATCHING[0]));

          allowing(mediaEvent0).getSignalString();
          will(returnValue("123"));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // create parameters
          allowing(group).createParameters();
          will(returnValue(new MockParameters()));

          oneOf(signalDetector).receiveSignals(with(equal(-1)), with(any(Parameter[].class)),
              with(new TypeSafeMatcher<RTC[]>() {
                @Override
                public boolean matchesSafely(final RTC[] item) {
                  signalDetector.setRtcs(item);
                  return true;
                }

                @Override
                public void describeTo(final Description description) {
                }

              }), with(any(Parameters.class)));
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  final Object[] ls = signalDetector.listeners.toArray();
                  for (final Object listerner : ls) {
                    ((MediaEventListener<SignalDetectorEvent>) listerner).onEvent(mediaEvent0);
                  }
                }
              });
              th.start();
              th.join();
              return null;
            }

          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // exercise.
    final Input input = service.input("123");
    InputCompleteEvent event = null;
    try {
      event = input.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify the result.
    assertTrue(event != null);
    assertTrue(event.getCause() == InputCompleteEvent.Cause.MATCH);
    assertTrue(group.settedParameters.get(SignalDetector.PATTERN[0]).equals("123"));
    mockery.assertIsSatisfied();
  }
}
