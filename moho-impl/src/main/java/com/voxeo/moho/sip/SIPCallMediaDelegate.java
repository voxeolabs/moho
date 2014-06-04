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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.sip.SIPCallImpl.HoldState;
import com.voxeo.moho.util.SDPUtils;

public class SIPCallMediaDelegate extends SIPCallDelegate {

  private static final Logger LOG = Logger.getLogger(SIPCallMediaDelegate.class);

  protected SipServletRequest _req;

  protected SipServletResponse _res;

  protected boolean _isWaiting;
  
  protected boolean _isUpdateWaiting;

  protected SIPCallMediaDelegate() {
    super();
  }

  @Override
  protected void handleAck(final SIPCallImpl call, final SipServletRequest req) throws Exception {
    try {
      if (call.getHoldState() == HoldState.None) {
        call.processSDPAnswer(req);
        _isWaiting = false;
        call.notifyAll();
      }
    }
    catch (final Exception e) {
      LOG.error("Exception", e);
      call.fail(e);
    }
  }

  @Override
  protected void handleReinvite(final SIPCallImpl call, final SipServletRequest req, final Map<String, String> headers)
      throws Exception {
    _req = req;

    if (call.getHoldState() == HoldState.None) {
      _isWaiting = true;
      call.processSDPOffer(req);
    }
  }

  @Override
  protected void handleReinviteResponse(SIPCallImpl call, SipServletResponse res, Map<String, String> headers)
      throws Exception {
    _res = res;

    if (res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_HOLD_REQUEST) != null
        || res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_UNHOLD_REQUEST) != null) {
      try {
        _res.createAck().send();
        call.holdResp();
      }
      catch (IOException e) {
        LOG.error("IOException when sending ACK", e);
        call.setHoldState(HoldState.None);
        call.notify();
        call.fail(e);
      }
    }
    else if (res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_MUTE_REQUEST) != null
        || res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_UNMUTE_REQUEST) != null) {
      try {
        _res.createAck().send();
        if (call.getMuteState() == HoldState.Muting) {
          call.setMuteState(HoldState.Muted);
        }
        else if (call.getMuteState() == HoldState.UnMuting) {
          call.setMuteState(HoldState.None);
        }
      }
      catch (IOException e) {
        LOG.error("IOException when sending ACK", e);
        call.setHoldState(HoldState.None);
        call.fail(e);
      }
      finally {
        call.notify();
      }
    }
    else {
      call.processSDPOffer(res);
    }
  }
  
  @Override
  protected void handleUpdate(final SIPCallImpl call, final SipServletRequest req, final Map<String, String> headers)
      throws Exception {
    if(req.getRawContent() != null){
      _isUpdateWaiting = true;
      _req = req;
      call.processSDPOffer(req);
    }
    else{
      req.createResponse(SipServletResponse.SC_OK).send();
    }
  }

  @Override
  protected void handleUpdateResponse(SIPCallImpl call, SipServletResponse res, Map<String, String> headers)
      throws Exception {
     if(SIPHelper.isSuccessResponse(res) && SIPHelper.getRawContentWOException(res) != null){
       call.processSDPOffer(res);
     }
  }

  @Override
  protected void handleSdpEvent(final SIPCallImpl call, final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if(_isUpdateWaiting){
        if(event.isSuccessful()){
          byte[] sdp = event.getMediaServerSdp();
          call.setLocalSDP(sdp);
          try {
            final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
            res.setContent(SDPUtils.formulateSDP(call, sdp), "application/sdp");
            res.send();
          }
          catch (final Exception e) {
            LOG.error("Exception when sending back response for UPDATE", e);
            call.fail(e);
          }
        }
        else{
          LOG.warn("Failed to process UPDATE request, got failure SdpPortManagerEvent:"+event);
          try {
            _req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, event.getErrorText()).send();
          }
          catch (IOException e) {
            LOG.error("Exception when sending back error response for UPDATE", e);
            call.fail(e);
          }
        }
        _isUpdateWaiting = false;
        return;
      }
      else if(call.isSendingUpdate()){
        // do nothing
        return;
      }
      
      if (call.isHoldingProcess()) {
        call.holdResp();
      }
      else {
        if (event.isSuccessful()) {
          byte[] sdp = event.getMediaServerSdp();

          SessionDescription sessionDescription = null;
          try {
            String remoteSdp = new String(call.getRemoteSdp(), "iso8859-1");
            // TODO improve the parse.
            if (remoteSdp.indexOf("sendonly") > 0) {
              sessionDescription = createRecvonlySDP(call, sdp);
              sdp = sessionDescription.toString().getBytes("iso8859-1");
            }
          }
          catch (UnsupportedEncodingException e1) {
            LOG.error("", e1);
          }
          catch (SdpException e) {
            LOG.error("", e);
          }

          call.setLocalSDP(sdp);
          try {
            final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
            res.setContent(SDPUtils.formulateSDP(call, sdp), "application/sdp");
            res.send();
          }
          catch (final Exception e) {
            LOG.error("Exception when sending back response", e);
            call.fail(e);
          }
        }
        else {
          SIPHelper.handleErrorSdpPortManagerEvent(event, _req);
          call.fail(new NegotiateException(event));
        }
      }

    }
  }

  @Override
  protected void hold(SIPCallImpl call, boolean send) throws MsControlException, IOException, SdpException {
    ((NetworkConnection) call.getMediaObject()).getSdpPortManager().processSdpOffer(
        send ? createRecvonlySDP(call, call.getRemoteSdp()).toString().getBytes() : createSendonlySDP(call,
            call.getRemoteSdp()).toString().getBytes());
    call.getMediaService(true);

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_HOLD_REQUEST, "true");
    call.reInviteRemote(createSendonlySDP(call, call.getLocalSDP()), null, attributes);
  }

  @Override
  protected void mute(SIPCallImpl call) throws  IOException, SdpException {
    call.getMediaService(true);

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_MUTE_REQUEST, "true");
    call.reInviteRemote(createSendonlySDP(call, call.getLocalSDP()), null, attributes);
  }

  @Override
  protected void unhold(SIPCallImpl call) throws MsControlException, IOException, SdpException {
    ((NetworkConnection) call.getMediaObject()).getSdpPortManager().processSdpOffer(
        createSendrecvSDP(call, call.getRemoteSdp()).toString().getBytes());

    call.getMediaService(true);
    
    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_UNHOLD_REQUEST, "true");
    call.reInviteRemote(createSendrecvSDP(call, call.getLocalSDP()), null, attributes);
  }

  @Override
  protected void unmute(SIPCallImpl call) throws  IOException, SdpException {
    call.getMediaService(true);
    
    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_UNMUTE_REQUEST, "true");
    call.reInviteRemote(createSendrecvSDP(call, call.getLocalSDP()), null, attributes);
  }
}
