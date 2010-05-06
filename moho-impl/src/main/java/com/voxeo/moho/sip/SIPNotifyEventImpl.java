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
import java.util.Map;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Subscription.Type;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.NotifyEvent;

public class SIPNotifyEventImpl extends SIPNotifyEvent {

  protected SIPNotifyEventImpl(final EventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    this.checkState();
    this.setState(AcceptableEventState.ACCEPTED);
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public void forwardTo(final Call call) throws SignalException, IllegalStateException {
    this.forwardTo(call, null);
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
    this.setState(ForwardableEventState.FORWARDED);

    final SipSession session = scall.getSipSession();
    final SipServletRequest req = session.createRequest(_req.getMethod());
    SIPHelper.addHeaders(req, headers);
    SIPHelper.copyContent(_req, req);
    SIPHelper.linkSIPMessage(_req, req);
    try {
      req.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public void forwardTo(final Endpoint endpoint) throws SignalException, IllegalStateException {
    this.forwardTo(endpoint, null);
  }

  @Override
  public synchronized void forwardTo(final Endpoint endpoint, final Map<String, String> headers)
      throws SignalException, IllegalStateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Type getEventType() {
    String event = null;
    try {
      event = _req.getParameterableHeader("Event").getValue();
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
    if (event.equalsIgnoreCase(Subscription.Type.DIALOG.name())) {
      return Subscription.Type.DIALOG;
    }
    else if (event.equalsIgnoreCase(Subscription.Type.PRESENCE.name())) {
      return Subscription.Type.PRESENCE;
    }
    else if (event.equalsIgnoreCase(Subscription.Type.REFER.name())) {
      return Subscription.Type.REFER;
    }

    return null;
  }

  @Override
  public SubscriptionState getSubscriptionState() {
    String state = null;
    try {
      state = _req.getParameterableHeader("Subscription-State").getValue();
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }

    if (state.equalsIgnoreCase(NotifyEvent.SubscriptionState.ACTIVE.name())) {
      return NotifyEvent.SubscriptionState.ACTIVE;
    }
    else if (state.equalsIgnoreCase(NotifyEvent.SubscriptionState.PENDING.name())) {
      return NotifyEvent.SubscriptionState.PENDING;
    }
    else if (state.equalsIgnoreCase(NotifyEvent.SubscriptionState.TERMINATED.name())) {
      return NotifyEvent.SubscriptionState.TERMINATED;
    }

    return null;
  }

  @Override
  public String getResourceState() {
    if (getEventType() == Subscription.Type.DIALOG) {
      // TODO improve the content parser
      Document doc = null;
      try {
        doc = DocumentHelper.parseText(new String(_req.getRawContent(), "UTF-8"));
      }
      catch (final Exception e) {
        throw new IllegalArgumentException(e);
      }
      // rfc4235 defined dialog state: Trying, Proceeding, Early, Confirmed,
      // Terminated
      return doc.getRootElement().element("dialog").element("state").getText();
    }
    else if (getEventType() == Subscription.Type.PRESENCE) {
      // TODO improve the content parser
      Document doc = null;
      try {
        doc = DocumentHelper.parseText(new String(_req.getRawContent(), "UTF-8"));
      }
      catch (final Exception e) {
        throw new IllegalArgumentException(e);
      }
      // rfc3863 defined "open" and "close".
      return doc.getRootElement().element("tuple").element("status").elementText("basic");
    }
    else if (getEventType() == Subscription.Type.REFER) {
      // TODO
      return null;
    }
    return null;
  }

}
