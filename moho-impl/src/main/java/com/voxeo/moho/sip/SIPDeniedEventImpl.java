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

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MohoDeniedEvent;

// TODO 401 and 407

public class SIPDeniedEventImpl<T extends EventSource> extends MohoDeniedEvent<T> implements SIPDeniedEvent<T> {

  protected SipServletResponse _res;

  protected SIPDeniedEventImpl(final T source, final SipServletResponse res) {
    super(source);
    _res = res;
  }

  @Override
  public SipServletResponse getSipResponse() {
    return _res;
  }

  @Override
  public Reason getReason() {
    switch (_res.getStatus()) {
      case SipServletResponse.SC_BUSY_HERE:
      case SipServletResponse.SC_BUSY_EVERYWHERE:
        return Reason.BUSY;

      case SipServletResponse.SC_REQUEST_TIMEOUT:
        return Reason.TIMEOUT;

      case SipServletResponse.SC_DECLINE:
        return Reason.DECLINE;
      default:
        return Reason.OTHER;
    }
  }
}
