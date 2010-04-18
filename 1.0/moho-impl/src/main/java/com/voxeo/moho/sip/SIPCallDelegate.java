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
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.ExecutionContext;

public abstract class SIPCallDelegate {

  protected static final String SIPCALL_HOLD_REQUEST = "SIPCALL_HOLD_REQUEST";

  protected static final String SIPCALL_MUTE_REQUEST = "SIPCALL_MUTE_REQUEST";

  protected static final String SIPCALL_UNHOLD_REQUEST = "SIPCALL_UNHOLD_REQUEST";

  protected static final String SIPCALL_UNMUTE_REQUEST = "SIPCALL_UNMUTE_REQUEST";

  protected static final String SIPCALL_DEAF_REQUEST = "SIPCALL_DEAF_REQUEST";

  protected abstract void handleAck(SIPCallImpl call, SipServletRequest req) throws Exception;

  protected abstract void handleReinvite(SIPCallImpl call, SipServletRequest req, final Map<String, String> headers)
      throws Exception;

  protected void handleReinviteResponse(final SIPCallImpl call, final SipServletResponse res,
      final Map<String, String> headers) throws Exception {
    throw new UnsupportedOperationException();
  }

  protected void handleSdpEvent(final SIPCallImpl call, final SdpPortManagerEvent event) throws Exception {
    throw new UnsupportedOperationException();
  }

  protected void hold(final SIPCallImpl call) throws MsControlException, IOException, SdpException {
    throw new UnsupportedOperationException();
  }

  protected void unhold(final SIPCallImpl call) throws MsControlException, IOException, SdpException {
    throw new UnsupportedOperationException();
  }

  protected void mute(final SIPCallImpl call) throws IOException, SdpException {
    throw new UnsupportedOperationException();
  }

  protected void unmute(final SIPCallImpl call) throws IOException, SdpException {
    throw new UnsupportedOperationException();
  }

  protected SessionDescription createSendonlySDP(final SIPCallImpl call, final byte[] sdpByte)
      throws UnsupportedEncodingException, SdpException {
    SessionDescription sd = ((ExecutionContext) call.getApplicationContext()).getSdpFactory().createSessionDescription(
        new String(sdpByte, "iso8859-1"));

    sd.removeAttribute("sendrecv");
    sd.removeAttribute("recvonly");

    MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
    md.removeAttribute("sendrecv");
    md.removeAttribute("recvonly");
    md.setAttribute("sendonly", null);

    return sd;
  }

  protected SessionDescription createSendrecvSDP(final SIPCallImpl call, final byte[] sdpByte)
      throws UnsupportedEncodingException, SdpException {
    SessionDescription sd = ((ExecutionContext) call.getApplicationContext()).getSdpFactory().createSessionDescription(
        new String(sdpByte, "iso8859-1"));

    sd.removeAttribute("sendonly");
    sd.removeAttribute("recvonly");

    MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
    md.removeAttribute("sendonly");
    md.removeAttribute("recvonly");
    md.setAttribute("sendrecv", null);

    return sd;
  }
}
