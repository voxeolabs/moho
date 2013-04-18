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

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletMessage;
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

  protected SipServletResponse _reliable183Resp;

  protected boolean _reInvited;

  protected SipServletResponse _responseCall1;

  protected SipServletRequest _pendingUpdateCall2;

  protected Object syncLock = new Object();

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
  protected void doUpdate(SipServletRequest req, SIPCallImpl call, Map<String, String> headers) throws Exception {
    if (SIPHelper.getRawContentWOException(req) == null) {
      req.createResponse(200).send();
    }
    else if (_call2.equals(call)) {
      if (_response != null) {
        LOG.warn("Receive UPDATE after INVITE processed, return peer remote SDP.");
        SipServletResponse resp = req.createResponse(200);
        resp.setContent(_call1.getRemoteSdp(), "application/sdp");
        resp.send();
      }
      else {
        _pendingUpdateCall2 = req;
        reInviteCall1(req);
      }
    }
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
          if (SIPHelper.getRawContentWOException(res) != null && SIPHelper.needPrack(res)) {
            _reliable183Resp = res;
            reInviteCall1(res);
            _reInvited = true;
          }
          else {
            SIPHelper.trySendPrack(res);
          }
        }
        catch (Exception e) {
          done(JoinCompleteEvent.Cause.ERROR, e);
          _call2.fail(e);
          throw e;
        }
      }
      else {
        SIPHelper.trySendPrack(res);
      }
    }
    else if (SIPHelper.isProvisionalResponse(res) && _call1.equals(call)) {
      SIPHelper.trySendPrack(res);
    }
    else if (SIPHelper.isSuccessResponse(res)) {
      try {
        boolean successJoin = false;
        synchronized (syncLock) {
          if (_call2.equals(call)) {
            _response = res;
            if (!_reInvited) {
              reInviteCall1(res);
            }
            else if (_responseCall1 != null) {
              SipServletRequest ack2 = res.createAck();
              if (SIPHelper.getRawContentWOException(res) != null) {
                SIPHelper.copyContent(_responseCall1, ack2);
              }
              _call2.setSIPCallState(State.ANSWERED);
              ack2.send();
              successJoin = true;
            }
          }
          else if (_call1.equals(call)) {
            res.createAck().send();
            if (_response != null) {
              SipServletRequest ack2 = _response.createAck();
              if (SIPHelper.getRawContentWOException(_response) != null) {
                SIPHelper.copyContent(res, ack2);
              }
              _call2.setSIPCallState(State.ANSWERED);
              ack2.send();
              successJoin = true;
            }
            else {
              _responseCall1 = res;
              if (_pendingUpdateCall2 != null) {
                SipServletResponse updateResp = _pendingUpdateCall2.createResponse(200);
                SIPHelper.copyContent(res, updateResp);
                updateResp.send();
                _pendingUpdateCall2 = null;
              }
              else if (_reliable183Resp != null) {
                try {
                  SipServletRequest prack = _reliable183Resp.createPrack();
                  SIPHelper.copyContent(res, prack);
                  prack.send();
                }
                catch (Rel100Exception ex) {
                  LOG.warn(ex.getMessage());
                }
                catch (IllegalStateException ex) {
                  LOG.warn(ex.getMessage());
                }
              }
            }
          }
        }

        if (successJoin) {
          successJoin();
        }
      }
      catch (final Exception e) {
        done(JoinCompleteEvent.Cause.ERROR, e);
        _call2.fail(e);
        throw e;
      }
    }
  }

  private void successJoin() throws MsControlException {
    doDisengage(_call1, JoinType.DIRECT);
    _call1.linkCall(_call2, JoinType.DIRECT, _direction);
    done(JoinCompleteEvent.Cause.JOINED, null);
  }

  private void reInviteCall1(SipServletMessage message) throws Exception {
    final SipServletRequest req = _call1.getSipSession().createRequest("INVITE");
    SIPHelper.copyContent(message, req);
    req.send();
  }
}
