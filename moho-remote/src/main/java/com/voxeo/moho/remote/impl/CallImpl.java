/**
 * Copyright 2010-2011 Voxeo Corporation Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.remote.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;

import org.apache.commons.collections.CollectionUtils;

import com.rayo.core.CallRejectReason;
import com.rayo.core.DtmfEvent;
import com.rayo.core.EndEvent;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.UnjoinedEvent;
import com.rayo.core.verb.OffHoldEvent;
import com.rayo.core.verb.OnHoldEvent;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoEarlyMediaEvent;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.CallEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.RequestEvent;
import com.voxeo.moho.event.ResponseEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.remote.MohoRemoteException;
import com.voxeo.moho.remote.impl.event.MohoHangupEventImpl;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Presence;

// TODO if we join two call in DIRECT mode, then one side hangup, we want join
// another side to media again, for example say something. how to do that with
// Rayo protocol?
public abstract class CallImpl extends MediaServiceSupport<Call> implements Call, RayoListener {

  protected CallableEndpoint _caller;

  protected CallableEndpoint _callee;

  protected List<Call> _peers = new ArrayList<Call>(0);

  protected State _state;

  protected Map<String, String> _headers;

  protected Boolean _isMuted = false;

  protected Boolean _isHold = false;

  protected Lock _stateLock = new ReentrantLock();

  protected CallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller, CallableEndpoint callee,
      Map<String, String> headers) {
    super(mohoRemote);
    _caller = caller;
    _callee = callee;
    _id = callID;
    if (_id != null) {
      _mohoRemote.addParticipant(this);
    }
    _headers = headers;
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
    Future<T> retval = null;
    if (!(event instanceof CallEvent) && !(event instanceof RequestEvent) && !(event instanceof ResponseEvent)) {
      retval = super.dispatch(event);
    }
    else {
      final Runnable acceptor = new Runnable() {
        @Override
        public void run() {
          if (event instanceof EarlyMediaEvent) {
            if (!((MohoEarlyMediaEvent) event).isProcessed()) {
              try {
                ((EarlyMediaEvent) event).reject(null);
              }
              catch (final SignalException e) {
                LOG.warn(e);
              }
            }
          }

          else if (event instanceof AcceptableEvent) {
            if (!((AcceptableEvent) event).isAccepted() && !((AcceptableEvent) event).isRejected()) {
              try {
                ((AcceptableEvent) event).accept();
              }
              catch (final SignalException e) {
                LOG.warn(e);
              }
            }
          }

        }
      };
      retval = super.dispatch(event, acceptor);
    }
    return retval;
  }

  @Override
  public Call[] getPeers() {
    return _peers.toArray(new CallImpl[_peers.size()]);
  }

  @Override
  public Joint join() {
    return join(Joinable.Direction.DUPLEX);
  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction) {
    return join(other, type, direction, null);
  }

  @Override
  public Joint join(CallableEndpoint other, JoinType type, Direction direction, Map<String, String> headers) {
    Call outboundCall = other.createCall(getAddress(), headers);
    return this.join(outboundCall, type, direction);
  }

  @Override
  public Joint join(Participant other, JoinType type, Direction direction) {
    return join(other, type, Boolean.TRUE, direction);
  }

  @Override
  public Unjoint unjoin(Participant other) {
    if (!_joinees.contains(other)) {
      throw new IllegalStateException("Not joined.");
    }
    UnJointImpl unJoint = null;
    try {
      JoinDestinationType type = null;
      if (other instanceof Call) {
        type = JoinDestinationType.CALL;
      }
      else {
        type = JoinDestinationType.MIXER;
      }
      unJoint = new UnJointImpl(this);
      _unjoints.put(other.getId(), unJoint);
      IQ iq = _mohoRemote.getRayoClient().unjoin(other.getId(), type, this.getId());

      if (iq.isError()) {
        _unjoints.remove(other.getId());
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
    }
    catch (XmppException e) {
      _unjoints.remove(other.getId());
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
    return unJoint;
  }

  @Override
  public Endpoint getInvitor() {
    return _caller;
  }

  @Override
  public CallableEndpoint getInvitee() {
    return _callee;
  }

  @Override
  public void mute() {
    checkIsConnected();
    if (_isMuted) {
      return;
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().mute(this.getId());
      if (iq.isError()) {
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isMuted = true;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public void unmute() {
    checkIsConnected();
    if (!_isMuted) {
      throw new IllegalStateException("This call hasn't been muted");
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().unmute(this.getId());
      if (iq.isError()) {
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isMuted = false;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public void hold() {
    checkIsConnected();
    if (_isHold) {
      return;
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().hold(this.getId());
      if (iq.isError()) {
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isHold = true;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public void unhold() {
    checkIsConnected();
    if (!_isHold) {
      throw new IllegalStateException("This call hasn't been hold");
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().unhold(this.getId());
      if (iq.isError()) {
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isHold = false;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public boolean isHold() {
    return _isHold;
  }

  @Override
  public boolean isMute() {
    return _isMuted;
  }

  public void hangup() {
    hangup(null);
  }

  @Override
  public void hangup(Map<String, String> headers) {
    // if (_state == Call.State.INPROGRESS || _state == Call.State.INITIALIZED
    // || _state == Call.State.ACCEPTED
    // || _state == Call.State.CONNECTED || _state == Call.State.INPROGRESS) {
    try {
      HangupCommand command = new HangupCommand();
      command.setCallId(this.getId());
      command.setHeaders(headers);

      IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());

      if (iq == null) {
          cleanUp();
          com.voxeo.rayo.client.xmpp.stanza.Error error = 
        		  new com.voxeo.rayo.client.xmpp.stanza.Error(Condition.remote_server_timeout);
          throw new SignalException(error.getCondition() + error.getText());
      } else if (iq.isError()) {
        cleanUp();
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
    }
    catch (XmppException e) {
      cleanUp();
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
    // }
  }

  @Override
  public JoinableStream getJoinableStream(StreamType value) {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public JoinableStream[] getJoinableStreams() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public Participant[] getParticipants() {
    return _joinees.getJoinees();
  }

  @Override
  public Participant[] getParticipants(Direction direction) {
    return _joinees.getJoinees(direction);
  }

  @Override
  public void disconnect() {
    this.hangup();
  }

  protected void cleanUp() {
    _mohoRemote.removeParticipant(_id);

    _peers.clear();
    _joinees.clear();

    // TODO
    // Commenting this as otherwise complete events for active verbs will not
    // make it to the client
    // _componentListeners.clear();

    Collection<JointImpl> joints = _joints.values();
    for (JointImpl joint : joints) {
      joint.done(new SignalException("Call end."));
    }
    _joints.clear();

    Collection<UnJointImpl> unjoints = _unjoints.values();
    for (UnJointImpl unjoint : unjoints) {
      unjoint.done(new SignalException("Call end."));
    }
    _unjoints.clear();
  }

  @Override
  public String getHeader(String name) {
    return _headers.get(name);
  }

  @Override
  public ListIterator<String> getHeaders(String name) {
    List<String> list = new ArrayList<String>();
    String value = _headers.get(name);
    list.add(value);
    return list.listIterator();
  }

  @Override
  public Iterator<String> getHeaderNames() {
    return CollectionUtils.unmodifiableCollection(_headers.values()).iterator();
  }

  @Override
  public void onRayoEvent(JID from, Presence presence) {
    if (from.getResource() != null) {
      super.onRayoEvent(from, presence);
    }
    else {
      LOG.debug("CallImpl Recived presence, processing:" + presence);
      Object object = presence.getExtension().getObject();
      if (object instanceof EndEvent) {
        EndEvent event = (EndEvent) object;
        EndEvent.Reason rayoReason = event.getReason();
        if (rayoReason == EndEvent.Reason.HANGUP) {
          MohoHangupEventImpl mohoEvent = new MohoHangupEventImpl(this);
          this.dispatch(mohoEvent);
        }
        MohoCallCompleteEvent mohoEvent = new MohoCallCompleteEvent(this,
            getMohoReasonByRayoEndEventReason(event.getReason()), null, event.getHeaders());

        if (getMohoReasonByRayoEndEventReason(event.getReason()) == CallCompleteEvent.Cause.DISCONNECT) {
          this.setCallState(State.DISCONNECTED);
        }
        else {
          this.setCallState(State.FAILED);
        }

        this.dispatch(mohoEvent);
        cleanUp();
      }
      else if (object instanceof DtmfEvent) {
        DtmfEvent event = (DtmfEvent) object;
        MohoInputDetectedEvent<Call> mohoEvent = new MohoInputDetectedEvent<Call>(this, event.getSignal());
        this.dispatch(mohoEvent);
      }
      else if (object instanceof JoinedEvent) {
        JoinedEvent event = (JoinedEvent) object;
        MohoJoinCompleteEvent mohoEvent = null;
        String id = event.getTo();
        JoinDestinationType type = event.getType();
        JointImpl joint = _joints.remove(id);
        if (type == JoinDestinationType.CALL) {
          Call peer = (Call) this.getMohoRemote().getParticipant(id);
          _joinees.add(peer, joint.getType(), joint.getDirection());
          _peers.add(peer);
          mohoEvent = new MohoJoinCompleteEvent(this, peer, JoinCompleteEvent.Cause.JOINED, true);
        }
        else {
          Mixer peer = (Mixer) this.getMohoRemote().getParticipant(id);
          _joinees.add(peer, joint.getType(), joint.getDirection());
          mohoEvent = new MohoJoinCompleteEvent(this, peer, JoinCompleteEvent.Cause.JOINED, true);
        }
        this.dispatch(mohoEvent);
        joint.done(mohoEvent);
      }
      else if (object instanceof UnjoinedEvent) {
        UnjoinedEvent event = (UnjoinedEvent) object;
        MohoUnjoinCompleteEvent mohoEvent = null;
        String id = event.getFrom();
        JoinDestinationType type = event.getType();
        UnJointImpl unjoint = _unjoints.remove(id);
        if (type == JoinDestinationType.CALL) {
          Call peer = (Call) _mohoRemote.getParticipant(id);
          _joinees.remove(peer);
          _peers.remove(peer);
          mohoEvent = new MohoUnjoinCompleteEvent(this, peer, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, true);
        }
        else {
          Mixer peer = (Mixer) this.getMohoRemote().getParticipant(id);
          _joinees.remove(peer);
          mohoEvent = new MohoUnjoinCompleteEvent(this, peer, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, true);
        }
        this.dispatch(mohoEvent);
        if (unjoint != null) {
          unjoint.done(mohoEvent);
        }
      }
      else if (object instanceof OffHoldEvent) {
        // TODO for conference
      }
      else if (object instanceof OnHoldEvent) {
        // TODO for conference
      }
      else {
        LOG.error("CallImpl Can't process presence:" + presence);
      }
    }
  }

  protected CallRejectReason getRayoCallRejectReasonByMohoReason(AcceptableEvent.Reason reason) {
    switch (reason) {
      case DECLINE:
        return CallRejectReason.DECLINE;
      case BUSY:
        return CallRejectReason.BUSY;
      case ERROR:
        return CallRejectReason.ERROR;
      default:
        return CallRejectReason.ERROR;
    }
  }

  protected CallCompleteEvent.Cause getMohoReasonByRayoEndEventReason(EndEvent.Reason reason) {
    switch (reason) {
      case HANGUP:
        return CallCompleteEvent.Cause.DISCONNECT;
      case TIMEOUT:
        return CallCompleteEvent.Cause.TIMEOUT;
      case BUSY:
        return CallCompleteEvent.Cause.BUSY;
      case REJECT:
        return CallCompleteEvent.Cause.DECLINE;
      case REDIRECT:
        return CallCompleteEvent.Cause.REDIRECT;
      case ERROR:
        return CallCompleteEvent.Cause.ERROR;
      default:
        return CallCompleteEvent.Cause.ERROR;
    }
  }

  @Override
  public Joint join(Participant other, JoinType type, boolean force, Direction direction) {
    return this.join(other, type, force, direction, true);
  }

  // TOD rayo support dtmfpassThrough?
  @Override
  public Joint join(Participant other, JoinType type, boolean force, Direction direction, boolean dtmfPassThrough) {
    JointImpl joint = null;
    try {
      joint = new JointImpl(this, type, direction, false);
      String thisID = startJoin();
      String otherID = ((ParticipantImpl) other).startJoin();

      _joints.put(other.getId(), joint);
      ((MediaServiceSupport<?>) other)._joints.put(this.getId(), joint);

      JoinCommand command = new JoinCommand();
      command.setCallId(thisID);
      command.setTo(otherID);
      command.setMedia(type);
      command.setDirection(direction);
      command.setForce(force);
      JoinDestinationType destinationType = null;
      if (other instanceof Call) {
        destinationType = JoinDestinationType.CALL;
      }
      else {
        destinationType = JoinDestinationType.MIXER;
      }
      command.setType(destinationType);

      IQ iq = _mohoRemote.getRayoClient().join(command, this.getId());
      if (iq.isError()) {
        _joints.remove(other.getId());
        ((MediaServiceSupport<?>) other)._joints.remove(this.getId());
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
    }
    catch (XmppException e) {
      _joints.remove(other.getId());
      ((MediaServiceSupport<?>) other)._joints.remove(this.getId());
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }

    return joint;
  }

  public boolean compareAndsetState(List<State> oldStates, State newValue) {
    _stateLock.lock();
    try {
      if (oldStates.contains(_state)) {
        _state = newValue;
        return true;
      }
      return false;
    }
    finally {
      _stateLock.unlock();
    }
  }

  public boolean compareAndsetState(State oldState, State newValue) {
    _stateLock.lock();
    try {
      if (oldState == _state) {
        _state = newValue;
        return true;
      }
      return false;
    }
    finally {
      _stateLock.unlock();
    }
  }

  @Override
  public State getCallState() {
    _stateLock.lock();
    try {
      return _state;
    }
    finally {
      _stateLock.unlock();
    }
  }

  public void setCallState(State state) {
    _stateLock.lock();
    try {
      _state = state;
    }
    finally {
      _stateLock.unlock();
    }
  }

  public void checkIsConnected() {
    if (_state != State.CONNECTED) {
      throw new IllegalStateException("Isn't connected");
    }
  }
  
  public void update() {
    
  }

  @Override
  public Joint join(JoinType type, boolean force, Direction direction, Map<String, String> headers,
      boolean dtmfPassThrough, CallableEndpoint... others) {
    throw new UnsupportedOperationException("Moho remote doens't support this operation now");
  }

  @Override
  public Joint join(JoinType type, boolean force, Direction direction, boolean dtmfPassThrough, Call... others) {
    throw new UnsupportedOperationException("Moho remote doens't support this operation now");
  }
}
