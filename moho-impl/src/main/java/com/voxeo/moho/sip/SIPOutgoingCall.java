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
import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.util.SessionUtils;

public class SIPOutgoingCall extends SIPCallImpl {

  protected SIPEndpoint _from;

  protected SipApplicationSession _appSession;

  protected Map<String, String> _headers;

  protected SIPOutgoingCall(final ExecutionContext context, final SIPEndpoint from, final SIPEndpoint to,
      final Map<String, String> headers) {
    super(context);
    _address = to;
    _from = from;
    _headers = headers;
  }

  @Override
  protected JoinDelegate createJoinDelegate(final Direction direction) {
    JoinDelegate retval = null;
    if (isNoAnswered()) {
      retval = new Media2NOJoinDelegate(this);
    }
    else if (isAnswered()) {
      retval = new Media2AOJoinDelegate(this);
    }
    else {
      throw new IllegalStateException("The SIPCall state is " + getSIPCallState());
    }
    return retval;
  }

  @Override
  protected JoinDelegate createJoinDelegate(final SIPCallImpl other, final JoinType type, final Direction direction) {
    JoinDelegate retval = null;
    if (type == JoinType.DIRECT) {
      if (isNoAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNO2NOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2NOJoinDelegate((SIPIncomingCall) other, this, direction);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNO2AOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2NOJoinDelegate((SIPIncomingCall) other, this, direction);
          }
        }
      }
      else if (isAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNO2AOJoinDelegate((SIPOutgoingCall) other, this, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AOJoinDelegate((SIPIncomingCall) other, this, direction);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAO2AOJoinDelegate(this, (SIPOutgoingCall) other, direction);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2AOJoinDelegate((SIPIncomingCall) other, this, direction);
          }
        }
      }
    }
    else {
      retval = new BridgeJoinDelegate(this, other, direction);
    }
    return retval;
  }

  protected synchronized void call(final byte[] sdp) throws IOException {
    if (isNoAnswered()) {
      if (_invite == null) {
        createRequest();
      }
      if (sdp != null) {
        _invite.setContent(sdp, "application/sdp");
      }
      setSIPCallState(SIPCall.State.INVITING);
      _invite.send();
    }
    else if (isAnswered()) {
      final SipServletRequest reinvte = _signal.createRequest("INVITE");
      if (sdp != null) {
        reinvte.setContent(sdp, "application/sdp");
      }
      reinvte.send();
    }
  }

  /**
   * create INVITE request and send
   * 
   * @param sdp
   *          sdp used in INVITE request.
   * @param appSession
   *          applicationSession that used to create INVITE request.
   * @throws IOException
   */
  protected synchronized void call(final byte[] sdp, final SipApplicationSession appSession) throws IOException {
    if (_appSession == null) {
      _appSession = appSession;
    }

    call(sdp);
  }

  /**
   * create INVITE request and send
   * 
   * @param sdp
   *          sdp used in INVITE request.
   * @param appSession
   *          applicationSession that used to create INVITE request.
   * @throws IOException
   */
  protected synchronized void call(final byte[] sdp, final SipApplicationSession appSession, final String replacesHeader)
      throws IOException {
    if (_appSession == null) {
      _appSession = appSession;
    }

    if (replacesHeader != null) {
      if (_headers == null) {
        _headers = new HashMap<String, String>();
      }
      _headers.put("Replaces", replacesHeader);
    }

    call(sdp);
  }

  private void createRequest() {
    _invite = SIPHelper.createSipInitnalRequest(_context.getSipFactory(), "INVITE", _from.getSipAddress(), _address
        .getSipAddress(), _headers, _appSession);

    _signal = _invite.getSession();

    if (_appSession == null) {
      _appSession = _signal.getApplicationSession();
    }

    SessionUtils.setEventSource(_signal, this);

    try {
      _signal.setHandler(((ApplicationContextImpl) getApplicationContext()).getController());
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }
}
