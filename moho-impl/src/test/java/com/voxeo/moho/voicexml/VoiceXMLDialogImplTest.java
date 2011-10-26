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

package com.voxeo.moho.voicexml;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.vxml.VxmlDialogEvent;
import javax.servlet.sip.SipServlet;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.legacy.ClassImposteriser;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoHangupEvent;
import com.voxeo.moho.media.fake.MockMediaSession;
import com.voxeo.moho.media.fake.MockParameters;
import com.voxeo.moho.media.fake.MockVxmlDialog;
import com.voxeo.moho.sip.SIPCallImpl;
import com.voxeo.moho.sip.fake.MockSipServlet;
import com.voxeo.moho.spi.ExecutionContext;

public class VoiceXMLDialogImplTest extends TestCase {

  Mockery mockery = new Mockery() {
    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  // JSR309 mock
  MsControlFactory msFactory = mockery.mock(MsControlFactory.class);

  MockMediaSession mediaSession = mockery.mock(MockMediaSession.class);

  MockVxmlDialog dialog = mockery.mock(MockVxmlDialog.class);

  // JSR289 mock
  SipServlet servlet = new MockSipServlet(mockery);

  // Moho
  TestApp app = mockery.mock(TestApp.class);

  // ApplicationContextImpl is simple, no need to mock it.
  ExecutionContext appContext = null;

  VoiceXMLEndpoint vXMLendPoint;

  VoiceXMLDialogImpl vXMLDialog;

  protected void setUp() throws Exception {
    super.setUp();
    if (appContext == null) {
      appContext = new ApplicationContextImpl(app, msFactory, servlet);
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    appContext.destroy();
  }

  /**
   * 
   */
  public void testJoinAndUnjoin() {
    // prepare
    // test data
    final String documentURL = "ftp://test";

    try {
      mockery.checking(new Expectations() {
        {
          // creation
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession).createVxmlDialog(with(any(Parameters.class)));
          will(returnValue(dialog));

          allowing(mediaSession).createParameters();
          will(returnValue(new MockParameters()));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // create vXMLDialog.
    vXMLendPoint = (VoiceXMLEndpoint) appContext.createEndpoint(documentURL);

    vXMLDialog = (VoiceXMLDialogImpl) vXMLendPoint.create(null);

    // mock the call
    final SIPCallImpl call = mockery.mock(SIPCallImpl.class);
    final NetworkConnection callNet = mockery.mock(NetworkConnection.class);
    try {
      mockery.checking(new Expectations() {
        {
          allowing(call).getMediaObject();
          will(returnValue(callNet));

          // join
          oneOf(call).join(vXMLDialog, JoinType.BRIDGE, Direction.DUPLEX);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              vXMLDialog.addParticipant(call, JoinType.BRIDGE, Direction.DUPLEX, null);
              return null;
            }
          });

          // unjoin
          oneOf(dialog).unjoin(callNet);

          oneOf(call).doUnjoin(vXMLDialog, false);
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // join
    vXMLDialog.join(call, JoinType.BRIDGE, Direction.DUPLEX);
    assertTrue(vXMLDialog.getParticipants()[0] == call);

    // unjoin
    try {
      vXMLDialog.unjoin(call).get();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    catch (ExecutionException e) {
      e.printStackTrace();
    }
    assertTrue(vXMLDialog.getParticipants().length == 0);

    // verify the result.
    mockery.assertIsSatisfied();
  }

  /**
   * 
   */
  public void testTerminate() {

    // test data
    final String documentURL = "ftp://test";

    // prepare
    try {
      mockery.checking(new Expectations() {
        {
          // creation
          oneOf(msFactory).createMediaSession();
          will(returnValue(mediaSession));

          oneOf(mediaSession).createVxmlDialog(with(any(Parameters.class)));
          will(returnValue(dialog));

          allowing(mediaSession).createParameters();
          will(returnValue(new MockParameters()));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    final VxmlDialogEvent mediaEvent0 = mockery.mock(VxmlDialogEvent.class, "mediaEvent0");
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent0).getEventType();
          will(returnValue(VxmlDialogEvent.PREPARED));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    final VxmlDialogEvent mediaEvent1 = mockery.mock(VxmlDialogEvent.class, "mediaEvent1");

    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent1).getEventType();
          will(returnValue(VxmlDialogEvent.STARTED));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    final VxmlDialogEvent mediaEvent2 = mockery.mock(VxmlDialogEvent.class, "mediaEvent2");
    final Map<String, Object> result = new HashMap<String, Object>();
    try {
      mockery.checking(new Expectations() {
        {
          allowing(mediaEvent2).getEventType();
          will(returnValue(VxmlDialogEvent.DISCONNECTION_REQUESTED));

          allowing(mediaEvent2).getNameList();
          will(returnValue(result));
        }
      });
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      mockery.checking(new Expectations() {
        {
          // dialog prepare
          oneOf(dialog).prepare(with(new TypeSafeMatcher<URL>() {

            @Override
            public boolean matchesSafely(URL item) {
              try {
                if (item.equals(new URL(documentURL))) {
                  return true;
                }
              }
              catch (MalformedURLException e) {
                e.printStackTrace();
                fail(e.getMessage());
              }
              return false;
            }

            @Override
            public void describeTo(Description description) {
            }
          }), with(any(Parameters.class)), with(any(Map.class)));
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  Object[] ls = dialog.listeners.toArray();
                  for (Object listerner : ls) {
                    ((MediaEventListener<VxmlDialogEvent>) listerner).onEvent(mediaEvent0);
                  }
                }
              });
              th.start();
              return null;
            }
          });

          // dialog start.
          oneOf(dialog).start(with(any(Map.class)));
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  Object[] ls = dialog.listeners.toArray();
                  for (Object listerner : ls) {
                    ((MediaEventListener<VxmlDialogEvent>) listerner).onEvent(mediaEvent1);
                  }
                }
              });
              th.start();
              return null;
            }
          });

          // dialog terminate
          oneOf(dialog).terminate(true);
          will(new Action() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
              Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                  Object[] ls = dialog.listeners.toArray();
                  for (Object listerner : ls) {
                    ((MediaEventListener<VxmlDialogEvent>) listerner).onEvent(mediaEvent2);
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

    // excute
    // create dialog.
    vXMLendPoint = (VoiceXMLEndpoint) appContext.createEndpoint(documentURL);

    vXMLDialog = (VoiceXMLDialogImpl) vXMLendPoint.create(null);

    vXMLDialog.prepare();

    vXMLDialog.start();

    try {
      Thread.sleep(1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    vXMLDialog.terminate(true);

    try {
      vXMLDialog.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }

    // verify the result.
    mockery.assertIsSatisfied();
  }

  abstract class TestApp implements Application {
    public abstract void handleDisconnect(MohoHangupEvent event);

    public final void destroy() {

    }
  }
}
