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

package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.SdpException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionsUtil;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.BusyException;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.CanceledException;
import com.voxeo.moho.DisconnectedException;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExceptionHandler;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.JoinWorker;
import com.voxeo.moho.JoineeData;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.RejectException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.TimeoutException;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.AutowiredEventTarget;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.EventDispatcher;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.ForwardableEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.media.GenericMediaService;
import com.voxeo.moho.util.SessionUtils;
import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;

public abstract class SIPCallImpl extends SIPCall implements MediaEventListener<SdpPortManagerEvent> {

  private static final Logger LOG = Logger.getLogger(SIPCallImpl.class);

  protected SIPCall.State _cstate;

  protected boolean _isSupervised;

  protected SIPEndpoint _address;

  protected SipServletRequest _invite;

  protected byte[] _remoteSDP;

  protected byte[] _localSDP;

  protected SipSession _signal;

  protected MediaSession _media;

  protected NetworkConnection _network;

  protected MediaService _service;

  protected JoinDelegate _joinDelegate;

  protected SIPCallDelegate _callDelegate;

  protected List<Call> _peers = new ArrayList<Call>(0);

  protected JoineeData _joinees = new JoineeData();

  protected boolean _operationInProcess;

  protected String _replacesHeader;

  protected Exception _exception;

  // invite event
  protected CallableEndpoint _caller;

  protected CallableEndpoint _callee;

  // dispatchable event source
  protected String _id;

  protected Map<String, String> _states = new ConcurrentHashMap<String, String>();

  protected ExecutionContext _context;

  protected EventDispatcher _dispatcher = new EventDispatcher();

  protected ConcurrentHashMap<Observer, AutowiredEventListener> _observers = new ConcurrentHashMap<Observer, AutowiredEventListener>();

  protected SIPCallImpl _bridgeJoiningPeer;

  // attribute store
  private Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();

  @Override
  public Object getAttribute(final String name) {
    if (name == null) {
      return null;
    }
    return _attributes.get(name);
  }

  @Override
  public Map<String, Object> getAttributeMap() {
    return new HashMap<String, Object>(_attributes);
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    if (value == null) {
      _attributes.remove(name);
    }
    else {
      _attributes.put(name, value);
    }
  }

  // attribute store over

