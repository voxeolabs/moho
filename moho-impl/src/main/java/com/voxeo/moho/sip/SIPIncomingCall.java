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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.Address;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.CanceledException;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPIncomingCall extends SIPCallImpl implements IncomingCall {

  private static final Logger LOG = Logger.getLogger(SIPIncomingCall.class);

  protected SIPIncomingCall(final ExecutionContext context, final SipServletRequest req) {
    super(context, req);
    setRemoteSDP(SIPHelper.getRawContentWOException(req));
  }

  @Override
  protected JoinDelegate createJoinDelegate(final Direction direction) {
    JoinDelegate retval = null;
    if (isNoAnswered()) {
      retval = new Media2NIJoinDelegate(this);
    }
    else if (isAnswered()) {
      retval = new Media2AIJoinDelegate(this);
    }
    else {
      throw new IllegalStateException("The SIPCall state is " + getSIPCallState());
    }
    return retval;
  }

  @Override
  protected JoinDelegate createJoinDelegate(final SIPCallImpl other, final JoinType type, final Direction direction) {
    JoinDelegate retval = null;
    if (type == JoinType.DIRECT) {
      if (isNoAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNI2NOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2NIJoinDelegate(this, (SIPIncomingCall) other, direction);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNI2AOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AIJoinDelegate(this, (SIPIncomingCall) other, direction);
          }
        }
      }
      else if (isAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAI2NOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AIJoinDelegate((SIPIncomingCall) other, this, direction);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAI2AOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2AIJoinDelegate(this, (SIPIncomingCall) other, direction);
          }
        }
      }
    }
    else {
      retval = new BridgeJoinDelegate(this, other, direction);
    }
    return retval;
  }

  @Override
  public synchronized void onEvent(final SdpPortManagerEvent event) {
    if (getSIPCallState() == SIPCall.State.PROGRESSING) {
      try {
        final byte[] sdp = event.getMediaServerSdp();
        this.setLocalSDP(sdp);
        final SipServletResponse res = getSipInitnalRequest().createResponse(SipServletResponse.SC_SESSION_PROGRESS);
        res.setContent(sdp, "application/sdp");
        try {
          res.sendReliably();
        }
        catch (final Rel100Exception e) {
          LOG.warn("", e);
          res.send();
        }
        setSIPCallState(SIPCall.State.PROGRESSED);
        this.notifyAll();
      }
      catch (final IOException e) {
        LOG.warn("", e);
      }
    }
    super.onEvent(event);
  }

  protected synchronized void doCancel() {
    if (isTerminated()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Receiving Cancel, but is already terminated. callID:"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
    }
    else if (isNoAnswered()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Receiving Cancel, not answered. terminating, callID"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
      if (_joinDelegate != null) {
        _joinDelegate.setException(new CanceledException());
      }
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
      terminate(CallCompleteEvent.Cause.CANCEL, null);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Receiving Cancel, but is already answered. terminating, callID"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
      terminate(CallCompleteEvent.Cause.CANCEL, null);
    }
  }

  protected synchronized void doInvite(final Map<String, String> headers) throws IOException {
    if (_cstate == SIPCall.State.INVITING) {
      setSIPCallState(SIPCall.State.RINGING);
      final SipServletResponse res = _invite.createResponse(SipServletResponse.SC_RINGING);
      SIPHelper.addHeaders(res, headers);
      res.send();
    }
  }

  protected synchronized void doInviteWithEarlyMedia(final Map<String, String> headers) throws MediaException {
    if (_cstate == SIPCall.State.INVITING) {
      setSIPCallState(SIPCall.State.PROGRESSING);
      processSDPOffer(getSipInitnalRequest());
      while (!this.isTerminated() && _cstate == SIPCall.State.PROGRESSING) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (_cstate != SIPCall.State.PROGRESSED) {
        throw new IllegalStateException("" + this);
      }
    }
  }

  protected synchronized void doPrack(final SipServletRequest req) throws IOException {
    if (_cstate == SIPCall.State.PROGRESSED) {
      final SipServletResponse res = req.createResponse(SipServletResponse.SC_OK);
      if (getLocalSDP() != null) {
        res.setContent(getLocalSDP(), "application/sdp");
      }
      res.send();
    }
  }

  protected boolean _acceptedWithEarlyMedia = false;

  protected boolean _rejected = false;

  protected boolean _redirected = false;

  protected boolean _accepted = false;
  
  protected boolean _proxied = false;

  @Override
  public synchronized boolean isAcceptedWithEarlyMedia() {
    return _acceptedWithEarlyMedia;
  }

  @Override
  public synchronized boolean isRedirected() {
    return _redirected;
  }

  @Override
  public synchronized boolean isRejected() {
    return _rejected;
  }

  @Override
  public synchronized boolean isAccepted() {
    return _accepted;
  }
  
  @Override
  public synchronized boolean isProxied() {
    return _proxied;
  }

  protected synchronized boolean isProcessed() {
    return isAccepted() || isAcceptedWithEarlyMedia() || isRejected() || isRedirected() || isProxied();
  }

  @Override
  public void acceptWithEarlyMedia() throws SignalException, MediaException {
    this.acceptWithEarlyMedia((Map<String, String>) null);
  }

  @Override
  public void redirect(final Endpoint other) throws SignalException {
    this.redirect(other, null);
  }

  @Override
  public void reject(final Reason reason) throws SignalException {
    this.reject(reason, null);
  }

  @Override
  public void answer() {
    this.answer((Map<String, String>) null);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    checkState();
    _accepted = true;
    try {
      ((SIPIncomingCall) this).doInvite(headers);
      return;
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized void acceptWithEarlyMedia(final Map<String, String> headers) throws SignalException,
      MediaException {
    checkState();
    _accepted = true;
    _acceptedWithEarlyMedia = true;
    try {
      ((SIPIncomingCall) this).doInviteWithEarlyMedia(headers);
      return;
    }
    catch (final Exception e) {
      if (e instanceof SignalException) {
        throw (SignalException) e;
      }
      else if (e instanceof MediaException) {
        throw (MediaException) e;
      }
      else {
        throw new SignalException(e);
      }
    }
  }

  @Override
  public void answer(final Map<String, String> headers) throws SignalException {

    if (!_accepted) {
      accept(headers);
    }

    final Joint joint = this.join();
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (final InterruptedException e) {
        // ignore
      }
      catch (final ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SignalException) {
          throw (SignalException) cause;
        }
        throw new SignalException(cause);
      }
    }

    return;
  }

  @Override
  public synchronized void redirect(final Endpoint o, final Map<String, String> headers) throws SignalException {
    checkState();
    _redirected = true;
    setSIPCallState(SIPCall.State.REDIRECTED);

    terminate(CallCompleteEvent.Cause.REDIRECT, null);

    if (o instanceof SIPEndpoint) {
      final SipServletResponse res = _invite.createResponse(SipServletResponse.SC_MOVED_TEMPORARILY);
      res.setHeader("Contact", ((SIPEndpoint) o).getURI().toString());
      SIPHelper.addHeaders(res, headers);
      try {
        res.send();
      }
      catch (final IOException e) {
        throw new SignalException(e);
      }
    }
    else {
      throw new IllegalArgumentException("Unable to redirect the call to a non-SIP participant.");
    }
  }

  @Override
  public synchronized void reject(final Reason reason, final Map<String, String> headers) throws SignalException {
    checkState();
    _rejected = true;
    setSIPCallState(SIPCall.State.REJECTED);

    terminate(CallCompleteEvent.Cause.DECLINE, null);

    try {
      final SipServletResponse res = _invite.createResponse(reason == null ? Reason.DECLINE.getCode() : reason
          .getCode());
      SIPHelper.addHeaders(res, headers);
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  protected synchronized void checkState() {
    if (isProcessed()) {
      throw new IllegalStateException("Event is already processed and can not be processed.");
    }
  }

  @Override
  public Call getSource() {
    return this;
  }

  @Override
  public void accept() throws SignalException {
    accept((Map<String, String>) null);
  }

  @Override
  public void acceptWithEarlyMedia(Observer... observer) throws SignalException, MediaException {
    addObserver(observer);
    acceptWithEarlyMedia();
  }

  @Override
  public void accept(Observer... observer) throws SignalException {
    addObserver(observer);
    accept();
  }

  @Override
  public void answer(Observer... observer) throws SignalException, MediaException {
    addObserver(observer);
    answer();
  }

  @Override
  public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... endpoints) throws SignalException {
    checkState();
    _proxied = true;
    setSIPCallState(SIPCall.State.PROXIED);
    if (endpoints == null || endpoints.length == 0) {
      throw new IllegalArgumentException("Illegal endpoints");
    }
    try {
      Proxy proxy = _invite.getProxy();

      proxy.setParallel(parallel);
      proxy.setRecordRoute(recordRoute);
      proxy.setSupervised(false);

      List<URI> uris = new LinkedList<URI>();
      for (Endpoint endpoint : endpoints) {
        if (endpoint.getURI() == null) {
          throw new IllegalArgumentException("Illegal endpoints:" + endpoint);
        }
        Address address = getApplicationContext().getSipFactory().createAddress(endpoint.getURI().toString());
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
}
