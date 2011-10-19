package com.voxeo.moho.sip;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.UnjointImpl;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.remote.RemoteEndpointImpl;
import com.voxeo.moho.remotejoin.RemoteParticipant;

public class RemoteParticipantImpl implements RemoteParticipant, ParticipantContainer {

  private static final Logger LOG = Logger.getLogger(RemoteParticipantImpl.class);

  protected String _id;

  protected ApplicationContextImpl _appContext;

  protected MediaSession _mediaSession;

  protected NetworkConnection _network;

  protected JoinDelegate _joinDelegate;

  protected boolean _operationInProcess;

  protected boolean _remoteInitiateJoin;

  protected boolean _remoteInitiateUnjoin;

  protected Participant _joiningParticipant;

  public RemoteParticipantImpl(final ApplicationContextImpl appContext, final String id) {
    super();
    _id = id;
    _appContext = appContext;
  }

  @Override
  public String getApplicationState() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getApplicationState(String FSM) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setApplicationState(String state) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setApplicationState(String FSM, String state) {
    throw new UnsupportedOperationException();

  }

  @Override
  public ApplicationContext getApplicationContext() {
    return _appContext;
  }

  @Override
  public void addObserver(Observer... observers) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeObserver(Observer listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event, Runnable afterExec) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getId() {
    return _id;
  }

  @Override
  public <T> T getAttribute(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(String name, Object value) {
    throw new UnsupportedOperationException();

  }

  @Override
  public Map<String, Object> getAttributeMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Endpoint getAddress() {
    return new RemoteEndpointImpl(_appContext, _id);
  }

  @Override
  public Joint join(Participant other, JoinType type, Direction direction) {
    LocalRemoteJoinDelegate joinDelegate = new LocalRemoteJoinDelegate(other, this, direction);
    SettableJointImpl joint = new SettableJointImpl();
    joinDelegate.setSettableJoint(joint);

    try {
      _joinDelegate.doJoin();
    }
    catch (Exception e) {
      // TODO
      throw new RuntimeException(e);
    }

    return joint;
  }

  @Override
  public Participant[] getParticipants() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Participant[] getParticipants(Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disconnect() {
    if (_network != null) {
      try {
        _network.release();
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing networkconnection", t);
      }
      _network = null;
    }
    if (_mediaSession != null) {
      try {
        _mediaSession.release();
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing media object", t);
      }
      _mediaSession = null;
    }
  }

  @Override
  public MediaObject getMediaObject() {
    return _network;
  }

  @Override
  public String getRemoteAddress() {
    return _id;
  }

  public MediaSession getMediaSession() {
    return _mediaSession;
  }

  public void setMediaSession(MediaSession mediaSession) {
    this._mediaSession = mediaSession;
  }

  public NetworkConnection getNetworkConnection() {
    return _network;
  }

  public void setNetworkConnection(NetworkConnection network) {
    this._network = network;
  }

  public synchronized void startJoin(final Participant participant, final JoinDelegate delegate) {
    if (_joinDelegate != null) {
      throw new IllegalStateException("other join operation in process.");
    }
    _operationInProcess = true;
    _joinDelegate = delegate;
    _joiningParticipant = participant;
  }

  @Override
  public void joinDone(Participant participant, JoinDelegate delegate) {
    _operationInProcess = false;
    _joinDelegate = null;
    _joiningParticipant = null;
  }

  @Override
  public JoinDelegate getJoinDelegate(String participantID) {
    return _joinDelegate;
  }

  public synchronized void joinDone(boolean notifyRemote) {
    try {
      JoinCompleteEvent.Cause cause = _joinDelegate.getCause();
      Exception exception = _joinDelegate.getException();

      if (notifyRemote) {
        _appContext.getRemoteCommunication().joinDone(_joiningParticipant.getRemoteAddress(), this.getId(), cause,
            exception);
      }
    }
    finally {
      _joinDelegate = null;
      _operationInProcess = false;
      _remoteInitiateJoin = false;
    }
  }

  public Unjoint unjoin(final Participant other) {
    Unjoint task = new UnjointImpl(_appContext.getExecutor(), new Callable<UnjoinCompleteEvent>() {
      @Override
      public UnjoinCompleteEvent call() throws Exception {
        return doUnjoin(other, true);
      }
    });

    return task;
  }

  public MohoUnjoinCompleteEvent doUnjoin(final Participant other, final boolean callPeerUnjoin) throws Exception {
    MohoUnjoinCompleteEvent event = null;
    try {
      disconnect();

      if (!_remoteInitiateUnjoin) {
        _appContext.getRemoteCommunication().unjoin(other.getRemoteAddress(), this.getId());
      }

      if (callPeerUnjoin) {
        ((ParticipantContainer) other).doUnjoin(this, false);
      }
    }
    catch (final Exception e) {
      LOG.error("", e);
      event = new MohoUnjoinCompleteEvent(this, other, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, e, callPeerUnjoin);
      throw e;
    }
    finally {
      if (event == null) {
        event = new MohoUnjoinCompleteEvent(this, other, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, callPeerUnjoin);
      }
      _remoteInitiateUnjoin = false;
    }

    return event;
  }

  @Override
  public void addParticipant(Participant p, JoinType type, Direction direction, Participant realJoined) {
    // TODO Auto-generated method stub

  }

  public void setRemoteInitiateUnjoin(boolean remoteInitiateUnjoin) {
    this._remoteInitiateUnjoin = remoteInitiateUnjoin;
  }

  public void setRemoteInitiateJoin(boolean remoteInitiateJoin) {
    this._remoteInitiateJoin = remoteInitiateJoin;
  }

  @Override
  public Direction getDirection(Participant participant) {
    // TODO Auto-generated method stub
    return null;
  }
}
