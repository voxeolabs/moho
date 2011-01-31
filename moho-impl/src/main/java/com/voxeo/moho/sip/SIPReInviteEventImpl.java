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

import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import org.apache.log4j.Logger;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventSource;

public class SIPReInviteEventImpl extends SIPReInviteEvent {

  private static final Logger LOG = Logger.getLogger(SIPReInviteEventImpl.class);

  protected Boolean isHold;

  protected SIPReInviteEventImpl(final EventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    this.checkState();
    _accepted = true;
    final SIPCallImpl call = (SIPCallImpl) this.source;
    try {
      call.doReinvite(_req, headers);
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public boolean isHold() {
    if (isHold == null) {
      try {
        final byte[] content = SIPHelper.getRawContentWOException(_req);
        if (content != null) {
          final String sdp = new String(content, "iso8859-1");
          // TODO improve the parse.
          if (sdp.indexOf("sendonly") > 0) {
            isHold = Boolean.TRUE;
            return isHold;
          }
        }
      }
      catch (final Exception ex) {
        LOG.error("", ex);
      }
      isHold = false;
    }
    return isHold;
  }
}
