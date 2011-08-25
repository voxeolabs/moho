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

import java.io.IOException;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectNI2NIJoinDelegate extends JoinDelegate {

  protected Direction _direction;

  protected DirectNI2NIJoinDelegate(final SIPIncomingCall call1, final SIPIncomingCall call2, final Direction direction) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
  }

  @Override
  protected void doJoin() throws IOException, MsControlException {
    final SipServletResponse res1 = _call1.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
    if (_call2.getRemoteSdp() != null) {
      res1.setContent(_call2.getRemoteSdp(), "application/sdp");
    }
    res1.send();
    final SipServletResponse res2 = _call2.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
    if (_call1.getRemoteSdp() != null) {
      res2.setContent(_call1.getRemoteSdp(), "application/sdp");
    }
    res2.send();
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws MsControlException {
    call.setSIPCallState(State.ANSWERED);
    if (_call1.equals(call)) {
      _call1.linkCall(_call2, JoinType.DIRECT, _direction);
      done(JoinCompleteEvent.Cause.JOINED, null);
    }
  }

}
