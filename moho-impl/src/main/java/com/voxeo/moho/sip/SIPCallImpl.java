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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
import javax.sdp.SdpException;
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
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.HangupException;
import com.voxeo.moho.JoinData;
import com.voxeo.moho.JoineeData;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.UnjointImpl;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.GenericMediaService;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.SessionUtils;

public abstract class SIPCallImpl extends CallImpl implements SIPCall, MediaEventListener<SdpPortManagerEvent>,
    ParticipantContainer {

  private static final Logger LOG = Logger.getLogger(SIPCallImpl.class);

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

  protected SIPCallImpl _bridgeJoiningPeer;

  protected MediaMixer _multiplejoiningMixer;

  // TODO: join to MediaGroup
  protected MediaMixer _multiplejoiningMixerForMedGrop;

  protected Lock mediaServiceLock = new ReentrantLock();

  protected Queue<JoinDelegate> _joinQueue = new LinkedList<JoinDelegate>();

  protected SipServletResponse _inviteResponse;

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
    return new StringBuilder().append(SIPCallImpl.class.getSimpleName()).append("[").append(_signal).append(",")
        .append(_cstate).append("]").toString();
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
          try {
            this.join(Direction.DUPLEX).get();
          }
          catch (final Exception e) {
            throw new MediaException(e);
          }
        }
        else {
          throw new IllegalStateException("the call is Direct mode but reinvite is false");
        }
      }

      try {
        if (_service == null) {
          Parameters params = null;
          if (getSipSession() != null) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Set mg id with call id :" + getSipSession().getCallId());
            }

            params = _media.createParameters();
            params.put(MediaObject.MEDIAOBJECT_ID, "MG-" + getSipSession().getCallId());
            _media.setParameters(params);
          }

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
    return _joinees.getJoinees();
  }

  @Override
  public Participant[] getParticipants(final Direction direction) {
    return _joinees.getJoinees(direction);
  }

  @Override
  public JoinType getJoinType(Participant participant) {
    return _joinees.getJoinType(participant);
  }

  @Override
  public Direction getDirection(Participant participant) {
    return _joinees.getDirection(participant);
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
    try {
      JoinData joinData = _joinees.remove(p);
      if (p instanceof Call) {
        synchronized (_peers) {
          _peers.remove(p);
        }
      }
      if (p.getMediaObject() instanceof Joinable) {
        if (initiator) {
          if (joinData.getRealJoined() == null) {
            JoinDelegate.bridgeUnjoin(this, p);
          }
          else {
            JoinDelegate.bridgeUnjoin(joinData.getRealJoined(), p);
          }
        }
      }

      if (initiator) {
        ((ParticipantContainer) p).unjoin(SIPCallImpl.this, false);
      }

      event = new MohoUnjoinCompleteEvent(SIPCallImpl.this, p, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, initiator);
    }
    catch (final Exception e) {
      LOG.error("", e);
      event = new MohoUnjoinCompleteEvent(SIPCallImpl.this, p, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, e, true);
      throw e;
    }
    finally {
      if (event == null) {
        event = new MohoUnjoinCompleteEvent(SIPCallImpl.this, p, UnjoinCompleteEvent.Cause.FAIL_UNJOIN, initiator);
      }
      SIPCallImpl.this.dispatch(event);
    }
    return event;

  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction,
      final Map<String, String> headers) {
    Participant p = null;
    try {
      p = other.call(getAddress(), headers);
    }
    catch (final Exception e) {
      LOG.error(e);
      return new JointImpl(_context.getExecutor(), new JointImpl.DummyJoinWorker(SIPCallImpl.this, p, e));
    }
    return join(p, type, direction);
  }

  @Override
  public synchronized Joint join(final Direction direction) {
    if (isTerminated()) {
      throw new IllegalStateException("already terminated.");
    }
    if (_operationInProcess) {
      if (_joinDelegate != null
          && !(_joinDelegate instanceof BridgeJoinDelegate || _joinDelegate instanceof OtherParticipantJoinDelegate
              || _joinDelegate instanceof LocalRemoteJoinDelegate || _joinDelegate instanceof RemoteLocalJoinDelegate)) {

        JoinDelegate joinDelegate = createJoinDelegate(direction);
        SettableJointImpl joint = new SettableJointImpl();
        joinDelegate.setSettableJoint(joint);
        _joinQueue.add(joinDelegate);
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

      SettableJointImpl joint = new SettableJointImpl();
      _joinDelegate.setSettableJoint(joint);
      _joinDelegate.doJoin();

      return joint;
    }
    catch (Exception ex) {
      // TODO
      throw new RuntimeException(ex);
    }
  }

  public synchronized void joinDone(final Participant participant, final JoinDelegate delegate) {
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

    if (_oldJoinDelegate != null) {
      JoinCompleteEvent.Cause cause = _joinDelegate.getCause();
      Exception exception = _joinDelegate.getException();
      _joinDelegate = _oldJoinDelegate;
      _oldJoinDelegate = null;
      if (cause == JoinCompleteEvent.Cause.JOINED) {
        try {
          _operationInProcess = true;
          _joinDelegate.doJoin();
        }
        catch (Exception e) {
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
      _operationInProcess = false;
    }
  }

  public synchronized void continueQueuedJoin() {
    JoinDelegate queuedJoinDelegate = _joinQueue.poll();

    if (queuedJoinDelegate != null) {
      try {
        _joinDelegate = queuedJoinDelegate;
        _operationInProcess = true;
        if (_joinDelegate.getPeer() != null) {
          _joinDelegate.getPeer().startJoin(this, _joinDelegate);
        }
        queuedJoinDelegate.doJoin();
      }
      catch (Exception e) {
        queuedJoinDelegate.done(Cause.ERROR, e);
        LOG.error("Exception when execute queued join", e);
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
    if (isTerminated()) {
      throw new IllegalStateException("This call is already terminated.");
    }
    if (other.equals(this)) {
      throw new IllegalStateException("Can't join to itself.");
    }

    if (_operationInProcess) {
      SettableJointImpl joint = new SettableJointImpl();
      if (other instanceof SIPCallImpl) {
        SIPCallImpl call = (SIPCallImpl) other;
        JoinDelegate joinDelegate = createJoinDelegate(call, type, direction);
        joinDelegate.setSettableJoint(joint);

        _joinQueue.add(joinDelegate);
      }

      else if (other instanceof RemoteParticipant) {
        RemoteParticipant remote = (RemoteParticipant) other;
        JoinDelegate joinDelegate = null;
        if (type != JoinType.DIRECT) {
          joinDelegate = new LocalRemoteJoinDelegate(this, remote, direction);
        }
        else {
          joinDelegate = new DirectLocalRemoteJoinDelegate(this, remote, direction);
        }

        joinDelegate.setSettableJoint(joint);

        _joinQueue.add(joinDelegate);
      }
      else {
        JoinDelegate joinDelegate = new OtherParticipantJoinDelegate(this, other, type, direction);
        joinDelegate.setSettableJoint(joint);

        _joinQueue.add(joinDelegate);
      }
      return joint;
    }

    _operationInProcess = true;

    try {
      if (other instanceof SIPCallImpl) {
        return doJoin((SIPCallImpl) other, type, force, direction);
      }
      else if (other instanceof RemoteParticipant) {
        return doJoin((RemoteParticipant) other, type, direction);
      }
      else {
        return doJoin(other, type, false, direction);
      }
    }
    catch (Exception ex) {
      // TODO
      throw new RuntimeException(ex);
    }
  }

  public synchronized void onEvent(final SdpPortManagerEvent event) {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
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
        LOG.warn("", e);
      }
    }
  }

  protected synchronized void doBye(final SipServletRequest req, final Map<String, String> headers) {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    else {
      if (_joinDelegate != null) {
        _joinDelegate.done(JoinCompleteEvent.Cause.DISCONNECTED, new HangupException());
      }
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
      terminate(CallCompleteEvent.Cause.DISCONNECT, null, headers);
    }

    try {
      req.createResponse(SipServletResponse.SC_OK).send();
    }
    catch (final Exception e) {
      LOG.warn("", e);
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
    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }
    if (_joinDelegate != null) {
      _joinDelegate.doAck(req, this);
    }
    else if (_callDelegate != null && isAnswered()) {
      _callDelegate.handleAck(this, req);
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
    final byte[] content = SIPHelper.getRawContentWOException(req);
    if (content != null) {
      setRemoteSDP(content);
    }
    if (_callDelegate != null) {
      _callDelegate.handleReinvite(this, req, headers);
    }
    else {
      LOG.debug("The SIP message will be discarded.");
    }
  }

  protected synchronized void doResponse(final SipServletResponse res, final Map<String, String> headers)
      throws Exception {
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(res);
    }
    if (SIPHelper.isInvite(res)) {
      _inviteResponse = res;
      if (SIPHelper.isSuccessResponse(res)) {
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
    }
    else if (SIPHelper.isCancel(res) || SIPHelper.isBye(res)) {
      ;
    }
    else {
      final SipServletRequest req = (SipServletRequest) SIPHelper.getLinkSIPMessage(res.getRequest());
      if (req != null) {
        final SipServletResponse newRes = req.createResponse(res.getStatus(), res.getReasonPhrase());
        SIPHelper.addHeaders(newRes, headers);
        SIPHelper.copyContent(res, newRes);
        newRes.send();
      }
    }
  }

  protected synchronized void setSIPCallState(final SIPCall.State state) {
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
    disconnect(true, CallCompleteEvent.Cause.ERROR, ex, null);
  }

  protected synchronized void disconnect(final boolean failed, final CallCompleteEvent.Cause cause,
      final Exception exception, Map<String, String> headers) {
    if (isTerminated()) {
      if (LOG.isTraceEnabled()) {
        LOG.trace(this + " is already terminated.");
      }
      return;
    }

    final SIPCall.State old = getSIPCallState();
    if (failed) {
      this.setSIPCallState(SIPCall.State.FAILED);
    }
    else {
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
    }
    terminate(cause, exception, null);
    if (isNoAnswered(old)) {
      try {
        if (this instanceof SIPOutgoingCall && !failed) {
          if (_invite != null) {
            SipServletRequest cancelRequest = _invite.createCancel();
            SIPHelper.addHeaders(cancelRequest, headers);
            cancelRequest.send();
          }
        }
        else if (this instanceof SIPIncomingCall) {
          SipServletResponse declineResponse = _invite.createResponse(SipServletResponse.SC_DECLINE);
          SIPHelper.addHeaders(declineResponse, headers);
          declineResponse.send();
        }
      }
      catch (final Exception t) {
        LOG.warn("Exception when disconnecting call", t);
      }
    }
    else if (isAnswered(old) && !failed) {
      try {
        _signal.createRequest("BYE").send();
      }
      catch (final Exception t) {
        LOG.warn("Exception when disconnecting call", t);
      }
    }
  }

  protected synchronized void terminate(final CallCompleteEvent.Cause cause, final Exception exception,
      final Map<String, String> headers) {
    _context.removeCall(getId());

    if (_service != null) {
      ((GenericMediaService) _service)
          .release((cause == CallCompleteEvent.Cause.DISCONNECT || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) ? true
              : false);
      _service = null;
    }

    destroyNetworkConnection();

    Participant[] _joineesArray = _joinees.getJoinees();
    for (Participant participant : _joineesArray) {
      UnjoinCompleteEvent.Cause unjoinCause = UnjoinCompleteEvent.Cause.ERROR;
      if (cause == CallCompleteEvent.Cause.DISCONNECT || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) {
        unjoinCause = UnjoinCompleteEvent.Cause.DISCONNECT;
      }

      dispatch(new MohoUnjoinCompleteEvent(this, participant, unjoinCause, exception, true));

      if (participant instanceof ParticipantContainer) {
        try {
          ((ParticipantContainer) participant).doUnjoin(this, false);
        }
        catch (Exception e) {
          LOG.error("", e);
        }
      }
    }
    _joinees.clear();

    synchronized (_peers) {
      for (final Call peer : _peers) {
        try {
          peer.disconnect();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
      _peers.clear();
    }

    // TODO
    if (_joinDelegate != null) {
      if (cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) {
        _joinDelegate.done(JoinCompleteEvent.Cause.DISCONNECTED, exception);
      }
      else {
        _joinDelegate.done(JoinCompleteEvent.Cause.ERROR, exception);
      }
    }

    this.dispatch(new MohoCallCompleteEvent(this, cause, exception, headers));

    _callDelegate = null;
  }

  @Override
  public void addParticipant(Participant p, JoinType type, Direction direction, Participant realJoined) {
    _joinees.add(p, type, direction, realJoined);
  }

  protected SipServletRequest getSipInitnalRequest() {
    return _invite;
  }

  protected byte[] getRemoteSdp() {
    return _remoteSDP;
  }

  protected void setRemoteSDP(final byte[] sdp) {
    _remoteSDP = sdp;
  }

  protected byte[] getLocalSDP() {
    return _localSDP;
  }

  protected void setLocalSDP(final byte[] sdp) {
    _localSDP = sdp;
  }

  public void addPeer(final Call call, final JoinType type, final Direction direction) {
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
    return isAnswered() && _network != null;
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
      if (getSipSession() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Set ms id with call id :" + getSipSession().getCallId());
        }

        final Parameters params = _media.createParameters();

        params.put(MediaObject.MEDIAOBJECT_ID, "MS-" + getSipSession().getCallId());
        _media.setParameters(params);
      }
    }
    if (_network == null) {
      Parameters params = null;
      if (getSipSession() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Set nc id with call id :" + getSipSession().getCallId());
        }

        params = _media.createParameters();

        params.put(MediaObject.MEDIAOBJECT_ID, "MS-" + getSipSession().getCallId());
        params.put(MediaObject.MEDIAOBJECT_ID, "NC-" + getSipSession().getCallId());
        _media.setParameters(params);
      }
      _network = _media.createNetworkConnection(NetworkConnection.BASIC, params);
      _network.getSdpPortManager().addListener(this);
    }
  }

  protected synchronized void destroyNetworkConnection() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("destroyNetworkConnection");
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

  protected Joint doJoin(final SIPCallImpl other, final JoinType type, final boolean force, final Direction direction)
      throws Exception {
    SettableJointImpl joint = new SettableJointImpl();

    // join strategy check on either side
    final ExecutionException e = JoinDelegate.checkJoinStrategy(this, other, type, force);
    if (e == null) {
      _joinDelegate = createJoinDelegate(other, type, direction);
      _joinDelegate.setSettableJoint(joint);
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

  protected Joint doJoin(final RemoteParticipant other, final JoinType type, final Direction direction)
      throws Exception {
    JoinDelegate joinDelegate = null;
    if (type != JoinType.DIRECT) {
      joinDelegate = new LocalRemoteJoinDelegate(this, other, direction);
    }
    else {
      joinDelegate = new DirectLocalRemoteJoinDelegate(this, other, direction);
    }

    SettableJointImpl joint = new SettableJointImpl();
    joinDelegate.setSettableJoint(joint);

    joinDelegate.doJoin();

    return joint;
  }

  protected Joint doJoin(final Participant other, final JoinType type, final boolean force, final Direction direction)
      throws Exception {
    if (!(other.getMediaObject() instanceof Joinable)) {
      throw new IllegalArgumentException("MediaObject is't joinable.");
    }

    SettableJointImpl joint = new SettableJointImpl();

    // join strategy check on either side
    final ExecutionException e = JoinDelegate.checkJoinStrategy(this, other, type, force);
    if (e == null) {
      _joinDelegate = new OtherParticipantJoinDelegate(this, other, type, direction);
      _joinDelegate.setSettableJoint(joint);
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

      while (getHoldState() != HoldState.Held && getHoldState() != HoldState.None) {
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

      while (getMuteState() != HoldState.Muted && getMuteState() != HoldState.None) {
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

      while (getHoldState() != HoldState.None) {
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

      while (getMuteState() != HoldState.None) {
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

  public SIPCallImpl getBridgeJoiningPeer() {
    return _bridgeJoiningPeer;
  }

  public void setBridgeJoiningPeer(final SIPCallImpl bridgeJoiningPeer) {
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
  
  
}
