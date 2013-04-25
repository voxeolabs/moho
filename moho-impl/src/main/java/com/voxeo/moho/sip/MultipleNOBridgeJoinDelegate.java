package com.voxeo.moho.sip;

import java.util.List;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.JoinData;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.dialect.MediaDialect;

// NOTE that moho application should process redirect event.
public class MultipleNOBridgeJoinDelegate extends JoinDelegate implements Observer {

  private static final Logger LOG = Logger.getLogger(MultipleNOBridgeJoinDelegate.class);

  protected List<SIPCallImpl> candidateCalls;

  protected boolean joinedOutgoingCall;

  protected boolean waitingACK;

  protected MultipleNOBridgeJoinDelegate(JoinType type, Direction direction, SIPCallImpl call1, List<SIPCallImpl> others) {
    _call1 = call1;
    _direction = direction;
    _joinType = type;
    candidateCalls = others;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    if (_call1.getMediaObject() == null && _call1 instanceof SIPOutgoingCall) {
      _call1.join(Direction.DUPLEX);
      return;
    }

    if (!joinedOutgoingCall) {
      for (SIPCallImpl other : candidateCalls) {
        other.addObserver(this);
        other.setJoiningPeer(new JoinData(_call1, _direction, _joinType));
        other.join(Direction.DUPLEX);
      }
      joinedOutgoingCall = true;
    }
  }

  @State
  public synchronized void onJoinCompleteEvent(JoinCompleteEvent event) {
    event.getSource().removeObserver(this);
    SIPCallImpl call = (SIPCallImpl) event.getSource();
    call.setJoiningPeer(null);
    if (done || _call2 != null) {
      return;
    }
    if (event.getCause() == JoinCompleteEvent.Cause.JOINED) {
      try {
        _call2 = call;
        candidateCalls.remove(event.getSource());
        disconnectCalls(candidateCalls);

        if (_call1 instanceof SIPIncomingCall && _call1.getSIPCallState() == SIPCall.State.PROGRESSED) {
          final SipServletResponse res = call.getSipInitnalRequest().createResponse(SipServletResponse.SC_OK);
          if (call.getLocalSDP() != null) {
            res.setContent(call.getLocalSDP(), "application/sdp");
          }
          waitingACK = true;
          res.send();
        }
        else {
          if (_call1 instanceof SIPIncomingCall && _call1.getMediaObject() == null) {
            _call1.join().get();
          }
          successJoin(call);
        }
      }
      catch (final Exception e) {
        LOG.error("Exception when doing join on delegate " + this, e);
        done(Cause.ERROR, e);
        failCall(call, e);
      }
    }
    else {
      candidateCalls.remove(event.getSource());
      if (candidateCalls.isEmpty() && _call2 == null) {
        done(event.getCause(), getExceptionByResponse(call.getLastReponse()));
      }
      disconnectCall(call, true, null, null);
    }
  }

  @Override
  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    if (waitingACK) {
      call.setSIPCallState(SIPCall.State.ANSWERED);
      // call.processSDPAnswer(req);
      successJoin(_call2);
    }
  }

  private void successJoin(SIPCallImpl call) {
    try {
      _peer = _call2;
      MediaDialect dialect = ((ApplicationContextImpl) _call1.getApplicationContext()).getDialect();
      dialect.setDtmfPassThrough((NetworkConnection) _call1.getMediaObject(), dtmfPassThrough);
      dialect.setDtmfPassThrough((NetworkConnection) call.getMediaObject(), dtmfPassThrough);

      _call1.linkCall(call, _joinType, _direction);

      _call1.setJoiningPeer(null);
      call.setJoiningPeer(null);
      done(Cause.JOINED, null);
    }
    catch (final Exception e) {
      LOG.error("Exception when doing join on delegate " + this, e);
      done(Cause.ERROR, e);
      failCall(call, e);
    }
  }
}
