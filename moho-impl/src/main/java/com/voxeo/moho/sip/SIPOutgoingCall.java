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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.util.Utils;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.SessionUtils;

public class SIPOutgoingCall extends SIPCallImpl implements OutgoingCall {
  private static final Logger LOG = Logger.getLogger(SIPCallImpl.class);

  protected SIPEndpoint _from;

  protected SipApplicationSession _appSession;

  protected Map<String, String> _headers;

  protected SIPCall _continueRoutingOrigCall;

  protected SIPOutgoingCall(final ExecutionContext context, final SIPEndpoint from, final SIPEndpoint to,
      final Map<String, String> headers) {
    this(context, from, to, headers, null);
  }
  
  protected SIPOutgoingCall(final ExecutionContext context, final SIPEndpoint from, final SIPEndpoint to,
      final Map<String, String> headers, SIPCall originalCall) {
    super(context);
    if (from == null || to == null) {
      throw new IllegalArgumentException("to or from can't be null");
    }
    _address = to;
    _from = from;
    _headers = headers;
    if(originalCall != null){
      setContinueRouting(originalCall);
    }
    createRequest();
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
            retval = new DirectNO2NOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2NOJoinDelegate((SIPIncomingCall) other, this, direction, (SIPIncomingCall) other);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNO2AOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2NOJoinDelegate((SIPIncomingCall) other, this, direction, (SIPIncomingCall) other);
          }
        }
      }
      else if (isAnswered()) {
        if (other.isNoAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectNO2AOJoinDelegate((SIPOutgoingCall) other, this, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectNI2AOJoinDelegate((SIPIncomingCall) other, this, direction, (SIPIncomingCall) other);
          }
        }
        else if (other.isAnswered()) {
          if (other instanceof SIPOutgoingCall) {
            retval = new DirectAO2AOJoinDelegate(this, (SIPOutgoingCall) other, direction, (SIPOutgoingCall) other);
          }
          else if (other instanceof SIPIncomingCall) {
            retval = new DirectAI2AOJoinDelegate((SIPIncomingCall) other, this, direction, (SIPIncomingCall) other);
          }
        }
      }
    }
    else {
      retval = new BridgeJoinDelegate(this, other, direction, type, other);
    }
    return retval;
  }
  
  protected JoinDelegate createJoinDelegate(final Call[] others, final JoinType type, final Direction direction) {
    if (this.isNoAnswered() && type == JoinType.DIRECT) {
      JoinDelegate retval = null;
      List<SIPCallImpl> candidates = new LinkedList<SIPCallImpl>();
      for (Call call : others) {
        candidates.add((SIPCallImpl) call);
      }
      retval = new DirectNO2MultipleNOJoinDelegate(type, direction, this,
          Utils.suppressEarlyMedia(getApplicationContext()), candidates);
      return retval;
    }
    else {
      return super.createJoinDelegate(others, type, direction);
    }
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
      reInviteRemote(sdp, null, null);
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
    try {
      _invite = SIPHelper.createSipInitnalRequest(_context.getSipFactory(), "INVITE", _from.getSipAddress(),
          _address.getSipAddress(), _headers, _appSession, _continueRoutingOrigCall!= null ? _continueRoutingOrigCall.getSipRequest() : null, this.getApplicationContext());
  
      _signal = _invite.getSession();
  
      if (_appSession == null) {
        _appSession = _signal.getApplicationSession();
      }
  
      SessionUtils.setEventSource(_signal, this);
    
      _signal.setHandler(((ApplicationContextImpl) getApplicationContext()).getSIPController().getServletName());
    }
    catch (final Exception e) {
      if(_invite != null && _invite.getSession() != null){
        try{
          _invite.getSession().invalidate();
        }
        catch(Exception ex){
          LOG.error("Exception when invalidating session:"+ _invite);
        }
        _invite = null;
      }
      ((ExecutionContext)getApplicationContext()).removeCall(this.getId());
      throw new SignalException(e);
    }
  }

  @Override
  public byte[] getJoinSDP() throws IOException {
    this.call(null);
    return null;
  }

  @Override
  public void processSDPAnswer(byte[] sdp) throws IOException {
    if (_inviteResponse != null) {
      SipServletRequest ack = _inviteResponse.createAck();
      ack.setContent(sdp, "application/sdp");
      ack.send();
    }
    else {
      throw new IllegalStateException("");
    }
  }

  @Override
  public byte[] processSDPOffer(byte[] sdp) throws IOException {
    this.call(sdp);
    return null;
  }

  public void setContinueRouting(final SIPCall origCall) {
    _continueRoutingOrigCall = origCall;
  }
  
  protected int getGlareReInivteDelay() {
    Random random = new Random();
    int delay = (random.nextInt(191) + 210) * 10;
    return delay;
  }
}
