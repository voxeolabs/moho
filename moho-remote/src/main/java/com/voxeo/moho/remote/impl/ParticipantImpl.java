package com.voxeo.moho.remote.impl;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.common.event.DispatchableEventSource;
import com.voxeo.moho.remote.MohoRemoteException;

public abstract class ParticipantImpl extends DispatchableEventSource implements Participant, RayoListener {
  protected JoineeData _joinees = new JoineeData();

  @Override
  public Joint join(Participant other, JoinType type, Direction direction) {
    return join(other, type, true, direction);
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
  public String getRemoteAddress() {
    return getId();
  }

  @Override
  public MediaObject getMediaObject() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public JoinType getJoinType(Participant participant) {
    return _joinees.getJoinType(participant);
  }

  @Override
  public Direction getDirection(Participant participant) {
    return _joinees.getDirection(participant);
  }

  // //////////////////
  protected void addParticipant(Participant peer, JoinType joinType, Direction direction) {
    addParticipant(peer, joinType, direction, null);
  }

  protected void addParticipant(Participant peer, JoinType joinType, Direction direction, Participant realJoined) {
    _joinees.add(peer, joinType, direction, realJoined);
  }

  protected void removeParticipant(Participant peer) {
    _joinees.remove(peer);
  }

  public abstract String startJoin() throws MohoRemoteException;
}
