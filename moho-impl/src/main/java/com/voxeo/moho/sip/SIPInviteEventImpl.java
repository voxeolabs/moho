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
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventState;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.util.SessionUtils;
import com.voxeo.utils.EventListener;

public class SIPInviteEventImpl extends SIPInviteEvent {
  private static final long serialVersionUID = 6616763570731305598L;

  protected SipServletRequest _invite;

  protected CallableEndpoint _caller;

  protected CallableEndpoint _callee;

  protected SIPIncomingCall _call;

  protected EventState _state;

  protected SIPInviteEventImpl(final ApplicationContext ctx, final SipServletRequest invite) {
    _invite = invite;
    _caller = new SIPEndpointImpl((ApplicationContextImpl) ctx, invite.getFrom());
    _callee = new SIPEndpointImpl((ApplicationContextImpl) ctx, invite.getTo());
    setState(InviteEventState.ALERTING);
  }

  @Override
  public SipServletRequest getSipRequest() {
    return _invite;
  }

  @Override
  public String getHeader(final String name) {
    return _invite.getHeader(name);
  }

  @Override
  public ListIterator<String> getHeaders(final String name) {
    return _invite.getHeaders(name);
  }

  @Override
  public Endpoint getInvitor() {
    return _caller;
  }

  @Override
  public CallableEndpoint getInvitee() {
    return _callee;
  }

  @Override
  public synchronized void redirect(final Endpoint o, final Map<String, String> headers) throws SignalException,
      IllegalArgumentException {
    this.checkState();
    this.setState(InviteEventState.REDIRECTING);
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
    this.checkState();
    this.setState(InviteEventState.REJECTING);
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

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    acceptCall(headers);
  }

  @Override
  public synchronized Call acceptCall(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, IllegalStateException {
    if (_call != null) {
      throw new IllegalStateException("...");
    }
    this.checkState();
    this.setState(InviteEventState.ACCEPTING);
    final ExecutionContext ctx = SessionUtils.getContext(_invite.getSession());
    _call = new SIPIncomingCall(ctx, this);
    _call.addListeners(listeners);
    try {
      _call.doInvite(headers);
      return _call;
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized Call acceptCall(final Map<String, String> headers, final Observer... observers)
      throws SignalException, IllegalStateException {
    if (_call != null) {
      throw new IllegalStateException("...");
    }
    this.checkState();
    this.setState(InviteEventState.ACCEPTING);
    final ExecutionContext ctx = SessionUtils.getContext(_invite.getSession());
    _call = new SIPIncomingCall(ctx, this);
    _call.addObservers(observers);
    try {
      _call.doInvite(headers);
      return _call;
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized Call acceptCallWithEarlyMedia(final Map<String, String> headers,
      final EventListener<?>... listeners) throws SignalException, MediaException, IllegalStateException {
    if (_call != null) {
      throw new IllegalStateException("...");
    }
    this.checkState();
    this.setState(InviteEventState.PROGRESSING);
    final ExecutionContext ctx = SessionUtils.getContext(_invite.getSession());
    _call = new SIPIncomingCall(ctx, this);
    _call.addListeners(listeners);
    try {
      _call.doInviteWithEarlyMedia(headers);
      return _call;
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
  public synchronized Call acceptCallWithEarlyMedia(final Map<String, String> headers, final Observer... observers)
      throws SignalException, MediaException, IllegalStateException {
    if (_call != null) {
      throw new IllegalStateException("...");
    }
    this.checkState();
    this.setState(InviteEventState.PROGRESSING);
    final ExecutionContext ctx = SessionUtils.getContext(_invite.getSession());
    _call = new SIPIncomingCall(ctx, this);
    _call.addObservers(observers);
    try {
      _call.doInviteWithEarlyMedia(headers);
      return _call;
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
  public Call answer(Map<String, String> headers, EventListener<?>... listeners) throws SignalException,
      IllegalStateException {
    Call call = acceptCall(headers, listeners);

    Joint joint = call.join();
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (ExecutionException e) {
        throw new SignalException(e.getCause());
      }
    }

    return call;
  }

  @Override
  public Call answer(Map<String, String> headers, Observer... observer) throws SignalException, IllegalStateException {
    Call call = acceptCall(headers, observer);

    Joint joint = call.join();
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (ExecutionException e) {
        throw new SignalException(e.getCause());
      }
    }

    return call;
  }
}
