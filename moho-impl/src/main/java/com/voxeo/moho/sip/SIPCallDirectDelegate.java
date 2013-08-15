/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.MsControlException;
import javax.sdp.SdpException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.sip.SIPCallImpl.HoldState;

public class SIPCallDirectDelegate extends SIPCallDelegate {

  private static final Logger LOG = Logger.getLogger(SIPCallDirectDelegate.class);

  private static final String REINVITE_PEER_RES = "com.voxeo.moho.reinvite-peer-res";

  protected SIPCallDirectDelegate() {
    super();
  }

  @Override
  protected void handleAck(final SIPCallImpl call, final SipServletRequest req) throws Exception {
    final SipServletResponse res = (SipServletResponse) req.getSession().getAttribute(REINVITE_PEER_RES);
    final SipServletRequest newReq = res.createAck();
    SIPHelper.copyContent(req, newReq);
    newReq.send();
  }

  @Override
  protected void handleReinvite(final SIPCallImpl call, final SipServletRequest req, final Map<String, String> headers)
      throws Exception {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();
    final SipServletRequest newReq = peer.getSipSession().createRequest(req.getMethod());
    SIPHelper.addHeaders(newReq, headers);
    SIPHelper.copyContent(req, newReq);
    SIPHelper.linkSIPMessage(req, newReq);
    newReq.send();
  }
  
  @Override
  protected void handleUpdate(final SIPCallImpl call, final SipServletRequest req, final Map<String, String> headers)
      throws Exception {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();
    final SipServletRequest newReq = peer.getSipSession().createRequest(req.getMethod());
    SIPHelper.addHeaders(newReq, headers);
    SIPHelper.copyContent(req, newReq);
    SIPHelper.linkSIPMessage(req, newReq);
    newReq.send();
  }
  
  @Override
  protected void handleUpdateResponse(final SIPCallImpl call, final SipServletResponse res,
      final Map<String, String> headers)
      throws Exception {
    try {
      final SipServletRequest req = res.getRequest();
      final SipServletRequest newReq = (SipServletRequest) SIPHelper.getLinkSIPMessage(req);
      if (newReq != null) {
        SIPHelper.unlinkSIPMessage(req);
        final SipServletResponse newRes = newReq.createResponse(res.getStatus(), res.getReasonPhrase());
        SIPHelper.addHeaders(newRes, headers);
        SIPHelper.copyContent(res, newRes);
        newRes.send();
      }
      else{
        LOG.warn("Can't find linked request, discarding this response:"+ res);
      }
    }
    catch (final Exception e) {
      LOG.warn("", e);
      return;
    }
  }

  @Override
  protected void handleReinviteResponse(final SIPCallImpl call, final SipServletResponse res,
      final Map<String, String> headers) {

    if (res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_HOLD_REQUEST) != null
        || res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_UNHOLD_REQUEST) != null
        || res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_DEAF_REQUEST) != null) {
      try {
        res.createAck().send();
        if (call.getHoldState() == HoldState.Holding) {
          call.setHoldState(HoldState.Held);
        }
        else if (call.getHoldState() == HoldState.UnHolding) {
          call.setHoldState(HoldState.None);
        }
        else if (call.getDeafState() == HoldState.Deafing) {
          call.setDeafState(HoldState.Deafed);
        }
        else if (call.getDeafState() == HoldState.Undeafing) {
          call.setDeafState(HoldState.None);
        }
      }
      catch (IOException e) {
        LOG.error("IOException when sending back ACK.", e);
        call.setHoldState(HoldState.None);
        call.setDeafState(HoldState.None);
        call.fail(e);
      }
      finally {
        call.notify();
      }
    }
    else if (res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_MUTE_REQUEST) != null
        || res.getRequest().getAttribute(SIPCallDelegate.SIPCALL_UNMUTE_REQUEST) != null) {
      // send ACK.
      try {
        res.createAck().send();

        // send the received SDP to peer.
        final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();
        synchronized (peer) {
          if (call.getMuteState() == HoldState.Muting) {
            peer.setDeafState(HoldState.Deafing);
          }
          else {
            peer.setDeafState(HoldState.Undeafing);
          }

          Map<String, String> attributes = new HashMap<String, String>();
          attributes.put(SIPCallDelegate.SIPCALL_DEAF_REQUEST, "true");
          peer.reInviteRemote(res.getRawContent(), null, attributes);

          while (peer.getDeafState() != HoldState.Deafed && peer.getDeafState() != HoldState.None) {
            try {
              peer.wait();
            }
            catch (InterruptedException e) {
              LOG.warn("InterruptedException when wait make peer deaf, the peer's DeafState " + peer.getDeafState());
            }
          }

          // set call deaf state
          if (call.getMuteState() == HoldState.Muting) {
            peer.setDeafState(HoldState.Deafed);
            call.setMuteState(HoldState.Muted);
          }
          else if (call.getMuteState() == HoldState.UnMuting) {
            peer.setDeafState(HoldState.None);
            call.setMuteState(HoldState.None);
          }
        }
      }
      catch (IOException e1) {
        LOG.error("IOException", e1);
        call.setMuteState(HoldState.None);
        call.fail(e1);
      }
      finally {
        call.notify();
      }
    }
    else {
      try {
        final SipServletRequest req = res.getRequest();
        final SipServletRequest newReq = (SipServletRequest) SIPHelper.getLinkSIPMessage(req);
        if (newReq != null) {
          SIPHelper.unlinkSIPMessage(req);
          final SipServletResponse newRes = newReq.createResponse(res.getStatus(), res.getReasonPhrase());
          SIPHelper.addHeaders(newRes, headers);
          SIPHelper.copyContent(res, newRes);
          if (SIPHelper.isReinvite(newRes)) {
            newRes.getSession().setAttribute(REINVITE_PEER_RES, res);
          }
          newRes.send();
        }
      }
      catch (final Exception e) {
        LOG.warn("", e);
        return;
      }
    }
  }

  @Override
  protected void hold(SIPCallImpl call, boolean send) throws MsControlException, IOException, SdpException {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_HOLD_REQUEST, "true");
    call.reInviteRemote(createSendonlySDP(call, peer.getRemoteSdp()), null, attributes);

    peer.hold();
  }

  @Override
  protected void mute(SIPCallImpl call) throws IOException, SdpException {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_MUTE_REQUEST, "true");
    call.reInviteRemote(createSendonlySDP(call, peer.getRemoteSdp()), null, attributes);
  }

  @Override
  protected void unhold(SIPCallImpl call) throws MsControlException, IOException, SdpException {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();

    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_UNHOLD_REQUEST, "true");
    call.reInviteRemote(createSendrecvSDP(call, peer.getRemoteSdp()), null, attributes);
    
    peer.unhold();
  }

  @Override
  protected void unmute(SIPCallImpl call) throws IOException, SdpException {
    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();
    
    Map<String, String> attributes = new HashMap<String, String>();
    attributes.put(SIPCallDelegate.SIPCALL_UNMUTE_REQUEST, "true");
    call.reInviteRemote(createSendrecvSDP(call, peer.getRemoteSdp()), null, attributes);
  }
}
