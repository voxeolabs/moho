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
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectNI2NOJoinDelegate extends JoinDelegate {
  private static final Logger LOG = Logger.getLogger(DirectAI2NOJoinDelegate.class);

  protected Direction _direction;

  protected SipServletResponse _response;

  protected SipServletResponse _responseWithSDP;

  protected boolean call1Processed;

  protected DirectNI2NOJoinDelegate(final SIPIncomingCall call1, final SIPOutgoingCall call2,
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
    if (_call1.getSIPCallState() == SIPCall.State.PROGRESSED) {
      call1Processed = true;
      SIPHelper.remove100relSupport(_call2.getSipInitnalRequest());
      ((SIPOutgoingCall) _call2).call(null, _call1.getSipSession().getApplicationSession());
    }
    else {
      ((SIPOutgoingCall) _call2).call(_call1.getRemoteSdp(), _call1.getSipSession().getApplicationSession(),
          _call1.useReplacesHeader());
    }
  }

  @Override
  protected void doUpdate(SipServletRequest req, SIPCallImpl call, Map<String, String> headers) throws Exception {
    if (_call2.equals(call)) {
      SIPHelper.sendSubsequentRequest(_call1.getSipSession(), req, headers);
    }
    else {
      LOG.error("Can't process UPDATE request:" + req);
    }
  }

  @Override
  protected void doUpdateResponse(SipServletResponse resp, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (_call1.equals(call)) {
      SIPHelper.relayResponse(resp);
    }
    else {
      LOG.error("Can't process UPDATE response, discarding it:" + resp);
    }
  }

  @Override
  protected void doPrack(SipServletRequest req, SIPCallImpl call, Map<String, String> headers) throws Exception {
    if (_call1.equals(call)) {
      SIPHelper.sendSubsequentRequest(_call2.getSipSession(), req, headers);
    }
    else {
      LOG.error("Can't process PRACK request:" + req);
    }
  }

  @Override
  protected void doPrackResponse(SipServletResponse resp, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (_call2.equals(call)) {
      SIPHelper.relayResponse(resp);
    }
    else {
      LOG.error("Can't process PRACK response, discarding it:" + resp);
    }
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (_call2.equals(call)) {
        if (SIPHelper.isSuccessResponse(res) || SIPHelper.isProvisionalResponse(res)) {
          _response = res;
          if (SIPHelper.getRawContentWOException(res) != null) {
            _responseWithSDP = res;
          }
          
          if(res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS && call1Processed && SIPHelper.needPrack(res)) {
            SipServletRequest prack = res.createPrack();
            prack.setContent(_call1.getRemoteSdp(), "application/sdp");
            prack.send();
          }
          else {
            final SipServletResponse newRes = _call1.getSipInitnalRequest().createResponse(res.getStatus(),
                res.getReasonPhrase());
            SIPHelper.copyContent(res, newRes);

            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS && SIPHelper.needPrack(res)) {
              newRes.addHeader("Require", "100rel");
            }

            // TODO should do this at container?
            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS || res.getStatus() == SipServletResponse.SC_OK) {
              SIPHelper.copyPandXHeaders(res, newRes);
            }

            newRes.send();
          }
        }
        else if (SIPHelper.isErrorResponse(res)) {
          Exception ex = getExceptionByResponse(res);
          done(getJoinCompleteCauseByResponse(res), ex);
          _call2.disconnect(true, getCallCompleteCauseByResponse(res), ex, null);
        }
      }
      else if (_call1.equals(call)) {
        if (SIPHelper.isSuccessResponse(res)) {
          res.createAck().send();
          try {
            final SipServletRequest ack2 = _response.createAck();
            if (SIPHelper.getRawContentWOException(_response) != null) {
              ack2.setContent(_call1.getRemoteSdp(), "application/sdp");
            }
            ack2.send();
            _call2.setSIPCallState(State.ANSWERED);
            successJoin();
          }
          catch (final Exception e) {
            done(JoinCompleteEvent.Cause.ERROR, e);
            _call1.fail(e);
            _call2.fail(e);
            throw e;
          }
        }
        else if (SIPHelper.isProvisionalResponse(res)) {
          SIPHelper.trySendPrack(res);
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

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    if (_call1.equals(call)) {
      _call1.setSIPCallState(State.ANSWERED);
      if (!call1Processed) {
        try {
          final SipServletRequest ack = _response.createAck();
          if (SIPHelper.getRawContentWOException(_response) != null) {
            SIPHelper.copyContent(req, ack);
          }
          ack.send();
          _call2.setSIPCallState(State.ANSWERED);
          successJoin();
        }
        catch (final Exception e) {
          done(JoinCompleteEvent.Cause.ERROR, e);
          _call1.fail(e);
          _call2.fail(e);
          throw e;
        }
      }
      else {
        // re-INVITE call1
        SipServletRequest reInvite = _call1.getSipSession().createRequest("INVITE");
        SIPHelper.copyContent(_responseWithSDP, reInvite);
        reInvite.send();
      }
    }
  }

  protected void successJoin() throws Exception {
    doDisengage(_call1, JoinType.DIRECT);
    _call1.linkCall(_call2, JoinType.DIRECT, _direction);
    _response = null;
    done(JoinCompleteEvent.Cause.JOINED, null);
  }
}
