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

import com.voxeo.moho.BusyException;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.RejectException;
import com.voxeo.moho.TimeoutException;

public class Media2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(Media2NOJoinDelegate.class);

  protected SIPOutgoingCall _call;

  protected boolean processedAnswer = false;

  protected Media2NOJoinDelegate(final SIPOutgoingCall call) {
    _call = call;
  }

  @Override
  protected void doJoin() throws MediaException {
    _call.processSDPOffer(null);
  }

  @Override
  protected void doSdpEvent(final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        try {
          final byte[] sdp = event.getMediaServerSdp();
          _call.setLocalSDP(sdp);
          _call.call(sdp);
          return;
        }
        catch (final Exception e) {
          setError(e);
          _call.fail(e);
          throw new RuntimeException(e);
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
      if (SIPHelper.isProvisionalResponse(res)) {
        _call.setSIPCallState(SIPCall.State.ANSWERING);

        if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
          if (SIPHelper.getRawContentWOException(res) != null) {
            processedAnswer = true;
            _call.processSDPAnswer(res);
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
        _call.setSIPCallState(SIPCall.State.ANSWERED);
        res.createAck().send();
        if (!processedAnswer) {
          processedAnswer = true;
          _call.processSDPAnswer(res);
        }
      }
      else if (SIPHelper.isErrorResponse(res)) {
        Exception e = null;
        if (SIPHelper.isBusy(res)) {
          e = new BusyException();
        }
        else if (SIPHelper.isRedirect(res)) {
          e = new RedirectException(res.getHeaders("Contact"));
        }
        else if (SIPHelper.isTimeout(res)) {
          e = new TimeoutException();
        }
        else {
          e = new RejectException();
        }
        setException(e);
        done();
      }
    }
    catch (final Exception e) {
      setError(e);
      _call.fail(e);
      throw e;
    }
  }

}
