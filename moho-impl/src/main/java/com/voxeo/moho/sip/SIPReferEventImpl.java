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

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.Subscription.Type;
import com.voxeo.moho.event.ApplicationEventSource;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.util.SessionUtils;
import com.voxeo.utils.EventListener;

public class SIPReferEventImpl extends SIPReferEvent {

  private static final Logger LOG = Logger.getLogger(SIPReferEventImpl.class);

  protected SIPReferEventImpl(final EventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public CallableEndpoint getReferee() {
    try {
      final SipURI sipURI = (SipURI) _req.getAddressHeader("Refer-To").getURI().clone();
      // sipURI.removeHeader("replaces");
      return new SIPEndpointImpl(SessionUtils.getContext(_req), SessionUtils.getContext(_req).getSipFactory()
          .createAddress(sipURI.toString()));
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public CallableEndpoint getReferredBy() {
    try {
      return new SIPEndpointImpl(SessionUtils.getContext(_req), _req.getAddressHeader("Referred-By"));
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void forwardTo(final Call call) throws SignalException, IllegalStateException {
    forwardTo(call, null);
  }

  @Override
  public synchronized void forwardTo(final Call call, final Map<String, String> headers) throws SignalException,
      IllegalStateException {
    if (!(call instanceof SIPCall)) {
      throw new UnsupportedOperationException("Cannot forward to non-SIPCall.");
    }
    if (_req.isInitial()) {
      throw new IllegalArgumentException("Cannot forward initial SIP request.");
    }
    final SIPCallImpl scall = (SIPCallImpl) call;
    if (!scall.isAnswered()) {
      throw new IllegalStateException("Cannot forward to no-answered call.");
    }
    this.checkState();
    _forwarded = true;

    final SipSession session = scall.getSipSession();
    final SipServletRequest req = session.createRequest(_req.getMethod());
    SIPHelper.addHeaders(req, headers);
    SIPHelper.copyContent(_req, req);
    SIPHelper.linkSIPMessage(_req, req);

    req.addHeader("Refer-To", _req.getHeader("Refer-To"));
    req.addHeader("Referred-By", _req.getHeader("Referred-By"));
    try {
      req.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public void forwardTo(final Endpoint endpoint) throws SignalException, IllegalStateException {
    forwardTo(endpoint, null);
  }

  @Override
  public void forwardTo(final Endpoint endpoint, final Map<String, String> headers) throws SignalException,
      IllegalStateException {
    if (source instanceof ApplicationEventSource && endpoint instanceof SIPEndpoint) {
      this.checkState();
      _forwarded = true;

      final ApplicationEventSource es = (ApplicationEventSource) source;
      final ApplicationContextImpl appContext = (ApplicationContextImpl) es.getApplicationContext();

      final SipServletRequest req = appContext.getSipFactory().createRequest(_req.getApplicationSession(), "REFER",
          _req.getFrom(), ((SIPEndpoint) endpoint).getSipAddress());
      SIPHelper.addHeaders(req, headers);
      req.addHeader("Refer-To", _req.getHeader("Refer-To"));
      req.addHeader("Referred-By", _req.getHeader("Referred-By"));
      SIPHelper.copyContent(_req, req);
      SIPHelper.linkSIPMessage(_req, req);

      try {
        // set the event source.
        final SIPSubscriptionImpl retval = new SIPSubscriptionImpl(appContext, Type.REFER, 180, new SIPEndpointImpl(
            appContext, _req.getFrom()), endpoint);
        //TODO should set event listener or observer.
        final SipSession outSession = req.getSession();
        outSession.setHandler(appContext.getController());
        SessionUtils.setEventSource(outSession, retval);

        req.send();
      }
      catch (final IOException e) {
        LOG.error("", e);
        throw new SignalException("", e);
      }
      catch (final ServletException e) {
        LOG.error("", e);
        throw new SignalException("", e);
      }
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException {
    this.accept(JoinType.DIRECT, Direction.DUPLEX, headers);
  }

  @Override
  public synchronized Call accept(final JoinType type, final Direction direction, final Map<String, String> headers)
      throws SignalException {
    this.checkState();
    _accepted = true;
    if (this.source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) this.source;
      if (!call.isAnswered()) {
        throw new IllegalStateException("...");
      }
      try {
        return transfer(call, type, direction, headers);
      }
      catch (final Exception e) {
        if (e instanceof SignalException) {
          throw (SignalException) e;
        }
        else if (e.getCause() instanceof SignalException) {
          throw (SignalException) e.getCause();
        }
        else {
          throw new SignalException(e);
        }
      }
    }
    return null;
  }

  private SIPCallImpl transfer(final SIPCallImpl call, final JoinType type, final Direction direction,
      final Map<String, String> headers) throws IOException, SignalException, MediaException {
    String replaces = null;
    try {
      final SipURI sipURI = (SipURI) _req.getAddressHeader("Refer-To").getURI();
      replaces = sipURI.getHeader("replaces");
      if (replaces != null) {
        // if there is replaces header in the sip URI, means it's a
        // attended transfer. can't accept the request in the server now. just
        // forward the request to the peer.
        LOG.warn("This is Attended Transfer request, can't be accepted, forward it, will return null!");
        final Call peer = call.getLastPeer();
        if (peer != null) {
          _accepted = false;
          _forwarded = false;
          this.forwardTo(peer);
          return null;
        }
      }
    }
    catch (final ServletParseException e) {
      LOG.error("Parse error", e);
    }

    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_ACCEPTED);
    sendNotify("SIP/2.0 100 Trying", "active;expires=180");
    SIPHelper.addHeaders(res, headers);
    res.send();

    // add Referred-By header and Replaces header.
    final Map<String, String> reqHeaders = new HashMap<String, String>();
    reqHeaders.put("Referred-By", ((SIPEndpoint) getReferredBy()).getSipAddress().toString());

    if (headers != null) {
      reqHeaders.putAll(headers);
    }

    final SIPCallImpl peer = (SIPCallImpl) call.getLastPeer();

    final SIPOutgoingCall newCall = (SIPOutgoingCall) getReferee().call(peer.getAddress(), reqHeaders,
        (EventListener<?>) null);
    if (call.getLastPeer() instanceof SIPCallImpl) {
      peer.unjoin(call);
      peer.join(newCall, type, direction);
    }
    else {
      newCall.join(direction);
    }
    // TODO how to process exceptions. e.g. BusyException
    sendNotify("SIP/2.0 200 OK", "terminated;reason=noresource");
    call.disconnect();
    return newCall;
  }

  private void sendNotify(final String content, final String state) {
    try {
      final SipServletRequest notify = _req.getSession().createRequest("NOTIFY");
      notify.addHeader("Event", "refer");
      notify.addHeader("Subscription-State", state);
      notify.setContent(content.getBytes("UTF-8"), "message/sipfrag");
      notify.send();
    }
    catch (final IOException t) {
      LOG.error("IOException when sending notify message.", t);
    }
  }
}
