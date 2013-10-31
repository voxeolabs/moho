/**
 * Copyright 2010-2011 Voxeo Corporation Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
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
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.util.Utils;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPIncomingCall extends SIPCallImpl implements IncomingCall {

  private static final Logger LOG = Logger.getLogger(SIPIncomingCall.class);

  protected Exception acceptEarlyMediaException;
  
  protected SIPCall.State oldStateBeforeEarlyMedia;
  
  protected boolean reliableEarlyMedia;

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
            retval = new DirectNI2NOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2NIJoinDelegate(this, (SIPIncomingCall) other, direction, (SIPIncomingCall) other);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNI2AOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AIJoinDelegate(this, (SIPIncomingCall) other, direction, (SIPIncomingCall) other);
          }
        }
      }
      else if (isAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAI2NOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AIJoinDelegate((SIPIncomingCall) other, this, direction, (SIPIncomingCall) other);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAI2AOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2AIJoinDelegate(this, (SIPIncomingCall) other, direction, (SIPIncomingCall) other);
          }
        }
      }
    }
    else {
      retval = new BridgeJoinDelegate(this, other, direction, type, other);
    }
    return retval;
  }
  
  protected JoinDelegate createJoinDelegate(final Call[] others, final JoinType type, final Direction direction) {
    if (this.isNoAnswered() && type == JoinType.DIRECT) {
      JoinDelegate retval = null;
      List<SIPCallImpl> candidates = new LinkedList<SIPCallImpl>();
      for (Call call : others) {
        candidates.add((SIPCallImpl) call);
      }
      retval = new DirectNI2MultipleNOJoinDelegate(type, direction, this,
          Utils.suppressEarlyMedia(getApplicationContext()), candidates);
      return retval;
    }
    else {
      return super.createJoinDelegate(others, type, direction);
    }
  }

  @Override
  public synchronized void onEvent(final SdpPortManagerEvent event) {
    if (getSIPCallState() == SIPCall.State.PROGRESSING) {
      if (event.getEventType() == SdpPortManagerEvent.OFFER_GENERATED
          || event.getEventType() == SdpPortManagerEvent.ANSWER_GENERATED) {
        final byte[] sdp = event.getMediaServerSdp();
        this.setLocalSDP(sdp);
        final SipServletResponse res = getSipInitnalRequest().createResponse(SipServletResponse.SC_SESSION_PROGRESS);

        try {
          res.setContent(sdp, "application/sdp");

          try {
            res.sendReliably();
            reliableEarlyMedia = true;
          }
          catch (Rel100Exception ex) {
            LOG.debug("Can't send reliably.");
            res.send();
          }
          
          if (!reliableEarlyMedia) {
            setSIPCallState(oldStateBeforeEarlyMedia);
            notifyAll();
          }
        }
        catch (final Exception e) {
          LOG.warn("Can't send early media response.", e);
          acceptEarlyMediaException = e;
          setSIPCallState(oldStateBeforeEarlyMedia);
          notifyAll();
        }
      }
      else if (event.getEventType() == SdpPortManagerEvent.ANSWER_PROCESSED) {
        if(reliableEarlyMedia) {
          setSIPCallState(SIPCall.State.PROGRESSED);
        }
        else {
          setSIPCallState(oldStateBeforeEarlyMedia);
        }
        notifyAll();
      }
    }
    else {
      super.onEvent(event);
    }
  }

  protected synchronized void doCancel(SipServletRequest req) {
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
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
      terminate(CallCompleteEvent.Cause.CANCEL, null, null);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Receiving Cancel, but is already answered. terminating, callID"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
      terminate(CallCompleteEvent.Cause.CANCEL, null, null);
    }

    try {
      req.createResponse(200).send();
    }
    catch (IOException e) {
      LOG.warn("Exception when sending back response for CANCEL." + req);
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

  protected synchronized void doInviteWithEarlyMedia(final Map<String, String> headers) throws MediaException,
      SignalException {
    if(!SIPHelper.support100rel(getSipInitnalRequest())) {
      LOG.error("Request doesn't support 100rel, can't acceptWithEarlyMedia.");
      throw new SignalException("Request doesn't support 100rel, can't acceptWithEarlyMedia." + this);
    }
    
    if (_cstate == SIPCall.State.INVITING || _cstate == SIPCall.State.RINGING) {
      oldStateBeforeEarlyMedia = _cstate;
      setSIPCallState(SIPCall.State.PROGRESSING);
      try {
        processSDPOffer(getSipInitnalRequest());
      }
      catch (MediaException ex) {
        acceptEarlyMediaException = ex;
        setSIPCallState(oldStateBeforeEarlyMedia);
      }
      while (!this.isTerminated() && _cstate == SIPCall.State.PROGRESSING) {
        try {
          wait(20000);
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (acceptEarlyMediaException != null
          || (_cstate != SIPCall.State.PROGRESSED && _cstate != oldStateBeforeEarlyMedia)) {
        if (acceptEarlyMediaException != null) {
          if (acceptEarlyMediaException instanceof SignalException) {
            throw (SignalException) acceptEarlyMediaException;
          }
          else {
            throw (MediaException) acceptEarlyMediaException;
          }
        }
        else {
          throw new SignalException("Can't acceptWithEarlyMedia." + this);
        }
      }
    }
  }

  protected synchronized void doPrack(final SipServletRequest req) throws IOException {
    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }

    if (_joinDelegate != null) {
      try {
        _joinDelegate.doPrack(req, this, null);
      }
      catch (Exception ex) {
        LOG.error("Exception when processing PRACK", ex);
      }
    }
    else {
      final SipServletResponse res = req.createResponse(SipServletResponse.SC_OK);
      if (_cstate == SIPCall.State.PROGRESSING) {
        if (content != null && SIPHelper.getRawContentWOException(_invite) == null) {
          try {
            processSDPAnswer(req);
          }
          catch (MediaException ex) {
            acceptEarlyMediaException = ex;
            setSIPCallState(oldStateBeforeEarlyMedia);
          }
        }
        else {
          setSIPCallState(SIPCall.State.PROGRESSED);
        }
        notifyAll();
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
    return !(getSIPCallState() == SIPCall.State.RINGING || getSIPCallState() == SIPCall.State.INITIALIZED
        || getSIPCallState() == SIPCall.State.PROGRESSED || getSIPCallState() == SIPCall.State.PROGRESSING || getSIPCallState() == SIPCall.State.INVITING);
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

    terminate(CallCompleteEvent.Cause.REDIRECT, null, headers);

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

    terminate(CallCompleteEvent.Cause.DECLINE, null, headers);

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
  public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {
    proxyTo(recordRoute, parallel, null, destinations);
  }

  @Override
  public synchronized void proxyTo(boolean recordRoute, boolean parallel, Map<String, String> headers,
      Endpoint... destinations) {
    checkState();
    _proxied = true;
    setSIPCallState(SIPCall.State.PROXIED);
    SIPHelper.proxyTo(getApplicationContext().getSipFactory(), _invite, headers, recordRoute, parallel, destinations);
  }

  @Override
  public void setAsync(boolean async) {

  }

  @Override
  public boolean isAsync() {
    return false;
  }

  @Override
  public byte[] getJoinSDP() throws IOException {
    if (!isAnswered()) {
      return _invite.getRawContent();
    }
    else {
      _invite = getSipSession().createRequest("INVITE");
      _invite.send();
      return null;
    }
  }

  @Override
  public void processSDPAnswer(byte[] sdp) throws IOException {
    if (!isAnswered()) {
      SipServletResponse newRes = _invite.createResponse(SipServletResponse.SC_OK);
      newRes.setContent(sdp, "application/sdp");
      newRes.send();
    }
    else if (_inviteResponse != null) {
      SipServletRequest ack = _inviteResponse.createAck();
      ack.setContent(sdp, "application/sdp");
      ack.send();
    }
    else {
      throw new IllegalStateException("SIPIncomingCall, answered and no re-INVITE response.");
    }
  }

  @Override
  public byte[] processSDPOffer(byte[] sdp) throws IOException {
    if (!isAnswered()) {
      final SipServletResponse newRes = _invite.createResponse(SipServletResponse.SC_OK);
      newRes.setContent(sdp, "application/sdp");
      newRes.send();
      return _invite.getRawContent();
    }
    else {
      _invite = getSipSession().createRequest("INVITE");
      _invite.setContent(sdp, "application/sdp");
      _invite.send();
      return null;
    }
  }

  @Override
  public void setContinueRouting(SIPCall origCall) {
    throw new UnsupportedOperationException("incoming call doesn't support this method.");

  }
}