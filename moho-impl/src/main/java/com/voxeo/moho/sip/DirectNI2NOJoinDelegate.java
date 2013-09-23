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

  protected Object _latestCall2SDP;
  
  protected boolean call1Processed;
  
  protected SipServletResponse _waitingPrackResponse;
  
  protected boolean _call1No100Rel;
  
  protected boolean updatedCall1Success;
  
  protected boolean call2Processed;

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
    if(!SIPHelper.support100rel(_call1.getSipInitnalRequest())) {
      _call1No100Rel =true;
      SIPHelper.remove100relSupport(_call2.getSipInitnalRequest());
    }
    ((SIPOutgoingCall) _call2).setContinueRouting(_call1);
    if (_call1.getSIPCallState() == SIPCall.State.PROGRESSED) {
      call1Processed = true;
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
      if (SIPHelper.getRawContentWOException(req) != null) {
        _latestCall2SDP = req.getContent();
      }
      if(_call1No100Rel) {
        SipServletResponse updateResp = req.createResponse(200);
        SIPHelper.copyContent(_call1.getSipInitnalRequest(), updateResp);
        updateResp.send();
      }
      else {
        SIPHelper.sendSubsequentRequest(_call1.getSipSession(), req, headers);
      }
    }
    else {
      LOG.error("Can't process UPDATE request:" + req);
    }
  }

  @Override
  protected void doUpdateResponse(SipServletResponse resp, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (_call1.equals(call)) {
      if(resp.getStatus() == SipServletResponse.SC_OK) {
        updatedCall1Success = true;
      }
        
      if(_waitingPrackResponse != null) {
        SipServletResponse prackResponse = _waitingPrackResponse;
        _waitingPrackResponse = null;
        if(resp.getStatus() == SipServletResponse.SC_OK) {
          final SipServletRequest newReq = prackResponse.createPrack();
          SIPHelper.addHeaders(newReq, headers);
          SIPHelper.copyContent(resp, newReq);
          newReq.send();
        }
        else {
          SipServletRequest prack = prackResponse.createPrack();
          prack.setContent(_call1.getRemoteSdp(), "application/sdp");
          prack.send();
        }
        _call1.destroyNetworkConnection();
      }
      else {
        SIPHelper.relayResponse(resp);
      }
    }
    else {
      LOG.error("Can't process UPDATE response, discarding it:" + resp);
    }
  }

  @Override
  protected void doPrack(SipServletRequest req, SIPCallImpl call, Map<String, String> headers) throws Exception {
    if (_call1.equals(call) && _waitingPrackResponse != null) {
      SipServletResponse prackResponse = _waitingPrackResponse;
      _waitingPrackResponse = null;
      SIPHelper.relayPrack(prackResponse, req, headers);
    }
    else {
      LOG.warn("Can't process PRACK, send back 200OK response:" + req);
      req.createResponse(200).send();
    }
  }

  @Override
  protected void doPrackResponse(SipServletResponse resp, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (_call2.equals(call)) {
      SIPHelper.relayResponse(resp);
    }
    else {
      LOG.warn("Didn't process PRACK response, discarding it:" + resp);
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
            _latestCall2SDP = res.getContent();
            
            if(SIPHelper.isProvisionalResponse(res) && SIPHelper.needPrack(res)) {
              call2Processed = true;
            }
          }
          
          if((call1Processed || _call1No100Rel) && SIPHelper.isProvisionalResponse(res)) {
            if(SIPHelper.getRawContentWOException(res) != null && SIPHelper.needPrack(res)) {
              if(call1Processed) {
                if(_waitingPrackResponse != null && _waitingPrackResponse.getHeader("RSeq").trim().equalsIgnoreCase(res.getHeader("RSeq").trim())) {
                  return;
                }
                _waitingPrackResponse = res;
                SipServletRequest updateCall1Req = _call1.getSipSession().createRequest("UPDATE");
                updateCall1Req.setContent(res.getContent(), "application/sdp");
                updateCall1Req.send();
              }
              else {
                SipServletRequest prack = res.createPrack();
                prack.setContent(_call1.getRemoteSdp(), "application/sdp");
                prack.send();
                relayUnreliableProvisionalResponse(res);
              }
            }
            else {
              SIPHelper.trySendPrack(res);
              if(!call1Processed) {
                relayUnreliableProvisionalResponse(res);
              }
            }
          }
          else {
            final SipServletResponse newRes = _call1.getSipInitnalRequest().createResponse(res.getStatus(),
                res.getReasonPhrase());

            // TODO should do this at container?
            if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS || res.getStatus() == SipServletResponse.SC_OK) {
              SIPHelper.copyPandXHeaders(res, newRes);
            }

            if(SIPHelper.isProvisionalResponse(res) && SIPHelper.needPrack(res)) {
              try{
                if(_waitingPrackResponse != null && _waitingPrackResponse.getHeader("RSeq").trim().equalsIgnoreCase(res.getHeader("RSeq").trim())) {
                  return;
                }
                _waitingPrackResponse = res;
                SIPHelper.copyContent(res, newRes);
                newRes.sendReliably();
              }
              catch(Exception ex) {
                LOG.warn("Got exception when trying send 183 reliably. trying send back PRACK.", ex);
                res.createPrack().send();
                
                newRes.send();
              }
            }
            else {
              if(res.getStatus() == SipServletResponse.SC_OK && SIPHelper.getRawContentWOException(newRes) == null && !call1Processed) {
                newRes.setContent(_latestCall2SDP, "application/sdp");
              }
              newRes.send();
            }
          }
        }
        else if (SIPHelper.isErrorResponse(res)) {
          Exception ex = getExceptionByResponse(res);
          done(getJoinCompleteCauseByResponse(res), ex);
          disconnectCall(_call2, true,  getCallCompleteCauseByResponse(res), ex);
        }
      }
      else if (_call1.equals(call)) {
        if (SIPHelper.isSuccessResponse(res)) {
          res.createAck().send();
          try {
            final SipServletRequest ack2 = _response.createAck();
            if (call1Processed && !call2Processed) {
              ack2.setContent(_call1.getRemoteSdp(), "application/sdp");
            }
            ack2.send();
            _call2.setSIPCallState(State.ANSWERED);
            successJoin();
          }
          catch (final Exception e) {
            done(JoinCompleteEvent.Cause.ERROR, e);
            failCall(_call1, e);
            failCall(_call2, e);
            throw e;
          }
        }
        else if (SIPHelper.isProvisionalResponse(res)) {
          SIPHelper.trySendPrack(res);
        }
        else {
          try {
            _response.createAck().send();
            _call2.setSIPCallState(State.ANSWERED);
          }
          catch(Exception ex) {
            LOG.warn("Exception when sending ACK.", ex);
          }
          
          Exception ex = getExceptionByResponse(res);
          done(getJoinCompleteCauseByResponse(res), ex);
          disconnectCall(_call2, true, getCallCompleteCauseByResponse(res), ex);
        }
      }
    }
    catch (final Exception e) {
      done(JoinCompleteEvent.Cause.ERROR, e);
      failCall(_call1, e);
      failCall(_call2, e);
      throw e;
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    if (_call1.equals(call)) {
      _call1.setSIPCallState(State.ANSWERED);
      if (!call1Processed || (call1Processed && updatedCall1Success)) {
        try {
          final SipServletRequest ack = _response.createAck();
          if (call1Processed && !call2Processed) {
            ack.setContent(_call1.getRemoteSdp(), "application/sdp");
          }
          ack.send();
          _call2.setSIPCallState(State.ANSWERED);
          successJoin();
        }
        catch (final Exception e) {
          done(JoinCompleteEvent.Cause.ERROR, e);
          failCall(_call1, e);
          failCall(_call2, e);
          throw e;
        }
      }
      else {
        // re-INVITE call1
        SipServletRequest reInvite = _call1.getSipSession().createRequest("INVITE");
        reInvite.setContent(_latestCall2SDP, "application/sdp");
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
  
  private void relayUnreliableProvisionalResponse(SipServletResponse res) throws IOException {
    final SipServletResponse newRes = _call1.getSipInitnalRequest().createResponse(res.getStatus(),
        res.getReasonPhrase());
    SIPHelper.copyContent(res, newRes);
    newRes.send();
  }
}
