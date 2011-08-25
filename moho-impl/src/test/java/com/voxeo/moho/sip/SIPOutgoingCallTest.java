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

import java.io.IOException;
import java.util.Map;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javax.sdp.SdpFactory;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.BusyException;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.fake.MockParameters;
import com.voxeo.moho.sip.SIPCall.State;
import com.voxeo.moho.sip.SIPIncomingCallTest.TestApp;
import com.voxeo.moho.sip.fake.MockSipServlet;
import com.voxeo.moho.sip.fake.MockSipServletRequest;
import com.voxeo.moho.sip.fake.MockSipServletResponse;
import com.voxeo.moho.sip.fake.MockSipSession;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPOutgoingCallTest extends TestCase {

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
  SipServlet servlet = new MockSipServlet(mockery);
  SipApplicationSession appSession = mockery.mock(SipApplicationSession.class);
  MockSipSession session = mockery.mock(MockSipSession.class);
  MockSipServletRequest initInviteReq = mockery.mock(MockSipServletRequest.class);

  // Moho
  TestApp app = mockery.mock(TestApp.class);

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = new ApplicationContextImpl(app, msFactory, servlet);
  SipFactory sipFactory = appContext.getSipFactory();
  SdpFactory sdpFactory = appContext.getSdpFactory();

  SIPEndpoint from = mockery.mock(SIPEndpoint.class, "from");;

  SIPEndpoint to = mockery.mock(SIPEndpoint.class, "to");

  Address fromAddr = mockery.mock(Address.class, "fromAddr");

  Address toAddr = mockery.mock(Address.class, "toAddr");

  byte[] msReqSDP = new byte[10];

  byte[] respSDP = new byte[10];

  // testing object
  private SIPOutgoingCall sipcall;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    initInviteReq.setSession(session);
    initInviteReq.setMethod("INVITE");
    initInviteReq.setIsInitial(true);

    // common Expectations.
    mockery.checking(new Expectations() {
      {
        allowing(session).getRemoteParty();
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

    try {
      mockery.checking(new Expectations() {
        {
          allowing(from).getSipAddress();
          will(returnValue(fromAddr));
          allowing(to).getSipAddress();
          will(returnValue(toAddr));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    sipcall = new SIPOutgoingCall(appContext, from, to, null);
  }

  /**
   * send req IOException
   */
  @Test
  public void testJoinWithSIPIOException() {
    joinExceptionWithSIPExpectations("testJoin");

    // execute
    JoinCompleteEvent event = null;
    try {
       event = sipcall.join(Joinable.Direction.DUPLEX).get();
      //fail("can't catch exception");
    }
    catch (Exception ex) {

    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.ERROR);
//    assertTrue(sipcall.getSIPCallState() == State.FAILED);
//    assertTrue(sipcall.getMediaObject() == null);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void joinExceptionWithSIPExpectations(String mockObjectNamePrefix) {
    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(200);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    final MockSipServletRequest cancelReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "cancelReq");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));

          final SipApplicationSession createdAppSession = mockery
              .mock(SipApplicationSession.class, "createdAppSession");
          // create outgoingcall expectations.
          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createApplicationSession();
                will(returnValue(createdAppSession));
                oneOf(sipFactory).createRequest(createdAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          allowing(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          allowing(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReqSDP));

          oneOf(initInviteReq).setContent(msReqSDP, "application/sdp");

          // throw io exception.
          oneOf(initInviteReq).send();
          will(throwException(new IOException("SIP IOException")));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(initInviteReq).createCancel();
          // will(returnValue(cancelReq));
          //
          // oneOf(cancelReq).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

//    try {
//      mockery.checking(new Expectations() {
//        {
//          oneOf(mediaSession).release();
//
//          oneOf(network).release();
//        }
//      });
//    }
//    catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  /**
   * send ack io exception.
   */
  public void testJoinWithSIPIOException2() {
    joinExceptionWithSIPExpectations2("testJoin");
    sipcall.addObserver(new MyObserver());
    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(Joinable.Direction.DUPLEX).get();
      //fail("can't catch exception");
    }
    catch (Exception ex) {

    }

    // verify result
    assertTrue(event.getCause() ==  JoinCompleteEvent.Cause.ERROR);
//    assertTrue(sipcall.getSIPCallState() == State.FAILED);
//    assertTrue(sipcall.getMediaObject() == null);
    mockery.assertIsSatisfied();
  }

  public class MyObserver implements Observer {

    @com.voxeo.moho.State
    public void listenCallcomplete(MohoCallCompleteEvent event) {
      System.out.println("received==========>");
    }
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void joinExceptionWithSIPExpectations2(String mockObjectNamePrefix) {

    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(200);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    final MockSipServletRequest byeReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix + "byeReq");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));

          final SipApplicationSession createdAppSession = mockery
              .mock(SipApplicationSession.class, "createdAppSession");
          // create outgoingcall expectations.
          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createApplicationSession();
                will(returnValue(createdAppSession));
                oneOf(sipFactory).createRequest(createdAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReqSDP));

          oneOf(initInviteReq).setContent(msReqSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(sipInviteResp, null);
                  }
                  catch (Exception e) {
                    // e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
          will(throwException(new IOException("send ack io exception")));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(session).createRequest("BYE");
          // will(returnValue(byeReq));
          //
          // oneOf(byeReq).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

//    try {
//      mockery.checking(new Expectations() {
//        {
//          oneOf(mediaSession).release();
//
//          oneOf(network).release();
//        }
//      });
//    }
//    catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  /**
   * sip response busy.
   */
  public void testJoinWithUnsuccessSipResp() {
    joinExceptionWithUnsuccessSipResp("testJoin");
    sipcall.addObserver(new MyObserver());
    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(Joinable.Direction.DUPLEX).get();
      //fail("can't catch join exception");
    }
    catch (Exception ex) {
      assertTrue(ex.getCause() instanceof BusyException);
    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.BUSY);
//    assertTrue(sipcall.getSIPCallState() == State.FAILED);
//    assertTrue(sipcall.getMediaObject() == null);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void joinExceptionWithUnsuccessSipResp(String mockObjectNamePrefix) {
    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(SipServletResponse.SC_BUSY_HERE);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));

          final SipApplicationSession createdAppSession = mockery
              .mock(SipApplicationSession.class, "createdAppSession");
          // create outgoingcall expectations.
          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createApplicationSession();
                will(returnValue(createdAppSession));
                oneOf(sipFactory).createRequest(createdAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReqSDP));

          oneOf(initInviteReq).setContent(msReqSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(sipInviteResp, null);
                  }
                  catch (Exception e) {
                    e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // TODO roll back to the original state??
//    try {
//      mockery.checking(new Expectations() {
//        {
//          oneOf(mediaSession).release();
//
//          oneOf(network).release();
//        }
//      });
//    }
//    catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  /**
   * process sdp answer exception.
   */
  public void testJoinWithMediaException() {
    joinExceptionWithMediaExpectations("testJoin");

    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(Joinable.Direction.DUPLEX).get();
    }
    catch (Exception ex) {

    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.ERROR);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void joinExceptionWithMediaExpectations(String mockObjectNamePrefix) {
    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(200);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    final MockSipServletRequest byeReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix + "byeReq");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));

          final SipApplicationSession createdAppSession = mockery
              .mock(SipApplicationSession.class, "createdAppSession");
          // create outgoingcall expectations.
          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createApplicationSession();
                will(returnValue(createdAppSession));
                oneOf(sipFactory).createRequest(createdAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReqSDP));

          oneOf(initInviteReq).setContent(msReqSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.doResponse(sipInviteResp, null);
                  }
                  catch (Exception e) {
                    // e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process response
    try {
      mockery.checking(new Expectations() {
        {
          // throw SdpPortManagerException.
          oneOf(sdpManager).processSdpAnswer(respSDP);
          will(throwException(new SdpPortManagerException("processSdpAnswer Exception")));

          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(session).createRequest("BYE");
          // will(returnValue(byeReq));
          //
          // oneOf(byeReq).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

//    try {
//      mockery.checking(new Expectations() {
//        {
//          oneOf(mediaSession).release();
//
//          oneOf(network).release();
//        }
//      });
//    }
//    catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  /**
   * generate sdp offer result in unsuccess. ?? Media2NOJoinDelegate line 30,
   * when media server response with an unsuccessful answer, the code can't tell
   * the error information.
   */
  public void testJoinWithErrorMediaResp() {

    joinWithErrorMediaRespExpectations("testJoin");

    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(Joinable.Direction.DUPLEX).get();
    }
    catch (Exception ex) {

    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.ERROR);
    mockery.assertIsSatisfied();
  }

  /**
   * @param mockObjectNamePrefix
   */
  private void joinWithErrorMediaRespExpectations(String mockObjectNamePrefix) {

    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(200);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    final MockSipServletRequest cancelReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "cancelReq");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          allowing(sdpPortManagerEvent).isSuccessful();
          will(returnValue(false));

          allowing(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(initInviteReq).createCancel();
          // will(returnValue(cancelReq));
          //
          // oneOf(cancelReq).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

//    try {
//      mockery.checking(new Expectations() {
//        {
//          oneOf(network).release();
//          oneOf(mediaSession).release();
//        }
//      });
//    }
//    catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  /**
   * testJoin.
   */
  public void testJoin() {

    joinExpectations("testJoin");

    // execute
    try {
      sipcall.join(Joinable.Direction.DUPLEX).get();
    }
    catch (Exception ex) {
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
  private void joinExpectations(String mockObjectNamePrefix) {

    // prepare;
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    initInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(initInviteReq);
    sipInviteResp.setStatus(200);

    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEvent");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEventAnswerProcessed = mockery.mock(SdpPortManagerEvent.class,
        mockObjectNamePrefix + "sdpPortManagerEventAnswerProcessed");

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
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));

          final SipApplicationSession createdAppSession = mockery
              .mock(SipApplicationSession.class, "createdAppSession");
          // create outgoingcall expectations.
          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createApplicationSession();
                will(returnValue(createdAppSession));
                oneOf(sipFactory).createRequest(createdAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReqSDP));

          oneOf(initInviteReq).setContent(msReqSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEventAnswerProcessed).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEventAnswerProcessed).getEventType();
          will(returnValue(SdpPortManagerEvent.ANSWER_PROCESSED));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).processSdpAnswer(respSDP);
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEventAnswerProcessed));

          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * test join(). join().
   */
  public void testJoinAfterJoin() {
    joinExpectations("testJoinAfterJoin1");

    serverReInviteExpectations("testJoinAfterJoin2");

    // execute
    try {
      sipcall.join(Joinable.Direction.DUPLEX).get();

      sipcall.join(Joinable.Direction.SEND).get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() != null);
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  private void serverReInviteExpectations(String mockObjectNamePrefix) {

    // prepare;
    // mock jsr289 object.
    final byte[] msReinviteReqSDP = new byte[10];

    final MockSipServletRequest reInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "reInviteReq");
    reInviteReq.setMethod("INVITE");
    reInviteReq.setIsInitial(false);
    reInviteReq.setRawContent(msReinviteReqSDP);
    reInviteReq.setContentType("application/sdp");

    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    reInviteReq.setResponse(sipInviteResp);
    sipInviteResp.setRequest(reInviteReq);
    sipInviteResp.setStatus(200);

    final byte[] respSDP = new byte[10];
    sipInviteResp.setRawContent(respSDP);

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");

    // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEvent = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEvent");
    
 // mock jsr309 object
    final SdpPortManagerEvent sdpPortManagerEventAnswerProcessed = mockery.mock(SdpPortManagerEvent.class, mockObjectNamePrefix
        + "sdpPortManagerEventAnswerProcessed");

    // invoke join()
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).generateSdpOffer();
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEvent));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEvent).isSuccessful();
          will(returnValue(true));

          oneOf(sdpPortManagerEvent).getEventType();
          will(returnValue(SdpPortManagerEvent.OFFER_GENERATED));

          oneOf(sdpPortManagerEvent).getMediaServerSdp();
          will(returnValue(msReinviteReqSDP));

          oneOf(session).createRequest("INVITE");
          will(returnValue(reInviteReq));

          oneOf(reInviteReq).setContent(msReinviteReqSDP, "application/sdp");

          oneOf(reInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpManager).processSdpAnswer(respSDP);
          will(new MockMediaServerSdpPortManagerEventAction(sipcall, sdpPortManagerEventAnswerProcessed));

          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    
    // process sdpPortManagerEvent
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sdpPortManagerEventAnswerProcessed).isSuccessful();
          will(returnValue(true));

          allowing(sdpPortManagerEventAnswerProcessed).getEventType();
          will(returnValue(SdpPortManagerEvent.ANSWER_PROCESSED));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * 
   */
  public void testJoinOutgoingCallBridge() {

    joinExpectations("testJoinOutgoingCallBridge");

    // Outgoingcall mock.
    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");

    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, "outgoingCallNetwork");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).setBridgeJoiningPeer(with(any(SIPCallImpl.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).getRemoteSdp();
          will(returnValue(null));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(State.INVITING));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(outgoingCall).getSIPCallState();
          will(returnValue(State.ANSWERED));
          when(incomingCallStates.is("resped"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(incomingCallStates.is("resped"));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // outgoingCall join().
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).join(Direction.DUPLEX);
          will(new Action() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              incomingCallStates.become("resped");
              sipcall.getJoinDelegate().doJoin();
              return null;
            }

          });
          
          oneOf(outgoingCall).joinDone();
        }
      });
    }
    catch (Exception ex) {
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.BRIDGE, Direction.DUPLEX);
          oneOf(outgoingCall).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // execute
    JoinCompleteEvent event =  null;
    try {
      event = sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.JOINED);
    mockery.assertIsSatisfied();
  }

  /**
   * send req io exception.
   */
  public void testJoinOutgoingCallBridgeWithSIPIOException() {

    joinExceptionWithSIPExpectations("testJoinOutgoingCallBridge");

    // Outgoingcall mock.
    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");

    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, "outgoingCallNetwork");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).setBridgeJoiningPeer(with(any(SIPCallImpl.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).getRemoteSdp();
          will(returnValue(null));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(incomingCallStates.is("resped"));
          
          oneOf(outgoingCall).joinDone();
          
          oneOf(outgoingCall).dispatch(with(any(MohoJoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
      //fail("can't catch exception.");
    }
    catch (Exception ex) {

    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.ERROR);
    mockery.assertIsSatisfied();
  }

  /**
   * TODO if the last link operation (jsr309 join) throws exception, should we
   * roll back to the original state?
   */
  public void testJoinOutgoingCallBridgeWithMediaException() {

    joinExpectations("testJoinOutgoingCallBridge");

    // Outgoingcall mock.
    // mock moho SIPOutgoingCall
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, "outgoingCall");

    final NetworkConnection outgoingCallNetwork = mockery.mock(NetworkConnection.class, "outgoingCallNetwork");

    final States incomingCallStates = mockery.states("incomingCall");
    incomingCallStates.become("incomingCallInit");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).setBridgeJoiningPeer(with(any(SIPCallImpl.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).getRemoteSdp();
          will(returnValue(null));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(incomingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(incomingCallStates.is("resped"));

          allowing(outgoingCall).getSIPCallState();
          will(returnValue(State.INVITING));
          when(incomingCallStates.is("incomingCallInit"));
          allowing(outgoingCall).getSIPCallState();
          will(returnValue(State.ANSWERED));
          when(incomingCallStates.is("resped"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(null));
          when(incomingCallStates.is("incomingCallInit"));

          allowing(outgoingCall).getMediaObject();
          will(returnValue(outgoingCallNetwork));
          when(incomingCallStates.is("resped"));
          
          oneOf(outgoingCall).joinDone();
          
          oneOf(outgoingCall).dispatch(with(any(MohoJoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // outgoingCall join().
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).join(Direction.DUPLEX);
          will(new Action() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              incomingCallStates.become("resped");
              sipcall.getJoinDelegate().doJoin();
              return null;
            }

          });
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // linkNetworkConnection
    try {
      mockery.checking(new Expectations() {
        {
          // throw exception.
          oneOf(network).join(Direction.DUPLEX, outgoingCallNetwork);
          will(throwException(new MsControlException("join MsControlException")));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(outgoingCall, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (Exception ex) {
      // ex.printStackTrace();
    }

    // verify result
    assertTrue(event.getCause() == JoinCompleteEvent.Cause.ERROR);
    mockery.assertIsSatisfied();
  }

  // ====================join outgoingcall direct.
  /**
   * 
   */
  public void testJoinOutgoingCallDirect() {

    SIPOutgoingCall outgoingCall = joinOutgoingCallDirectExpectations("testJoinOutgoingCallDirect");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (Throwable ex) {
      ex.printStackTrace();
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();

  }

  private SIPOutgoingCall joinOutgoingCallDirectExpectations(String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    sipInviteResp.setRequest(initInviteReq);
    initInviteReq.setResponse(sipInviteResp);

    sipInviteResp.setRawContent(respSDP);
    sipInviteResp.setContentType("application/sdp");

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SipSession outgoingSession = mockery.mock(SipSession.class, "outgoingCallSession");
    final SipApplicationSession outgoingAppSession = mockery.mock(SipApplicationSession.class,
        "outgoingCallApplicationSession");
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final MockSipServletRequest outgoingCallInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteReq");
    outgoingCallInviteReq.setMethod("INVITE");
    outgoingCallInviteReq.setIsInitial(true);

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
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

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

          allowing(outgoingCall).getSipInitnalRequest();
          will(returnValue(outgoingCallInviteReq));

          allowing(outgoingCall).getSipSession();
          will(returnValue(outgoingSession));

          allowing(outgoingSession).getApplicationSession();
          will(returnValue(outgoingAppSession));

          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createRequest(outgoingAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }

          oneOf(outgoingCall).call(null);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (Exception e) {
                    e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
          oneOf(outgoingCall).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).setContent(outgoingCallRespSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).setSIPCallState(State.ANSWERED);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              outgoingCallStates.become("resped");
              return null;
            }
          });

          // outgoing call ack.
          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).setContent(respSDP, "application/sdp");

          oneOf(outgoingCallInviteAck).send();

          // sipcall ack
          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
          
          oneOf(outgoingCall).joinDone();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testJoinOutgoingCallDirectWithSIPIOException() {

    SIPOutgoingCall outgoingCall = joinOutgoingCallDirectExpectationsWithSIPIOException("testJoinOutgoingCallDirectWithSIPIOException");

    // execute
    JoinCompleteEvent event = null;
    try {
      event = sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (Throwable ex) {

    }

    // verify result
    assertTrue(event.getCause() ==  JoinCompleteEvent.Cause.ERROR);
    mockery.assertIsSatisfied();

  }

  private SIPOutgoingCall joinOutgoingCallDirectExpectationsWithSIPIOException(String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipInviteResp");
    sipInviteResp.setStatus(200);

    sipInviteResp.setRequest(initInviteReq);
    initInviteReq.setResponse(sipInviteResp);

    sipInviteResp.setRawContent(respSDP);
    sipInviteResp.setContentType("application/sdp");

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipInviteAck");
    sipInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SipSession outgoingSession = mockery.mock(SipSession.class, "outgoingSipSession");
    final SipApplicationSession outgoingAppSession = mockery.mock(SipApplicationSession.class, "outgoingAppSession");
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final MockSipServletRequest outgoingCallInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteReq");
    outgoingCallInviteReq.setMethod("INVITE");
    outgoingCallInviteReq.setIsInitial(true);

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
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).setBridgeJoiningPeer(with(any(SIPCallImpl.class)));

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

          allowing(outgoingCall).getSipInitnalRequest();
          will(returnValue(outgoingCallInviteReq));

          allowing(outgoingCall).getSipSession();
          will(returnValue(outgoingSession));

          allowing(outgoingSession).getApplicationSession();
          will(returnValue(outgoingAppSession));

          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createRequest(outgoingAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }

          oneOf(outgoingCall).call(null);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (Exception e) {
                    e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).setContent(outgoingCallRespSDP, "application/sdp");

          oneOf(initInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response.
    try {
      mockery.checking(new Expectations() {
        {
          // sipcall ack
          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
          will(throwException(new IOException("send back ack io exception.")));
        }
      });
    }
    catch (Exception ex) {
      // ex.printStackTrace();
    }

    final MockSipServletRequest sipcallCancelReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipcallCancelReq");

    try {
      mockery.checking(new Expectations() {
        {
          // oneOf(initInviteReq).createCancel();
          // will(returnValue(sipcallCancelReq));
          //
          // oneOf(sipcallCancelReq).send();
          
          oneOf(outgoingCall).joinDone();
          
          allowing(outgoingCall).fail(with(any(Exception.class)));
          
          oneOf(outgoingCall).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testJoinAnsweredOutgoingCallDirectAfterJoin() {

    joinExpectations("testJoinOutgoingCallDirectAfterJoin");

    SIPOutgoingCall outgoingCall = joinAnsweredOutgoingCallDirectAfterJoinExpectations("testJoinAnsweredOutgoingCallDirectAfterJoin");

    // execute
    
    JoinCompleteEvent event = null;
    try {
      sipcall.join().get();

      assertTrue(sipcall.getRemoteSdp() != null);

      event = sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    
    mockery.assertIsSatisfied();
  }

  private SIPOutgoingCall joinAnsweredOutgoingCallDirectAfterJoinExpectations(String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.

    final MockSipServletRequest sipReInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteReq");
    sipReInviteReq.setMethod("INVITE");
    sipReInviteReq.setIsInitial(false);

    final MockSipServletResponse sipReInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipReInviteResp");
    sipReInviteResp.setStatus(200);

    sipReInviteResp.setRequest(initInviteReq);
    initInviteReq.setResponse(sipReInviteResp);

    final MockSipServletRequest sipReInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteAck");
    sipReInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SipSession outgoingSession = mockery.mock(SipSession.class, "outgoingSipSession");
    final SipApplicationSession outgoingAppSession = mockery.mock(SipApplicationSession.class, "outgoingAppSession");
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final MockSipServletRequest outgoingCallInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteReq");
    outgoingCallInviteReq.setMethod("INVITE");
    outgoingCallInviteReq.setIsInitial(true);

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
    outgoingCallStates.become("resped");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

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
          will(returnValue(true));

          allowing(outgoingCall).unlinkDirectlyPeer();

          allowing(outgoingCall).getSipInitnalRequest();
          will(returnValue(outgoingCallInviteReq));

          allowing(outgoingCall).getRemoteSdp();
          will(returnValue(outgoingCallRespSDP));

          allowing(outgoingCall).getSipSession();
          will(returnValue(outgoingSession));

          allowing(outgoingSession).getApplicationSession();
          will(returnValue(outgoingAppSession));

          oneOf(outgoingCall).call(null);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
                  }
                  catch (Exception e) {
                    e.printStackTrace();
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
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // release sipcall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(network).release();

          oneOf(mediaSession).release();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
          oneOf(outgoingCall).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingcall response.
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(session).createRequest("INVITE");
          will(returnValue(sipReInviteReq));

          oneOf(sipReInviteReq).setContent(outgoingCallRespSDP, "application/sdp");

          oneOf(sipReInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipReInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response.
    try {
      mockery.checking(new Expectations() {
        {
          // outgoing call ack.
          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).send();

          // sipcall ack
          oneOf(sipReInviteResp).createAck();
          will(returnValue(sipReInviteAck));

          oneOf(sipReInviteAck).send();
          
          oneOf(outgoingCall).joinDone();
          
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  /**
   * 
   */
  public void testJoinAnsweredOutgoingCallDirect() {

    SIPOutgoingCall outgoingCall = joinAnsweredOutgoingCallDirectExpectations("testJoinAnsweredOutgoingCallDirect");

    // execute
    try {
      sipcall.join(outgoingCall, JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (Throwable ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify result
    assertEquals(sipcall.getSIPCallState(), SIPCall.State.ANSWERED);
    assertTrue(sipcall.getMediaObject() == null);
    assertTrue(sipcall.getPeers()[0] == outgoingCall);
    mockery.assertIsSatisfied();
  }

  private SIPOutgoingCall joinAnsweredOutgoingCallDirectExpectations(String mockObjectNamePrefix) {

    // prepare
    // mock jsr289 object.
    final MockSipServletResponse sipInviteResp = mockery.mock(MockSipServletResponse.class, mockObjectNamePrefix
        + "sipReInviteResp");
    sipInviteResp.setStatus(200);

    sipInviteResp.setRequest(initInviteReq);
    initInviteReq.setResponse(sipInviteResp);

    final byte[] sipInviteRespSDP = new byte[10];
    sipInviteResp.setRawContent(sipInviteRespSDP);
    sipInviteResp.setContentType("application/sdp");

    final MockSipServletRequest sipInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "sipReInviteAck");
    sipInviteAck.setMethod("ACK");

    // mock moho SIPOutgoingCall
    final SipSession outgoingCallSession = mockery.mock(SipSession.class, mockObjectNamePrefix + "outgoingCallSession");
    final SipApplicationSession outgoingAppSession = mockery.mock(SipApplicationSession.class, mockObjectNamePrefix
        + "outgoingCallAppSession");
    final SIPOutgoingCall outgoingCall = mockery.mock(SIPOutgoingCall.class, mockObjectNamePrefix + "outgoingCall");

    final String outgoingCallId = "testoutgoingCall";

    final byte[] originOutgoingCallRespSDP = new byte[10];

    final MockSipServletRequest outgoingCallInviteReq = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteReq");
    outgoingCallInviteReq.setMethod("INVITE");
    outgoingCallInviteReq.setIsInitial(true);

    final MockSipServletResponse outgoingCallInviteResp = mockery.mock(MockSipServletResponse.class,
        mockObjectNamePrefix + "outgoingCallInviteResp");
    outgoingCallInviteResp.setStatus(200);
    outgoingCallInviteResp.setReasonPhrase("OK");

    outgoingCallInviteResp.setRequest(outgoingCallInviteReq);
    outgoingCallInviteReq.setResponse(outgoingCallInviteResp);

    final MockSipServletRequest outgoingCallInviteAck = mockery.mock(MockSipServletRequest.class, mockObjectNamePrefix
        + "outgoingCallInviteAck");
    outgoingCallInviteAck.setMethod("ACK");

    final States outgoingCallStates = mockery.states("outgoingCall");
    outgoingCallStates.become("resped");
    // outgoingCall call(byte[]).
    try {
      mockery.checking(new Expectations() {
        {
          allowing(outgoingCall).getJoinDelegate();
          will(returnValue(null));
          allowing(outgoingCall).startJoin(with(any(JoinDelegate.class)));

          allowing(outgoingCall).setCallDelegate(with(any(SIPCallDelegate.class)));

          allowing(outgoingCall).isAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("outgoingCallInit"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("outgoingCallInit"));

          allowing(outgoingCall).getId();
          will(returnValue(outgoingCallId));

          allowing(outgoingCall).isTerminated();
          will(returnValue(false));

          allowing(outgoingCall).isAnswered();
          will(returnValue(true));
          when(outgoingCallStates.is("resped"));
          allowing(outgoingCall).isNoAnswered();
          will(returnValue(false));
          when(outgoingCallStates.is("resped"));

          allowing(outgoingCall).getSipSession();
          will(returnValue(outgoingCallSession));

          allowing(outgoingCallSession).getApplicationSession();
          will(returnValue(outgoingAppSession));

          try {
            mockery.checking(new Expectations() {
              {
                oneOf(sipFactory).createRequest(outgoingAppSession, "INVITE", fromAddr, toAddr);
                will(returnValue(initInviteReq));
              }
            });
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }

          allowing(outgoingCall).getRemoteSdp();
          will(returnValue(originOutgoingCallRespSDP));

          allowing(outgoingCall).isDirectlyJoined();
          will(returnValue(true));

          allowing(outgoingCall).unlinkDirectlyPeer();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // linkCall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).addPeer(sipcall, JoinType.DIRECT, Direction.DUPLEX);
          oneOf(outgoingCall).dispatch(with(any(JoinCompleteEvent.class)));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // call sipcall
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(initInviteReq).send();
          will(new MockClientDoResponseAction(sipcall, sipInviteResp, null));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process sipcall response
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(outgoingCall).call(sipInviteRespSDP);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              sipcall.getJoinDelegate().doInviteResponse(outgoingCallInviteResp, outgoingCall, null);
              return null;
            }
          });
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // process outgoingCall response.
    try {
      mockery.checking(new Expectations() {
        {
          // outgoing call ack.
          oneOf(outgoingCallInviteResp).createAck();
          will(returnValue(outgoingCallInviteAck));

          oneOf(outgoingCallInviteAck).send();

          // sipcall ack
          oneOf(sipInviteResp).createAck();
          will(returnValue(sipInviteAck));

          oneOf(sipInviteAck).send();
          
          oneOf(outgoingCall).joinDone();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return outgoingCall;
  }

  // ========================inner class===================================
  // mock jsr289 client send back ack
  class MockClientDoAckAction implements Action {
    SipServletRequest _ack;

    SIPCallImpl _call;

    public MockClientDoAckAction(SipServletRequest theAck, SIPCallImpl theCall) {
      _ack = theAck;
      _call = theCall;
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
      Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            _call.doAck(_ack);
          }
          catch (Exception e) {
            e.printStackTrace();
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

    SIPCallImpl _sipCall;

    public MockMediaServerSdpPortManagerEventAction(SIPCallImpl sipCall, SdpPortManagerEvent theEvent) {
      _event = theEvent;
      _sipCall = sipCall;
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
      Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
          _sipCall.onEvent(_event);
        }
      });
      th.start();

      return null;
    }
  }

  // mock jsr289 client send back response
  class MockClientDoResponseAction implements Action {
    SipServletResponse _resp;

    Map<String, String> _headers;

    SIPCallImpl _sipCall;

    public MockClientDoResponseAction(SIPCallImpl sipCall, SipServletResponse theResp, Map<String, String> theHeaders) {
      _sipCall = sipCall;
      _resp = theResp;
      _headers = theHeaders;
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
      Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            _sipCall.doResponse(_resp, _headers);
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      th.start();

      return null;
    }
  }
}
