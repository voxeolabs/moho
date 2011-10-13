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

package com.voxeo.moho.reg.impl.sip;

import java.io.IOException;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.reg.sip.SIPRegistration;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.SessionUtils;

// TODO AUTH INFO
public class SIPRegistrationImpl extends DispatchableEventSource implements SIPRegistration {

  protected SipApplicationSession _session;

  protected Address _from;

  protected URI _target;

  protected Address[] _contacts;

  protected int _expiration;

  protected SIPRegistrationImpl(final ExecutionContext ctx, final Endpoint ep, final Endpoint target,
      final Endpoint... contacts) {
    this(ctx, ep, target, 3600, contacts);
  }

  protected SIPRegistrationImpl(final ExecutionContext ctx, final Endpoint ep, final Endpoint target,
      final int expiration, final Endpoint... contacts) {
    super(ctx);
    try {
      _from = ctx.getSipFactory().createAddress(ep.getURI().toString());
      _target = ctx.getSipFactory().createURI(target.getURI().toString());
      _contacts = new Address[contacts.length];
      for (int i = 0; i < contacts.length; i++) {
        _contacts[i] = ctx.getSipFactory().createAddress(contacts[i].getURI().toString());
      }
    }
    catch (final ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
    _expiration = expiration;
    _session = ctx.getSipFactory().createApplicationSession();
  }

  public void login() {
    sendRegister(_expiration, _contacts);
  }

  public void logout() {
    sendRegister(0, null);
  }

  public void renew() {
    login();
  }

  private void sendRegister(final int expiration, final Address[] contacts) {
    final SipServletRequest req = _context.getSipFactory().createRequest(_session, "REGISTER", _from, _from);
    req.setExpires(expiration);
    if (contacts != null) {
      for (final Address contact : contacts) {
        req.addAddressHeader("Contact", contact, false);
      }
    }
    SessionUtils.setEventSource(req.getSession(), this);
    try {
      req.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

}
