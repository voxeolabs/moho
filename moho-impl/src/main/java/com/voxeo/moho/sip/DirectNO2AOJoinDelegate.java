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

public class DirectNO2AOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectNO2AOJoinDelegate.class);

  protected SipServletResponse _response;

  protected SipServletResponse _reliable183Resp;

  protected Direction _direction;

  protected boolean _reInvited;

  protected SipServletResponse _responseCall2;

  protected SipServletRequest _pendingUpdateCall1;

  protected Object syncLock = new Object();

  protected DirectNO2AOJoinDelegate(final SIPOutgoingCall call1, final SIPOutgoingCall call2,
      final Direction direction, final SIPCallImpl peer) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
    _peer = peer;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    ((SIPOutgoingCall) _call1).call(null, _call2.getSipSession().getApplicationSession());
  }

  @Override
  protected void doUpdate(SipServletRequest req, SIPCallImpl call, Map<String, String> headers) throws Exception {
    if (SIPHelper.getRawContentWOException(req) == null) {
      req.createResponse(200).send();
    }
    else if (_call1.equals(call)) {
      if (_response != null) {
        LOG.warn("Receive UPDATE after INVITE processed, return peer remote SDP.");
        SipServletResponse resp = req.createResponse(200);
        resp.setContent(_call2.getRemoteSdp(), "application/sdp");
        resp.send();
      }
      else {
        _pendingUpdateCall1 = req;
        reInviteCall2(req);
      }
    }
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      boolean successJoin = false;
      synchronized (syncLock) {
        if (_call1.equals(call)) {
          if (SIPHelper.isProvisionalResponse(res)) {
            _call1.setSIPCallState(SIPCall.State.ANSWERING);

            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
              if (SIPHelper.getRawContentWOException(res) != null && SIPHelper.needPrack(res)) {
                _reliable183Resp = res;
                reInviteCall2(res);
                _reInvited = true;
              }
              else {
                SIPHelper.trySendPrack(res);
              }
            }
            else {
              SIPHelper.trySendPrack(res);
            }
          }
          else if (SIPHelper.isSuccessResponse(res)) {
            _response = res;
            if (!_reInvited) {
              reInviteCall2(res);
            }
            else if (_responseCall2 != null) {
              SipServletRequest ack1 = res.createAck();
              if (SIPHelper.getRawContentWOException(res) != null) {
                SIPHelper.copyContent(_responseCall2, ack1);
              }
              _call1.setSIPCallState(State.ANSWERED);
              ack1.send();
              successJoin = true;
            }
          }
          else if (SIPHelper.isErrorResponse(res)) {
            Exception ex = getExceptionByResponse(res);
            done(getJoinCompleteCauseByResponse(res), ex);
            disconnectCall(_call1, true, getCallCompleteCauseByResponse(res), ex);
          }
        }
        else if (_call2.equals(call)) {
          if (SIPHelper.isSuccessResponse(res)) {
            res.createAck().send();

            if (_response != null) {
              SipServletRequest ack1 = _response.createAck();
              if (SIPHelper.getRawContentWOException(_response) != null) {
                SIPHelper.copyContent(res, ack1);
              }
              ack1.send();
              _call1.setSIPCallState(State.ANSWERED);
              successJoin = true;
            }
            else {
              _responseCall2 = res;
              if (_pendingUpdateCall1 != null) {
                SipServletResponse updateResp = _pendingUpdateCall1.createResponse(200);
                SIPHelper.copyContent(res, updateResp);
                updateResp.send();
                _pendingUpdateCall1 = null;
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
          else if (SIPHelper.isProvisionalResponse(res)) {
            SIPHelper.trySendPrack(res);
          }
          else if (SIPHelper.isErrorResponse(res)) {
            Exception ex = getExceptionByResponse(res);
            done(getJoinCompleteCauseByResponse(res), ex);
            disconnectCall(_call1, true, getCallCompleteCauseByResponse(res), ex);
          }
        }
      }
      if (successJoin) {
        successJoin();
      }
    }
    catch (final Exception e) {
      done(JoinCompleteEvent.Cause.ERROR, e);
      failCall(_call1, e);
      throw e;
    }
  }

  private void successJoin() throws MsControlException {
    doDisengage(_call2, JoinType.DIRECT);
    _call1.linkCall(_call2, JoinType.DIRECT, _direction);
    done(JoinCompleteEvent.Cause.JOINED, null);
  }

  private void reInviteCall2(SipServletMessage res) throws Exception {
    final SipServletRequest req = _call2.getSipSession().createRequest("INVITE");
    SIPHelper.copyContent(res, req);
    req.send();
  }
}
