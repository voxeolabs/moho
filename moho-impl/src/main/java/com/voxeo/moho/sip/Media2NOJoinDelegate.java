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

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Constants;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class Media2NOJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(Media2NOJoinDelegate.class);

  protected boolean processedAnswer = false;

  protected SipServletResponse _response;

  protected SipServletResponse _earlyMediaResponse;

  protected Media2NOJoinDelegate(final SIPOutgoingCall call) {
    _call1 = call;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    _call1.processSDPOffer((SipServletMessage) null);
  }

  @Override
  protected void doSdpEvent(final SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
        || event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        try {
          final byte[] sdp = event.getMediaServerSdp();
          _call1.setLocalSDP(sdp);
          ((SIPOutgoingCall) _call1).call(sdp);
          return;
        }
        catch (final Exception e) {
          done(Cause.ERROR, e);
          _call1.fail(e);
        }
      }

      Exception ex = new NegotiateException(event);
      done(Cause.ERROR, ex);
      _call1.fail(ex);
    }
    else if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_PROCESSED)) {
      if (event.isSuccessful()) {
        if (processedAnswer && _call1.getSIPCallState() == SIPCall.State.ANSWERED) {
          try {
            _response.createAck().send();
            done(JoinCompleteEvent.Cause.JOINED, null);
          }
          catch (IOException e) {
            LOG.error("IOException when sending back ACK", e);
            Exception ex = new NegotiateException(e);
            done(Cause.ERROR, ex);
            _call1.fail(ex);
          }
        }
        else if (processedAnswer && _call1.getSIPCallState() == SIPCall.State.PROGRESSING) {
          // accept early media
          if (_earlyMediaResponse.getAttribute(Constants.Attribute_AcceptEarlyMedia) != null
              && _call1.getJoiningPeer() != null) {
            try {
              // bridge join peer is not null
              if (_call1.getJoiningPeer().getParticipant().getMediaObject() == null) {
                if (_call1.getJoiningPeer().getParticipant() instanceof SIPIncomingCall) {
                  SIPIncomingCall incomingCall = (SIPIncomingCall) _call1.getJoiningPeer().getParticipant();
                  if (incomingCall.getSIPCallState() == SIPCall.State.INVITING
                      || incomingCall.getSIPCallState() == SIPCall.State.RINGING) {
                    ((SIPIncomingCall) _call1.getJoiningPeer().getParticipant()).acceptWithEarlyMedia();
                  }
                }
                else {
                  ((SIPOutgoingCall) _call1.getJoiningPeer().getParticipant()).join().get();
                }
              }

              if (_call1.getMediaObject() instanceof Joinable
                  && _call1.getJoiningPeer().getParticipant().getMediaObject() instanceof Joinable) {
                JoinDelegate.bridgeJoin(_call1, _call1.getJoiningPeer().getParticipant(), Direction.DUPLEX);
              }
              // direct join peer is not null and peer is a not-answerd incoming
              // call, accept with early media. and join two networkconnection
            }
            catch (final Exception e) {
              throw new SignalException(e);
            }
          }
          
          _call1.setSIPCallState(SIPCall.State.PROGRESSED);
          
          synchronized (this) {
            this.notify();
          }
        }
        return;
      }
      Exception ex = new NegotiateException(event);
      done(Cause.ERROR, ex);
      _call1.fail(ex);
    }

    Exception ex = new NegotiateException(event);
    done(Cause.ERROR, ex);
    _call1.fail(ex);
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    try {
      if (SIPHelper.isProvisionalResponse(res)) {
        _call1.setSIPCallState(SIPCall.State.ANSWERING);

        if (res.getStatus() == SipServletResponse.SC_SESSION_PROGRESS) {
          _earlyMediaResponse = res;
          _call1.setSIPCallState(SIPCall.State.PROGRESSING);
          if (SIPHelper.getRawContentWOException(res) != null) {
            if (!processedAnswer) {
              processedAnswer = true;
              _call1.processSDPAnswer(res);
            }
          }

          try {
            res.createPrack().send();
          }
          catch (Rel100Exception ex) {
            LOG.warn(ex.getMessage());
          }
          catch (IllegalStateException ex) {
            LOG.warn(ex.getMessage());
          }
        }
      }
      else if (SIPHelper.isSuccessResponse(res)) {
        _call1.setSIPCallState(SIPCall.State.ANSWERED);
        _response = res;

        if (!processedAnswer) {
          processedAnswer = true;
          _call1.processSDPAnswer(res);
        }
        else {
          res.createAck().send();
          done(JoinCompleteEvent.Cause.JOINED, null);
        }
      }
      else if (SIPHelper.isErrorResponse(res)) {
        Exception e = getExceptionByResponse(res);
        done(this.getJoinCompleteCauseByResponse(res), e);

        call.disconnect(true, getCallCompleteCauseByResponse(res), e, null);
      }
    }
    catch (final Exception e) {
      done(Cause.ERROR, e);
      _call1.fail(e);
      throw e;
    }
  }

}
