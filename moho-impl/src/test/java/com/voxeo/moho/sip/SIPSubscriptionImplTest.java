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

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.URI;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.Subscription.Type;
import com.voxeo.moho.sip.SIPIncomingCallTest.TestApp;
import com.voxeo.moho.sip.fake.MockSipServletRequest;
import com.voxeo.moho.sip.fake.MockSipSession;

public class SIPSubscriptionImplTest extends TestCase {

  Mockery mockery = new Mockery() {
    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  // JSR309 mock
  MsControlFactory msFactory = mockery.mock(MsControlFactory.class);

  // JSR289 mock
  SipFactory sipFactory = mockery.mock(SipFactory.class);

  SdpFactory sdpFactory = mockery.mock(SdpFactory.class);

  MockSipSession session = mockery.mock(MockSipSession.class);

  MockSipServletRequest subscribeReq = mockery.mock(MockSipServletRequest.class);

  // Moho
  TestApp app = mockery.mock(TestApp.class);

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = new ApplicationContextImpl(app, msFactory, sipFactory, sdpFactory, "test", null, 2);

  SIPEndpoint from = mockery.mock(SIPEndpoint.class, "from");;

  SIPEndpoint to = mockery.mock(SIPEndpoint.class, "to");

  Endpoint reqEnd = mockery.mock(Endpoint.class, "reqURI");

  Address fromAddr = mockery.mock(Address.class, "fromAddr");

  Address toAddr = mockery.mock(Address.class, "toAddr");

  protected void setUp() throws Exception {
    super.setUp();

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

  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testRenew() {

    final SipApplicationSession appSession = mockery.mock(SipApplicationSession.class);
    final URI uri = mockery.mock(URI.class);
    // prepare
    try {
      mockery.checking(new Expectations() {
        {
          oneOf(sipFactory).createApplicationSession();
          will(returnValue(appSession));

          oneOf(sipFactory).createRequest(appSession, "SUBSCRIBE", fromAddr, toAddr);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              subscribeReq.setSession(session);
              return subscribeReq;
            }
          });

          allowing(reqEnd).getURI();
          will(returnValue("test"));

          allowing(subscribeReq).addHeader(with(any(String.class)), with(any(String.class)));

          oneOf(sipFactory).createURI("test");
          will(returnValue(uri));

          oneOf(subscribeReq).setRequestURI(uri);

          oneOf(subscribeReq).send();
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // execute
    SIPSubscriptionImpl subscription = new SIPSubscriptionImpl(appContext, Type.DIALOG, 1000, from, to, reqEnd);
    subscription.subscribe();

    assertTrue(subscription.getType() == Type.DIALOG.name());
    assertTrue(subscribeReq.getExpires() == 1000);
    mockery.assertIsSatisfied();
  }

}
