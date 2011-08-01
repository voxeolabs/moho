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

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectNO2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectNO2NOJoinDelegate.class);

  protected SIPOutgoingCall _call1;

  protected SIPOutgoingCall _call2;

  protected SipServletResponse _response;

  protected Direction _direction;

  protected boolean _invitedCall1;

  protected boolean _ackedCall2;

  protected DirectNO2NOJoinDelegate(final SIPOutgoingCall call1, final SIPOutgoingCall call2, final Direction direction) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
  }

  @Override
  protected void doJoin() throws MsControlException, IOException {
    _call2.call(null);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (_call2.equals(call)) {
        if (SIPHelper.isProvisionalResponse(res)) {
          call.setSIPCallState(SIPCall.State.ANSWERING);

          if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
            if (SIPHelper.getRawContentWOException(res) != null) {
              _call1.call(res.getRawContent(), _call2.getSipSession().getApplicationSession());
              _invitedCall1 = true;
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
          _response = res;
          if (!_invitedCall1) {
            _call1.call(res.getRawContent(), _call2.getSipSession().getApplicationSession());
          }
        }
        else if (SIPHelper.isErrorResponse(res)) {
          setException(getExceptionByResponse(res));
          _call1.disconnect(true, this.getCallCompleteCauseByResponse(res), this.getExceptionByResponse(res), null);
          _call2.disconnect(true, this.getCallCompleteCauseByResponse(res), this.getExceptionByResponse(res), null);
        }
      }
      else if (_call1.equals(call)) {
        if (SIPHelper.isProvisionalResponse(res)) {
          call.setSIPCallState(SIPCall.State.ANSWERING);

          if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
            if (SIPHelper.getRawContentWOException(res) != null) {
              SipServletRequest ack2 = null;
              if (_response != null) {
                final SipServletResponse origRes = _response;
                _response = null;
                ack2 = origRes.createAck();
                SIPHelper.copyContent(res, ack2);
              }
              ack2.send();

              _ackedCall2 = true;
            }
            try {
              res.createPrack().send();
            }
            catch (Rel100Exception ex) {
              LOG.warn("", ex);
            }
          }
        }
        else if (SIPHelper.isSuccessResponse(res)) {
          final SipServletRequest ack1 = res.createAck();
          ack1.send();

          if (!_ackedCall2) {
            SipServletRequest ack2 = null;
            if (_response != null) {
              final SipServletResponse origRes = _response;
              _response = null;
              ack2 = origRes.createAck();
              SIPHelper.copyContent(res, ack2);
            }
            ack2.send();
          }

          _call2.setSIPCallState(State.ANSWERED);
          _call1.setSIPCallState(State.ANSWERED);
          _call1.linkCall(_call2, JoinType.DIRECT, _direction);
          done();
        }
        else if (SIPHelper.isErrorResponse(res)) {
          setException(getExceptionByResponse(res));
          _call1.disconnect(true, this.getCallCompleteCauseByResponse(res), this.getExceptionByResponse(res), null);
          _call2.disconnect(true, this.getCallCompleteCauseByResponse(res), this.getExceptionByResponse(res), null);
        }
      }
    }
    catch (final Exception e) {
      setError(e);
      _call1.fail(e);
      _call2.fail(e);
      throw e;
    }

  }
}