  protected SIPCallImpl(final ExecutionContext context, final SipServletRequest req) {
    _context = context;
    _dispatcher.setExecutor(getThreadPool(), true);
    _id = UUID.randomUUID().toString();

    _caller = new SIPEndpointImpl(context, req.getFrom());
    _callee = new SIPEndpointImpl(context, req.getTo());

    _invite = req;

    // process Replaces header.
    final SipSessionsUtil sessionUtil = (SipSessionsUtil) _invite.getSession().getServletContext().getAttribute(
        "javax.servlet.sip.SipSessionsUtil");
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
    _context = context;
    _dispatcher.setExecutor(getThreadPool(), true);
    _id = UUID.randomUUID().toString();
    context.addCall(this);
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
        return Call.State.FAILED;
      case DISCONNECTED:
        return Call.State.DISCONNECTED;
    }
    return null;
  }

  @Override
  public synchronized SIPCall.State getSIPCallState() {
    return _cstate;
  }

  @Override
  public synchronized MediaService getMediaService(final boolean reinvite) throws IllegalStateException, MediaException {
    if (getSIPCallState() != SIPCall.State.ANSWERED && getSIPCallState() != SIPCall.State.PROGRESSED) {
      throw new IllegalStateException();
    }
    if (_network == null) {
      if (reinvite) {
        try {
          this.join().get();
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
      Direction direction = Direction.DUPLEX;
      if (_joinees.getJoinees().length > 0) {
        direction = Direction.RECV;
      }
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

        _service = _context.getMediaServiceFactory().create(this, _media, params);
        _service.getMediaGroup().join(direction, _network);
      }
      else if (reinvite) {
        _service.getMediaGroup().join(direction, _network);
      }
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
    return _service;
  }

  @Override
  public boolean isSupervised() {
    return _isSupervised;
  }

  @Override
  public void setSupervised(final boolean supervised) {
    _isSupervised = supervised;
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
    Future<T> retval = null;
    if (!(event instanceof SignalEvent)) {
      retval = this.internalDispatch(event);
    }
    else {
      final Runnable acceptor = new Runnable() {
        @Override
        public void run() {
          if (!((SignalEvent) event).isProcessed()) {
            try {
              if (event instanceof EarlyMediaEvent) {
                ((EarlyMediaEvent) event).reject(null);
              }
              else {
                ((SignalEvent) event).accept();
              }
            }
            catch (final SignalException e) {
              LOG.warn("", e);
            }
          }
        }
      };
      if (isSupervised() || event instanceof ForwardableEvent) {
        retval = this.dispatch(event, acceptor);
      }
      else {
        acceptor.run();
      }
      // retval = super.dispatch(event, acceptor);
    }
    return retval;
  }

  @Override
  public synchronized Endpoint getAddress() {
    return _address;
  }

  @Override
  public Call[] getPeers() {
    synchronized (_peers) {
      return _peers.toArray(new Call[_peers.size()]);
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
  public MediaService getMediaService() throws IllegalStateException, MediaException {
    return getMediaService(false);
  }

  @Override
  public void disconnect() {
    this.disconnect(false, CallCompleteEvent.Cause.NEAR_END_DISCONNECT, null, null);
  }

  @Override
  public void disconnect(Map<String, String> headers) {
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

  @Override
  public synchronized void unjoin(final Participant p) {
    if (!isAnswered()) {
      return;
    }
    if (!_joinees.contains(p)) {
      return;
    }
    _joinees.remove(p);
    if (p instanceof Call) {
      synchronized (_peers) {
        _peers.remove(p);
      }
    }
    if (p.getMediaObject() instanceof Joinable) {
      try {
        _network.unjoin((Joinable) p.getMediaObject());
      }
      catch (final Exception e) {
        LOG.warn("", e);
      }
    }
    p.unjoin(this);
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
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction,
      final Map<String, String> headers) {
    Participant p = null;
    try {
      p = other.call(getAddress(), headers, (EventListener<?>) null);
    }
    catch (final Exception e) {
      return new JointImpl(_context.getExecutor(), new JointImpl.DummyJoinWorker(SIPCallImpl.this, p, e));
    }
    return join(p, type, direction);
  }

  @Override
  public Joint join(final Direction direction) {
    if (isTerminated()) {
      throw new IllegalStateException("...");
    }
    return new JointImpl(_context.getExecutor(), new JoinWorker() {
      @Override
      public JoinCompleteEvent call() throws Exception {
        JoinCompleteEvent event = null;
        try {
          doJoin(direction);
          event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.JOINED);
        }
        catch (final Exception e) {
          _exception = e;
          event = handleJoinException(e);
          throw e;
        }
        finally {
          if (event.getCause() != Cause.JOINED) {
            CallCompleteEvent.Cause cause = null;

            switch (event.getCause()) {
              case ERROR:
                cause = CallCompleteEvent.Cause.ERROR;
            }
            SIPCallImpl.this.disconnect(true, cause, _exception, null);
          }
          SIPCallImpl.this.dispatch(event);

          if (isTerminated()) {
            SIPCallImpl.this
                .dispatch(new CallCompleteEvent(SIPCallImpl.this, CallCompleteEvent.Cause.ERROR, _exception));
          }
        }
        return event;
      }

      @Override
      public boolean cancel() {
        return false;
      }
    });
  }

  /**
   * this is special method used only by BridgeJoinDelegate.
   * 
   * @param direction
   * @return
   */
  protected void joinWithoutCheckOperation(final Direction direction) throws Exception {
    if (isTerminated()) {
      throw new IllegalStateException("...");
    }
    doJoinWithoutCheckOperation(direction);
  }

  @Override
  public Joint join(final Participant other, final JoinType type, final Direction direction) {
    if (isTerminated()) {
      throw new IllegalStateException("...");
    }
    return new JointImpl(_context.getExecutor(), new JoinWorker() {
      @Override
      public JoinCompleteEvent call() throws Exception {
        JoinCompleteEvent event = null;
        try {
          if (other instanceof SIPCallImpl) {
            doJoin((SIPCallImpl) other, type, direction);
          }
          else {
            doJoin(other, type, direction);
          }
          event = new JoinCompleteEvent(SIPCallImpl.this, other, Cause.JOINED);
        }
        catch (final Exception e) {
          event = handleJoinException(e);
          throw e;
        }
        finally {
          if (event.getCause() != Cause.JOINED) {
            CallCompleteEvent.Cause cause = null;
            switch (event.getCause()) {
              case BUSY:
                cause = CallCompleteEvent.Cause.BUSY;
              case REJECT:
                cause = CallCompleteEvent.Cause.DECLINE;
              case TIMEOUT:
                cause = CallCompleteEvent.Cause.TIMEOUT;
              case ERROR:
                cause = CallCompleteEvent.Cause.ERROR;
            }
            ((SIPCallImpl) other).disconnect(true, cause, _exception, null);
          }

          SIPCallImpl.this.dispatch(event);

          if (isTerminated()) {
            SIPCallImpl.this
                .dispatch(new CallCompleteEvent(SIPCallImpl.this, CallCompleteEvent.Cause.ERROR, _exception));
          }
        }
        return event;
      }

      @Override
      public boolean cancel() {
        synchronized (SIPCallImpl.this) {
          if (_joinDelegate != null) {
            _joinDelegate.done();
            return true;
          }
          return false;
        }
      }
    });
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
    try {
      req.createResponse(SipServletResponse.SC_OK).send();
    }
    catch (final Exception e) {
      LOG.warn("", e);
    }
    if (isTerminated()) {
      LOG.debug(this + " is already terminated.");
      return;
    }
    if (_joinDelegate != null) {
      _joinDelegate.setException(new DisconnectedException());
    }
    this.setSIPCallState(SIPCall.State.DISCONNECTED);
    terminate(CallCompleteEvent.Cause.DISCONNECT, null);
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
      System.out.print("test");
      if (LOG.isTraceEnabled()) {
        LOG.trace(this + " is already terminated.");
      }
      return;
    }
    if (_service != null) {
      ((GenericMediaService) _service).release();
    }
    final SIPCall.State old = getSIPCallState();
    if (failed) {
      this.setSIPCallState(SIPCall.State.FAILED);
    }
    else {
      this.setSIPCallState(SIPCall.State.DISCONNECTED);
    }
    terminate(cause, exception);
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
      catch (final IOException t) {
        LOG.warn("IOException when disconnecting call", t);
      }
    }
    else if (isAnswered(old) && !failed) {
      try {
        _signal.createRequest("BYE").send();
      }
      catch (final IOException t) {
        LOG.warn("IOException when disconnecting call", t);
      }
    }
  }

  protected synchronized void terminate(final CallCompleteEvent.Cause cause, final Exception exception) {
    _context.removeCall(getId());

    destroyNetworkConnection();
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
    if (_joinDelegate != null && _joinDelegate.getCondition() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("terminating call. Notifying joinDelegate conditaion. callID:"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
      _joinDelegate.getCondition().notifyAll();

    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("terminating call. Notifying this. callID:"
            + (getSipSession() != null ? getSipSession().getCallId() : ""));
      }
      this.notifyAll();
      this.dispatch(new CallCompleteEvent(this, cause, exception));
    }
    _callDelegate = null;
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

  protected void addPeer(final Call call, final JoinType type, final Direction direction) {
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
    if (type == JoinType.BRIDGE) {
      if (_network != null && call.getMediaObject() instanceof Joinable) {
        _network.join(direction, ((Joinable) call.getMediaObject()));
      }
    }
    this.addPeer(call, type, direction);
    call.addPeer(this, type, direction);
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

  protected void setJoinDelegate(final JoinDelegate delegate) {
    _joinDelegate = delegate;
  }

  protected JoinDelegate getJoinDelegate() {
    return _joinDelegate;
  }

  protected void setCallDelegate(final SIPCallDelegate delegate) {
    _callDelegate = delegate;
  }

  protected synchronized void doJoin(final Direction direction) throws Exception {
    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    try {
      _joinDelegate = createJoinDelegate(direction);
      _joinDelegate.setCondition(this);
      _joinDelegate.setWaiting(true);
      _joinDelegate.doJoin();
      while (!this.isTerminated() && _joinDelegate.isWaiting()) {
        try {
          if (LOG.isDebugEnabled()) {
            LOG
                .debug("Start wait joinDelegate. CallID:"
                    + (getSipSession() != null ? getSipSession().getCallId() : ""));
          }

          this.wait(1000 * 30);
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (_joinDelegate != null) {
        if (_joinDelegate.getException() != null) {
          final Exception e = _joinDelegate.getException();
          _joinDelegate.setException(null);
          throw e;
        }
        if (_joinDelegate.getError() != null) {
          final Exception e = _joinDelegate.getError();
          _joinDelegate.setError(null);
          throw e;
        }
      }
      if (!this.isAnswered()) {
        throw new IllegalStateException(this + " is no answered.");
      }
      _callDelegate = new SIPCallMediaDelegate();
    }
    finally {
      _joinDelegate = null;
      _operationInProcess = false;
    }
  }

  /**
   * this is special method used only by BridgeJoinDelegate.
   * 
   * @param direction
   * @throws Exception
   */
  protected synchronized void doJoinWithoutCheckOperation(final Direction direction) throws Exception {
    try {
      _joinDelegate = createJoinDelegate(direction);
      _joinDelegate.setCondition(this);
      _joinDelegate.setWaiting(true);
      _joinDelegate.doJoin();
      while (!this.isTerminated() && _joinDelegate.isWaiting()) {
        try {
          this.wait();
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (_joinDelegate != null) {
        if (_joinDelegate.getException() != null) {
          final Exception e = _joinDelegate.getException();
          _joinDelegate.setException(null);
          throw e;
        }
        if (_joinDelegate.getError() != null) {
          final Exception e = _joinDelegate.getError();
          _joinDelegate.setError(null);
          throw e;
        }
      }
      if (!this.isAnswered()) {
        throw new IllegalStateException(this + " is no answered.");
      }
      _callDelegate = new SIPCallMediaDelegate();
    }
    finally {
      _joinDelegate = null;
    }
  }

  protected synchronized void doJoin(final SIPCallImpl other, final JoinType type, final Direction direction)
      throws Exception {
    if (_operationInProcess) {
      throw new IllegalStateException("other operation in process.");
    }
    _operationInProcess = true;

    try {
      _joinDelegate = createJoinDelegate(other, type, direction);
      other.setJoinDelegate(_joinDelegate);
      if (_joinDelegate != null) {
        _joinDelegate.setCondition(this);
        if (type == JoinType.DIRECT) {
          _joinDelegate.setWaiting(true);
        }
        _joinDelegate.doJoin();
        if (type == JoinType.DIRECT) {
          while (!this.isTerminated() && !other.isTerminated() && _joinDelegate.isWaiting()) {
            try {
              this.wait();
            }
            catch (final InterruptedException e) {
              // ignore
            }
          }
        }
      }
      if (_joinDelegate != null) {
        if (_joinDelegate.getException() != null) {
          final Exception e = _joinDelegate.getException();
          _joinDelegate.setException(null);
          throw e;
        }
        if (_joinDelegate.getError() != null) {
          final Exception e = _joinDelegate.getError();
          _joinDelegate.setError(null);
          throw e;
        }
      }
      if (!this.isAnswered() || !other.isAnswered()) {
        throw new IllegalStateException(this + " is no answered.");
      }
      if (type == JoinType.DIRECT) {
        _callDelegate = new SIPCallDirectDelegate();
      }
      else {
        _callDelegate = new SIPCallBridgeDelegate();
      }
      other.setCallDelegate(_callDelegate);
    }
    finally {
      _joinDelegate = null;
      other.setJoinDelegate(null);
      _operationInProcess = false;
    }
  }

  protected synchronized void doJoin(final Participant other, final JoinType type, final Direction direction)
      throws Exception {
    if (!(other.getMediaObject() instanceof Joinable)) {
      throw new IllegalArgumentException("MediaObject is't joinable.");
    }
    if (isTerminated()) {
      throw new IllegalStateException("...");
    }
    if (_joinees.contains(other)) {
      return;
    }
    unlinkDirectlyPeer();
    if (_network == null) {
      this.join().get();
    }
    ((Joinable) other.getMediaObject()).join(direction, _network);
    _joinees.add(other, type, direction);
    ((ParticipantContainer) other).addParticipant(this, type, direction);
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

  protected Lock lock = new ReentrantLock();

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

  private JoinCompleteEvent handleJoinException(final Exception e) {
    JoinCompleteEvent event = null;
    if (e instanceof BusyException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.BUSY, e);
    }
    else if (e instanceof TimeoutException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.TIMEOUT, e);
    }
    else if (e instanceof RedirectException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.REDIRECT, e);
    }
    else if (e instanceof RejectException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.REJECT, e);
    }
    else if (e instanceof CanceledException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.CANCELED, e);
    }
    else if (e instanceof DisconnectedException) {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.DISCONNECTED, e);
    }
    else {
      event = new JoinCompleteEvent(SIPCallImpl.this, null, Cause.ERROR, e);
    }
    return event;
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
  public Endpoint getInvitor() {
    return _caller;
  }

  @Override
  public CallableEndpoint getInvitee() {
    return _callee;
  }

  @Override
  public synchronized void redirect(final Endpoint o, final Map<String, String> headers) throws SignalException,
      IllegalArgumentException {
    checkState();
    _redirected = true;
    setSIPCallState(SIPCall.State.REDIRECTED);
    if (o instanceof SIPEndpoint) {
      final SipServletResponse res = _invite.createResponse(SipServletResponse.SC_MOVED_TEMPORARILY);
      res.setHeader("Contact", ((SIPEndpoint) o).getURI().toString());
      SIPHelper.addHeaders(res, headers);
      try {
        res.send();
      }
      catch (final IOException e) {
        throw new SignalException(e);
      }
    }
    else {
      throw new IllegalArgumentException("Unable to redirect the call to a non-SIP participant.");
    }
  }

  @Override
  public synchronized void reject(final Reason reason, final Map<String, String> headers) throws SignalException {
    checkState();
    _rejected = true;
    setSIPCallState(SIPCall.State.REJECTED);
    try {
      final SipServletResponse res = _invite.createResponse(reason == null ? Reason.DECLINE.getCode() : reason
          .getCode());
      SIPHelper.addHeaders(res, headers);
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    acceptCall(headers);
  }

  @Override
  public synchronized Call acceptCall(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, IllegalStateException {
    checkState();
    _accepted = true;
    ((SIPIncomingCall) this).addListeners(listeners);
    try {
      ((SIPIncomingCall) this).doInvite(headers);
      return this;
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized Call acceptCall(final Map<String, String> headers, final Observer... observers)
      throws SignalException, IllegalStateException {
    checkState();
    _accepted = true;
    ((SIPIncomingCall) this).addObservers(observers);
    try {
      ((SIPIncomingCall) this).doInvite(headers);
      return this;
    }
    catch (final Exception e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized Call acceptCallWithEarlyMedia(final Map<String, String> headers,
      final EventListener<?>... listeners) throws SignalException, MediaException, IllegalStateException {
    if (isProcessed()) {
      throw new IllegalStateException("...");
    }
    _accepted = true;
    _acceptedWithEarlyMedia = true;
    ((SIPIncomingCall) this).addListeners(listeners);
    try {
      ((SIPIncomingCall) this).doInviteWithEarlyMedia(headers);
      return this;
    }
    catch (final Exception e) {
      if (e instanceof SignalException) {
        throw (SignalException) e;
      }
      else if (e instanceof MediaException) {
        throw (MediaException) e;
      }
      else {
        throw new SignalException(e);
      }
    }
  }

  @Override
  public synchronized Call acceptCallWithEarlyMedia(final Map<String, String> headers, final Observer... observers)
      throws SignalException, MediaException, IllegalStateException {
    if (isProcessed()) {
      throw new IllegalStateException("...");
    }
    _accepted = true;
    _acceptedWithEarlyMedia = true;
    ((SIPIncomingCall) this).addObservers(observers);
    try {
      ((SIPIncomingCall) this).doInviteWithEarlyMedia(headers);
      return this;
    }
    catch (final Exception e) {
      if (e instanceof SignalException) {
        throw (SignalException) e;
      }
      else if (e instanceof MediaException) {
        throw (MediaException) e;
      }
      else {
        throw new SignalException(e);
      }
    }
  }

  @Override
  public Call answer(final Map<String, String> headers, final EventListener<?>... listeners) throws SignalException,
      IllegalStateException {
    final Call call = acceptCall(headers, listeners);

    final Joint joint = call.join();
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (final InterruptedException e) {
        // ignore
      }
      catch (final ExecutionException e) {
        throw new SignalException(e.getCause());
      }
    }

    return call;
  }

  @Override
  public Call answer(final Map<String, String> headers, final Observer... observer) throws SignalException,
      IllegalStateException {
    final Call call = acceptCall(headers, observer);

    final Joint joint = call.join();
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (final InterruptedException e) {
        // ignore
      }
      catch (final ExecutionException e) {
        throw new SignalException(e.getCause());
      }
    }

    return call;
  }

  // for invite event over==========

  // for dispatchable eventsource=========
  @Override
  public void addListener(final EventListener<?> listener) {
    if (listener != null) {
      _dispatcher.addListener(Event.class, listener);
    }
  }

  @Override
  public void addListeners(final EventListener<?>... listeners) {
    if (listeners != null) {
      for (final EventListener<?> listener : listeners) {
        this.addListener(listener);
      }
    }
  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListener(final Class<E> type, final T listener) {
    if (listener != null) {
      _dispatcher.addListener(type, listener);
    }
  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListeners(final Class<E> type, final T... listeners) {
    if (listeners != null) {
      for (final T listener : listeners) {
        this.addListener(type, listener);
      }
    }
  }

  @Override
  public void addObserver(final Observer observer) {
    if (observer != null) {
      final AutowiredEventListener autowire = new AutowiredEventListener(observer);
      if (_observers.putIfAbsent(observer, autowire) == null) {
        _dispatcher.addListener(Event.class, autowire);
      }
    }
  }

  @Override
  public void addObservers(final Observer... observers) {
    if (observers != null) {
      for (final Observer o : observers) {
        addObserver(o);
      }
    }
  }

  @Override
  public void removeListener(final EventListener<?> listener) {
    _dispatcher.removeListener(listener);
  }

  @Override
  public void removeObserver(final Observer listener) {
    final AutowiredEventListener autowiredEventListener = _observers.remove(listener);
    if (autowiredEventListener != null) {
      _dispatcher.removeListener(autowiredEventListener);
    }
  }

  public <S extends EventSource, T extends Event<S>> Future<T> internalDispatch(final T event) {
    return _dispatcher.fire(event, true, null);
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
    return _dispatcher.fire(event, true, afterExec);
  }

  @Override
  public String getId() {
    return _id;
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return _context;
  }

  @Override
  public String getApplicationState() {
    return _states.get(AutowiredEventTarget.DEFAULT_FSM);
  }

  @Override
  public void setApplicationState(final String state) {
    _states.put(AutowiredEventTarget.DEFAULT_FSM, state);
  }

  public String getApplicationState(final String FSM) {
    return _states.get(FSM);
  }

  @Override
  public void setApplicationState(final String FSM, final String state) {
    _states.put(FSM, state);
  }

  protected Executor getThreadPool() {
    return _context.getExecutor();
  }

  @Override
  public void addExceptionHandler(final ExceptionHandler... handlers) {
    _dispatcher.addExceptionHandler(handlers);
  }

  // for dispatchable eventsource over=========

  public SIPCallImpl getBridgeJoiningPeer() {
    return _bridgeJoiningPeer;
  }

  public void setBridgeJoiningPeer(final SIPCallImpl bridgeJoiningPeer) {
    _bridgeJoiningPeer = bridgeJoiningPeer;
  }

}
