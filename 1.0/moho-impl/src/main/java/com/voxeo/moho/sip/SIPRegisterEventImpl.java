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
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.ApplicationEventSource;

public class SIPRegisterEventImpl extends SIPRegisterEvent {

  public SIPRegisterEventImpl(final ApplicationEventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public Endpoint getEndpoint() {
    return _ctx.getEndpoint(_req.getFrom().getURI().toString());
  }

  @Override
  public Endpoint[] getContacts() {
    final List<Endpoint> retval = new ArrayList<Endpoint>();
    ListIterator<Address> headers = null;
    try {
      headers = _req.getAddressHeaders("Contact");
    }
    catch (ServletParseException e) {
      e.printStackTrace();
    }
    while (headers.hasNext()) {
      retval.add(_ctx.getEndpoint(headers.next().getURI().toString()));
    }
    return retval.toArray(new Endpoint[retval.size()]);
  }

  @Override
  public int getExpiration() {
    // TODO
    return _req.getExpires();
  }

  @Override
  public synchronized void accept(final Endpoint[] contacts, final int expiration, final Map<String, String> headers) {
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    for (final Endpoint contact : contacts) {
      res.addHeader("Contact", contact.toString());
    }
    res.setExpires(expiration);
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
    final SipServletResponse res = _req.createResponse(reason == null ? Reason.DECLINE.getCode() : reason.getCode());
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

}
