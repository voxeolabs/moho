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

import java.io.IOException;

import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class Media2NIJoinDelegate extends JoinDelegate {

  protected boolean waitAnswerProcessed = false;
  
  protected boolean callProcessed;

  protected Media2NIJoinDelegate(final SIPIncomingCall call) {
    _call1 = call;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    if (_call1.getSIPCallState() == SIPCall.State.PROGRESSED) {
      try {
        callProcessed = true;
        final SipServletResponse res = _call1.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
        res.setContent(_call1.getLocalSDP(), "application/sdp");
        res.send();
      }
      catch (final IOException e) {
        done(Cause.ERROR, e);
        _call1.fail(e);
      }
    }
    else {
      _call1.setSIPCallState(SIPCall.State.ANSWERING);
      _call1.processSDPOffer(_call1.getSipInitnalRequest());
    }
  }

  @Override
  protected void doSdpEvent(final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)) {
          waitAnswerProcessed = true;
        }

        final byte[] sdp = event.getMediaServerSdp();
        _call1.setLocalSDP(sdp);
        final SipServletResponse res = _call1.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
        try {
          res.setContent(sdp, "application/sdp");
          res.send();
        }
        catch (final IOException e) {
          done(Cause.ERROR, e);
          _call1.fail(e);
        }
      }
      else {
        SIPHelper.handleErrorSdpPortManagerEvent(event, _call1.getSipInitnalRequest());
        Exception ex = new NegotiateException(event);
        done(Cause.ERROR, ex);
        _call1.fail(ex);
      }
    }
    else if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_PROCESSED)) {
      if (event.isSuccessful()) {
        if (waitAnswerProcessed) {
          done(JoinCompleteEvent.Cause.JOINED, null);
          return;
        }
      }
      Exception ex = new NegotiateException(event);
      done(Cause.ERROR, ex);
      _call1.fail(ex);
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    try {
      _call1.setSIPCallState(SIPCall.State.ANSWERED);
      
      if(!callProcessed) {
        _call1.processSDPAnswer(req);
      }
      
      if (!waitAnswerProcessed) {
        done(JoinCompleteEvent.Cause.JOINED, null);
      }
    }
    catch (final Exception e) {
      done(Cause.ERROR, e);
      _call1.fail(e);
      throw e;
    }
  }

}
