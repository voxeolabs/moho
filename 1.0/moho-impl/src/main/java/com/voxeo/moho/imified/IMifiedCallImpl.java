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

package com.voxeo.moho.imified;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.Observer;
import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;

public class IMifiedCallImpl implements IMifiedCall, MediaEventListener<SdpPortManagerEvent> {
  protected ExecutionContext _context;

  protected MediaSession _media;

  protected NetworkConnection _network;

  protected Call.State _cstate;

  protected List<String> _messages;

  IMifiedCallImpl(final ExecutionContext context, final IMifiedInviteEvent event) {
    _messages = new LinkedList<String>();
    _messages.add(event.getMessage());
    _cstate = State.ACCEPTED;
  }

  @Override
  public State getCallState() {
    return _cstate;
  }

  @Override
  public MediaService getMediaService(final boolean reinvite) {
    return getMediaService();
  }

  @Override
  public MediaService getMediaService() {
    return null;
  }

  public MediaObject getMediaObject() {
    return null;
  }

  @Override
  public boolean isSupervised() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Joint join() {
    return join(Joinable.Direction.DUPLEX);
  }

  @Override
  public Joint join(final Direction direction) {
    /*
     * if (_cstate == State.ACCEPTED) { MsControlFactory mf =
     * _context.getMSFactory(); _media = mf.createMediaSession(); _network =
     * _media.createNetworkConnection(NetworkConnection.BASIC);
     * _network.getSdpPortManager().addListener(this); try { byte[] sdpOffer =
     * _invite.getSipRequest().getRawContent(); if (sdpOffer == null) {
     * _network.getSdpPortManager().generateSdpOffer(); } else {
     * _network.getSdpPortManager().processSdpOffer(sdpOffer); } _cstate =
     * SIPCall.State.ANSWERING; } catch (IOException e) { release(); _cstate =
     * SIPCall.State.FAILED; throw new JoinException("..."); } while(_cstate ==
     * SIPCall.State.ANSWERING) { try { wait(); } catch(InterruptedException e)
     * { //ignore } } if (_cstate == SIPCall.State.ANSWERED) { return; } throw
     * new JoinException("...."); } else if (_cstate == SIPCall.State.ANSWERED)
     * { // reinvite to rejoin the call back to media server } else if (_cstate
     * == SIPCall.State.PROGRESSED) { // }
     */
    return null;
  }

  @Override
  public void setSupervised(final boolean supervised) {
    // TODO Auto-generated method stub

  }

  @Override
  public void disconnect() {
    // TODO Auto-generated method stub

  }

  @Override
  public Endpoint getAddress() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Joint join(final Participant other, final JoinType type, final Direction direction) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction) {
    return null;
  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction,
      final Map<String, String> headers) {
    return null;
  }

  @Override
  public void addListener(final EventListener<?> listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListener(final Class<E> type, final T listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addObserver(final Observer listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addObservers(final Observer... listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public <S, T extends Event<S>> Future<T> dispatch(final T event) {
    return null;
  }

  @Override
  public ApplicationContext getApplicationContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getApplicationState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getApplicationState(final String FSM) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeListener(final EventListener<?> listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeObserver(final Observer listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setApplicationState(final String state) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setApplicationState(final String FSM, final String state) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getAttribute(final String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> getAttributeMap() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    // TODO Auto-generated method stub

  }

  @Override
  public void unjoin(final Participant arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public JoinableStream getJoinableStream(final StreamType arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JoinableStream[] getJoinableStreams() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getMessage() {
    return _messages.get(0);
  }

  @Override
  public void onEvent(final SdpPortManagerEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListeners(final EventListener<?>... listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListeners(final Class<E> type, final T... listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public Call[] getPeers() {
    return null;
  }

  @Override
  public Participant[] getParticipants() {
    return null;
  }

  @Override
  public Participant[] getParticipants(final Direction direction) {
    return null;
  }

  @Override
  public <S, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
    return null;
  }

  @Override
  public void hold() {
    // TODO Auto-generated method stub

  }

  @Override
  public void mute() {
    // TODO Auto-generated method stub

  }

  @Override
  public void unhold() {
    // TODO Auto-generated method stub

  }

  @Override
  public void unmute() {
    // TODO Auto-generated method stub

  }

}
