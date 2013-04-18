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
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectNO2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectNO2NOJoinDelegate.class);

  protected SipServletResponse _responseCall2;

  protected SipServletResponse _reliable183RespCall2;

  protected SipServletResponse _responseCall1;

  protected Direction _direction;

  protected boolean _invitedCall1;

  protected boolean _ackedCall2;

  protected boolean _ackedCall1;

  protected Object respSyncLock = new Object();

  protected DirectNO2NOJoinDelegate(final SIPOutgoingCall call1, final SIPOutgoingCall call2,
      final Direction direction, final SIPOutgoingCall peer) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
    _peer = peer;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    // TODO try to disable 183. should we disable 183 here?
    // should we use mock SDP (SDP connection address 0.0.0.0 or with sendonly
    // atrribute) to disable 183 here?
    SIPHelper.remove100relSupport(_call2.getSipInitnalRequest());
    SIPHelper.remove100relSupport(_call1.getSipInitnalRequest());
    ((SIPOutgoingCall) _call2).call(null);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      synchronized (respSyncLock) {
        if (_call2.equals(call)) {
          if (SIPHelper.isProvisionalResponse(res)) {
            call.setSIPCallState(SIPCall.State.ANSWERING);

            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
              if (SIPHelper.getRawContentWOException(res) != null && SIPHelper.needPrack(res)) {
                _reliable183RespCall2 = res;
                ((SIPOutgoingCall) _call1).setContinueRouting(_call2);
                ((SIPOutgoingCall) _call1).call(res.getRawContent(), _call2.getSipSession().getApplicationSession());
                _invitedCall1 = true;
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
            _responseCall2 = res;
            if (!_invitedCall1) {
              ((SIPOutgoingCall) _call1).setContinueRouting(_call2);
              ((SIPOutgoingCall) _call1).call(res.getRawContent(), _call2.getSipSession().getApplicationSession());
            }
            else if (_responseCall1 != null) {
              SipServletRequest ackCall2 = res.createAck();
              if (SIPHelper.getRawContentWOException(res) != null) {
                SIPHelper.copyContent(_responseCall1, ackCall2);
              }
              ackCall2.send();
              _ackedCall2 = true;
              _call2.setSIPCallState(State.ANSWERED);

              if (_ackedCall1) {
                successJoin();
              }
            }
          }
          else if (SIPHelper.isErrorResponse(res)) {
            Exception ex = getExceptionByResponse(res);
            done(getJoinCompleteCauseByResponse(res), ex);
            _call2.disconnect(true, getCallCompleteCauseByResponse(res), ex, null);
            _call1.disconnect(true, getCallCompleteCauseByResponse(res), ex, null);
          }
        }
        else if (_call1.equals(call)) {
          if (SIPHelper.isProvisionalResponse(res)) {
            call.setSIPCallState(SIPCall.State.ANSWERING);

            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
              if (SIPHelper.getRawContentWOException(res) != null) {
                _responseCall1 = res;
                if (_responseCall2 != null) {
                  SipServletRequest ack2 = _responseCall2.createAck();
                  if (SIPHelper.getRawContentWOException(_responseCall2) != null) {
                    SIPHelper.copyContent(res, ack2);
                  }
                  ack2.send();
                  _ackedCall2 = true;
                  _call2.setSIPCallState(State.ANSWERED);
                }
                else if (_reliable183RespCall2 != null) {
                  SipServletRequest call2Prack = _reliable183RespCall2.createPrack();
                  SIPHelper.copyContent(res, call2Prack);
                  call2Prack.send();
                }
              }
            }

            SIPHelper.trySendPrack(res);
          }
          else if (SIPHelper.isSuccessResponse(res)) {
            if (_responseCall1 == null) {
              _responseCall1 = res;
            }
            final SipServletRequest ack1 = res.createAck();
            ack1.send();
            _call1.setSIPCallState(State.ANSWERED);
            _ackedCall1 = true;

            if (!_ackedCall2) {
              if (_responseCall2 != null) {
                SipServletRequest ack2 = _responseCall2.createAck();
                if (SIPHelper.getRawContentWOException(_responseCall2) != null) {
                  SIPHelper.copyContent(res, ack2);
                }
                ack2.send();
                _call2.setSIPCallState(State.ANSWERED);
                _ackedCall2 = true;
                successJoin();
              }
              else if (_reliable183RespCall2 != null) {
                SipServletRequest call2Prack = _reliable183RespCall2.createPrack();
                SIPHelper.copyContent(res, call2Prack);
                call2Prack.send();
              }
            }
            else {
              successJoin();
            }
          }
          else if (SIPHelper.isErrorResponse(res)) {
            Exception ex = getExceptionByResponse(res);
            done(getJoinCompleteCauseByResponse(res), ex);
            _call1.disconnect(true, getCallCompleteCauseByResponse(res), ex, null);
            _call2.disconnect(true, getCallCompleteCauseByResponse(res), ex, null);
          }
        }
      }
    }
    catch (final Exception e) {
      done(JoinCompleteEvent.Cause.ERROR, e);
      _call1.fail(e);
      _call2.fail(e);
      throw e;
    }
  }

  private void successJoin() throws MsControlException {
    _call1.linkCall(_call2, JoinType.DIRECT, _direction);
    done(JoinCompleteEvent.Cause.JOINED, null);
  }
}
