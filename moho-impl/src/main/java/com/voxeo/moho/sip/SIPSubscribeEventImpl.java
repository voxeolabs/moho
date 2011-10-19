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

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Framework;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.MohoSubscribeEvent;
import com.voxeo.moho.sip.SIPEndpoint;
import com.voxeo.moho.sip.SIPSubscribeEvent;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.SpiFramework;

public class SIPSubscribeEventImpl extends MohoSubscribeEvent implements SIPSubscribeEvent {

  protected SipServletRequest _req;
  
  protected ExecutionContext _ctx;

  protected SIPSubscribeEventImpl(final Framework source, final SipServletRequest req) {
    super(source);
    _ctx = ((SpiFramework) source).getExecutionContext();
    _req = req;
  }

  public SipServletRequest getSipRequest() {
    return _req;
  }
  
  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
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
  public synchronized void reject(Reason reason, Map<String, String> headers) throws SignalException {
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

  @Override
  public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {
    proxyTo(recordRoute, parallel, null, destinations);
  }

  @Override
  public synchronized void proxyTo(boolean recordRoute, boolean parallel, Map<String, String> headers, Endpoint... destinations) {
    checkState();
    _proxied = true;
    SIPHelper.proxyTo(getSource().getApplicationContext().getSipFactory(), _req, headers, recordRoute, parallel, destinations);
  }

  @Override
  public SubscriptionContext getSubscription() {
    try {
      Address from = _req.getAddressHeader("From");
      Address to = _req.getAddressHeader("To");
      return (SubscriptionContext) Class.forName("com.voxeo.moho.presence.sip.impl.SIPSubscriptionContextImpl")
          .getConstructor(new Class<?>[] {URI.class, URI.class, SipServletRequest.class}).newInstance(from.getURI(), to.getURI(), _req);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException("Can't find presence module", e);
    }
    catch (ServletParseException e) {
      throw new IllegalArgumentException(e);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
