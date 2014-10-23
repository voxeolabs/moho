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
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.JoinData;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.sip.SIPCall.State;
import com.voxeo.moho.util.SDPUtils;

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
    _call1.setJoiningPeer(new JoinData(_call2, _direction, _joinType));
    _call2.setJoiningPeer(new JoinData(_call1, _direction, _joinType));

    if (_call1.getMediaObject() == null) {
      if(_call1 instanceof SIPOutgoingCall){
        ((SIPOutgoingCall)_call1).setContinueRouting(_call2);
      }
      _call1.join(Direction.DUPLEX);
      return;

    }
    if (_call2.getMediaObject() == null) {
      if(_call2 instanceof SIPOutgoingCall){
        ((SIPOutgoingCall)_call2).setContinueRouting(_call1);
      }
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
    if (call != null && call instanceof IncomingCall) {
      final SipServletResponse res = call.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
      res.send();
    }
    else {
      MediaDialect dialect = ((ApplicationContextImpl)_call1.getApplicationContext()).getDialect();
      dialect.setDtmfPassThrough((NetworkConnection)_call1.getMediaObject(), dtmfPassThrough);
      dialect.setDtmfPassThrough((NetworkConnection)_call2.getMediaObject(), dtmfPassThrough);
      
      _call1.linkCall(_call2, _joinType, _direction);

      _call1.setJoiningPeer(null);
      _call2.setJoiningPeer(null);

      done(Cause.JOINED, null);
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    try {
      call.setSIPCallState(SIPCall.State.ANSWERED);
      call.processSDPAnswer(req);

      MediaDialect dialect = ((ApplicationContextImpl)_call1.getApplicationContext()).getDialect();
      dialect.setDtmfPassThrough((NetworkConnection)_call1.getMediaObject(), dtmfPassThrough);
      dialect.setDtmfPassThrough((NetworkConnection)_call2.getMediaObject(), dtmfPassThrough);
      
      _call1.linkCall(_call2, _joinType, _direction);

      _call1.setJoiningPeer(null);
      _call2.setJoiningPeer(null);
      done(Cause.JOINED, null);
    }
    catch (final Exception e) {
      done(Cause.ERROR, e);
      call.fail(e);
      throw e;
    }
  }
}
