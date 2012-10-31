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

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectAI2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectAI2NOJoinDelegate.class);

  protected Direction _direction;

  protected SipServletResponse _response;

  protected boolean _reInvited;

  protected DirectAI2NOJoinDelegate(final SIPIncomingCall call1, final SIPOutgoingCall call2,
      final Direction direction, final SIPCallImpl peer) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
    _peer = peer;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    ((SIPOutgoingCall) _call2).setContinueRouting(_call1);
    ((SIPOutgoingCall) _call2).call(null, _call1.getSipSession().getApplicationSession());
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    if (SIPHelper.isErrorResponse(res)) {
      done(getJoinCompleteCauseByResponse(res), getExceptionByResponse(res));
      _call2.disconnect(true, this.getCallCompleteCauseByResponse(res), this.getExceptionByResponse(res), null);
    }
    else if (SIPHelper.isProvisionalResponse(res) && _call2.equals(call)) {
      _call2.setSIPCallState(SIPCall.State.ANSWERING);

      if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
        try {
          if (SIPHelper.getRawContentWOException(res) != null) {
            reInviteCall1(res);
            _reInvited = true;
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
        catch (Exception e) {
          done(JoinCompleteEvent.Cause.ERROR, e);
          _call2.fail(e);
          throw e;
        }
      }
    }
    else if (SIPHelper.isSuccessResponse(res)) {
      try {
        if (_call2.equals(call)) {
          _response = res;
          if (!_reInvited) {
            reInviteCall1(res);
          }
        }
        else if (_call1.equals(call)) {
          final SipServletRequest ack1 = res.createAck();
          SipServletRequest ack2 = null;
          if (_response != null) {
            final SipServletResponse origRes = _response;
            _response = null;
            ack2 = origRes.createAck();
            SIPHelper.copyContent(res, ack2);
          }
          _call2.setSIPCallState(State.ANSWERED);
          ack1.send();
          ack2.send();
          doDisengage(_call1, JoinType.DIRECT);
          _call1.linkCall(_call2, JoinType.DIRECT, _direction);
          done(JoinCompleteEvent.Cause.JOINED, null);
        }
      }
      catch (final Exception e) {
        done(JoinCompleteEvent.Cause.ERROR, e);
        _call2.fail(e);
        throw e;
      }
    }
  }

  private void reInviteCall1(SipServletResponse res) throws Exception {
    final SipServletRequest req = _call1.getSipSession().createRequest("INVITE");
    SIPHelper.copyContent(res, req);
    req.send();
  }
}
