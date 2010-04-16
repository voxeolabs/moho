/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.util.SessionUtils;

public class SIPSubscriptionImpl extends DispatchableEventSource implements SIPSubscription {

  protected SipSession _session;

  protected Subscription.Type _type;

  protected int _expiration;

  protected Endpoint _from;

  protected Endpoint _to;

  protected Endpoint _uri;

  public SIPSubscriptionImpl(final ExecutionContext applicationContext, final Subscription.Type type,
      final int expiration, final Endpoint from, final Endpoint to) {
    this(applicationContext, type, expiration, from, to, to);
  }

  public SIPSubscriptionImpl(final ExecutionContext applicationContext, final Subscription.Type type,
      final int expiration, final Endpoint from, final Endpoint to, final Endpoint uri) {
    super(applicationContext);
    _from = from;
    _to = to;
    _uri = uri;
    _type = type;
    _expiration = expiration;
    subscribe(from, to, uri, expiration);
  }

  public SipSession getSipSession() {
    return _session;
  }

  public int getExpiration() {
    return _expiration;
  }

  public String getType() {
    return _type.name();
  }

  public void renew() {
    if (_session != null && _session.isValid()) {
      try {
        final SipServletRequest req = _session.createRequest("SUBSCRIBE");
        if (_type == Subscription.Type.DIALOG) {
          req.addHeader("Event", "dialog");
          req.addHeader("Accept", "application/dialog-info+xml");
        }
        else if (_type == Subscription.Type.PRESENCE) {
          req.addHeader("Event", "presence");
          req.addHeader("Accept", "application/pidf+xml");
        }
        else if (_type == Subscription.Type.REFER) {
          req.addHeader("Event", "refer");
        }
        req.setExpires(_expiration);
        req.send();
      }
      catch (final Exception t) {
        throw new SignalException(t);
      }
    }
    else {
      subscribe(_from, _to, _uri, _expiration);
    }
  }

  private void subscribe(final Endpoint from, final Endpoint to, final Endpoint uri, final int expiration) {
    try {
      final SipServletRequest req = _context.getSipFactory().createRequest(
          _context.getSipFactory().createApplicationSession(), "SUBSCRIBE", ((SIPEndpoint) from).getSipAddress(),
          ((SIPEndpoint) to).getSipAddress());
      if (_type == Subscription.Type.DIALOG) {
        req.addHeader("Event", "dialog");
        req.addHeader("Accept", "application/dialog-info+xml");
      }
      else if (_type == Subscription.Type.PRESENCE) {
        req.addHeader("Event", "presence");
        req.addHeader("Accept", "application/pidf+xml");
      }
      else if (_type == Subscription.Type.REFER) {
        req.addHeader("Event", "refer");
      }

      _session = req.getSession();
      _session.setHandler(((ApplicationContextImpl) getApplicationContext()).getController());

      SessionUtils.setEventSource(_session, this);
      if (uri != null) {
        req.setRequestURI(_context.getSipFactory().createURI(uri.getURI()));
      }
      req.setExpires(expiration);
      req.send();
    }
    catch (final Exception t) {
      throw new SignalException(t);
    }
  }

  public Endpoint getAddress() {
    return _to;
  }

}
