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

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Participant.JoinType;

public class DirectAI2AOJoinDelegate extends JoinDelegate {

  protected SIPIncomingCall _call1;

  protected SIPOutgoingCall _call2;

  protected Direction _direction;

  protected SipServletResponse _response;

  protected DirectAI2AOJoinDelegate(final SIPIncomingCall call1, final SIPOutgoingCall call2, final Direction direction) {
    _call1 = call1;
    _call2 = call2;
    _direction = direction;
  }

  @Override
  protected void doJoin() throws MsControlException, IOException {
    _call2.call(null);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    if (SIPHelper.isErrorResponse(res)) {
      setException(getExceptionByResponse(res));
      done();
    }
    else if (SIPHelper.isSuccessResponse(res)) {
      try {
        if (_call2.equals(call)) {
          _response = res;
          final SipServletRequest req = _call1.getSipSession().createRequest("INVITE");
          SIPHelper.copyContent(res, req);
          req.send();
        }
        else if (_call1.equals(call)) {
          final SipServletRequest ack1 = res.createAck();
          SipServletRequest ack2 = null;
          if (_response != null) {
            final SipServletResponse origRes = _response;
            _response = null;
            ack2 = origRes.createAck();
            SIPHelper.copyContent(res, ack2);
          }
          ack1.send();
          ack2.send();
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
