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
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.BusyException;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.RejectException;

public class Media2NOJoinDelegate extends JoinDelegate {

  protected SIPOutgoingCall _call;

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
        catch (final Throwable e) {
          _call.fail();
          throw new RuntimeException(e);
        }
      }
      _call.fail();
    }
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (SIPHelper.isProvisionalResponse(res)) {
        _call.setSIPCallState(SIPCall.State.ANSWERING);
        _call.processSDPAnswer(res);
      }
      else if (SIPHelper.isSuccessResponse(res)) {
        _call.setSIPCallState(SIPCall.State.ANSWERED);
        res.createAck().send();
        _call.processSDPAnswer(res);
        done();
      }
      else if (SIPHelper.isErrorResponse(res)) {
        Exception e = null;
        if (SIPHelper.isBusy(res)) {
          e = new BusyException();
        }
        else if (SIPHelper.isRedirect(res)) {
          e = new RedirectException(res.getHeader("Contact"));
        }
        else {
          e = new RejectException();
        }
        setException(e);
        done();
      }
    }
    catch (final Exception e) {
      _call.fail();
      throw e;
    }
  }

}
