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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Framework;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.MohoRegisterEvent;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPRegisterEventImpl extends MohoRegisterEvent implements SIPRegisterEvent {

  protected SipServletRequest _req;

  protected ExecutionContext _ctx;

  protected Contact[] _contacts;
  
  protected Endpoint _endpoint;

  class ContactImpl implements Contact {
    Endpoint _endpoint;
    int _expiration;

    ContactImpl(Endpoint ep, int expiration) {
      _endpoint = ep;
      _expiration = expiration;
    }

    @Override
    public Endpoint getEndpoint() {
      return _endpoint;
    }

    @Override
    public int getExpiration() {
      return _expiration;
    }

    @Override
    public String toString() {
      return _endpoint.toString() + ";expiration=" + _expiration;
    }

    @Override
    public int hashCode() {
      return _endpoint.hashCode() + _expiration;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Contact) {
        Contact c = (Contact) o;
        if (!_endpoint.equals(((Contact) o).getEndpoint())) {
          return false;
        }
        if (_expiration != c.getExpiration()) {
          return false;
        }
        return true;
      }
      return false;
    }
  }

  protected SIPRegisterEventImpl(final Framework source, final SipServletRequest req) {
    super(source);
    _req = req;
    _ctx = (ExecutionContext) source.getApplicationContext();
    _endpoint = new SIPEndpointImpl(_ctx, _req.getFrom());
  }

  @Override
  public SipServletRequest getSipRequest() {
    return _req;
  }

  @Override
  public Endpoint getEndpoint() {
    return _endpoint;
  }

  @Override
  public synchronized Contact[] getContacts() {
    if (_contacts == null) {
      final List<Contact> retval = new ArrayList<Contact>();
      try {
        final ListIterator<Address> headers = _req.getAddressHeaders("Contact");
        int expiration = _req.getExpires();
        while (headers.hasNext()) {
          Address addr = headers.next();
          Endpoint ep = new SIPEndpointImpl(_ctx, addr);
          int exp = addr.getExpires();
          if (exp == 0) {
            exp = expiration;
          }
          Contact contact = new ContactImpl(ep, exp);
          retval.add(contact);
        }
      }
      catch (final ServletParseException e) {
        throw new IllegalArgumentException(e);
      }
      _contacts = retval.toArray(new Contact[retval.size()]);
    }
    return _contacts;
  }

  @Override
  public synchronized void accept(final Contact[] contacts, final Map<String, String> headers) {
    this.checkState();
    _accepted = true;
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    for (final Contact contact : contacts) {
      res.addHeader("Contact", contact.toString());
    }
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized void reject(final Reason reason, final Map<String, String> headers) {
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
  public synchronized void redirect(Endpoint o, Map<String, String> headers) throws SignalException {
    checkState();
    _redirected = true;

    if (o instanceof SIPEndpoint) {
      final SipServletResponse res = _req.createResponse(SipServletResponse.SC_MOVED_TEMPORARILY);
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
}
