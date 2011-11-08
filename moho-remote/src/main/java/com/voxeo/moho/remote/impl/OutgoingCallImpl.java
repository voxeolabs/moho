package com.voxeo.moho.remote.impl;

import java.util.Map;
import java.util.concurrent.ExecutionException;

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
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
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

  protected synchronized void call() throws XmppException {
    if (getId() == null) {
      DialCommand command = new DialCommand();
      command.setFrom(_caller.getURI());
      command.setTo(_callee.getURI());
      command.setHeaders(_headers);

      _mohoRemote.getParticipantsLock().lock();
      try {
        VerbRef verbRef = _mohoRemote.getRayoClient().dial(command);
        setID(verbRef.getVerbId());
      }
      finally {
        _mohoRemote.getParticipantsLock().unlock();
      }

      this.setState(Call.State.INITIALIZED);
    }
  }

  @Override
  public synchronized Joint join(Direction direction) {
    if (waitAnswerJoint == null) {
      waitAnswerJoint = new JointImpl(this, direction);
      try {
        call();
      }
      catch (XmppException e) {
        LOG.error("", e);
        waitAnswerJoint = null;
        throw new SignalException(e);
      }
    }
    else {
      if (waitAnswerJoint.isDone()) {
        try {
          JoinCompleteEvent event = waitAnswerJoint.get();
          dispatch(event);
        }
        catch (InterruptedException e) {
          // can't happen
        }
        catch (ExecutionException e) {
          // can't happen
        }
      }
    }

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
    if (object instanceof AnsweredEvent) {
      AnsweredEvent event = (AnsweredEvent) object;
      MohoAnsweredEventImpl<Call> mohoEvent = new MohoAnsweredEventImpl<Call>(this, event.getHeaders());
      _state = State.CONNECTED;
      this.dispatch(mohoEvent);

      if (waitAnswerJoint != null) {
        MohoJoinCompleteEvent joinComplete = new MohoJoinCompleteEvent(this, null, JoinCompleteEvent.Cause.JOINED, true);
        this.dispatch(joinComplete);
        waitAnswerJoint.done(joinComplete);
        waitAnswerJoint = null;
      }
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

      if (_state == State.CONNECTED) {
        _state = State.DISCONNECTED;
      }
      else {
        _state = State.FAILED;
      }
      MohoCallCompleteEvent mohoEvent = new MohoCallCompleteEvent(this,
          getMohoReasonByRayoEndEventReason(event.getReason()), null, event.getHeaders());
      this.dispatch(mohoEvent);

      if (waitAnswerJoint != null) {
        MohoJoinCompleteEvent joinComplete = new MohoJoinCompleteEvent(this, null,
            getMohoJoinCompleteReasonByRayoEndEventReason(rayoReason), true);
        waitAnswerJoint.done(joinComplete);
        waitAnswerJoint = null;
      }

      cleanUp();
    }
    else {
      super.onRayoEvent(from, presence);
    }
  }

  public void startJoin() throws XmppException {
    call();
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
}
