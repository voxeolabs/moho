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

import java.util.Map;

import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class Media2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(Media2NOJoinDelegate.class);

  protected boolean processedAnswer = false;

  protected Media2NOJoinDelegate(final SIPOutgoingCall call) {
    _call1 = call;
  }

  @Override
  protected void doJoin() throws Exception {
    super.doJoin();
    _call1.processSDPOffer(null);
  }

  @Override
  protected void doSdpEvent(final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        try {
          final byte[] sdp = event.getMediaServerSdp();
          _call1.setLocalSDP(sdp);
          ((SIPOutgoingCall) _call1).call(sdp);
          return;
        }
        catch (final Exception e) {
          done(Cause.ERROR, e);
          _call1.fail(e);
        }
      }

      Exception ex = new NegotiateException(event);
      done(Cause.ERROR, ex);
      _call1.fail(ex);
    }
    else if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_PROCESSED)) {
      if (event.isSuccessful()) {
        if (processedAnswer) {
        	if(_call1.getSIPCallState() == SIPCall.State.ANSWERED){
                done(JoinCompleteEvent.Cause.JOINED, null);
        	}
          return;
        }
      }
      Exception ex = new NegotiateException(event);
      done(Cause.ERROR, ex);
      _call1.fail(ex);
    }

    Exception ex = new NegotiateException(event);
    done(Cause.ERROR, ex);
    _call1.fail(ex);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (SIPHelper.isProvisionalResponse(res)) {
        _call1.setSIPCallState(SIPCall.State.ANSWERING);

        if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
          if (SIPHelper.getRawContentWOException(res) != null) {
        	  if (!processedAnswer) {
            processedAnswer = true;
            _call1.processSDPAnswer(res);
        	  }
          }

          try {
            res.createPrack().send();
          }
          catch (Rel100Exception ex) {
            LOG.warn(ex.getMessage());
          }
          catch (IllegalStateException ex) {
            LOG.warn(ex.getMessage());
          }
        }
      }
      else if (SIPHelper.isSuccessResponse(res)) {
        _call1.setSIPCallState(SIPCall.State.ANSWERED);
        res.createAck().send();
        if (!processedAnswer) {
          processedAnswer = true;
          _call1.processSDPAnswer(res);
        }
        else{
        	done(JoinCompleteEvent.Cause.JOINED, null);
        }
      }
      else if (SIPHelper.isErrorResponse(res)) {
        Exception e = getExceptionByResponse(res);
        done(this.getJoinCompleteCauseByResponse(res), e);

        call.disconnect(true, getCallCompleteCauseByResponse(res), e, null);
      }
    }
    catch (final Exception e) {
      done(Cause.ERROR, e);
      _call1.fail(e);
      throw e;
    }
  }

}
