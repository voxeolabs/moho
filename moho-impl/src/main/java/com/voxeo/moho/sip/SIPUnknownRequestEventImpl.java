/**
 * Copyright 2010-2011 Voxeo Corporation
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
import javax.servlet.sip.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MohoEvent;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPUnknownRequestEventImpl<T extends EventSource> extends MohoEvent<T> implements SIPUnknownRequestEvent<T> {

  protected ExecutionContext _ctx;

  protected SipServletRequest _req;

  protected boolean _forwarded = false;

  protected boolean _rejected = false;
  
  protected boolean _accepted = false;

  protected SIPUnknownRequestEventImpl(final T source, final SipServletRequest req) {
    super(source);
    _req = req;
    _ctx = (ExecutionContext) source.getApplicationContext();
  }

  public SipServletRequest getSipRequest() {
    return _req;
  }

  @Override
  public synchronized boolean isForwarded() {
    return _forwarded;
  }

  @Override
  public synchronized boolean isRejected() {
    return _rejected;
  }

  @Override
  public synchronized boolean isProcessed() {
    return isAccepted() || isForwarded() || isRejected();
  }
  
  protected synchronized void checkState() {
    if (isProcessed()) {
      throw new IllegalStateException("Event is already processed and can not be processed.");
    }
  }

  @Override
  public String getType() {
    return _req.getMethod();
  }

  @Override
  public void reject(final Reason reason) throws SignalException {
    this.reject(reason, null);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    this.checkState();
    _accepted = true;
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
  public synchronized void reject(final Reason reason, final Map<String, String> headers) throws SignalException {
    this.checkState();
    _rejected = true;
    final SipServletResponse res = _req.createResponse(reason == null ? Reason.DECLINE.getCode() : reason.getCode());
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
    forwardTo(call, null);
  }

  @Override
  public synchronized void forwardTo(final Call call, final Map<String, String> headers) throws SignalException {
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
  public synchronized void forwardTo(final Endpoint endpoint, final Map<String, String> headers)
      throws SignalException, IllegalStateException {
    URI target = null;
    try {
      target = _ctx.getSipFactory().createURI(endpoint.getURI().toString());
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
    this.checkState();
    _forwarded = true;
    final SipServletRequest req = _ctx.getSipFactory().createRequest(_req.getApplicationSession(), _req.getMethod(),
        _req.getFrom(), _req.getTo());
    req.setRequestURI(target);
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
  public synchronized boolean isAccepted() {
    return _accepted;
  }

  @Override
  public void accept() throws SignalException {
    accept(null);
  }

}
