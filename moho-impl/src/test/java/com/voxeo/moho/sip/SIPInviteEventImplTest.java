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

package com.voxeo.moho.sip;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.SdpFactory;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletResponse;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.fake.MockParameters;
import com.voxeo.moho.media.fake.MockSdpPortManager;
import com.voxeo.moho.sip.SIPIncomingCallTest.TestApp;
import com.voxeo.moho.sip.fake.MockServletContext;
import com.voxeo.moho.sip.fake.MockSipServletRequest;
import com.voxeo.moho.sip.fake.MockSipServletResponse;
import com.voxeo.moho.sip.fake.MockSipSession;

public class SIPInviteEventImplTest extends TestCase {

  Mockery mockery = new Mockery() {
    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  // JSR309 mock
  MsControlFactory msFactory = mockery.mock(MsControlFactory.class);

  MediaSession mediaSession = mockery.mock(MediaSession.class);

  NetworkConnection network = mockery.mock(NetworkConnection.class);

  MockSdpPortManager sdpManager = mockery.mock(MockSdpPortManager.class);

  // JSR289 mock
  SipFactory sipFactory = mockery.mock(SipFactory.class);

  SdpFactory sdpFactory = mockery.mock(SdpFactory.class);

  MockSipSession session = mockery.mock(MockSipSession.class);

  MockSipServletRequest inviteReq = mockery.mock(MockSipServletRequest.class);

  MockServletContext servletContext = mockery.mock(MockServletContext.class);

  // Moho
  TestApp app = mockery.mock(TestApp.class);

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = new ApplicationContextImpl(app, msFactory, sipFactory, sdpFactory, "test", null, 2);

  Address fromAddr = mockery.mock(Address.class, "fromAddr");

  Address toAddr = mockery.mock(Address.class, "toAddr");

  protected void setUp() throws Exception {
    super.setUp();

    servletContext.setAttribute(ApplicationContext.APPLICATION_CONTEXT, appContext);
    session.setServletContext(servletContext);
    inviteReq.setSession(session);
    inviteReq.setRawContent(new byte[10]);

    try {
      mockery.checking(new Expectations() {
        {
          allowing(session).getRemoteParty();
          will(returnValue(fromAddr));

          allowing(inviteReq).getFrom();
          will(returnValue(fromAddr));

          oneOf(inviteReq).getTo();
          will(returnValue(toAddr));

          allowing(mediaSession).createParameters();
          will(returnValue(new MockParameters()));

          allowing(mediaSession).setParameters(with(any(MockParameters.class)));

          allowing(session).getCallId();
          will(returnValue("test"));
        }
      });
    }
    catch (Throwable ex) {
      ex.printStackTrace();
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * @throws Exception
   */
  public void testAccept() throws Exception {
    // prepare
    Observer ob = mockery.mock(Observer.class);
    final MockSipServletResponse inviteResp = mockery.mock(MockSipServletResponse.class, "inviteResp");

    mockery.checking(new Expectations() {
      {
        oneOf(inviteReq).createResponse(SipServletResponse.SC_RINGING);
        will(returnValue(inviteResp));

        oneOf(inviteResp).send();
      }
    });

    // execute
    SIPInviteEventImpl invite = new SIPInviteEventImpl(appContext, inviteReq);
    Call call = invite.acceptCall(ob);

    // assert
    assertTrue(call.getCallState() == State.ACCEPTED);
    mockery.assertIsSatisfied();
  }

  /**
   * @throws Exception
   */
  public void testAcceptWithEarlyMedia() throws Exception {
    final byte[] requestSDP = new byte[10];

    // prepare
    Observer ob = mockery.mock(Observer.class);
    final MockSipServletResponse inviteResp = mockery.mock(MockSipServletResponse.class, "inviteResp");

    final SdpPortManagerEvent mediaEvent0 = mockery.mock(SdpPortManagerEvent.class, "mediaEvent0");
    final byte[] sdpOffer = new byte[10];
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          allowing(mediaEvent0).getMediaServerSdp();
          will(returnValue(sdpOffer));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    mockery.checking(new Expectations() {
      {
        oneOf(inviteReq).createResponse(SipServletResponse.SC_SESSION_PROGRESS);
        will(returnValue(inviteResp));

        oneOf(inviteResp).setContent(with(same(sdpOffer)), with(any(String.class)));

        oneOf(inviteResp).sendReliably();

        oneOf(msFactory).createMediaSession();
        will(returnValue(mediaSession));

        oneOf(mediaSession).createNetworkConnection(with(equal(NetworkConnection.BASIC)), with(any(Parameters.class)));
        will(returnValue(network));

        allowing(network).getSdpPortManager();
        will(returnValue(sdpManager));

        oneOf(sdpManager).processSdpOffer(requestSDP);
        will(new Action() {
          @Override
          public void describeTo(Description description) {
          }

          @Override
          public Object invoke(Invocation invocation) throws Throwable {
            Thread th = new Thread(new Runnable() {
              @Override
              public void run() {
                Object[] ls = sdpManager.listeners.toArray();
                for (Object listerner : ls) {
                  ((MediaEventListener<SdpPortManagerEvent>) listerner).onEvent(mediaEvent0);
                }
              }
            });
            th.start();

            return null;
          }

        });
      }
    });

    // execute
    SIPInviteEventImpl invite = new SIPInviteEventImpl(appContext, inviteReq);
    Call call = invite.acceptCallWithEarlyMedia(ob);

    // assert
    assertTrue(call.getCallState() == State.INPROGRESS);
    mockery.assertIsSatisfied();
  }

}
