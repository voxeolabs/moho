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

import java.util.Map;

import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;

public abstract class JoinDelegate {

  protected boolean _isWaiting;

  protected Exception _exception;

  protected Object _condition;

  protected void done() {
    setWaiting(false);
    synchronized (_condition) {
      _condition.notifyAll();
    }
  }

  protected boolean isWaiting() {
    return _isWaiting;
  }

  protected void setWaiting(final boolean waiting) {
    _isWaiting = waiting;
  }

  protected void setException(final Exception e) {
    _exception = e;
  }

  protected Exception getException() {
    return _exception;
  }

  protected void setCondition(final Object o) {
    _condition = o;
  }

  protected Object getCondition() {
    return _condition;
  }

  protected abstract void doJoin() throws Exception;

  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doSdpEvent(final SdpPortManagerEvent event) {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doDisengage(final SIPCallImpl call, final JoinType type) {
    if (call.isDirectlyJoined()) {
      call.unlinkDirectlyPeer();
    }
    else if (call.isBridgeJoined() && type == JoinType.DIRECT) {
      for (final Participant p : call.getParticipants()) {
        call.unjoin(p);
      }
      call.destroyNetworkConnection();
    }
  }
}
