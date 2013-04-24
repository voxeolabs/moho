package com.voxeo.moho.sip;

import java.util.List;
import java.util.Map;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.sdp.SessionDescription;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.sip.SIPCall.State;

public class DirectNI2MultipleNOJoinDelegate extends JoinDelegate {
  private static final Logger LOG = Logger.getLogger(DirectNI2MultipleNOJoinDelegate.class);

  protected List<SIPCallImpl> candidateCalls;

  protected boolean _suppressEarlyMedia;

  protected SipServletResponse _response;

  protected boolean call1Processed;

  protected Object syncLock = new Object();

  protected DirectNI2MultipleNOJoinDelegate(JoinType type, Direction direction, SIPCallImpl call1,
      boolean suppressEarlyMedia, List<SIPCallImpl> others) {
    _call1 = call1;
    _suppressEarlyMedia = suppressEarlyMedia;
    _direction = direction;
    _joinType = type;
    candidateCalls = others;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    if (_call1.getSIPCallState() == SIPCall.State.PROGRESSED) {
      call1Processed = true;
    }

    for (SIPCallImpl call : candidateCalls) {
      ((SIPOutgoingCall) call).setContinueRouting(_call1);
      SIPHelper.remove100relSupport(call.getSipInitnalRequest());

      if (_suppressEarlyMedia) {
        SessionDescription mockSDP = _call1.createSendonlySDP(_call1.getRemoteSdp());
        ((SIPOutgoingCall) call).call(mockSDP.toString().getBytes("iso8859-1"), _call1.getSipSession()
            .getApplicationSession());
      }
      else {
        if (call1Processed) {
          ((SIPOutgoingCall) call).call(null, _call1.getSipSession().getApplicationSession());
        }
        else {
          ((SIPOutgoingCall) call).call(_call1.getRemoteSdp(), _call1.getSipSession().getApplicationSession(),
              _call1.useReplacesHeader());
        }
      }
    }
  }

  @Override
  protected void doInviteResponse(SipServletResponse res, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    try {
      synchronized (syncLock) {
        if (SIPHelper.isProvisionalResponse(res)) {
          SIPHelper.trySendPrack(res);
          return;
        }
        else if (SIPHelper.isSuccessResponse(res)) {
          if (_call1.equals(call)) {
            res.createAck().send();
            SipServletRequest ack2 = _response.createAck();
            if (call1Processed) {
              SIPHelper.copyContent(res, ack2);
            }
            _call2.setSIPCallState(State.ANSWERED);
            ack2.send();
            successJoin();
          }
          else {
            if (_response != null && !call.equals(_call2)) {
              // receive second success response, ignore it.
              return;
            }
            
            if (_response == null) {
              _response = res;
              _call2 = call;
              candidateCalls.remove(call);
              disconnectCalls(candidateCalls);

              if (_suppressEarlyMedia) {
                res.createAck().send();
                // re-INVITE call2 with real SDP
                final SipServletRequest req = _call2.getSipSession().createRequest("INVITE");
                if (!call1Processed) {
                  req.setContent(_call1.getRemoteSdp(), "application/sdp");
                }
                req.send();
              }
              else {
                answerCall(_call1, res);
              }
            }
            else {
              _response = res;
              answerCall(_call1, res);
            }
          }
        }
        else if (SIPHelper.isErrorResponse(res)) {
          if (_call1.equals(call)) {
            LOG.warn("re-INVITE call1 got error response, failed join on delegate " + this);
            done(this.getJoinCompleteCauseByResponse(res), this.getExceptionByResponse(res));
            _call2.disconnect();
          }
          else {
            candidateCalls.remove(call);
            if (candidateCalls.isEmpty() && _call2 == null) {
              done(this.getJoinCompleteCauseByResponse(res), this.getExceptionByResponse(res));
            }
          }
        }
      }
    }
    catch (Exception ex) {
      LOG.error("Exception when joining using delegate " + this, ex);
      done(JoinCompleteEvent.Cause.ERROR, ex);
      if (_call2 != null) {
        _call2.fail(ex);
      }
      throw ex;
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    if (_call1.equals(call)) {
      _call1.setSIPCallState(State.ANSWERED);
      if (!call1Processed) {
        try {
          final SipServletRequest ack = _response.createAck();
          ack.send();
          _call2.setSIPCallState(State.ANSWERED);
          successJoin();
        }
        catch (final Exception e) {
          done(JoinCompleteEvent.Cause.ERROR, e);
          _call1.fail(e);
          _call2.fail(e);
          throw e;
        }
      }
      else {
        // re-INVITE call1
        SipServletRequest reInvite = _call1.getSipSession().createRequest("INVITE");
        SIPHelper.copyContent(_response, reInvite);
        reInvite.send();
      }
    }
  }

  private void successJoin() throws MsControlException {
    _peer = _call2;
    doDisengage(_call1, JoinType.DIRECT);
    _call1.linkCall(_call2, JoinType.DIRECT, _direction);
    done(JoinCompleteEvent.Cause.JOINED, null);
  }

  private void answerCall(SIPCallImpl call, SipServletMessage message) throws Exception {
    final SipServletResponse newRes = call.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
    SIPHelper.copyContent(message, newRes);
    newRes.send();
  }
}
