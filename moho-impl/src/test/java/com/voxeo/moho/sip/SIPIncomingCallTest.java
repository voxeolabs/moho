/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.sip;

import java.util.concurrent.Future;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.SdpFactory;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.ExceptionHandler;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.DisconnectEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.EventState;
import com.voxeo.moho.media.fake.MockParameters;
import com.voxeo.moho.sip.fake.MockServletContext;
import com.voxeo.moho.sip.fake.MockSipServletRequest;
import com.voxeo.moho.sip.fake.MockSipServletResponse;
import com.voxeo.moho.sip.fake.MockSipSession;
import com.voxeo.utils.Event;

public class SIPIncomingCallTest extends TestCase {

  Mockery mockery = new Mockery() {
    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  // JSR309 mock
  MsControlFactory msFactory = mockery.mock(MsControlFactory.class);

  MediaSession mediaSession = mockery.mock(MediaSession.class);

  NetworkConnection network = mockery.mock(NetworkConnection.class);

  SdpPortManager sdpManager = mockery.mock(SdpPortManager.class);

  // JSR289 mock
  SipFactory sipFactory = mockery.mock(SipFactory.class);

  SdpFactory sdpFactory = mockery.mock(SdpFactory.class);

  SipApplicationSession appSession = mockery.mock(SipApplicationSession.class);

  MockSipSession session = mockery.mock(MockSipSession.class);

  MockSipServletRequest initInviteReq = mockery.mock(MockSipServletRequest.class);

  MockServletContext servletContext = mockery.mock(MockServletContext.class);

  // Moho
  TestApp app;

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = new ApplicationContextImpl(app, msFactory, sipFactory, sdpFactory, "test", null, 2);

  Address fromAddr = mockery.mock(Address.class, "fromAddr");

  Address toAddr = mockery.mock(Address.class, "toAddr");

  boolean invoked;

  // testing object
  private SIPIncomingCall sipcall;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    initInviteReq.setSession(session);
    initInviteReq.setMethod("INVITE");
    initInviteReq.setIsInitial(true);
    session.setServletContext(servletContext);

    // common Expectations.
    mockery.checking(new Expectations() {
      {
        allowing(session).getRemoteParty();
        will(returnValue(fromAddr));

        allowing(fromAddr).clone();
        will(returnValue(fromAddr));

        allowing(toAddr).clone();
        will(returnValue(toAddr));

        allowing(initInviteReq).getFrom();
        will(returnValue(fromAddr));

        oneOf(initInviteReq).getTo();
        will(returnValue(toAddr));

        allowing(session).getApplicationSession();
        will(returnValue(appSession));

        allowing(network).getSdpPortManager();
        will(returnValue(sdpManager));

        allowing(mediaSession).createParameters();
        will(returnValue(new MockParameters()));

        allowing(mediaSession).setParameters(with(any(MockParameters.class)));

        allowing(session).getCallId();
        will(returnValue("test"));
      }
    });

    app = new TestApp();
    invoked = false;
  }

