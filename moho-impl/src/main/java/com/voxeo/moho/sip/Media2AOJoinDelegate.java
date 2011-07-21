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
import java.util.Map;

import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.RejectException;

public class Media2AOJoinDelegate extends JoinDelegate {

  protected SIPOutgoingCall _call;

  protected boolean processedAnswer = false;

  protected Media2AOJoinDelegate(final SIPOutgoingCall call) {
    _call = call;
  }

  @Override
  protected void doJoin() throws MediaException {
    doDisengage(_call, JoinType.BRIDGE);
    _call.processSDPOffer(null);
  }

  @Override
  protected void doSdpEvent(final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        if (isWaiting()) {
          try {
            final byte[] sdp = event.getMediaServerSdp();
            _call.setLocalSDP(sdp);
            final SipServletMessage message = _call.getSipSession().createRequest("INVITE");
            message.setContent(sdp, "application/sdp");
            message.send();
            return;
          }
          catch (final IOException e) {
            setError(e);
            _call.fail(e);
            throw new RuntimeException(e);
          }
        }
      }
      Exception ex = new NegotiateException(event);
      setError(ex);
      _call.fail(ex);
    }
    else if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_PROCESSED)) {
      if (event.isSuccessful()) {
        if (processedAnswer) {
          done();
          return;
        }
      }
      Exception ex = new NegotiateException(event);
      setError(ex);
      _call.fail(ex);
    }

    Exception ex = new NegotiateException(event);
    setError(ex);
    _call.fail(ex);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (isWaiting()) {
        if (SIPHelper.isSuccessResponse(res)) {
          res.createAck().send();
          processedAnswer = true;
          _call.processSDPAnswer(res);
        }
        else if (SIPHelper.isErrorResponse(res)) {
          setException(new RejectException());
          done();
        }
      }
    }
    catch (final Exception e) {
      setError(e);
      _call.fail(e);
      throw e;
    }
  }

}
