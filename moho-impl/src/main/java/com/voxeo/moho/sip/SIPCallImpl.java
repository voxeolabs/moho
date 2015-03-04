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

package com.voxeo.moho.sip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionsUtil;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallImpl;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Configuration;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.JoinData;
import com.voxeo.moho.JoineeData;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.UnjointImpl;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.common.util.InheritLogContextRunnable;
import com.voxeo.moho.common.util.Utils;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.GenericMediaService;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.remote.sipbased.RemoteJoinOutgoingCall;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.moho.util.SDPUtils;
import com.voxeo.moho.util.SessionUtils;

public abstract class SIPCallImpl extends CallImpl implements SIPCall, MediaEventListener<SdpPortManagerEvent>,
    ParticipantContainer {

  private static final Logger LOG = Logger.getLogger(SIPCallImpl.class);
  
  protected static String Att_REINVITE_HEADERS = "Att_REINVITE_HEADERS";
  protected static String Att_REINVITE_ATTRIBUTES = "Att_REINVITE_ATTRIBUTES";

  protected SIPCall.State _cstate;

  protected SIPEndpoint _address;

  protected SipServletRequest _invite;

  protected byte[] _remoteSDP;

  protected byte[] _localSDP;

  protected SipSession _signal;

  protected MediaSession _media;

  protected NetworkConnection _network;

  protected MediaService<Call> _service;

  protected JoinDelegate _joinDelegate;

  protected JoinDelegate _oldJoinDelegate;

  protected SIPCallDelegate _callDelegate;

  protected JoineeData _joinees = new JoineeData();

  protected boolean _operationInProcess;

  protected String _replacesHeader;

  protected Exception _exception;

  protected JoinData _bridgeJoiningPeer;

  protected MediaMixer _multiplejoiningMixer;

  // TODO: join to MediaGroup
  protected MediaMixer _multiplejoiningMixerForMedGrop;

  protected Lock mediaServiceLock = new ReentrantLock();

  protected Queue<JoinRequest> _joinQueue = new LinkedList<JoinRequest>();

  protected SipServletResponse _inviteResponse;

  protected boolean reInvitingRemote;
  
  protected boolean needReInivteRemote;
  
  protected boolean processingReinvite;
  
  protected boolean pendingReinvite;
  
  protected Origin previousOrigin;
  
  protected boolean processingNon100relEarlyMedia;
  
  protected boolean dispatchedNon100relEarlyMedia;

  protected SIPCallImpl(final ExecutionContext context, final SipServletRequest req) {
    super(context);
    _caller = new SIPEndpointImpl(context, req.getFrom());
    _callee = new SIPEndpointImpl(context, req.getTo());

    _invite = req;

    // process Replaces header.
    final SipSessionsUtil sessionUtil = (SipSessionsUtil) _invite.getSession().getServletContext()
        .getAttribute("javax.servlet.sip.SipSessionsUtil");
    if (sessionUtil != null) {
      final SipSession peerSession = sessionUtil.getCorrespondingSipSession(_invite.getSession(), "Replaces");
      if (peerSession != null) {
        final SIPCallImpl call = (SIPCallImpl) SessionUtils.getParticipant(peerSession);
        final SipSession replacedSession = ((SIPCallImpl) call.getLastPeer()).getSipSession();

        final String callId = replacedSession.getCallId();
        final String toTag = replacedSession.getRemoteParty().getParameter("tag");
        final String fromTag = replacedSession.getLocalParty().getParameter("tag");

        // the format
        // Replaces: call-id;to-tag=7743;from-tag=6472
        final StringBuilder sb = new StringBuilder();
        sb.append(callId);
        sb.append(";to-tag=");
        sb.append(toTag);
        sb.append(";from-tag=");
        sb.append(fromTag);
        _replacesHeader = sb.toString();
      }
    }

    _signal = req.getSession();
    _address = new SIPEndpointImpl((ApplicationContextImpl) getApplicationContext(), _signal.getRemoteParty());
    SessionUtils.setEventSource(_signal, this);
    context.addCall(this);
    _cstate = SIPCall.State.INVITING;
  }

  protected SIPCallImpl(final ExecutionContext context) {
    super(context);
    _cstate = SIPCall.State.INITIALIZED;
  }

  @Override
  public int hashCode() {
    return "SIPCall".hashCode() + getId().hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SIPCall)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    return this.getId().equals(((SIPCall) o).getId());
  }
  
  @Override
  public String toString() {
    return String.format("%s[sipsessionid=%s id=%s callstate=%s]", getClass().getSimpleName(), _signal.getId(), _id,
        _cstate);
  }

  public String useReplacesHeader() {
    final String ret = _replacesHeader;
    _replacesHeader = null;
    return ret;
  }

  @Override
  public Call.State getCallState() {
    switch (_cstate) {
      case INITIALIZED:
        return Call.State.INITIALIZED;
      case PROGRESSING:
      case PROGRESSED:
        return Call.State.INPROGRESS;
      case INVITING:
      case RINGING:
      case ANSWERING:
        return Call.State.ACCEPTED;
      case ANSWERED:
        return Call.State.CONNECTED;
      case FAILED:
      case REJECTED:
        return Call.State.FAILED;
      case DISCONNECTED:
      case REDIRECTED:
        return Call.State.DISCONNECTED;
    }
    return null;
  }

  @Override
  public SIPCall.State getSIPCallState() {
    return _cstate;
  }

  @Override
  public MediaService<Call> getMediaService(final boolean reinvite) throws IllegalStateException, MediaException {
    if (getSIPCallState() != SIPCall.State.ANSWERED && getSIPCallState() != SIPCall.State.RINGING
        && getSIPCallState() != SIPCall.State.PROGRESSED) {
      throw new IllegalStateException("The call has not been answered or there was no progress in the call");
    }

    mediaServiceLock.lock();
    try {
      if (_network == null) {
        if (reinvite) {
          JoinCompleteEvent joinResult = null;
          try {
            joinResult = this.join(Direction.DUPLEX).get();
          }
          catch (final Exception e) {
            throw new MediaException(e);
          }
          
          if(joinResult.getCause() != JoinCompleteEvent.Cause.JOINED) {
            throw new MediaException("Join result in error result:" + joinResult.getCause());
          }
        }
        else {
          throw new IllegalStateException("the call is Direct mode but reinvite is false");
        }
      }

      try {
        if (_service == null) {
          Parameters params = _media.createParameters();

          _service = _context.getMediaServiceFactory().create((Call) this, _media, params);
          JoinDelegate.bridgeJoin(this, _service.getMediaGroup());
        }
      }
      catch (final Exception e) {
        throw new MediaException(e);
      }
      return _service;
    }
    finally {
      mediaServiceLock.unlock();
    }
  }

  @Override
  public SipSession getSipSession() {
    return _signal;
  }

  public MediaObject getMediaObject() {
    return _network;
  }

  @Override
  public void disconnect() {
    this.disconnect(false, CallCompleteEvent.Cause.NEAR_END_DISCONNECT, null, null);
  }

  @Override
  public void hangup(Map<String, String> headers) {
    this.disconnect(false, CallCompleteEvent.Cause.NEAR_END_DISCONNECT, null, headers);
  }

  @Override
  public Participant[] getParticipants() {
    if (this.getJoiningPeer() != null) {
      Participant[] participants = _joinees.getJoinees();
      Participant[] allParticipants = Arrays.copyOf(participants, participants.length + 1);
      allParticipants[participants.length] = this.getJoiningPeer().getParticipant();
      return allParticipants;
    }
    return _joinees.getJoinees();
  }

  @Override
  public Participant[] getParticipants(final Direction direction) {
    if (this.getJoiningPeer() != null) {
      Participant[] participants = _joinees.getJoinees(direction);
      Participant[] allParticipants = Arrays.copyOf(participants, participants.length + 1);
      allParticipants[participants.length] = this.getJoiningPeer().getParticipant();
      return allParticipants;
    }
    return _joinees.getJoinees(direction);
  }

  @Override
  public JoinType getJoinType(Participant participant) {
    return _joinees.getJoinType(participant);
  }

  @Override
  public Direction getDirection(Participant participant) {
    Direction direction = _joinees.getDirection(participant);
    if(direction == null && getJoiningPeer() != null && participant == getJoiningPeer().getParticipant() && getJoiningPeer().getRealJoined() != null) {
      return Direction.DUPLEX;
    }
    return direction;
  }

  @Override
  public JoinableStream getJoinableStream(final StreamType arg0) throws MediaException, IllegalStateException {
    if (_network == null) {
      throw new IllegalStateException();
    }
    try {
      return _network.getJoinableStream(arg0);
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
  }

  @Override
  public JoinableStream[] getJoinableStreams() throws MediaException, IllegalStateException {
    if (_network == null) {
      throw new IllegalStateException();
    }
    try {
      return _network.getJoinableStreams();
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
  }

  public Unjoint unjoin(final Participant other) {
    Unjoint task = new UnjointImpl(_context.getExecutor(), new Callable<UnjoinCompleteEvent>() {
      @Override
      public UnjoinCompleteEvent call() throws Exception {
        return doUnjoin(other, true);
      }
    });

    return task;
  }

  public Unjoint unjoin(final Participant other, final boolean isInitiator) {
    Unjoint task = new UnjointImpl(_context.getExecutor(), new Callable<UnjoinCompleteEvent>() {
      @Override
      public UnjoinCompleteEvent call() throws Exception {
        return doUnjoin(other, isInitiator);
      }
    });

    return task;
  }

  @Override
  public synchronized MohoUnjoinCompleteEvent doUnjoin(final Participant p, boolean initiator) throws Exception {
    MohoUnjoinCompleteEvent event = null;
    if (!isAnswered()) {
      event = new MohoUnjoinCompleteEvent(SIPCallImpl.this, p, UnjoinCompleteEvent.Cause.NOT_JOINED, initiator);
      SIPCallImpl.this.dispatch(event);
      return event;
    }
    if (!_joinees.contains(p)) {
      event = new MohoUnjoinCompleteEvent(SIPCallImpl.this, p, UnjoinCompleteEvent.Cause.NOT_JOINED, initiator);
      SIPCallImpl.this.dispatch(event);
      return event;
    }
    LOG.debug(String.format("%s unjoining %s, initiator %s", this, p, initiator));
    
    // wait if processing re-INVITE from remote
    waitProcessReInvite();
    
    Participant participant = p;
    Participant local = this;

    try {
      JoinData joinData = _joinees.remove(p);

      Participant other = joinData.getParticipant();

      if (other instanceof Call) {
        synchronized (_peers) {
          _peers.remove(other);
        }
      }
      if (other.getMediaObject() instanceof Joinable) {
        if (initiator) {
          if (joinData.getRealJoined() == null) {
            JoinDelegate.bridgeUnjoin(this, other);
          }
          else {
            JoinDelegate.bridgeUnjoin(joinData.getRealJoined(), other);
          }
        }
      }

      if (initiator) {
        ((ParticipantContainer) other).unjoin(SIPCallImpl.this, false);
      }

      // for remote unjoin
      if (p instanceof RemoteParticipant) {
        participant = this.getApplicationContext().getParticipant(((RemoteParticipant) p).getRemoteParticipantID());
      }
      if (this instanceof RemoteParticipant) {
        local = this.getApplicationContext().getParticipant(((RemoteParticipant) this).getRemoteParticipantID());
      }

      event = new MohoUnjoinCompleteEvent(local, participant, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, initiator);
    }
    catch (final Exception e) {
      LOG.error("Exception when doing unjoin.", e);
      event = new MohoUnjoinCompleteEvent(local, participant, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, e, true);
      throw e;
    }
    finally {
      if (event == null) {
        event = new MohoUnjoinCompleteEvent(local, participant, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, initiator);
      }
      SIPCallImpl.this.dispatch(event);
      
      if(_joinees.getJoinees().length == 0 && _network == null) {
        _callDelegate = null;
      }
    }
    return event;
  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction,
      final Map<String, String> headers) {
    Participant p = null;
    try {
      p = other.createCall(getAddress(), headers, this);
    }
    catch (final Exception e) {
      LOG.error(e);
      return new JointImpl(_context.getExecutor(), new JointImpl.DummyJoinWorker(SIPCallImpl.this, p, e));
    }
    return join(p, type, direction);
  }

  @Override
  public synchronized Joint join(final Direction direction) {
    return this.join(direction, null);
  }

  public synchronized Joint join(final Direction direction, SettableJointImpl joint) {
    if (isTerminated()) {
      throw new IllegalStateException("already terminated.");
    }
    LOG.debug(String.format("%s joining to media, direction %s", this, direction.toString()));

    //wait if processing re-INVITE from remote.
    waitProcessReInvite();
    
    if (_operationInProcess) {
      if (_joinDelegate != null
          && !(_joinDelegate instanceof BridgeJoinDelegate || _joinDelegate instanceof MultipleNOBridgeJoinDelegate
              || _joinDelegate instanceof OtherParticipantJoinDelegate
              || _joinDelegate instanceof LocalRemoteJoinDelegate || _joinDelegate instanceof RemoteLocalJoinDelegate)) {

        if (joint == null) {
          joint = new SettableJointImpl();
        }
        JoinRequest joinReq = new JoinRequest(direction, joint);
        _joinQueue.add(joinReq);
        return joint;
      }
    }

    _operationInProcess = true;

    try {
      if (_joinDelegate != null) {
        _oldJoinDelegate = _joinDelegate;
        _joinDelegate = null;
      }
      _joinDelegate = createJoinDelegate(direction);

      if (joint == null) {
        joint = new SettableJointImpl();
      }

      _joinDelegate.setSettableJoint(joint);
      _joinDelegate.doJoin();

      return joint;
    }
    catch (Exception ex) {
      // TODO
      throw new RuntimeException(ex);
    }
  }

  public void joinDone(final Participant participant, final JoinDelegate delegate) {
    if (delegate != _joinDelegate) {
      return;
    }
    if(delegate instanceof BridgeJoinDelegate || delegate instanceof MultipleNOBridgeJoinDelegate) {
      setJoiningPeer(null);
    }
    if (_joinDelegate.getPeer() != null) {
      if (JoinType.isBridge(_joinDelegate.getJoinType())) {
        _callDelegate = new SIPCallBridgeDelegate();
      }
      else {
        _callDelegate = new SIPCallDirectDelegate();
      }
    }
    else {
      _callDelegate = new SIPCallMediaDelegate();
    }
    if (_oldJoinDelegate != null && !(_oldJoinDelegate instanceof MultipleNOBridgeJoinDelegate)) {
      JoinCompleteEvent.Cause cause = _joinDelegate.getCause();
      Exception exception = _joinDelegate.getException();
      _joinDelegate = _oldJoinDelegate;
      _oldJoinDelegate = null;
      if (cause == JoinCompleteEvent.Cause.JOINED) {
        try {
          _operationInProcess = true;
          LOG.debug("starting the old join delegte.");
          _joinDelegate.doJoin();
        }
        catch (Exception e) {
          LOG.error(e.getMessage(), e);
          _joinDelegate.done(JoinCompleteEvent.Cause.ERROR, e);
        }
      }
      else {
        _joinDelegate.done(cause, exception);
      }

      return;
    }
    else {
      _joinDelegate = null;
      _oldJoinDelegate = null;
      _operationInProcess = false;
      reInvitingRemote = false;
    }
    
    _context.getExecutor().execute(new Runnable() {
      @Override
      public void run() {
        synchronized (SIPCallImpl.this) {
          SIPCallImpl.this.notifyAll();
        }
      }
    });
  }

  public synchronized void continueQueuedJoin() {
    JoinRequest queuedJoin = _joinQueue.poll();

    if (queuedJoin != null) {
      if (queuedJoin.getCalls() != null) {
        this.join(queuedJoin.getJoint(), queuedJoin.getType(), queuedJoin.isForce(), queuedJoin.getDirection(),
            queuedJoin.isDtmfPassThrough(), queuedJoin.getCalls());
      }
      else if (queuedJoin.getPeer() != null) {
        this.join(queuedJoin.getPeer(), queuedJoin.getType(), queuedJoin.isForce(), queuedJoin.getDirection(),
            queuedJoin.isDtmfPassThrough(), queuedJoin.getJoint());
      }
      else {
        this.join(queuedJoin.getDirection(), queuedJoin.getJoint());
      }
    }
  }

  public synchronized int queuedJoinSize() {
    return _joinQueue.size();
  }

  @Override
  public synchronized Joint join(final Participant other, final JoinType type, final Direction direction) {
    return this.join(other, type, false, direction);
  }

  @Override
  public synchronized Joint join(final Participant other, final JoinType type, final boolean force,
      final Direction direction) {
    return this.join(other, type, force, direction, true);
  }

  @Override
  public synchronized Joint join(final Participant other, final JoinType type, final boolean force,
      final Direction direction, boolean dtmfPassThrough) {
    return join(other, type, force, direction, dtmfPassThrough, null);
  }

  @Override
  public synchronized Joint join(JoinType type, boolean force, Direction direction, Map<String, String> headers,
      boolean dtmfPassThrough, CallableEndpoint... others) {
    if (others == null || others.length == 0 || type == null || direction == null) {
      throw new IllegalArgumentException("others should not be empty.");
    }
    
    List<Call> calls = new ArrayList<Call>();
    for (CallableEndpoint other : others) {
      calls.add(other.createCall(getAddress(), headers, this));
    }
    
    if(calls.size() ==1) {
      return this.join(calls.get(0), type, force, direction, dtmfPassThrough);
    }
    
    return join(type, force, direction, dtmfPassThrough, calls.toArray(new Call[calls.size()]));
  }
  
  @Override
  public synchronized Joint join(JoinType type, boolean force, Direction direction, boolean dtmfPassThrough,
      Call... others) {
    return this.join(null, type, force, direction, dtmfPassThrough, others);
  }
  
  protected synchronized Joint join(SettableJointImpl joint, JoinType type, boolean force, Direction direction, boolean dtmfPassThrough,
      Call... others) {
    if (isTerminated()) {
      throw new IllegalStateException("This call is already terminated.");
    }
    
    if(others.length ==1) {
      return this.join(others[0], type, force, direction, dtmfPassThrough);
    }
    
    for (Call other : others) {
      if (other.equals(this)) {
        throw new IllegalArgumentException("Can't join to itself.");
      }
      if (((SIPCallImpl) other).isAnswered()) {
        throw new IllegalArgumentException("Doesn't support join to multiple answered calls now.");
      }
    }
    
    LOG.debug(String.format("%s joining to %s, JoinType %s, force %s, Direction %s, dtmfPassThrough %s", this, others,
        type, force, direction.toString(), dtmfPassThrough));
    
    //wait if processing re-INVITE from remote.
    waitProcessReInvite();
    
    if (_operationInProcess) {
      if (joint == null) {
        joint = new SettableJointImpl();
      }
      JoinRequest joinReq = new JoinRequest(others, type, direction, force, dtmfPassThrough, joint);
      _joinQueue.add(joinReq);
      return joint;
    }

    _operationInProcess = true;

    try {
      ExecutionException ex = JoinDelegate.checkJoinStrategy(this, type, force);

      if (joint == null) {
        joint = new SettableJointImpl();
      }
      if (ex == null) {
        _joinDelegate = createJoinDelegate(others, type, direction);
        _joinDelegate.setSettableJoint(joint);
        _joinDelegate.setDtmfPassThrough(dtmfPassThrough);
        
        for(Call other : others){
          ((SIPCallImpl)other).startJoin(this, _joinDelegate);
        }
        
        _joinDelegate.doJoin();
      }
      else {
        // dispatch BUSY event
        JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(this, others[0], Cause.BUSY, ex, true);
        dispatch(joinCompleteEvent);
        joint.done(ex);
      }
      return joint;
    }
    catch (Exception ex) {
      if (_joinDelegate != null) {
        _joinDelegate.done(Cause.ERROR, ex);
      }
      throw new RuntimeException(ex);
    }
  }

  protected synchronized Joint join(final Participant other, final JoinType type, final boolean force,
      final Direction direction, boolean dtmfPassThrough, SettableJointImpl joint) {
    if (isTerminated()) {
      throw new IllegalStateException("This call is already terminated.");
    }
    if (other.equals(this)) {
      throw new IllegalStateException("Can't join to itself.");
    }
    LOG.debug(String.format("%s joining to %s, JoinType %s, force %s, Direction %s, dtmfPassThrough %s", this, other,
        type, force, direction.toString(), dtmfPassThrough));
    
    //wait if processing re-INVITE from remote.
    waitProcessReInvite();

    if (_operationInProcess) {
      // this is used for the case that receive 183 from not-answered outgoing
      // call
      if (other instanceof SIPCallImpl && getJoiningPeer() == null) {
        this.setJoiningPeer(new JoinData(other, direction, type));
      }

      if (joint == null) {
        joint = new SettableJointImpl();
      }
      JoinRequest joinReq = new JoinRequest(other, type, direction, force, dtmfPassThrough, joint);
      _joinQueue.add(joinReq);
      return joint;
    }

    _operationInProcess = true;

    try {
      if (other instanceof SIPCallImpl) {
        return doJoin((SIPCallImpl) other, type, force, direction, dtmfPassThrough, joint);
      }
      else if (other instanceof RemoteParticipant) {
        return doJoin((RemoteParticipant) other, type, force, direction, dtmfPassThrough, joint);
      }
      else {
        return doJoin(other, type, false, direction, dtmfPassThrough, joint);
      }
    }
    catch (Exception ex) {
      if (_joinDelegate != null) {
        _joinDelegate.done(Cause.ERROR, ex);
      }
      throw new RuntimeException(ex);
    }
  }

  public synchronized void onEvent(final SdpPortManagerEvent event) {
    LOG.debug("Receive SdpPortManagerEvent:" + event);
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if(event.getEventType() == SdpPortManagerEvent.NETWORK_STREAM_FAILURE) {
      LOG.info(this + " detected media failure, hanguping call.");
      hangup();
    }
    else {
      try {
        if (_joinDelegate != null) {
          _joinDelegate.doSdpEvent(event);
        }
        else if (_callDelegate != null) {
          _callDelegate.handleSdpEvent(this, event);
        }
        else {
          LOG.debug("The SDP event will be discarded.");
        }
      }
      catch (final Exception e) {
        LOG.warn("Exception when processing SdpPortManagerEvent:" + event, e);
      }
    }
  }

  protected void doBye(final SipServletRequest req, final Map<String, String> headers) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "Processing BYE request.");
    }

    synchronized (this) {
      if (isTerminated()) {
        LOG.debug(this + " is already terminated.");
        try {
          req.createResponse(SipServletResponse.SC_OK).send();
        }
        catch (final Exception e) {
          LOG.warn("Excetion sending back SIP response", e);
        }
        return;
      }
      else {
        this.setSIPCallState(SIPCall.State.DISCONNECTED);
      }
    }

    terminate(CallCompleteEvent.Cause.DISCONNECT, null, headers);

    try {
      req.createResponse(SipServletResponse.SC_OK).send();
    }
    catch (final Exception e) {
      LOG.warn("Excetion sending back SIP response", e);
    }
  }

  protected synchronized void doAck(final SipServletRequest req) throws Exception {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if (!SIPHelper.isAck(req)) {
      LOG.debug("The SIP request isn't ACK.");
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "Processing ACK.");
    }

    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }
    
    if(pendingReinvite) {
      pendingReinvite = false;
      LOG.debug(this +" process pending re-INVITE complete.");
      notifyAll();
      return;
    }
    
    if (_joinDelegate != null) {
      _joinDelegate.doAck(req, this);
    }
    else if (_callDelegate != null && isAnswered()) {
      _callDelegate.handleAck(this, req);
      processingReinvite = false;
      LOG.debug(this +" process re-INVITE complete.");
      notifyAll();
    }
    else {
      LOG.debug("The SIP message will be discarded.");
    }
  }

  protected synchronized void doReinvite(final SipServletRequest req, final Map<String, String> headers)
      throws Exception {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if (!SIPHelper.isReinvite(req)) {
      LOG.debug("The SIP request isn't Re-INVITE.");
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "Processing re-INVITE.");
    }

    //joining and sent out re-INVITE
    if(_operationInProcess && reInvitingRemote) {
      req.createResponse(SipServletResponse.SC_REQUEST_PENDING).send();
      return;
    }
    
    //joining and not send out re-INVITE
    while(_operationInProcess && isAnswered()) {
      pendingReinvite = true;
      LOG.debug(this + "operation is in progress, waiting it complete.");
      wait();
      //sent out re-INVITE when joining
      if(needReInivteRemote) {
        SipServletResponse resp = req.createResponse(SipServletResponse.SC_OK);
        resp.setContent(SDPUtils.formulateSDP(this, getLocalSDP()), "application/sdp");
        resp.send();
        return;
      }
      pendingReinvite = false;
      
      if(!isAnswered()) {
        req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
        LOG.warn(this + " is already terminated. return error response for re-INVITE.");
        return;
      }
    }
    
    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }
    
    if (_callDelegate != null) {
      _callDelegate.handleReinvite(this, req, headers);
      processingReinvite = true;
    }
    else {
      LOG.debug("_callDelegate is null, operation in progress, returning blackhole SDP response.");
      SipServletResponse resp = req.createResponse(SipServletResponse.SC_OK);
      resp.setContent(SDPUtils.makeBlackholeSDP(SDPUtils.formulateSDP(this, getLocalSDP())), "application/sdp");
      resp.send();
    }
  }

  protected synchronized void doUpdate(final SipServletRequest req, final Map<String, String> headers) throws Exception {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "Processing UPDATE.");
    }

    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }
    if (_joinDelegate != null) {
      _joinDelegate.doUpdate(req, this, headers);
    }
    if (_callDelegate != null) {
      _callDelegate.handleUpdate(this, req, headers);
    }
    else {
      LOG.debug("CallDelegate is null, the SIP message will be discarded." + req);
    }
  }
  
  protected int getGlareReInivteDelay() {
    return 100;
  }

  protected synchronized void doResponse(final SipServletResponse res, final Map<String, String> headers)
      throws Exception {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "Processing response.");
    }
    if (SIPHelper.isInvite(res)) {
      if (res.getStatus() == SipServletResponse.SC_REQUEST_PENDING && isAnswered()) {
        // glare re-INVITE. send again after a random delay between 2.1 and 4
        // second.
        LOG.debug(String.format("%s received 491 response, glare re-INVITE, will retry re-INVITE later.", this));
        reInvitingRemote = false;
        final Map<String, String> logContexts = Utils.getCurrentLogContexts();
        ((ApplicationContextImpl) _context).getScheduledEcutor().schedule(new InheritLogContextRunnable() {
          @SuppressWarnings("unchecked")
          @Override
          public void run() {
            Utils.inheritLogContexts(logContexts);
            try {
              SIPCallImpl.this.reInviteRemote(res.getRequest().getContent(), (Map<String, String>) res.getRequest()
                  .getAttribute(Att_REINVITE_HEADERS),
                  (Map<String, String>) res.getRequest().getAttribute(Att_REINVITE_ATTRIBUTES));
            }
            catch (Exception e) {
              LOG.debug("Exception when sending re-INVITE", e);
              SIPCallImpl.this.fail(e);
            }
            finally{
              Utils.clearContexts();
            }
          }
        }, getGlareReInivteDelay(), TimeUnit.MILLISECONDS);
        return;
      }
      _inviteResponse = res;
      if (SIPHelper.isSuccessResponse(res)) {
        //For early media w/o 100rel case, we should wait until the early media response is processed.
        while(isProcessingNon100relEarlyMedia() && !isTerminated()) {
          LOG.debug("processing non100rel early media, delaying 200OK.");
          wait(2000);
        }
        final byte[] content = SIPHelper.getRawContentWOException(res);
        if (content != null) {
          setRemoteSDP(content);
        }
      }
      
      if (_joinDelegate != null) {
        _joinDelegate.doInviteResponse(res, this, headers);
      }
      else if (_callDelegate != null) {
        _callDelegate.handleReinviteResponse(this, res, headers);
      }
      
      reInvitingRemote = false;
    }
    else if (SIPHelper.isCancel(res) || SIPHelper.isBye(res)) {
      // ignore the response
    }
    else if (SIPHelper.isPrack(res)) {
      if (SIPHelper.isSuccessResponse(res)) {
        final byte[] content = SIPHelper.getRawContentWOException(res);
        if (content != null) {
          setRemoteSDP(content);
        }
      }

      if (_joinDelegate != null) {
        _joinDelegate.doPrackResponse(res, this, headers);
      }
    }
    else if (SIPHelper.isUpdate(res)) {
      if (SIPHelper.isSuccessResponse(res)) {
        final byte[] content = SIPHelper.getRawContentWOException(res);
        if (content != null) {
          setRemoteSDP(content);
        }
      }
      if (_joinDelegate != null) {
        _joinDelegate.doUpdateResponse(res, this, headers);
      }
      if (_callDelegate != null) {
        _callDelegate.handleUpdateResponse(this, res, headers);
      }
      else {
        if(sendingUpdate) {
          sendingUpdate = false;
          LOG.debug("receive response for UPDATE:" + res);
        }
        else {
          LOG.warn("Call delegate is null, discarding UPDATE response:" + res);
        }
      }
    }
    else {
      final SipServletRequest req = (SipServletRequest) SIPHelper.getLinkSIPMessage(res.getRequest());
      if (req != null) {
        LOG.debug("Found linked message, sending response to linked message:" + req);
        final SipServletResponse newRes = req.createResponse(res.getStatus(), res.getReasonPhrase());
        SIPHelper.addHeaders(newRes, headers);
        SIPHelper.copyContent(res, newRes);
        newRes.send();
      }
      else {
        LOG.warn("Discarding this response:" + res);
      }
    }
  }

  protected synchronized void setSIPCallState(final SIPCall.State state) {
    LOG.debug(this + " state changed from " + _cstate + " to " + state);
    _cstate = state;
  }

  protected synchronized boolean isNoAnswered() {
    return isNoAnswered(_cstate);
  }

  protected boolean isNoAnswered(final SIPCall.State state) {
    return state == SIPCall.State.INITIALIZED || state == SIPCall.State.INVITING || state == SIPCall.State.RINGING
        || state == SIPCall.State.ANSWERING || state == SIPCall.State.PROGRESSING || state == SIPCall.State.PROGRESSED;
  }

  protected synchronized boolean isAnswered() {
    return isAnswered(_cstate);
  }

  protected boolean isAnswered(final SIPCall.State state) {
    return state == SIPCall.State.ANSWERED;
  }

  protected synchronized boolean isTerminated() {
    return _cstate == SIPCall.State.FAILED || _cstate == SIPCall.State.DISCONNECTED
        || _cstate == SIPCall.State.REJECTED || _cstate == SIPCall.State.REDIRECTED;
  }

  protected void fail(Exception ex) {
    LOG.error(this + " failed.", ex);
    disconnect(true, CallCompleteEvent.Cause.ERROR, ex, null);
  }

  protected void doDisconnect(final boolean failed, final CallCompleteEvent.Cause cause, final Exception exception,
      Map<String, String> headers, SIPCall.State old) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + " is disconnecting.");
    }

    try {
      if (isNoAnswered(old)) {
        try {
          if (this instanceof SIPOutgoingCall) {
            if (_invite != null
                && (_invite.getSession().getState() == SipSession.State.EARLY || _invite.getSession().getState() == SipSession.State.INITIAL)) {
              try {
                SipServletRequest cancelRequest = _invite.createCancel();
                SIPHelper.addHeaders(cancelRequest, headers);
                cancelRequest.send();
              }
              catch (Exception ex) {
                LOG.warn("Exception when disconnecting failed outbound call:" + ex.getMessage());
                _invite.getSession().invalidate();
              }
            }
          }
          else if (this instanceof SIPIncomingCall) {
            if (_invite != null
                && (_invite.getSession().getState() == SipSession.State.EARLY || _invite.getSession().getState() == SipSession.State.INITIAL)) {
              SipServletResponse declineResponse = _invite.createResponse(SipServletResponse.SC_DECLINE);
              SIPHelper.addHeaders(declineResponse, headers);
              declineResponse.send();
            }
          }
        }
        catch (final Exception t) {
          LOG.warn("Exception when disconnecting call:" + t.getMessage());
        }
      }
      else if (isAnswered(old) && _invite.getSession().getState() != SipSession.State.TERMINATED) {
        try {
          SipServletRequest byeReq = _signal.createRequest("BYE");
          SIPHelper.addHeaders(byeReq, headers);
          byeReq.send();
        }
        catch (final Exception t) {
          LOG.warn("Exception when disconnecting call:" + t.getMessage());
        }
      }
    }
    finally {
      if (_invite != null) {
        SipApplicationSession appSession = _invite.getApplicationSession();

        try {
          if (appSession.isReadyToInvalidate()) {
            appSession.invalidate();
            if (LOG.isDebugEnabled()) {
              LOG.debug(appSession.getId() + " invalidated");
            }
          }
        }
        catch (IllegalStateException doofus) {
          try {
            appSession.invalidate();
            if (LOG.isDebugEnabled()) {
              LOG.debug(appSession.getId() + " invalidated anyway");
            }
          }
          catch (Exception ex) {
            LOG.warn("Exception caught while invalidating SipApplicationSession " + appSession.getId(), ex);
          }
        }
      }
    }
    terminate(cause, exception, null);
  }

  protected void disconnect(final boolean failed, final CallCompleteEvent.Cause cause, final Exception exception,
      final Map<String, String> headers) {
    final SIPCall.State old = getSIPCallState();

    synchronized (this) {
      if (isTerminated()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(this + " is already terminated.");
        }
        return;
      }

      if (failed) {
        this.setSIPCallState(SIPCall.State.FAILED);
      }
      else {
        this.setSIPCallState(SIPCall.State.DISCONNECTED);
      }

      notifyAll();
    }

    doDisconnect(failed, cause, exception, headers, old);
  }

  protected void terminate(final CallCompleteEvent.Cause cause, final Exception exception,
      final Map<String, String> headers) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + " is terminating.");
    }

    _context.removeCall(getId());

    if (_service != null) {
      ((GenericMediaService) _service).release((cause == CallCompleteEvent.Cause.DISCONNECT
          || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT || cause == CallCompleteEvent.Cause.CANCEL) ? true
          : false);
      _service = null;
    }

    destroyNetworkConnection();

    final Participant[] _joineesArray = _joinees.getJoinees();
    
    _context.getExecutor().execute(new InheritLogContextRunnable() {
      @Override
      public void run() {
        for (final Participant participant : _joineesArray) {
          _joinees.remove(participant);

          if (participant instanceof ParticipantContainer) {
            try {
              ((ParticipantContainer) participant).doUnjoin(SIPCallImpl.this, false);
            }
            catch (Exception e) {
              LOG.error("Exception when unjoining participant" + participant, e);
            }
          }
          
          UnjoinCompleteEvent.Cause unjoinCause = UnjoinCompleteEvent.Cause.ERROR;
          if (cause == CallCompleteEvent.Cause.DISCONNECT || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) {
            unjoinCause = UnjoinCompleteEvent.Cause.DISCONNECT;
          }
          dispatch(new MohoUnjoinCompleteEvent(SIPCallImpl.this, participant, unjoinCause, exception, true));
        }

        synchronized (_peers) {
          for (final Call peer : _peers) {
            try {
              _context.getExecutor().execute(new InheritLogContextRunnable() {

                @Override
                public void run() {
                  peer.disconnect();
                }
              });

            }
            catch (final Throwable t) {
              LOG.warn("Exception when disconnecting peer:" + peer, t);
            }
          }
          _peers.clear();
        }

        // TODO
        if (_joinDelegate != null) {
          if(!((_joinDelegate instanceof DirectAnswered2MultipleNOJoinDelegate ||
             _joinDelegate instanceof DirectNI2MultipleNOJoinDelegate ||
             _joinDelegate instanceof DirectNO2MultipleNOJoinDelegate) && _joinDelegate._call1 != SIPCallImpl.this)) {
            if (cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT || cause == CallCompleteEvent.Cause.DISCONNECT) {
              _joinDelegate.done(JoinCompleteEvent.Cause.DISCONNECTED, exception);
            }
            else if (cause == CallCompleteEvent.Cause.CANCEL) {
              _joinDelegate.done(JoinCompleteEvent.Cause.DISCONNECTED, exception);
            }
            else {
              _joinDelegate.done(JoinCompleteEvent.Cause.ERROR, exception);
            }
          }
          _joinDelegate = null;
        }

        SIPCallImpl.this.dispatch(new MohoCallCompleteEvent(SIPCallImpl.this, cause, exception, headers));
        _callDelegate = null;
      }
    });
  }

  @Override
  public void addParticipant(Participant p, JoinType type, Direction direction, Participant realJoined) {
    _joinees.add(p, type, direction, realJoined);
  }

  protected SipServletRequest getSipInitnalRequest() {
    return _invite;
  }
  
  protected SipServletResponse getLastReponse() {
    return _inviteResponse;
  }

  public byte[] getRemoteSdp() {
    return _remoteSDP;
  }

  public void setRemoteSDP(final byte[] sdp) {
    _remoteSDP = sdp;
  }

  public byte[] getLocalSDP() {
    return _localSDP;
  }

  public void setLocalSDP(final byte[] sdp) {
    _localSDP = sdp;
  }

  public void addPeer(final Call call, final JoinType type, final Direction direction) {
    if(type == JoinType.DIRECT && call instanceof SIPCallImpl) {
      setLocalSDP(((SIPCallImpl)call).getRemoteSdp());
    }
    synchronized (_peers) {
      if (!_peers.contains(call)) {
        _peers.add(call);
      }
    }
    _joinees.add(call, type, direction);
  }

  protected void removePeer(final Call call) {
    synchronized (_peers) {
      if (_peers.contains(call)) {
        _peers.remove(call);
      }
    }
    _joinees.remove(call);
  }

  protected Call getLastPeer() {
    synchronized (_peers) {
      if (_peers.size() == 0) {
        return null;
      }
      return _peers.get(_peers.size() - 1);
    }
  }

  protected boolean isDirectlyJoined() {
    synchronized (_peers) {
      return isAnswered() && _network == null && _peers.size() > 0;
    }
  }

  protected boolean isBridgeJoined() {
    return (isAnswered() || _cstate == SIPCall.State.PROGRESSED) && _network != null;
  }

  protected synchronized void linkCall(final SIPCallImpl call, final JoinType type, final Direction direction)
      throws MsControlException {
    if (JoinType.isBridge(type)) {
      JoinDelegate.bridgeJoin(this, call, direction);
    }
    this.addPeer(call, type, direction);
    call.addPeer(this, type, JoinDelegate.reserve(direction));
  }

  protected synchronized void unlinkDirectlyPeer() {
    if (isDirectlyJoined()) {
      synchronized (_peers) {
        for (final Call peer : _peers) {
          if (peer instanceof SIPCallImpl) {
            ((SIPCallImpl) peer).removePeer(this);
          }
        }
        _peers.clear();
      }
      _joinees.clear();
    }
  }

  protected synchronized void createNetworkConnection() throws MsControlException {
    if (_media == null) {
      final MsControlFactory mf = _context.getMSFactory();

      _media = mf.createMediaSession();
      // if (getSipSession() != null) {
      // if (LOG.isDebugEnabled()) {
      // LOG.debug("Set ms id with call id :" + getSipSession().getCallId());
      // }
      //
      // final Parameters params = _media.createParameters();
      //
      // params.put(MediaObject.MEDIAOBJECT_ID, "MS-" +
      // getSipSession().getCallId());
      // _media.setParameters(params);
      // }
    }
    if (_network == null) {
      Parameters params = _media.createParameters();

      _network = _media.createNetworkConnection(NetworkConnection.BASIC, params);
      _network.getSdpPortManager().addListener(this);
    }
  }
  
  protected synchronized void destroyNetworkConnection() {
    this.destroyNetworkConnection(true);
  }

  protected synchronized void destroyNetworkConnection(boolean disconnectedCall) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + "destroying NetworkConnection" + disconnectedCall);
    }
    if (_network != null) {
      try {
        MediaDialect dialect = ((ApplicationContextImpl) this.getApplicationContext()).getDialect();
        if (dialect != null) {
          dialect.stopCallRecord(_network);
        }
      }
      catch (final Throwable t) {
        LOG.warn("Exception when stopping call record", t);
      }
    }

    if (_service != null) {
      try {
        ((GenericMediaService) _service).release(disconnectedCall);
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing media service", t);
      }
      _service = null;
    }

    if (_network != null) {
      try {
        _network.release();
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing networkconnection", t);
      }
      _network = null;
    }
    if (_multiplejoiningMixer != null) {
      try {
        destroyMultipleJoiningMixer();
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing multiplejoiningMixer", t);
      }
      _multiplejoiningMixer = null;
    }
    if (_media != null) {
      try {
        _media.release();
      }
      catch (final Throwable t) {
        LOG.warn("Exception when releasing media object", t);
      }
      _media = null;
    }
  }

  protected synchronized void processSDPOffer(final SipServletMessage msg) throws MediaException {
    try {
      if (_network == null) {
        createNetworkConnection();
      }
      final byte[] sdpOffer = msg == null ? null : msg.getRawContent();
      if (sdpOffer == null) {
        _network.getSdpPortManager().generateSdpOffer();
      }
      else {
        _network.getSdpPortManager().processSdpOffer(sdpOffer);
      }
    }
    catch (final Throwable t) {
      LOG.error(t);
      if (msg instanceof SipServletRequest) {
        try {
          ((SipServletRequest) msg).createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
        }
        catch (final IOException e1) {
          LOG.warn("Exception when sending error response ", e1);
        }
      }
      throw new MediaException(t);
    }
  }

  protected synchronized void processSDPAnswer(final SipServletMessage msg) throws MediaException {
    if (_network == null) {
      throw new MediaException("NetworkConnection is NULL");
    }
    try {
      final byte[] remoteSdp = msg.getRawContent();
      if (remoteSdp != null) {
        _network.getSdpPortManager().processSdpAnswer(remoteSdp);
      }
      return;
    }
    catch (final Throwable t) {
      throw new MediaException(t);
    }
  }

  public synchronized void startJoin(final Participant participant, final JoinDelegate delegate) {
    if (_joinDelegate != null) {
      throw new IllegalStateException("other join operation in process.");
    }
    _operationInProcess = true;
    _joinDelegate = delegate;
  }

  public JoinDelegate getJoinDelegate() {
    return _joinDelegate;
  }

  @Override
  public JoinDelegate getJoinDelegate(String participantID) {
    return _joinDelegate;
  }

  protected void setCallDelegate(final SIPCallDelegate delegate) {
    _callDelegate = delegate;
  }

  protected Joint doJoin(final SIPCallImpl other, final JoinType type, final boolean force, final Direction direction,
      boolean dtmfPassThrough, SettableJointImpl joint) throws Exception {

    if (joint == null) {
      joint = new SettableJointImpl();
    }

    // join strategy check on either side
    final ExecutionException e = JoinDelegate.checkJoinStrategy(this, other, type, force);
    if (e == null) {
      _joinDelegate = createJoinDelegate(other, type, direction);
      _joinDelegate.setSettableJoint(joint);
      _joinDelegate.setDtmfPassThrough(dtmfPassThrough);
      other.startJoin(this, _joinDelegate);
      _joinDelegate.doJoin();
    }
    else {
      // dispatch BUSY event
      JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(this, other, Cause.BUSY, e, true);
      dispatch(joinCompleteEvent);
      joint.done(e);
    }

    return joint;
  }

  // RMI based.
  // protected Joint doJoin(final RemoteParticipant other, final JoinType type,
  // final Direction direction)
  // throws Exception {
  // JoinDelegate joinDelegate = null;
  // if (type != JoinType.DIRECT) {
  // joinDelegate = new LocalRemoteJoinDelegate(this, other, direction);
  // }
  // else {
  // joinDelegate = new DirectLocalRemoteJoinDelegate(this, other, direction);
  // }
  //
  // SettableJointImpl joint = new SettableJointImpl();
  // joinDelegate.setSettableJoint(joint);
  //
  // joinDelegate.doJoin();
  //
  // return joint;
  // }

  protected Joint doJoin(final RemoteParticipant other, final JoinType type, boolean force, final Direction direction,
      boolean dtmfPassThrough, SettableJointImpl joint) throws Exception {
    // 1 create outgoing call.
    // 2 join the outgoing call and return joint.
    String[] parsedJoinerID = ParticipantIDParser.parseEncodedId(this.getId());
    String[] parsedJoineeID = ParticipantIDParser.parseEncodedId(other.getId());

    SIPEndpoint joinerEndpoint = (SIPEndpoint) this.getApplicationContext().createEndpoint(
        "sip:" + this.getId() + "@" + parsedJoinerID[0]);
    SIPEndpoint joineeEndpoint = (SIPEndpoint) this.getApplicationContext().createEndpoint(
        "sip:" + other.getId() + "@" + parsedJoineeID[0]);

    RemoteJoinOutgoingCall outgoingCall = new RemoteJoinOutgoingCall((ExecutionContext) this.getApplicationContext(),
        joinerEndpoint, joineeEndpoint, null);
    outgoingCall.setX_Join_Direction(direction);
    outgoingCall.setX_Join_Force(force);
    outgoingCall.setX_Join_Type(type);
    LOG.debug("Starting remotejoin. joiner:" + this + ". joinee:" + other.getId() + ". created RemoteJoinOutgoingCall:"
        + outgoingCall);
    _operationInProcess = false;
    return this.join(outgoingCall, type, force, direction, dtmfPassThrough, joint);
  }

  protected Joint doJoin(final Participant other, final JoinType type, final boolean force, final Direction direction,
      boolean dtmfPassThrough, SettableJointImpl joint) throws Exception {
    if (!(other.getMediaObject() instanceof Joinable)) {
      throw new IllegalArgumentException("MediaObject is't joinable.");
    }

    if (joint == null) {
      joint = new SettableJointImpl();
    }

    // join strategy check on either side
    final ExecutionException e = JoinDelegate.checkJoinStrategy(this, other, type, force);
    if (e == null) {
      _joinDelegate = new OtherParticipantJoinDelegate(this, other, type, direction);
      _joinDelegate.setSettableJoint(joint);
      _joinDelegate.setDtmfPassThrough(dtmfPassThrough);
      _joinDelegate.doJoin();
    }
    else {
      // dispatch BUSY event
      JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(this, other, Cause.BUSY, e, true);
      dispatch(joinCompleteEvent);
      JoinCompleteEvent peerJoinCompleteEvent = new MohoJoinCompleteEvent(other, this, Cause.BUSY, e, false);
      other.dispatch(peerJoinCompleteEvent);
      joint.done(e);
    }
    return joint;
  }

  protected abstract JoinDelegate createJoinDelegate(final Direction direction);

  protected abstract JoinDelegate createJoinDelegate(final SIPCallImpl other, final JoinType type,
      final Direction direction);

  protected JoinDelegate createJoinDelegate(final Call[] others, final JoinType type, final Direction direction) {
    JoinDelegate retval = null;
    List<SIPCallImpl> candidates = new LinkedList<SIPCallImpl>();
    for (Call call : others) {
      candidates.add((SIPCallImpl) call);
    }
    if (type == JoinType.DIRECT) {
      if (this.isAnswered()) {
        retval = new DirectAnswered2MultipleNOJoinDelegate(type, direction, this,
            Utils.suppressEarlyMedia(getApplicationContext()), candidates);
      }
    }
    else {
      retval = new MultipleNOBridgeJoinDelegate(type, direction, this, candidates);
    }

    return retval;
  }

  protected boolean isOperationInprocess() {
    return _operationInProcess;
  }

  protected void setOperationInprocess(final boolean flag) {
    _operationInProcess = flag;
  }

  protected enum HoldState {
    None, Holding, Held, UnHolding, Muting, Muted, UnMuting, Deafing, Deafed, Undeafing
  }

  protected HoldState _holdState = HoldState.None;

  protected HoldState _muteState = HoldState.None;

  protected HoldState _deafState = HoldState.None;

  protected int waitRespNum;

  protected synchronized HoldState getMuteState() {
    return _muteState;
  }

  protected synchronized void setMuteState(final HoldState muteState) {
    _muteState = muteState;
  }

  protected synchronized HoldState getHoldState() {
    return _holdState;
  }

  protected synchronized void setHoldState(final HoldState holdState) {
    _holdState = holdState;
    if (_holdState == HoldState.Holding || _holdState == HoldState.UnHolding) {
      waitRespNum = 2;
    }
  }

  protected synchronized void setDeafState(final HoldState deafState) {
    _deafState = deafState;
  }

  protected synchronized HoldState getDeafState() {
    return _deafState;
  }

  protected synchronized boolean isHoldingProcess() {
    return _holdState == HoldState.Holding || _holdState == HoldState.UnHolding;
  }

  protected synchronized boolean isMutingProcess() {
    return _muteState == HoldState.Muting || _muteState == HoldState.UnMuting;
  }

  protected synchronized boolean isDeafingProcess() {
    return _deafState == HoldState.Deafing || _deafState == HoldState.Undeafing;
  }

  protected synchronized void holdResp() {
    waitRespNum--;
    if (waitRespNum == 0) {
      if (getHoldState() == HoldState.Holding) {
        setHoldState(HoldState.Held);
      }
      else if (getHoldState() == HoldState.UnHolding) {
        setHoldState(HoldState.None);
      }
      this.notify();
    }
  }

  /**
   * send a sendonly SDP and stop to send media data to this endpoint
   */
  @Override
  public synchronized void hold() {
    hold(false);
  }

  /**
   * send a sendonly SDP and stop to send media data to this endpoint
   */
  public synchronized void hold(final boolean send) {
    if (this.getSIPCallState() != SIPCall.State.ANSWERED) {
      throw new IllegalStateException("call have not been answered");
    }

    if (_holdState == HoldState.Held || _holdState == HoldState.Holding) {
      return;
    }

    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    try {
      setHoldState(HoldState.Holding);
      _callDelegate.hold(this, send);

      while (getHoldState() != HoldState.Held && getHoldState() != HoldState.None && isAnswered()) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          LOG.warn("InterruptedException when wait hold, the HoldState " + getHoldState());
        }
      }
    }
    catch (final MsControlException e) {
      setHoldState(HoldState.None);
      throw new MediaException("exception when holding", e);
    }
    catch (final IOException e) {
      setHoldState(HoldState.None);
      throw new SignalException("exception when holding", e);
    }
    catch (final SdpException e) {
      setHoldState(HoldState.None);
      throw new SignalException("exception when holding", e);
    }
    catch (final Throwable t) {
      setHoldState(HoldState.None);
      LOG.error("Error when holding", t);
    }
    finally {
      _operationInProcess = false;
      reInvitingRemote = false;
    }
  }

  @Override
  public boolean isHold() {
    return _holdState == HoldState.Held;
  }

  @Override
  public boolean isMute() {
    return _muteState == HoldState.Muted;
  }

  /**
   * send a sendonly SDP to the endpoint, but still send media data to this
   * endpoint
   */
  @Override
  public synchronized void mute() {
    if (this.getSIPCallState() != SIPCall.State.ANSWERED) {
      throw new IllegalStateException("call have not been answered");
    }

    if (_muteState == HoldState.Muted || _muteState == HoldState.Muting) {
      return;
    }

    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    try {
      setMuteState(HoldState.Muting);
      _callDelegate.mute(this);

      while (getMuteState() != HoldState.Muted && getMuteState() != HoldState.None&& isAnswered()) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          LOG.warn("InterruptedException when wait mute, the MuteState " + getMuteState());
        }
      }
    }
    catch (final IOException e) {
      setMuteState(HoldState.None);
      throw new SignalException("exception when muting", e);
    }
    catch (final SdpException e) {
      setMuteState(HoldState.None);
      throw new SignalException("exception when muting", e);
    }
    catch (final Throwable t) {
      setHoldState(HoldState.None);
      LOG.error("Error when mute", t);
    }
    finally {
      _operationInProcess = false;
      reInvitingRemote = false;
    }
  }

  @Override
  public synchronized void unhold() {
    if (_holdState != HoldState.Held) {
      return;
    }
    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    HoldState oldHoldState = null;
    try {
      oldHoldState = getHoldState();
      setHoldState(HoldState.UnHolding);

      _callDelegate.unhold(this);

      while (getHoldState() != HoldState.None && isAnswered()) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          LOG.warn("InterruptedException when wait unhold, the HoldState " + getHoldState());
        }
      }
    }
    catch (final MsControlException e) {
      setHoldState(oldHoldState);
      throw new SignalException("exception when unholding", e);
    }
    catch (final IOException e) {
      setHoldState(oldHoldState);
      throw new SignalException("exception when unholding", e);
    }
    catch (final SdpException e) {
      setHoldState(oldHoldState);
      throw new SignalException("exception when unholding", e);
    }
    catch (final Throwable t) {
      setHoldState(HoldState.None);
      LOG.error("Error when unhold", t);
    }
    finally {
      _operationInProcess = false;
      reInvitingRemote = false;
    }
  }

  @Override
  public synchronized void unmute() {
    if (_muteState != HoldState.Muted) {
      return;
    }

    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    HoldState oldMuteState = null;
    try {
      oldMuteState = getMuteState();
      setMuteState(HoldState.UnMuting);

      _callDelegate.unmute(this);

      while (getMuteState() != HoldState.None && isAnswered()) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          LOG.warn("InterruptedException when wait unmute, the MuteState " + getMuteState());
        }
      }
    }
    catch (final IOException e) {
      setMuteState(oldMuteState);
      throw new SignalException("exception when unmuting", e);
    }
    catch (final SdpException e) {
      setMuteState(oldMuteState);
      throw new SignalException("exception when unmuting", e);
    }
    catch (final Throwable t) {
      setHoldState(HoldState.None);
      LOG.error("Error when unmute", t);
    }
    finally {
      _operationInProcess = false;
      reInvitingRemote = false;
    }
  }

  // for invite event =============
  @Override
  public SipServletRequest getSipRequest() {
    return _invite;
  }

  @Override
  public String getHeader(final String name) {
    return _invite.getHeader(name);
  }

  @Override
  public ListIterator<String> getHeaders(final String name) {
    return _invite.getHeaders(name);
  }

  @Override
  public Iterator<String> getHeaderNames() {
    return _invite.getHeaderNames();
  }

  @Override
  public Endpoint getInvitor() {
    return _caller;
  }

  @Override
  public CallableEndpoint getInvitee() {
    return _callee;
  }

  // for dispatchable eventsource over=========

  public JoinData getJoiningPeer() {
    return _bridgeJoiningPeer;
  }

  public void setJoiningPeer(final JoinData bridgeJoiningPeer) {
    _bridgeJoiningPeer = bridgeJoiningPeer;
  }

  @Override
  public synchronized Endpoint getAddress() {
    return _address;
  }

  @Override
  public String getRemoteAddress() {
    return _id;
  }

  public void createMultipleJoiningMixer() throws MsControlException {
    if (_multiplejoiningMixer == null) {
      Parameters params = Parameters.NO_PARAMETER;
      params = _network.getParameters(new Parameter[] {MediaObject.MEDIAOBJECT_ID});
      params.put(MediaObject.MEDIAOBJECT_ID, "JoinStrategy-ShadowMixer-" + params.get(MediaObject.MEDIAOBJECT_ID));
      _multiplejoiningMixer = _media.createMediaMixer(MediaMixer.AUDIO, params);
    }
  }

  public void destroyMultipleJoiningMixer() throws MsControlException {
    try {
      if (_multiplejoiningMixer != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("destroyMultipleJoiningMixer: " + _multiplejoiningMixer);
        }

        if (_network != null) {
          _network.unjoin(_multiplejoiningMixer);
        }
        _multiplejoiningMixer.release();
      }
    }
    finally {
      _multiplejoiningMixer = null;
    }
  }

  public MediaMixer getMultipleJoiningMixer() {
    return _multiplejoiningMixer;
  }

  class JoinRequest {
    private Participant peer;
    
    private Call[] calls;

    private JoinType type;

    private Direction direction;

    private boolean force;

    private boolean dtmfPassThrough;

    private SettableJointImpl joint;

    public JoinRequest(Participant peer, JoinType type, Direction direction, boolean force, boolean dtmfPassThrough,
        SettableJointImpl joint) {
      super();
      this.peer = peer;
      this.direction = direction;
      this.type = type;
      this.dtmfPassThrough = dtmfPassThrough;
      this.joint = joint;
      this.force = force;
    }
    
    public JoinRequest(Call[] calls, JoinType type, Direction direction, boolean force, boolean dtmfPassThrough,
        SettableJointImpl joint) {
      super();
      this.calls = calls;
      this.direction = direction;
      this.type = type;
      this.dtmfPassThrough = dtmfPassThrough;
      this.joint = joint;
      this.force = force;
    }

    public JoinRequest(Direction direction, SettableJointImpl joint) {
      super();
      this.direction = direction;
      this.joint = joint;
    }

    public SettableJointImpl getJoint() {
      return joint;
    }

    public Participant getPeer() {
      return peer;
    }

    public Direction getDirection() {
      return direction;
    }

    public JoinType getType() {
      return type;
    }

    public boolean isDtmfPassThrough() {
      return dtmfPassThrough;
    }

    public boolean isForce() {
      return force;
    }

    public Call[] getCalls() {
      return calls;
    }
  }

  private boolean sendingUpdate;

  public boolean isSendingUpdate() {
    return sendingUpdate;
  }

  public void setSendingUpdate(boolean sendingUpdate) {
    this.sendingUpdate = sendingUpdate;
  }

  public void update() {
    if (_cstate != SIPCall.State.PROGRESSED && _cstate != SIPCall.State.ANSWERED) {
      LOG.warn("Call media didn't negotiate, can't send UPDATE");
      return;
    }
    try {
      SipServletRequest updateReq = _invite.getSession().createRequest("UPDATE");
      if (_localSDP != null) {
        updateReq.setContent(SDPUtils.formulateSDP(this, _localSDP), "application/sdp");
      }
      sendingUpdate = true;
      updateReq.send();
    }
    catch (Exception ex) {
      sendingUpdate = false;
      LOG.error("Can't send UPDATE reqeust.", ex);
    }
  }
  
  public SessionDescription createSendonlySDP(final byte[] sdpByte) throws UnsupportedEncodingException, SdpException {
    SdpFactory sdpFactory = ((ExecutionContext) getApplicationContext()).getSdpFactory();
    SessionDescription sd = sdpFactory.createSessionDescription(new String(sdpByte, "iso8859-1"));

    sd.removeAttribute("sendrecv");
    sd.removeAttribute("recvonly");

    MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
    md.removeAttribute("sendrecv");
    md.removeAttribute("recvonly");
    md.setAttribute("sendonly", null);

    return sd;
  }
  
  public synchronized void reInviteRemote(Object sdp, Map<String, String> headers, Map<String, String> attributes) throws IOException {
    if(!isAnswered()) {
      throw new IllegalStateException(this + " was terminated.");
    }
    
    while(pendingReinvite && isAnswered()) {
      try {
        needReInivteRemote = true;
        notifyAll();
        wait();
      }
      catch (InterruptedException e) {
        //ignore
      }
    }
    
    needReInivteRemote = false;
    
    SipServletRequest reInvite = getSipSession().createRequest("INVITE");
    if(sdp != null) {
      reInvite.setContent(SDPUtils.formulateSDP(this, sdp), "application/sdp");
    }
    if(headers != null) {
      reInvite.setAttribute(Att_REINVITE_HEADERS, headers);
      SIPHelper.addHeaders(reInvite, headers);
    }
    
    if(attributes != null) {
      reInvite.setAttribute(Att_REINVITE_ATTRIBUTES, attributes);
      for(Entry<String, String> entry: attributes.entrySet()) {
        reInvite.setAttribute(entry.getKey(), entry.getValue());
      }
    }
    
    if(this instanceof OutgoingCall && _invite.getHeader("ALLOW") != null) {
      reInvite.addHeader("ALLOW", _invite.getHeader("ALLOW"));
    }
    
    reInvite.send();
    reInvitingRemote = true;
  }
  
  private synchronized void waitProcessReInvite() {
    while(processingReinvite && isAnswered()) {
      try {
        LOG.debug(this +" processing re-INVITE. wait it complete.");
        wait();
      }
      catch (InterruptedException e) {
        //ignore
      }
      
      if(!isAnswered()) {
        String err = this + " is already terminated. operation can't complete.";
        LOG.warn(err);
        throw new IllegalStateException(err);
      }
    }
  }

  public Origin getPreviousOrigin() {
    return previousOrigin;
  }

  public void setPreviousOrigin(Origin previousOrigin) {
    this.previousOrigin = previousOrigin;
  }
  
  public boolean isProcessingNon100relEarlyMedia() {
    return processingNon100relEarlyMedia;
  }

  public void setProcessingNon100relEarlyMedia(boolean processingNon100relEarlyMedia) {
    this.processingNon100relEarlyMedia = processingNon100relEarlyMedia;
  }
  
  public boolean isDispatchedNon100relEarlyMedia() {
    return dispatchedNon100relEarlyMedia;
  }

  public void setDispatchedNon100relEarlyMedia(boolean dispatchedNon100relEarlyMedia) {
    this.dispatchedNon100relEarlyMedia = dispatchedNon100relEarlyMedia;
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event) {
    if(Configuration.isEarlyMediaWithout100rel() && event instanceof SIPEarlyMediaEvent) {
      if(!SIPHelper.needPrack(((SIPEarlyMediaEvent)event).getSipResponse())) {
        LOG.debug("Start process non100rel early media.");
        processingNon100relEarlyMedia = true;
        dispatchedNon100relEarlyMedia = true;
      }
    }
    return super.dispatch(event);
  }
}
