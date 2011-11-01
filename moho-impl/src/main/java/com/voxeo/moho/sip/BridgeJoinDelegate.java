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

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.sip.SIPCall.State;

public class BridgeJoinDelegate extends JoinDelegate {

  protected BridgeJoinDelegate(final SIPCallImpl call1, final SIPCallImpl call2, final Direction direction,
      final JoinType type, final SIPCallImpl peer) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
    _joinType = type;
    _peer = peer;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    _call1.setBridgeJoiningPeer(_call2);
    _call2.setBridgeJoiningPeer(_call1);

    if (_call1.getMediaObject() == null) {
      _call1.join(Direction.DUPLEX);
      return;

    }
    if (_call2.getMediaObject() == null) {
      _call2.join(Direction.DUPLEX);
      return;
    }
    SIPCallImpl call = null;
    if (_call1.getSIPCallState() == State.PROGRESSED) {
      call = _call1;
    }
    else if (_call2.getSIPCallState() == State.PROGRESSED) {
      call = _call2;
    }
    if (call != null) {
      final SipServletResponse res = call.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
      if (call.getLocalSDP() != null) {
        res.setContent(call.getLocalSDP(), "application/sdp");
      }
      res.send();
    }
    else {
      _call1.linkCall(_call2, _joinType, _direction);

      _call1.setBridgeJoiningPeer(null);
      _call2.setBridgeJoiningPeer(null);

      done(Cause.JOINED, null);
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    try {
      call.setSIPCallState(SIPCall.State.ANSWERED);
      call.processSDPAnswer(req);

      _call1.linkCall(_call2, _joinType, _direction);

      _call1.setBridgeJoiningPeer(null);
      _call2.setBridgeJoiningPeer(null);
      done(Cause.JOINED, null);
    }
    catch (final Exception e) {
      done(Cause.ERROR, e);
      call.fail(e);
      throw e;
    }
  }
}
