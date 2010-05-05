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
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.SignalException;

public class SIPOutgoingCall extends SIPCallImpl {

  // private static final Logger LOG = Logger.getLogger(SIPOutgoingCall.class);

  protected SIPOutgoingCall(final ExecutionContext context, final SIPEndpoint from, final SIPEndpoint to,
      final Map<String, String> headers) {
    this(context, SIPHelper.createSipInitnalRequest(context.getSipFactory(), "INVITE", from.getSipAddress(), to
        .getSipAddress(), headers));
  }

  protected SIPOutgoingCall(final ExecutionContext context, final SipServletRequest req) {
    super(context, req);
    try {
      _signal.setHandler(((ApplicationContextImpl) getApplicationContext()).getController());
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
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
      if (sdp != null) {
        _invite.setContent(sdp, "application/sdp");
      }
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

}
