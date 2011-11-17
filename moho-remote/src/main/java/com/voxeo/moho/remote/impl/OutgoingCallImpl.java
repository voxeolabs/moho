package com.voxeo.moho.remote.impl;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.core.AnsweredEvent;
import com.rayo.core.DialCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.RingingEvent;
import com.rayo.core.verb.VerbRef;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.remote.MohoRemoteException;
import com.voxeo.moho.remote.impl.event.MohoAnsweredEventImpl;
import com.voxeo.moho.remote.impl.event.MohoHangupEventImpl;
import com.voxeo.moho.remote.impl.event.MohoRingEventImpl;

public class OutgoingCallImpl extends CallImpl implements OutgoingCall {
  private static final Logger LOG = Logger.getLogger(OutgoingCallImpl.class);

  protected JointImpl waitAnswerJoint = null;

  protected OutgoingCallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller,
      CallableEndpoint callee, Map<String, String> headers) {
    super(mohoRemote, callID, caller, callee, headers);
  }

  protected boolean internalCall(Direction direction) throws MohoRemoteException {
    _mohoRemote.getParticipantsLock().lock();
    try {
      if (_id == null) {
        DialCommand command = new DialCommand();
        command.setFrom(_caller.getURI());
        command.setTo(_callee.getURI());
        command.setHeaders(_headers);
        waitAnswerJoint = new JointImpl(this, direction);
        
        VerbRef verbRef = _mohoRemote.getRayoClient().dial(command);
        setID(verbRef.getVerbId());
        return true;
      }
      else {
        return false;
      }
    }
    catch (XmppException ex) {
      this.setCallState(null);
      waitAnswerJoint = null;
      throw new MohoRemoteException(ex);
    }
    finally {
      _mohoRemote.getParticipantsLock().unlock();
    }
  }

  @Override
  public Joint join(Direction direction) {
    internalCall(direction);

    return waitAnswerJoint;
  }

  private void setID(String id) {
    _id = id;
    if (_id != null) {
      _mohoRemote.addParticipant(this);
    }
  }

  @Override
  public void onRayoEvent(JID from, Presence presence) {
    Object object = presence.getExtension().getObject();
    LOG.debug("OutgoingCallImpl Recived presence, processing:" + presence);
    if (object instanceof AnsweredEvent) {
      AnsweredEvent event = (AnsweredEvent) object;
      MohoAnsweredEventImpl<Call> mohoEvent = new MohoAnsweredEventImpl<Call>(this, event.getHeaders());
      this.setCallState(State.CONNECTED);
      this.dispatch(mohoEvent);

      MohoJoinCompleteEvent joinComplete = new MohoJoinCompleteEvent(this, null, JoinCompleteEvent.Cause.JOINED, true);
      this.dispatch(joinComplete);
      waitAnswerJoint.done(joinComplete);
    }
    else if (object instanceof RingingEvent) {
      RingingEvent event = (RingingEvent) object;
      MohoRingEventImpl mohoEvent = new MohoRingEventImpl(this, event.getHeaders());
      this.dispatch(mohoEvent);
    }
    else if (object instanceof EndEvent) {
      EndEvent event = (EndEvent) object;
      EndEvent.Reason rayoReason = event.getReason();
      if (rayoReason == EndEvent.Reason.HANGUP) {
        MohoHangupEventImpl mohoEvent = new MohoHangupEventImpl(this);
        this.dispatch(mohoEvent);
      }

      if (!compareAndsetState(State.CONNECTED, State.DISCONNECTED)) {
        _state = State.FAILED;
        MohoJoinCompleteEvent joinComplete = new MohoJoinCompleteEvent(this, null,
            getMohoJoinCompleteReasonByRayoEndEventReason(rayoReason), true);
        waitAnswerJoint.done(joinComplete);
      }

      MohoCallCompleteEvent mohoEvent = new MohoCallCompleteEvent(this,
          getMohoReasonByRayoEndEventReason(event.getReason()), null, event.getHeaders());
      this.dispatch(mohoEvent);

      cleanUp();
    }
    else {
      super.onRayoEvent(from, presence);
    }
  }

  @Override
  public String startJoin() throws MohoRemoteException {
    internalCall(Direction.DUPLEX);
    return _id;
  }

  protected JoinCompleteEvent.Cause getMohoJoinCompleteReasonByRayoEndEventReason(EndEvent.Reason reason) {
    switch (reason) {
      case TIMEOUT:
        return JoinCompleteEvent.Cause.TIMEOUT;
      case BUSY:
        return JoinCompleteEvent.Cause.BUSY;
      case REJECT:
        return JoinCompleteEvent.Cause.REJECT;
      case REDIRECT:
        return JoinCompleteEvent.Cause.REDIRECT;
      case ERROR:
        return JoinCompleteEvent.Cause.ERROR;
      default:
        return JoinCompleteEvent.Cause.ERROR;
    }
  }

  @Override
  public Endpoint getAddress() {
    return _callee;
  }

  @Override
  public String getRemoteAddress() {
    return _caller.getURI().toString();
  }
}
