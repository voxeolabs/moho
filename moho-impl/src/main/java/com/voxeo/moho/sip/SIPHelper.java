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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public class SIPHelper {

  private static final Logger LOG = Logger.getLogger(SIPHelper.class);

  private static final String LINKED_MESSAGE = "linked.message";

  public static SipServletRequest createSipInitnalRequest(final SipFactory factory, final String method,
      final Address from, final Address to, final Map<String, String> headers, SipApplicationSession applicationSession, SipServletRequest origRequest) {
    SipServletRequest req = null;
    if (origRequest != null) {
      LOG.debug("Continue routing from orig req:" + origRequest);
      req = origRequest.getB2buaHelper().createRequest(origRequest);
      try {
        req.setContent(null, null);
      }
      catch (UnsupportedEncodingException e) {
        LOG.error("", e);
      }
      req.removeHeader("Content-Type");
      Address reqFrom = req.getFrom();
      if(from.getDisplayName() != null){
        reqFrom.setDisplayName(from.getDisplayName());
      }
      reqFrom.setURI(from.getURI());
      for (final Iterator<String> names = from.getParameterNames(); names.hasNext(); ) {
        String name =  names.next();
        if(name.equalsIgnoreCase("tag")){
          continue;
        }
        reqFrom.setParameter(name, from.getParameter(name));
      }
      
      Address reqTo = req.getTo();
      if(to.getDisplayName() != null){
        reqTo.setDisplayName(to.getDisplayName());
      }
      reqTo.setURI(to.getURI());
      for (final Iterator<String> names = to.getParameterNames(); names.hasNext(); ) {
        String name =  names.next();
        if(name.equalsIgnoreCase("tag")){
          continue;
        }
        reqTo.setParameter(name, to.getParameter(name));
      }
    }
    else {
      req = factory.createRequest(applicationSession != null ? applicationSession : factory.createApplicationSession(),
          method, from, to);
    }
    
    req.setRequestURI(to.getURI());
    URI ruri = req.getRequestURI();
    if (ruri instanceof SipURI) {
      SipURI sruri = (SipURI)ruri;
      if (sruri.getUserParam() == null) {
        sruri.setUserParam("phone");
        req.setRequestURI(sruri);
      }
    }
    if (headers != null) {
      for (final Map.Entry<String, String> e : headers.entrySet()) {
        if (e.getKey().equalsIgnoreCase("Route")) {
          try {
          String[] values = e.getValue().split("\\|\\|\\|");
          int length = values.length;
          for (int i = length-1; i >= 0; i--) {
            LOG.debug("route["+i+"]: "+values[i]);
            try {
              req.pushRoute(factory.createAddress(values[i]));
            }
            catch (ServletParseException ex) {
              LOG.error("Invalid Route Header: " + values[i]);
            }              
          }
          }
          catch(PatternSyntaxException ex) {
            LOG.error(ex);
          }
        }
      }
      headers.remove("Route");
      headers.remove("route");    
    }
    SIPHelper.addHeaders(req, headers);
    return req;
  }

  public static void addHeaders(final SipServletMessage message, final Map<String, String> headers) {
    if (headers != null) {
      for (final Map.Entry<String, String> e : headers.entrySet()) {
        message.addHeader(e.getKey(), e.getValue());
      }
    }
  }

  public static byte[] getRawContentWOException(final SipServletMessage msg) {
    try {
      return msg.getRawContent();
    }
    catch (final IOException e) {
      LOG.warn("", e);
      return null;
    }
  }

  public static void copyContent(final SipServletMessage source, final SipServletMessage target) {
    try {
      final byte[] content = source.getRawContent();
      if (content != null) {
        target.setContent(content, source.getContentType());
      }
    }
    catch (final Throwable t) {
      throw new IllegalArgumentException(t);
    }
  }

  public static boolean isProvisionalResponse(final SipServletResponse res) {
    return res.getStatus() < 200;
  }

  public static boolean isSuccessResponse(final SipServletResponse res) {
    return res.getStatus() >= 200 && res.getStatus() <= 299;
  }

  public static boolean isErrorResponse(final SipServletResponse res) {
    return res.getStatus() >= 300;
  }

  public static boolean isBusy(final SipServletResponse res) {
    return res.getStatus() == SipServletResponse.SC_BUSY_HERE
        || res.getStatus() == SipServletResponse.SC_BUSY_EVERYWHERE;
  }

  public static boolean isDecline(final SipServletResponse res) {
    return res.getStatus() == SipServletResponse.SC_DECLINE;
  }

  public static boolean isTimeout(final SipServletResponse res) {
    return res.getStatus() == SipServletResponse.SC_REQUEST_TIMEOUT;
  }

  public static boolean isRedirect(final SipServletResponse res) {
    return res.getStatus() >= 300 && res.getStatus() <= 399;
  }

  public static boolean isInvite(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("INVITE");
  }

  public static boolean isInitial(final SipServletRequest req) {
    return req.isInitial();
  }

  public static boolean isReinvite(final SipServletMessage msg) {
    if (msg instanceof SipServletRequest) {
      return msg.getMethod().equalsIgnoreCase("INVITE") && !((SipServletRequest) msg).isInitial();
    }
    else {
      return msg.getMethod().equalsIgnoreCase("INVITE") && !((SipServletResponse) msg).getRequest().isInitial();
    }
  }

  public static boolean isAck(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("ACK");
  }

  public static boolean isCancel(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("CANCEL");
  }

  public static boolean isBye(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("BYE");
  }
  
  public static boolean isPrack(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("PRACK");
  }
  
  public static boolean isRegister(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("REGISTER");
  }
  
  public static boolean isUpdate(final SipServletMessage msg) {
    return msg.getMethod().equalsIgnoreCase("UPDATE");
  }

  public static void forwardRequestByB2buaHelper(final SipServletRequest req, final Map<String, String> headers)
      throws IOException {
    final B2buaHelper b2b = req.getB2buaHelper();
    if (req.getMethod().equalsIgnoreCase("ACK")) {
      final SipSession ss = b2b.getLinkedSession(req.getSession());
      final java.util.List<SipServletMessage> msgs = b2b.getPendingMessages(ss, UAMode.UAC);
      for (final SipServletMessage msg : msgs) {
        if (msg instanceof SipServletResponse) {
          final SipServletResponse res = (SipServletResponse) msg;
          // send Ack for SUCCESS response
          if (res.getStatus() == SipServletResponse.SC_OK) {
            final SipServletRequest ack = res.createAck();
            SIPHelper.copyContent(req, ack);
            ack.send();
          }
        }
      }
    }
    else if (req.getMethod().equalsIgnoreCase("CANCEL")) {
      final SipSession ss = b2b.getLinkedSession(req.getSession());
      final SipServletRequest cancel = b2b.createCancel(ss);
      cancel.send();
    }
    else {
      final SipSession leg1 = req.getSession();
      final SipSession leg2 = req.getB2buaHelper().getLinkedSession(leg1);
      final SipServletRequest req2 = req.getB2buaHelper().createRequest(leg2, req, null);
      SIPHelper.copyContent(req, req2);
      req2.send();
    }
  }

  public static void forwardResponseByB2buaHelper(final SipServletResponse res, final Map<String, String> headers)
      throws IOException {
    final B2buaHelper b2b = res.getRequest().getB2buaHelper();
    final SipSession peer = b2b.getLinkedSession(res.getSession());
    SipServletResponse cpyresp = null;
    if (res.getRequest().isInitial()) {
      cpyresp = b2b.createResponseToOriginalRequest(peer, res.getStatus(), res.getReasonPhrase());
    }
    else {
      final SipServletRequest otherReq = b2b.getLinkedSipServletRequest(res.getRequest());
      cpyresp = otherReq.createResponse(res.getStatus(), res.getReasonPhrase());
    }
    SIPHelper.copyContent(res, cpyresp);
    cpyresp.send();
  }

  public static void handleErrorSdpPortManagerEvent(final SdpPortManagerEvent event, final SipServletRequest req) {
    final MediaErr error = event.getError();
    try {
      if (SdpPortManagerEvent.SDP_NOT_ACCEPTABLE.equals(error)) {
        // Send 488 error response to INVITE
        req.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE_HERE).send();
      }
      else if (SdpPortManagerEvent.RESOURCE_UNAVAILABLE.equals(error)) {
        // Send 486 error response to INVITE
        req.createResponse(SipServletResponse.SC_BUSY_HERE).send();
      }
      else {
        // Some unknown error. Send 500 error response to INVITE
        req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
      }
    }
    catch (final IOException e) {
      LOG.warn("IOException when sending error response ", e);
    }
  }

  public static void linkSIPMessage(final SipServletMessage msg1, final SipServletMessage msg2) {
    msg1.setAttribute(LINKED_MESSAGE, msg2);
    msg2.setAttribute(LINKED_MESSAGE, msg1);
  }

  public static SipServletMessage getLinkSIPMessage(final SipServletMessage msg) {
    return (SipServletMessage) msg.getAttribute(LINKED_MESSAGE);
  }

  public static void unlinkSIPMessage(final SipServletMessage msg1) {
    final SipServletMessage msg2 = getLinkSIPMessage(msg1);
    if (msg2 != null) {
      msg1.removeAttribute(LINKED_MESSAGE);
      msg2.removeAttribute(LINKED_MESSAGE);
    }
  }

  public static void sendSubsequentRequest(final SipSession session, final SipServletRequest origReq,
      final Map<String, String> headers) throws IOException {
    final SipServletRequest newReq = session.createRequest(origReq.getMethod());
    SIPHelper.addHeaders(newReq, headers);
    SIPHelper.copyContent(origReq, newReq);
    SIPHelper.linkSIPMessage(origReq, newReq);
    newReq.send();
  }

  public static void sendReinvite(final SipSession session, final SipServletMessage origReq,
      final Map<String, String> headers) throws IOException {
    final SipServletRequest reinvite = session.createRequest("INVITE");
    SIPHelper.addHeaders(reinvite, headers);
    if (origReq != null) {
      SIPHelper.copyContent(origReq, reinvite);
    }
    reinvite.send();
  }

  public static boolean isContainSDP(final SipServletRequest req) {
    try {
      if (req.getContent() == null) {
        return false;
      }
      else {
        return true;
      }
    }
    catch (final Throwable t) {
      return false;
    }
  }
  
  public static void proxyTo(final SipFactory factory, SipServletRequest initialRequest, final Map<String, String> headers, boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {
    if (destinations == null || destinations.length == 0) {
      throw new IllegalArgumentException("Illegal endpoints");
    }
    try {
      addHeaders(initialRequest, headers);
      Proxy proxy = initialRequest.getProxy();

      proxy.setParallel(parallel);
      proxy.setRecordRoute(recordRoute);
      if (isRegister(initialRequest)) {
        proxy.setAddToPath(true);
      }
      proxy.setSupervised(false);

      List<URI> uris = new LinkedList<URI>();
      for (Endpoint endpoint : destinations) {
        if (endpoint.getURI() == null) {
          throw new IllegalArgumentException("Illegal endpoints:" + endpoint);
        }
        Address address = factory.createAddress(endpoint.getURI().toString());
        uris.add(address.getURI());
      }

      proxy.proxyTo(uris);
    }
    catch (TooManyHopsException e) {
      LOG.error("", e);
      throw new SignalException(e);
    }
    catch (ServletParseException e) {
      LOG.error("", e);
      throw new SignalException(e);
    }
  }
  
  public static URI getCleanUri(URI uri) {
    if (uri.isSipURI()) {
      SipURI sipURI = (SipURI) uri.clone();
      Iterator<String> iterator = sipURI.getParameterNames();
      while (iterator != null && iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
      return sipURI;
    }
    else {
      return uri;
    }
  }
}
