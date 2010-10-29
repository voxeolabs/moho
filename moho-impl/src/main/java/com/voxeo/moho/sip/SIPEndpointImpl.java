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

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.Subscription.Type;
import com.voxeo.moho.event.Observer;
import com.voxeo.utils.EventListener;

public class SIPEndpointImpl implements SIPEndpoint {

  protected ExecutionContext _ctx;

  protected Address _address;

  public SIPEndpointImpl(final ExecutionContext ctx, final Address address) {
    _ctx = ctx;
    _address = (Address) address.clone();
  }

  @Override
  public int hashCode() {
    return _address.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SIPEndpoint)) {
      return false;
    }
    return _address.equals(((SIPEndpoint) o).getSipAddress());
  }

  @Override
  public String toString() {
    return _address.toString();
  }

  @Override
  public String getName() {
    return ((SipURI) _address.getURI()).getUser();
  }

  @Override
  public String getURI() {
    return _address.getURI().toString();
  }

  @Override
  public Address getSipAddress() {
    return _address;
  }

  @Override
  public SipURI getSipURI() throws IllegalArgumentException {
    final URI uri = _address.getURI();
    if (uri.isSipURI()) {
      return (SipURI) uri;
    }
    throw new IllegalArgumentException(_address.toString());
  }

  @Override
  public Call call(final Endpoint caller, final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException {
    final SIPOutgoingCall retval = new SIPOutgoingCall(_ctx, ((SIPEndpoint) caller), this, headers);
    retval.addListeners(listeners);
    return retval;
  }

  @Override
  public Call call(final Endpoint caller, final Map<String, String> headers, final Observer... observers) {
    final SIPOutgoingCall retval = new SIPOutgoingCall(_ctx, ((SIPEndpoint) caller), this, headers);
    retval.addObservers(observers);
    return retval;
  }

  @Override
  public Subscription subscribe(final Endpoint caller, final Type type, final int expiration,
      final EventListener<?>... listeners) throws SignalException {
    final SIPSubscriptionImpl retval = new SIPSubscriptionImpl(_ctx, type, expiration, caller, this);
    retval.subscribe();
    retval.addListeners(listeners);
    return retval;
  }

  @Override
  public Subscription subscribe(final Endpoint caller, final Type type, final int expiration,
      final Observer... observers) {
    final SIPSubscriptionImpl retval = new SIPSubscriptionImpl(_ctx, type, expiration, caller, this);
    retval.subscribe();
    retval.addObservers(observers);
    return retval;
  }

  @Override
  public void sendText(final TextableEndpoint from, final String text) throws IOException {
    sendText(from, text, null);
  }

  @Override
  public void sendText(final TextableEndpoint from, final String text, final String type) throws IOException {
    // TODO improve
    final SipServletRequest req = _ctx.getSipFactory().createRequest(_ctx.getSipFactory().createApplicationSession(),
        "MESSAGE", ((SIPEndpoint) from).getSipAddress(), _address);
    try {
      req.setContent(text, type == null ? "text/plain" : type);
      req.send();
    }
    catch (final Exception ex) {
      throw new SignalException(ex);
    }
  }
}
