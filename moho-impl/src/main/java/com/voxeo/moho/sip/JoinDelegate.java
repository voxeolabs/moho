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

import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.BusyException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.RejectException;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.TimeoutException;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.MohoJoinCompleteEvent;

public abstract class JoinDelegate {

  protected SettableJointImpl _settableJoint;

  protected SIPCallImpl _call1;

  protected SIPCallImpl _call2;

  protected JoinType _joinType;

  protected Direction _direction;

  protected boolean done;

  protected Cause _cause;

  protected Exception _exception;

  public void setSettableJoint(SettableJointImpl settableJoint) {
    _settableJoint = settableJoint;
  }

  public synchronized void done(final Cause cause, Exception exception) {
    if (done) {
      return;
    }

    _cause = cause;
    _exception = exception;

    _call1.joinDone();
    JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(_call1, _call2, cause, exception, true);
    _call1.dispatch(joinCompleteEvent);

    if (_call2 != null) {
      _call2.joinDone();
      JoinCompleteEvent peerJoinCompleteEvent = new MohoJoinCompleteEvent(_call2, _call1, cause, exception, false);
      _call2.dispatch(peerJoinCompleteEvent);
    }

    _settableJoint.done(joinCompleteEvent);
    done = true;
  }

  public JoinType getJoinType() {
    return _joinType;
  }

  public SIPCallImpl getInitiator() {
    return _call1;
  }

  public SIPCallImpl getPeer() {
    return _call2;
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

  protected Exception getExceptionByResponse(SipServletResponse res) {
    Exception e = null;
    if (SIPHelper.isBusy(res)) {
      e = new BusyException();
    }
    else if (SIPHelper.isRedirect(res)) {
      e = new RedirectException(res.getHeaders("Contact"));
    }
    else if (SIPHelper.isTimeout(res)) {
      e = new TimeoutException();
    }
    else if (SIPHelper.isDecline(res)) {
      e = new RejectException();
    }
    else {
      e = new RejectException();
    }

    return e;
  }

  protected CallCompleteEvent.Cause getCallCompleteCauseByResponse(SipServletResponse res) {
    CallCompleteEvent.Cause cause = null;
    if (SIPHelper.isBusy(res)) {
      cause = CallCompleteEvent.Cause.BUSY;
    }
    else if (SIPHelper.isRedirect(res)) {
      cause = CallCompleteEvent.Cause.REDIRECT;
    }
    else if (SIPHelper.isTimeout(res)) {
      cause = CallCompleteEvent.Cause.TIMEOUT;
    }
    else if (SIPHelper.isDecline(res)) {
      cause = CallCompleteEvent.Cause.DECLINE;
    }
    else {
      cause = CallCompleteEvent.Cause.ERROR;
    }

    return cause;
  }

  protected JoinCompleteEvent.Cause getJoinCompleteCauseByResponse(SipServletResponse res) {
    JoinCompleteEvent.Cause cause = null;
    if (SIPHelper.isBusy(res)) {
      cause = JoinCompleteEvent.Cause.BUSY;
    }
    else if (SIPHelper.isRedirect(res)) {
      cause = JoinCompleteEvent.Cause.REDIRECT;
    }
    else if (SIPHelper.isTimeout(res)) {
      cause = JoinCompleteEvent.Cause.TIMEOUT;
    }
    else if (SIPHelper.isDecline(res)) {
      cause = JoinCompleteEvent.Cause.REJECT;
    }
    else {
      cause = JoinCompleteEvent.Cause.ERROR;
    }
    return cause;
  }

  public Cause getCause() {
    return _cause;
  }

  public Exception getException() {
    return _exception;
  }
}