  /**
   * test addObserver() and dispatch event. Supervised
   */
  public void testAddObserverAndDispatchEventUnderSupervised() {

    sipcall = new SIPIncomingCall(appContext, initInviteReq);
    // prepare
    final DisconnectEvent disconnectEvent = mockery.mock(DisconnectEvent.class);

    sipcall.setSupervised(true);
    mockery.checking(new Expectations() {
      {
        // TODO
        // oneOf(app).handleDisconnect(disconnectEvent);

        oneOf(disconnectEvent).accept();

        oneOf(disconnectEvent).getState();
        will(returnValue(EventState.InitialEventState.INITIAL));
      }
    });

    // execute test.

    sipcall.addObserver(app);
    final Future<DisconnectEvent> future = sipcall.dispatch(disconnectEvent);

    // verify result
    assert future != null;
    try {
      future.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    assertTrue(invoked);
    mockery.assertIsSatisfied();
  }

  /**
   * test addObserver() and dispatch event. Supervised
   */
  public void testExceptionHandler() {

    sipcall = new SIPIncomingCall(appContext, initInviteReq);
    final MyExceptionHandler handler = new MyExceptionHandler();
    sipcall.addExceptionHandler(handler);
    // prepare
    final DisconnectEvent disconnectEvent = mockery.mock(DisconnectEvent.class);

    sipcall.setSupervised(true);
    mockery.checking(new Expectations() {
      {

        oneOf(disconnectEvent).accept();

        oneOf(disconnectEvent).getState();
        will(returnValue(EventState.InitialEventState.INITIAL));
      }
    });

    // execute test.

    sipcall.addObserver(new ThrowExceptionApp());
    final Future<DisconnectEvent> future = sipcall.dispatch(disconnectEvent);

    // verify result
    assert future != null;
    try {
      future.get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    assertTrue(handler.ex.getMessage().equalsIgnoreCase("test exception"));
    mockery.assertIsSatisfied();
  }

  class MyExceptionHandler implements ExceptionHandler {
    Exception ex;

    @Override
    public boolean handle(final Exception ex, final Event<? extends EventSource> event) {
      this.ex = ex;
      return false;
    }
  }

  class ThrowExceptionApp implements Application {
    @State
    public void handleDisconnect(final DisconnectEvent event) throws Exception {
      throw new Exception("test exception");
    }

    @Override
    public void destroy() {

    }

    @Override
    public void init(final ApplicationContext ctx) {

    }
  }

  class TestApp implements Application {
    @State
    public void handleDisconnect(final DisconnectEvent event) {
      invoked = true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void init(final ApplicationContext ctx) {

    }
  }

  /**
   * test SIPIncomingCall.join(final Joinable.Direction direction) .
   */
  public void testJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoin");

    // execute
    try {
      sipcall.join(Joinable.Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    mockery.assertIsSatisfied();
  }

  /**
   * SIPIncomingCall.join(final Joinable.Direction direction). Expectations.
   * 
   * @param mockObjectNamePrefix
   */
  private void joinToMSExpectations(final String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final byte[] reqSDP = new byte[10];
    initInviteReq.setRawContent(reqSDP);

    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    final byte[] msRespSDP = new byte[10];

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEvent");

    // invoke join()
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession)
              .createNetworkConnection(with(equal(NetworkConnection.BASIC)), with(any(Parameters.class)));
          will(returnValue(network));

          oneOf(sdpManager).addListener(sipcall);

          oneOf(sdpManager).processSdpOffer(reqSDP);
          will(new MockMediaServerSdpPortManagerEventAction(sdpPortManagerEvent));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // process jsr309 SdpPortManagerEvent.
    try {
      mockery.checking(new Expectations() {
        {
          allowing(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.ANSWER_GENERATED));

          allowing(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msRespSDP));

          oneOf(initInviteReq).createResponse(200);
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              initInviteReq.setResponse(sipInviteResp);
              sipInviteResp.setRequest(initInviteReq);
              return sipInviteResp;
            }
          });

          oneOf(sipInviteResp).setContent(msRespSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * test SIPIncomingCall.join(Joinable.Direction direction). The sipservlet
   * invite request haven't sdp.
   */
  public void testJoinRequestNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testJoinRequestNoSDP");

    // execute
    try {
      sipcall.join(Joinable.Direction.DUPLEX).get();

    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    mockery.assertIsSatisfied();
  }

  /**
   * SIPIncomingCall.join(Joinable.Direction direction). Expectations. Initial
   * invite request haven't SDP.
   * 
   * @param mockObjectNamePrefix
   */
  private void joinToMSExpectationsInitRequestNoSDP(final String mockObjectNamePrefix) {
    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    final byte[] msRespSDP = new byte[10];

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    final byte[] ackSDP = new byte[10];
    sipInviteAck.setRawContent(ackSDP);
    sipInviteAck.setMethod("ACK");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEvent");

    // invoke join()
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession)
              .createNetworkConnection(with(equal(NetworkConnection.BASIC)), with(any(Parameters.class)));
          will(returnValue(network));

          oneOf(sdpManager).addListener(sipcall);

          oneOf(sdpManager).generateSdpOffer();
          will(new MockMediaServerSdpPortManagerEventAction(sdpPortManagerEvent));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process jsr309 SdpPortManagerEvent.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msRespSDP));

          oneOf(initInviteReq).createResponse(200);
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              initInviteReq.setResponse(sipInviteResp);
              sipInviteResp.setRequest(initInviteReq);
              return sipInviteResp;
            }
          });

          oneOf(sipInviteResp).setContent(msRespSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process jsr289 ack.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).processSdpAnswer(ackSDP);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * test join(Joinable.Direction direction). join(Joinable.Direction
   * direction).
   */
  public void testJoinAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinAfterJoin");

    serverReinviteFromMSExpectations("testJoinAfterJoin2");

    // execute
    try {
      sipcall.join(Joinable.Direction.DUPLEX).get();

      sipcall.join(Joinable.Direction.RECV).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void serverReinviteFromMSExpectations(final String mockObjectNamePrefix) {

    // mock jsr289 object.
    final MockSipServletRequest reInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "reInviteReq");
    reInviteReq.setMethod("INVITE");
    reInviteReq.setIsInitial(false);

    final byte[] msReinviteReqSDP = new byte[10];

    final MockSipServletResponse reInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "reInviteResp");
    reInviteReq.setResponse(reInviteResp);
    reInviteResp.setRequest(reInviteReq);
    reInviteResp.setStatus(200);

    final byte[] reinviteRespSDP = new byte[10];
    reInviteResp.setRawContent(reinviteRespSDP);

    final MockSipServletRequest reInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "reInviteAck");
    reInviteAck.setMethod("ACK");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEvent");

    // invoke join().
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).generateSdpOffer();
          will(new MockMediaServerSdpPortManagerEventAction(sdpPortManagerEvent));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process jsr309 sdp event.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(session).createRequest("INVITE");
          will(returnValue(reInviteReq));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReinviteReqSDP));

          oneOf(reInviteReq).setContent(msReinviteReqSDP, "application/sdp");

          oneOf(reInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(reInviteResp, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process jsr289 response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).processSdpAnswer(reinviteRespSDP);

          oneOf(reInviteResp).createAck();
          will(returnValue(reInviteAck));

          oneOf(reInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }
  }

  // ===================join outgoingcall bridge
  /**
   * test SIPIncomingCall.joinToOutgoingCall() . JoinType BRIDGE.
   */
  public void testJoinOutgoingCallBridgeInitReqNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testJoinOutgoingCallBridge1");

    final SIPOutgoingCall outgoingCall = joinOutgoingCallBridgeExpectations("testJoinOutgoingCallBridge2");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();

    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      // fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  private SIPOutgoingCall joinOutgoingCallBridgeExpectations(final String mockObjectNamePrefix) {
    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, mockObjectNamePrefix
        + "outgoingCallNetwork");

    final States outgoingCallStates = mockery.states("outgoingCallStates");
    outgoingCallStates.become("outgoingCallInit");

    // outgoingCall join().
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.INVITING));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(outgoingCallStates.is("resped"));

          oneOf(outgoingCall).joinWithoutCheckOperation(Direction.DUPLEX);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              outgoingCallStates.become("resped");
              return null;
            }

          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).join(Direction.DUPLEX, outgoingCallNetwork);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testJoinOutgoingCallBridgeAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinOutgoingCallBridgeAfterJoinMS");

    final SIPOutgoingCall outgoingCall = joinOutgoingCallBridgeExpectations("testJoinOutgoingCallBridgeAfterJoinMS3");

    // execute
    try {
      sipcall.join().get();

      sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() != null);
    assertTrue(sipcall.getPeers().length == 1);
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  public void testJoinAnsweredOutgoingCallBridge() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testJoinOutgoingCallBridge1");

    final SIPOutgoingCall outgoingCall = joinAnsweredOutgoingCallBridgeExpectations("testJoinOutgoingCallBridge2");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() != null);
    assertTrue(sipcall.getPeers() != null);
    mockery.assertIsSatisfied();
  }

  private SIPOutgoingCall joinAnsweredOutgoingCallBridgeExpectations(final String mockObjectNamePrefix) {
    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, mockObjectNamePrefix
        + "outgoingCallNetwork");

    final States outgoingCallStates = mockery.states("outgoingCall");
    outgoingCallStates.become("outgoingCallAnswered");

    // outgoingCall join().
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(outgoingCallStates.is("outgoingCallAnswered"));
          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(outgoingCallStates.is("rejoined"));

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(outgoingCallStates.is("outgoingCallAnswered"));
          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(outgoingCallStates.is("rejoined"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).isDirectlyJoined();
          will(returnValue(true));

          allowing(outgoingCall).unlinkDirectlyPeer();

          oneOf(outgoingCall).joinWithoutCheckOperation(Direction.DUPLEX);
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              outgoingCallStates.become("rejoined");
              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).join(Direction.DUPLEX, outgoingCallNetwork);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testAnsweredJoinOutgoingCallBridgeAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinOutgoingCallBridgeAfterJoinMS");

    final SIPOutgoingCall outgoingCall = joinAnsweredOutgoingCallBridgeExpectations("testJoinOutgoingCallBridgeAfterJoinMS3");

    // execute
    try {
      sipcall.join().get();

      sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  // ==============join incomingcall bridge
  /**
   * test SIPIncomingCall.joinToOutgoingCall() . JoinType BRIDGE.
   */
  public void testJoinIncomingCallBridgeReqNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testJoinIncomingCallBridge1");

    final SIPIncomingCall incomingCall = joinIncomingCallBridgeExpectations("testJoinIncomingCallBridge2");

    // execute
    try {
      sipcall.join(incomingCall, JoinType.BRIDGE, Direction.DUPLEX).get();

    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == incomingCall);
    mockery.assertIsSatisfied();
  }

  private SIPIncomingCall joinIncomingCallBridgeExpectations(final String mockObjectNamePrefix) {
    // mock moho SIPOutgoingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");
    final NetworkConnection incomingCallNetwork = mockery.mock(NetworkConnection.class, mockObjectNamePrefix
        + "incomingCallNetwork");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");

    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(incomingCall).getSIPCallState();
          will(returnValue(SIPCall.State.INVITING));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(incomingCallStates.is("resped"));

          allowing(incomingCall).getMediaObject();
          will(returnValue(null));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).getMediaObject();
          will(returnValue(incomingCallNetwork));
          when(incomingCallStates.is("resped"));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          oneOf(incomingCall).joinWithoutCheckOperation(Direction.DUPLEX);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              incomingCallStates.become("resped");
              return null;
            }

          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {

          oneOf(network).join(Direction.DUPLEX, incomingCallNetwork);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return incomingCall;
  }

  /**
   * test SIPIncomingCall.joinToOutgoingCall() . JoinType BRIDGE.
   */
  public void testJoinAnsweredIncomingCallBridgeReqNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testJoinIncomingCallBridge1");

    final SIPIncomingCall incomingCall = joinAnsweredIncomingCallBridgeExpectations("testJoinIncomingCallBridge2");

    // execute
    try {
      sipcall.join(incomingCall, JoinType.BRIDGE, Direction.DUPLEX).get();

    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == incomingCall);
    mockery.assertIsSatisfied();
  }

  private SIPIncomingCall joinAnsweredIncomingCallBridgeExpectations(final String mockObjectNamePrefix) {
    // mock moho SIPOutgoingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");
    final NetworkConnection incomingCallNetwork = mockery.mock(NetworkConnection.class, mockObjectNamePrefix
        + "incomingCallNetwork");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallAnswered");

    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallAnswered"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallAnswered"));

          allowing(incomingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(incomingCallStates.is("incomingCallAnswered"));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          allowing(incomingCall).isDirectlyJoined();
          will(returnValue(false));

          allowing(incomingCall).isBridgeJoined();
          will(returnValue(true));

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getMediaObject();
          will(returnValue(incomingCallNetwork));

          oneOf(network).join(Direction.DUPLEX, incomingCallNetwork);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return incomingCall;
  }

  /**
   * 
   */
  public void testJoinIncomingCallBridgeAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinIncomingCallBridgeAfterJoin");

    final SIPIncomingCall incomingCall = joinIncomingCallBridgeExpectations("testJoinIncomingCallBridgeAfterJoin3");

    // execute
    try {
      sipcall.join().get();

      assertTrue(sipcall.getMediaObject() == network);

      sipcall.join(incomingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == incomingCall);
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  public void testJoinAnsweredIncomingCallBridgeAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinIncomingCallBridgeAfterJoin");

    final SIPIncomingCall incomingCall = joinAnsweredIncomingCallBridgeExpectations("testJoinIncomingCallBridgeAfterJoin3");

    // execute
    try {
      sipcall.join().get();

      sipcall.join(incomingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == network);
    assertTrue(sipcall.getPeers()[0] == incomingCall);
    mockery.assertIsSatisfied();
  }

  // ================join outgoingcall direct
  /**
   * test SIPIncomingCall.joinToOutgoingCall() . JoinType DIRECT. init req
   * haven't sdp.
   */
  public void testJoinOutgoingCallDirectInitReqNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    final SIPOutgoingCall outgoingCall = joinOutgoingCallDirectInitReqNoSDPExpectations("testJoinOutgoingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  /**
   * init req haven't sdp.
   * 
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPOutgoingCall joinOutgoingCallDirectInitReqNoSDPExpectations(final String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    sipInviteResp.setRequest(initInviteReq);
    initInviteReq.setResponse(sipInviteResp);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    final byte[] ackSDP = new byte[10];
    sipInviteAck.setRawContent(ackSDP);
    sipInviteAck.setContentType("application/sdp");

    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final MockSipServletResponse outgoingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "outgoingCallInviteResp");
    outgoingCallInviteResp.setStatus(200);
    outgoingCallInviteResp.setReasonPhrase("OK");

    final byte[] outgoingCallRespSDP = new byte[10];
    outgoingCallInviteResp.setRawContent(outgoingCallRespSDP);
    outgoingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest outgoingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteAck");
    outgoingCallInviteAck.setMethod("ACK");

    final States outgoingCallStates = mockery.states("outgoingCall");
    outgoingCallStates.become("outgoingCallInit");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          oneOf(outgoingCall).call(null, appSession, null);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }

                }
              });
              th.start();
              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipInviteResp.setRequest(initInviteReq);
              initInviteReq.setResponse(sipInviteResp);
              return sipInviteResp;
            }
          });
          oneOf(sipInviteResp).setContent(outgoingCallRespSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall ACK.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).setSIPCallState(SIPCall.State.ANSWERED);
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              outgoingCallStates.become("resped");
              return null;
            }
          });

          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).setContent(ackSDP, "application/sdp");

          oneOf(outgoingCallInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testJoinAnsweredOutgoingCallDirectInitReqNoSDP() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    final SIPOutgoingCall outgoingCall = joinAnsweredOutgoingCallDirectInitReqNoSDPExpectations("testJoinAnsweredOutgoingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  /**
   * init req haven't sdp.
   * 
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPOutgoingCall joinAnsweredOutgoingCallDirectInitReqNoSDPExpectations(final String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    final byte[] ackSDP = new byte[10];
    sipInviteAck.setRawContent(ackSDP);
    sipInviteAck.setContentType("application/sdp");

    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final MockSipServletResponse outgoingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "outgoingCallInviteResp");
    outgoingCallInviteResp.setStatus(200);
    outgoingCallInviteResp.setReasonPhrase("OK");

    final byte[] outgoingCallRespSDP = new byte[10];
    outgoingCallInviteResp.setRawContent(outgoingCallRespSDP);
    outgoingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest outgoingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteAck");
    outgoingCallInviteAck.setMethod("ACK");

    final States outgoingCallStates = mockery.states("outgoingCall");
    // outgoingCallStates.become("outgoingCallInit");
    outgoingCallStates.become("resped");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).isDirectlyJoined();
          will(returnValue(false));

          allowing(outgoingCall).isBridgeJoined();
          will(returnValue(true));

          allowing(outgoingCall).getParticipants();
          will(returnValue(new Participant[0]));

          oneOf(outgoingCall).destroyNetworkConnection();

          oneOf(outgoingCall).call(null);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();
              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipInviteResp.setRequest(initInviteReq);
              initInviteReq.setResponse(sipInviteResp);
              return sipInviteResp;
            }
          });
          oneOf(sipInviteResp).setContent(outgoingCallRespSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall ACK.
    try {
      mockery.checking(new Expectations() {
        {
          // removed
          // oneOf(outgoingCall).setSIPCallState(State.ANSWERED);

          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).setContent(ackSDP, "application/sdp");

          oneOf(outgoingCallInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * test SIPIncomingCall.joinToOutgoingCall() . JoinType DIRECT.
   */
  public void testJoinOutgoingCallDirectAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinOutgoingCallDirectAfterJoin");

    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).release();
          oneOf(mediaSession).release();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // mock jsr289 object.
    final MockSipServletRequest reInviteReq = mockery.mock(MockSipServletRequest.class, "reInviteReq");
    reInviteReq.setMethod("INVITE");
    reInviteReq.setIsInitial(false);

    final MockSipServletResponse reInviteResp = mockery.mock(MockSipServletResponse.class, "reInviteResp");
    reInviteReq.setResponse(reInviteResp);
    reInviteResp.setRequest(reInviteReq);
    reInviteResp.setStatus(200);

    final byte[] reinviteRespSDP = new byte[10];
    reInviteResp.setRawContent(reinviteRespSDP);
    reInviteResp.setContentType("application/sdp");

    final MockSipServletRequest reInviteAck = mockery.mock(MockSipServletRequest.class, "reInviteAck");
    reInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");

    // mock outgoingCall jsr289 object.
    final MockSipServletResponse outgoingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        "outgoingCallInviteResp");
    final byte[] outgoingCallInviteRespSDP = new byte[10];
    outgoingCallInviteResp.setRawContent(outgoingCallInviteRespSDP);
    outgoingCallInviteResp.setStatus(200);
    outgoingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest outgoingCallInviteAck = mockery.mock(MockSipServletRequest.class,
        "outgoingCallInviteAck");
    outgoingCallInviteAck.setMethod("ACK");

    final States outgoingCallStates = mockery.states("outgoingCall");
    outgoingCallStates.become("outgoingCallInit");
    // outgoingCall unjoin() and call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          oneOf(outgoingCall).call(null, appSession);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                  outgoingCallStates.become("resped");
                }
              });
              th.start();

              return null;
            }

          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(session).createRequest("INVITE");
          will(returnValue(reInviteReq));

          oneOf(reInviteReq).setContent(outgoingCallInviteRespSDP, "application/sdp");

          oneOf(reInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(reInviteResp, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).setSIPCallState(SIPCall.State.ANSWERED);

          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).setContent(reinviteRespSDP, "application/sdp");

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.INVITING));

          oneOf(reInviteResp).createAck();
          will(returnValue(reInviteAck));

          oneOf(reInviteAck).send();

          oneOf(outgoingCallInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    try {
      sipcall.join().get();

      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  /**
   * JoinType DIRECT.
   */
  public void testJoinAnsweredOutgoingCallDirectAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoinOutgoingCallDirectAfterJoin");

    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).release();
          oneOf(mediaSession).release();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // mock jsr289 object.
    final MockSipServletRequest reInviteReq = mockery.mock(MockSipServletRequest.class, "reInviteReq");
    reInviteReq.setMethod("INVITE");
    reInviteReq.setIsInitial(false);

    final MockSipServletResponse reInviteResp = mockery.mock(MockSipServletResponse.class, "reInviteResp");
    reInviteReq.setResponse(reInviteResp);
    reInviteResp.setRequest(reInviteReq);
    reInviteResp.setStatus(200);

    final byte[] reinviteRespSDP = new byte[10];
    reInviteResp.setRawContent(reinviteRespSDP);
    reInviteResp.setContentType("application/sdp");

    final MockSipServletRequest reInviteAck = mockery.mock(MockSipServletRequest.class, "reInviteAck");
    reInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");

    // mock outgoingCall jsr289 object.
    final MockSipServletResponse outgoingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        "outgoingCallInviteResp");
    final byte[] outgoingCallInviteRespSDP = new byte[10];
    outgoingCallInviteResp.setRawContent(outgoingCallInviteRespSDP);
    outgoingCallInviteResp.setStatus(200);
    outgoingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest outgoingCallInviteAck = mockery.mock(MockSipServletRequest.class,
        "outgoingCallInviteAck");
    outgoingCallInviteAck.setMethod("ACK");

    final States outgoingCallStates = mockery.states("outgoingCall");
    // outgoingCallStates.become("outgoingCallInit");
    outgoingCallStates.become("resped");
    // outgoingCall unjoin() and call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).isDirectlyJoined();
          will(returnValue(true));

          oneOf(outgoingCall).unlinkDirectlyPeer();

          oneOf(outgoingCall).call(null);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                  // remove
                  // outgoingCallStates.become("resped");
                }
              });
              th.start();

              return null;
            }

          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(session).createRequest("INVITE");
          will(returnValue(reInviteReq));

          oneOf(reInviteReq).setContent(outgoingCallInviteRespSDP, "application/sdp");

          oneOf(reInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(reInviteResp, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall response
    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(outgoingCall).setSIPCallState(SIPCall.State.ANSWERED);

          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).setContent(reinviteRespSDP, "application/sdp");

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.INVITING));

          oneOf(reInviteResp).createAck();
          will(returnValue(reInviteAck));

          oneOf(reInviteAck).send();

          oneOf(outgoingCallInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    try {
      sipcall.join().get();

      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  // ================join incomingcall direct
  /**
   * 
   */
  public void testJoinincomingCallDirect() {

    final SIPIncomingCall incomingCall = joinincomingCallDirectExpectations("testJoinincomingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join(incomingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == incomingCall);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPIncomingCall joinincomingCallDirectExpectations(final String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final byte[] inviteReqSDP = new byte[10];
    initInviteReq.setRawContent(inviteReqSDP);
    initInviteReq.setContentType("application/sdp");

    //
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    // mock moho SIPincomingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");

    final MockSipServletRequest incomingCallInvite = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInvite");
    incomingCallInvite.setMethod("INVITE");
    incomingCallInvite.setIsInitial(true);

    final byte[] incomingCallInviteReqSDP = new byte[10];
    incomingCallInvite.setRawContent(incomingCallInviteReqSDP);
    incomingCallInvite.setContentType("application/sdp");

    final MockSipServletResponse incomingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "incomingCallInviteResp");
    incomingCallInviteResp.setStatus(200);
    incomingCallInviteResp.setReasonPhrase("OK");

    final MockSipServletRequest incomingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInviteAck");
    incomingCallInviteAck.setMethod("ACK");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          allowing(incomingCall).getRemoteSdp();
          will(returnValue(incomingCallInviteReqSDP));

          allowing(incomingCall).getSipInitnalRequest();
          will(returnValue(incomingCallInvite));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // create response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipInviteResp.setRequest(initInviteReq);
              initInviteReq.setResponse(sipInviteResp);
              return sipInviteResp;
            }
          });
          oneOf(sipInviteResp).setContent(incomingCallInviteReqSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));

          // incomingcall create response
          oneOf(incomingCallInvite).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              incomingCallInviteResp.setRequest(incomingCallInvite);
              incomingCallInvite.setResponse(incomingCallInviteResp);
              return incomingCallInviteResp;
            }
          });
          oneOf(incomingCallInviteResp).setContent(incomingCallInviteReqSDP, "application/sdp");

          oneOf(incomingCallInviteResp).send();
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              incomingCallStates.become("resped");
              return null;
            }

          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall ACK.
    // try {
    // mockery.checking(new Expectations() {
    // {
    // oneOf(incomingCall).setSIPCallState(State.ANSWERED);
    // }
    // });
    // }
    // catch (Exception ex) {
    // ex.printStackTrace();
    // }

    return incomingCall;
  }

  /**
   * 
   */
  public void testJoinincomingCallDirectAfterJoin() {

    final SIPIncomingCall incomingCall = joinincomingCallDirectAfterJoinExpectations("testJoinincomingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join().get();
      sipcall.join(incomingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);

    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPIncomingCall joinincomingCallDirectAfterJoinExpectations(final String mockObjectNamePrefix) {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    // prepare
    joinToMSExpectations(mockObjectNamePrefix);

    final MockSipServletRequest sipReInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteReq");
    sipReInviteReq.setMethod("INVITE");
    sipReInviteReq.setIsInitial(false);

    final MockSipServletResponse sipReInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipReInviteResp");
    sipReInviteResp.setStatus(200);
    sipReInviteResp.setRequest(sipReInviteReq);
    sipReInviteReq.setResponse(sipReInviteResp);

    final byte[] sipReInviteRespSDP = new byte[10];
    sipReInviteResp.setContentType("application/sdp");
    sipReInviteResp.setRawContent(sipReInviteRespSDP);

    final MockSipServletRequest sipReInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteAck");
    sipReInviteAck.setMethod("ACK");

    // mock moho SIPincomingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");

    final MockSipServletRequest incomingCallInvite = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInvite");
    incomingCallInvite.setMethod("INVITE");
    incomingCallInvite.setIsInitial(true);

    final MockSipServletResponse incomingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "incomingCallInviteResp");
    incomingCallInviteResp.setStatus(200);
    incomingCallInviteResp.setReasonPhrase("OK");

    final MockSipServletRequest incomingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInviteAck");
    incomingCallInviteAck.setMethod("ACK");

    final byte[] incomingCallInviteAckSDP = new byte[10];
    incomingCallInviteAck.setRawContent(incomingCallInviteAckSDP);
    incomingCallInviteAck.setContentType("application/sdp");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          allowing(incomingCall).getRemoteSdp();
          will(returnValue(null));

          allowing(incomingCall).getSipInitnalRequest();
          will(returnValue(incomingCallInvite));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // unjoin
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).release();
          oneOf(mediaSession).release();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).linkCall(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // reinvite sipcall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(session).createRequest("INVITE");
          will(returnValue(sipReInviteReq));

          oneOf(sipReInviteReq).send();
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(sipReInviteResp, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response.
    try {
      mockery.checking(new Expectations() {
        {
          // incomingcall create response
          oneOf(incomingCallInvite).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              incomingCallInviteResp.setRequest(incomingCallInvite);
              incomingCallInvite.setResponse(incomingCallInviteResp);
              return incomingCallInviteResp;
            }
          });
          oneOf(incomingCallInviteResp).setContent(sipReInviteRespSDP, "application/sdp");

          oneOf(incomingCallInviteResp).send();
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipcall.getJoinDelegate().doAck(incomingCallInviteAck, incomingCall);
              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall ACK.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).setSIPCallState(SIPCall.State.ANSWERED);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              incomingCallStates.become("resped");
              return null;
            }
          });

          oneOf(sipReInviteResp).createAck();
          will(returnValue(sipReInviteAck));

          oneOf(sipReInviteAck).setContent(incomingCallInviteAckSDP, "application/sdp");

          oneOf(sipReInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return incomingCall;
  }

  /**
   * 
   */
  public void testJoinAnsweredincomingCallDirectAfterJoin() {

    final SIPIncomingCall incomingCall = joinAnsweredincomingCallDirectAfterJoinExpectations("testJoinincomingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join().get();

      sipcall.join(incomingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);

    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPIncomingCall joinAnsweredincomingCallDirectAfterJoinExpectations(final String mockObjectNamePrefix) {
    final byte[] reqSDP = new byte[10];
    initInviteReq.setRawContent(reqSDP);

    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    // prepare
    joinToMSExpectations(mockObjectNamePrefix);

    final MockSipServletRequest sipReInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteReq");
    sipReInviteReq.setMethod("INVITE");
    sipReInviteReq.setIsInitial(false);

    final MockSipServletResponse sipReInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipReInviteResp");
    sipReInviteResp.setStatus(200);
    sipReInviteResp.setRequest(sipReInviteReq);
    sipReInviteReq.setResponse(sipReInviteResp);

    final byte[] sipReInviteRespSDP = new byte[10];
    sipReInviteResp.setContentType("application/sdp");
    sipReInviteResp.setRawContent(sipReInviteRespSDP);

    final MockSipServletRequest sipReInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteAck");
    sipReInviteAck.setMethod("ACK");

    // mock moho SIPincomingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");

    final MockSipSession incomingCallSession = mockery.mock(MockSipSession.class, "incomingCallSession");

    final MockSipServletRequest incomingCallInvite = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInvite");
    incomingCallInvite.setMethod("INVITE");
    incomingCallInvite.setIsInitial(true);

    final MockSipServletResponse incomingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "incomingCallInviteResp");
    incomingCallInviteResp.setStatus(200);
    incomingCallInviteResp.setReasonPhrase("OK");

    final byte[] incomingCallInviteRespSDP = new byte[10];
    incomingCallInviteResp.setRawContent(incomingCallInviteRespSDP);
    incomingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest incomingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInviteAck");
    incomingCallInviteAck.setMethod("ACK");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("resped");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(incomingCall).getSipSession();
          will(returnValue(incomingCallSession));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(incomingCall).isDirectlyJoined();
          will(returnValue(true));

          allowing(incomingCall).unlinkDirectlyPeer();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // unjoin
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).release();
          oneOf(mediaSession).release();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // reinvite incomingcall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCallSession).createRequest("INVITE");
          will(returnValue(incomingCallInvite));

          oneOf(incomingCallInvite).setContent(reqSDP, "application/sdp");

          oneOf(incomingCallInvite).send();
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(incomingCallInviteResp, incomingCall, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(session).createRequest("INVITE");
          will(returnValue(sipReInviteReq));

          oneOf(sipReInviteReq).setContent(incomingCallInviteRespSDP, "application/sdp");

          oneOf(sipReInviteReq).send();
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              final Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    final SIPSuccessEventImpl respEvent = new SIPSuccessEventImpl(sipcall, sipReInviteResp);
                    respEvent.accept();
                    // sipcall.doResponse(sipReInviteResp, null);
                  }
                  catch (final Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                  }
                }
              });
              th.start();

              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response.
    try {
      mockery.checking(new Expectations() {
        {
          // incomingcall create response
          oneOf(incomingCallInviteResp).createAck();
          will(returnValue(incomingCallInviteAck));

          oneOf(incomingCallInviteAck).send();

          oneOf(sipReInviteResp).createAck();
          will(returnValue(sipReInviteAck));

          oneOf(sipReInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return incomingCall;
  }

  /**
   * 
   */
  public void testJoinAnsweredincomingCallDirect() {

    final SIPIncomingCall incomingCall = joinAnsweredincomingCallDirectInitExpectations("testJoinincomingCallDirectInitReqNoSDP");

    // execute
    try {
      sipcall.join(incomingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers() != null);
    mockery.assertIsSatisfied();
  }

  /**
   * init req haven't sdp.
   * 
   * @param mockObjectNamePrefix
   * @return
   */
  private SIPIncomingCall joinAnsweredincomingCallDirectInitExpectations(final String mockObjectNamePrefix) {
    //
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    final byte[] sipInviteAckSDP = new byte[10];
    sipInviteAck.setRawContent(sipInviteAckSDP);
    sipInviteAck.setContentType("application/sdp");

    // mock moho SIPincomingCall
    final SIPIncomingCall incomingCall = mockery.mock(SIPIncomingCall.class, mockObjectNamePrefix + "incomingCall");

    final MockSipSession incomingCallSession = mockery.mock(MockSipSession.class, "incomingCallSession");

    final MockSipServletRequest incomingCallInvite = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInvite");
    incomingCallInvite.setMethod("INVITE");
    incomingCallInvite.setIsInitial(true);
    incomingCallInvite.setSession(incomingCallSession);

    final MockSipServletResponse incomingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "incomingCallInviteResp");
    incomingCallInviteResp.setStatus(200);
    incomingCallInviteResp.setReasonPhrase("OK");

    final byte[] incomingCallInviteRespSDP = new byte[10];
    incomingCallInviteResp.setRawContent(incomingCallInviteRespSDP);
    incomingCallInviteResp.setContentType("application/sdp");

    final MockSipServletRequest incomingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "incomingCallInviteAck");
    incomingCallInviteAck.setMethod("ACK");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("resped");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(incomingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(incomingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(incomingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(incomingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(incomingCall).isTerminated();
          will(returnValue(false));

          allowing(incomingCall).getSipSession();
          will(returnValue(incomingCallSession));

          allowing(incomingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(incomingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(incomingCall).isDirectlyJoined();
          will(returnValue(true));

          allowing(incomingCall).unlinkDirectlyPeer();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // incomingcall reinvite.
    try {
      mockery.checking(new Expectations() {
        {
          // incomingcall create response
          oneOf(incomingCallSession).createRequest("INVITE");
          will(returnValue(incomingCallInvite));
          // oneOf(incomingCallInvite).setContent(null, "application/sdp");

          oneOf(incomingCallInvite).send();
          will(new Action() {

            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipcall.getJoinDelegate().doInviteResponse(incomingCallInviteResp, incomingCall, null);
              return null;
            }
          });

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process incomingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              sipInviteResp.setRequest(initInviteReq);
              initInviteReq.setResponse(sipInviteResp);
              return sipInviteResp;
            }
          });
          oneOf(sipInviteResp).setContent(incomingCallInviteRespSDP, "application/sdp");

          oneOf(sipInviteResp).send();
          will(new MockClientDoAckAction(sipInviteAck, sipcall));

        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall ACK.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(incomingCallInviteResp).createAck();
          will(returnValue(incomingCallInviteAck));

          oneOf(incomingCallInviteAck).setContent(sipInviteAckSDP, "application/sdp");

          oneOf(incomingCallInviteAck).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    return incomingCall;
  }

  // ====================subsequent request
  /**
   * test disconnect.
   */
  public void testDisconnectAfterJoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testJoin");

    // prepare
    // mock jsr289 object. final
    final MockSipServletRequest byeReq = mockery.mock(MockSipServletRequest.class, "byeReq");
    byeReq.setMethod("BYE");

    final MockSipServletResponse byeResp = mockery.mock(MockSipServletResponse.class, "byeResp");

    try {
      mockery.checking(new Expectations() {
        {
          oneOf(byeReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              byeReq.setResponse(byeResp);
              byeResp.setRequest(byeReq);
              byeResp.setStatus(200);
              return byeResp;
            }
          });

          oneOf(byeResp).send();
        }
      });

      mockery.checking(new Expectations() {
        {
          oneOf(network).release();
          oneOf(mediaSession).release();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    try {
      sipcall.join().get();
      assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
      sipcall.setSupervised(true);
      sipcall.dispatch(new SIPDisconnectEventImpl(sipcall, byeReq)).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.DISCONNECTED);
    assertTrue(sipcall.getMediaObject() == null);
    mockery.assertIsSatisfied();
  }

  /**
   * reinvite event after join().
   */
  public void testReinviteAfterjoin() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectations("testReinviteAfterjoin");

    // mock jsr289 object.
    final MockSipServletRequest reInviteReq = mockery.mock(MockSipServletRequest.class, "reInviteReq");
    reInviteReq.setMethod("INVITE");
    reInviteReq.setIsInitial(false);

    final byte[] reinviteReqSDP = new byte[10];
    reInviteReq.setRawContent(reinviteReqSDP);

    final MockSipServletResponse reInviteResp = mockery.mock(MockSipServletResponse.class, "reInviteResp");
    reInviteResp.setStatus(200);

    final byte[] msReinviteRespSDP = new byte[10];

    final MockSipServletRequest reInviteAck = mockery.mock(MockSipServletRequest.class, "reInviteAck");
    reInviteAck.setMethod("ACK");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, "sdpPortManagerEvent");

    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).processSdpOffer(reinviteReqSDP);
          will(new MockMediaServerSdpPortManagerEventAction(sdpPortManagerEvent));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // process jsr309 sdp event.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReinviteRespSDP));

          oneOf(reInviteReq).createResponse(200);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              reInviteReq.setResponse(reInviteResp);
              reInviteResp.setRequest(reInviteReq);
              return reInviteResp;
            }
          });

          oneOf(reInviteResp).setContent(msReinviteRespSDP, "application/sdp");

          oneOf(reInviteResp).send();
          will(new MockClientDoAckAction(reInviteAck, sipcall));
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    try {
      sipcall.join().get();

      assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
      sipcall.setSupervised(true);
      sipcall.dispatch(new SIPReInviteEventImpl(sipcall, reInviteReq)).get();
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() != null);
    mockery.assertIsSatisfied();
  }

  /**
   * refer event after join to outgoingcall bridge.
   */
  public void testNotifyEventAfterjoinOutgoingCallBridge() {
    sipcall = new SIPIncomingCall(appContext, initInviteReq);

    joinToMSExpectationsInitRequestNoSDP("testNotifyEventAfterjoinOutgoingCallBridge");

    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");
    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, "outgoingCallNetwork");

    final States outgoingCallStates = mockery.states("outgoingCall");
    outgoingCallStates.become("outgoingCallInit");

    // join outgoingCall.
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).setJoinDelegate(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.INVITING));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).getSIPCallState();
          will(returnValue(SIPCall.State.ANSWERED));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(outgoingCallStates.is("resped"));

          oneOf(outgoingCall).joinWithoutCheckOperation(Direction.DUPLEX);
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              outgoingCallStates.become("resped");
              return null;
            }
          });
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).join(Direction.DUPLEX, outgoingCallNetwork);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // mock jsr289 object.
    final MockSipServletRequest notifyReq = mockery.mock(MockSipServletRequest.class, "notifyReq");
    notifyReq.setMethod("NOTIFY");

    final MockSipServletResponse notifyResp = mockery.mock(MockSipServletResponse.class, "notifyResp");
    notifyResp.setStatus(200);

    final MockSipSession outgoingCallSession = mockery.mock(MockSipSession.class, "outgoingCallSession");
    // mock jsr289 object.
    final MockSipServletRequest outgoingCallNotifyReq = mockery.mock(MockSipServletRequest.class,
        "outgoingCallNotifyReq");
    outgoingCallNotifyReq.setMethod("NOTIFY");

    final MockSipServletResponse outgoingCallNotifyResp = mockery.mock(MockSipServletResponse.class,
        "outgoingCallNotifyResp");
    outgoingCallNotifyResp.setStatus(200);
    outgoingCallNotifyReq.setResponse(outgoingCallNotifyResp);
    outgoingCallNotifyResp.setRequest(outgoingCallNotifyReq);

    // received notify event.
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getSipSession();
          will(returnValue(outgoingCallSession));
          oneOf(outgoingCallSession).createRequest("NOTIFY");
          will(new Action() {
            @Override
            public void describeTo(final Description description) {
            }

            @Override
            public Object invoke(final Invocation invocation) throws Throwable {
              outgoingCallNotifyReq.setSession(outgoingCallSession);
              return outgoingCallNotifyReq;
            }
          });

          oneOf(outgoingCallNotifyReq).send();
        }
      });
    }
    catch (final Exception ex) {
      ex.printStackTrace();
    }

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();

      assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);

      new SIPNotifyEventImpl(sipcall, notifyReq).forwardTo(outgoingCall);
    }
    catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() != null);
    mockery.assertIsSatisfied();
  }

  // ===========================inner class================================
  // mock jsr289 client send back ack
  class MockClientDoAckAction implements Action {
    SipServletRequest _ack;

    SIPCallImpl _call;

    public MockClientDoAckAction(final SipServletRequest theAck, final SIPCallImpl theCall) {
      _ack = theAck;
      _call = theCall;
    }

    @Override
    public void describeTo(final Description description) {
    }

    @Override
    public Object invoke(final Invocation invocation) throws Throwable {
      final Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            _call.doAck(_ack);
          }
          catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
          }
        }
      });
      th.start();

      return null;
    }
  }

  // mock jsr309 send back SdpPortManagerEvent.
  class MockMediaServerSdpPortManagerEventAction implements Action {
    SdpPortManagerEvent _event;

    public MockMediaServerSdpPortManagerEventAction(final SdpPortManagerEvent theEvent) {
      _event = theEvent;
    }

    @Override
    public void describeTo(final Description description) {
    }

    @Override
    public Object invoke(final Invocation invocation) throws Throwable {
      final Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
          sipcall.onEvent(_event);
        }
      });
      th.start();

      return null;
    }
  }
}
