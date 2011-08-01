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
import com.voxeo.moho.sip.SIPCall.State;

public class BridgeJoinDelegate extends JoinDelegate {

  protected SIPCallImpl _call1;

  protected SIPCallImpl _call2;

  protected Direction _direction;

  protected BridgeJoinDelegate(final SIPCallImpl call1, final SIPCallImpl call2, final Direction direction) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
  }

  @Override
  protected void doJoin() throws Exception {
    _call1.setBridgeJoiningPeer(_call2);
    _call2.setBridgeJoiningPeer(_call1);

    if (_call1.getMediaObject() == null) {
      _call1.joinWithoutCheckOperation(Direction.DUPLEX);
    }
    if (_call2.getMediaObject() == null) {
      _call2.joinWithoutCheckOperation(Direction.DUPLEX);
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
      this.setWaiting(true);
      while (!_call1.isTerminated() && !_call2.isTerminated() && this.isWaiting()) {
        try {
          getCondition().wait();
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (!_call1.isAnswered() || !_call1.isAnswered()) {
        throw new IllegalStateException(call + " is no answered.");
      }
    }

    _call1.linkCall(_call2, JoinType.BRIDGE, _direction);

    _call1.setBridgeJoiningPeer(null);
    _call2.setBridgeJoiningPeer(null);
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    try {
      call.setSIPCallState(SIPCall.State.ANSWERED);
      call.processSDPAnswer(req);
      done();
    }
    catch (final Exception e) {
      setError(e);
      call.fail(e);
      throw e;
    }
  }
}
