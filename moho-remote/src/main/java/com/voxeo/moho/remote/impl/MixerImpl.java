package com.voxeo.moho.remote.impl;

import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;

import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.SpeakingEvent;
import com.rayo.core.UnjoinedEvent;
import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.common.event.MohoActiveSpeakerEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.remote.MohoRemoteException;
import com.voxeo.rayo.client.xmpp.stanza.Presence;

public class MixerImpl extends MediaServiceSupport<Mixer> implements Mixer {

  private MixerEndpointImpl _mixerEndpoint;

  protected Map<Object, Object> _params;

  protected String _name;

  public MixerImpl(MixerEndpointImpl mixerEndpoint, String name, Map<Object, Object> params) {
    super(mixerEndpoint.getMohoRemote());
    _mixerEndpoint = mixerEndpoint;
    _params = params;
    _id = name;
    _mohoRemote.addParticipant(this);
    _name = name;
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
  public Endpoint getAddress() {
    return _mixerEndpoint;
  }

  @Override
  public Joint join(Participant other, JoinType type, Direction direction) {
    return this.join(other, type, false, direction);
  }

  @Override
  public Joint join(Participant other, JoinType type, boolean force, Direction direction) {
    Joint joint = null;
    if (other instanceof CallImpl) {
      joint = other.join(this, type, reserve(direction));
      return joint;
    }
    else {
      // TODO mixer join mixer
    }
    return joint;
  }

  private Direction reserve(Direction direction) {
    if (direction == Direction.RECV) {
      return Direction.SEND;
    }
    else if (direction == Direction.SEND) {
      return Direction.RECV;
    }
    return Direction.DUPLEX;
  }

  @Override
  public Unjoint unjoin(Participant other) {
    if (!_joinees.contains(other)) {
      throw new IllegalStateException("Not joined.");
    }
    Unjoint unjoint = null;
    if (other instanceof CallImpl) {
      unjoint = other.unjoin(this);
    }
    return unjoint;
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
    _joinees.clear();
  }

  @Override
  public MediaObject getMediaObject() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public String getRemoteAddress() {
    return _id;
  }

  @Override
  public MediaService<Mixer> getMediaService() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public Joint join(Participant other, JoinType type, Direction direction, Properties props) {
    return join(other, type, false, direction, props);
  }

  @Override
  public Joint join(Participant other, JoinType type, boolean force, Direction direction, Properties props) {
    return join(other, type, direction);
  }

  @Override
  public JoinType getJoinType(Participant participant) {
    return _joinees.getJoinType(participant);
  }

  @Override
  public void onRayoEvent(JID from, Presence presence) {
    Object object = presence.getExtension().getObject();
    if (object instanceof JoinedEvent) {
      MohoJoinCompleteEvent mohoEvent = null;
      JoinedEvent event = (JoinedEvent) object;
      String id = event.getTo();
      JoinDestinationType type = event.getType();
      JointImpl joint = _joints.remove(id);
      if (type == JoinDestinationType.CALL) {
        Call peer = (Call) _mohoRemote.getParticipant(id);
        _joinees.add(peer, joint.getType(), joint.getDirection());
        mohoEvent = new MohoJoinCompleteEvent(this, peer, JoinCompleteEvent.Cause.JOINED, true);
      }
      else {
        // TODO support mixer join mixer

      }
      this.dispatch(mohoEvent);
    }
    else if (object instanceof UnjoinedEvent) {
      UnjoinedEvent event = (UnjoinedEvent) object;
      MohoUnjoinCompleteEvent mohoEvent = null;
      String id = event.getFrom();
      JoinDestinationType type = event.getType();
      _unjoints.remove(id);
      if (type == JoinDestinationType.CALL) {
        Call peer = (Call) _mohoRemote.getParticipant(id);
        _joinees.remove(peer);
        mohoEvent = new MohoUnjoinCompleteEvent(this, peer, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, true);
      }
      else {
        // TODO mixer unjoin mixer
      }
      this.dispatch(mohoEvent);
    }
    else if (object instanceof SpeakingEvent) {
      SpeakingEvent event = (SpeakingEvent) object;
      Call speaker = (Call) _mohoRemote.getParticipant(event.getSpeakerId());
      MohoActiveSpeakerEvent mohoEvent = new MohoActiveSpeakerEvent(this, new Participant[] {speaker});
      this.dispatch(mohoEvent);
    }
  }

  @Override
  public String startJoin() throws MohoRemoteException {
    return _id;
  }

  @Override
  public String getName() {
    return _name;
  }
}
