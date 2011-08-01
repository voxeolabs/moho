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
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Participant.JoinType;

public class DirectAI2AIJoinDelegate extends JoinDelegate {

  protected SIPIncomingCall _call1;

  protected SIPIncomingCall _call2;

  protected Direction _direction;

  protected DirectAI2AIJoinDelegate(final SIPIncomingCall call1, final SIPIncomingCall call2, final Direction direction) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
  }

  @Override
  protected void doJoin() throws IOException {
    final SipServletRequest req = _call2.getSipSession().createRequest("INVITE");
    if (_call1.getRemoteSdp() != null) {
      req.setContent(_call1.getRemoteSdp(), "application/sdp");
    }
    req.send();
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    if (SIPHelper.isErrorResponse(res)) {
      setException(getExceptionByResponse(res));
      done();
    }
    else {
      try {
        if (SIPHelper.isSuccessResponse(res)) {
          res.createAck().send();
        }
        if (_call2.equals(call)) {
          final SipServletRequest req = _call1.getSipSession().createRequest("INVITE");
          SIPHelper.copyContent(res, req);
          req.send();
        }
        else if (_call1.equals(call)) {
          doDisengage(_call1, JoinType.DIRECT);
          doDisengage(_call2, JoinType.DIRECT);
          _call1.linkCall(_call2, JoinType.DIRECT, _direction);
          done();
        }
      }
      catch (final Exception e) {
        setError(e);
        done();
        throw e;
      }
    }
  }
}
