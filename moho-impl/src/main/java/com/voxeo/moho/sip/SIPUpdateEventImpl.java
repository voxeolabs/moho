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

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoUpdateEvent;

public class SIPUpdateEventImpl extends MohoUpdateEvent implements SIPUpdateEvent {

  private static final Logger LOG = Logger.getLogger(SIPUpdateEventImpl.class);

  protected SipServletRequest _req;

  protected SIPUpdateEventImpl(final Call source, final SipServletRequest req) {
    super(source);
    _req = req;
  }

  public SipServletRequest getSipRequest() {
    return _req;
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    this.checkState();
    _accepted = true;
    final SIPCallImpl call = (SIPCallImpl) this.source;
    try {
      call.doUpdate(_req, headers);
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized void reject(Reason reason, Map<String, String> headers) throws SignalException {
    checkState();
    _rejected = true;

    try {
      final SipServletResponse res = _req.createResponse(reason == null ? Reason.DECLINE.getCode() : reason.getCode());
      SIPHelper.addHeaders(res, headers);
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }
}
