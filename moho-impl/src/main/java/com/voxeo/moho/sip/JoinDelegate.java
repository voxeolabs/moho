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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.BusyException;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.MixerImpl.ClampDtmfMixerAdapter;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.RedirectException;
import com.voxeo.moho.RejectException;
import com.voxeo.moho.SettableJointImpl;
import com.voxeo.moho.TimeoutException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.util.InheritLogContextRunnable;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.JoinLockService;

public abstract class JoinDelegate {

  private static final Logger LOG = Logger.getLogger(JoinDelegate.class);

  protected SettableJointImpl _settableJoint;

  protected SIPCallImpl _call1;

  protected SIPCallImpl _call2;

  protected JoinType _joinType;

  protected Direction _direction;

  protected boolean done;

  protected Cause _cause;

  protected Exception _exception;

  protected SIPCallImpl _peer;

  protected boolean dtmfPassThrough;

  public void setSettableJoint(SettableJointImpl settableJoint) {
    _settableJoint = settableJoint;
  }

  public void setDtmfPassThrough(boolean dtmfPassThrough) {
    this.dtmfPassThrough = dtmfPassThrough;
  }

  public SettableJointImpl getSettableJoint() {
    return _settableJoint;
  }

  public synchronized void done(final Cause cause, Exception exception) {
    if (exception != null) {
      LOG.error("Join complete in error cause:" + cause + " for joinDelegate" + this, exception);
    }
    else {
      LOG.debug("Join complete with cause:" + cause + " for joinDelegate" + this);
    }

    if (done) {
      return;
    }

    _cause = cause;
    _exception = exception;

    _call1.joinDone(_call2, this);

    // for remote join
    Participant p1 = _call1;
    if (_call1 instanceof RemoteParticipant) {
      p1 = _call1.getApplicationContext().getParticipant(((RemoteParticipant) _call1).getRemoteParticipantID());
    }
    // for remote join
    Participant p2 = _call2;
    if (_call2 != null && _call2 instanceof RemoteParticipant) {
      p2 = _call2.getApplicationContext().getParticipant(((RemoteParticipant) _call2).getRemoteParticipantID());
    }

    JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(p1, p2, cause, exception, _call2 == null ? true
        : _peer == null ? true : _peer.equals(_call2));
    _call1.dispatch(joinCompleteEvent);

    if (_call2 != null) {
      _call2.joinDone(_call1, this);
      JoinCompleteEvent peerJoinCompleteEvent = new MohoJoinCompleteEvent(p2, p1, cause, exception,
          _peer == null ? false : !_peer.equals(_call2));
      _call2.dispatch(peerJoinCompleteEvent);
    }

    _settableJoint.done(joinCompleteEvent);
    done = true;

    ((ApplicationContextImpl) _call1.getApplicationContext()).getExecutor().execute(new InheritLogContextRunnable() {
      @Override
      public void run() {
        _call1.continueQueuedJoin();
      }
    });

    if (_call2 != null) {
      ((ApplicationContextImpl) _call1.getApplicationContext()).getExecutor().execute(new InheritLogContextRunnable() {
        @Override
        public void run() {
          _call2.continueQueuedJoin();
        }
      });
    }
  }

  public JoinType getJoinType() {
    return _joinType;
  }

  public SIPCallImpl getInitiator() {
    return _peer == _call2 ? _call1 : _call2;
  }

  public SIPCallImpl getPeer() {
    return _peer;
  }

  public void doJoin() throws Exception {
  }

  protected void doInviteResponse(final SipServletResponse res, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doUpdate(final SipServletRequest req, final SIPCallImpl call, final Map<String, String> headers)
      throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doUpdateResponse(final SipServletResponse resp, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doPrack(final SipServletRequest req, final SIPCallImpl call, final Map<String, String> headers)
      throws Exception {
    req.createResponse(200).send();
  }

  protected void doPrackResponse(final SipServletResponse resp, final SIPCallImpl call,
      final Map<String, String> headers) throws Exception {
    // do nothing
  }

  protected void doAck(final SipServletRequest req, final SIPCallImpl call) throws Exception {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doSdpEvent(final SdpPortManagerEvent event) {
    throw new UnsupportedOperationException("" + this);
  }

  protected void doDisengage(final SIPCallImpl call, final JoinType type) {
    if (call.isDirectlyJoined()) {
      call.unlinkDirectlyPeer();
    }
    else if (call.isBridgeJoined() && type == JoinType.DIRECT) {
      for (final Participant p : call.getParticipants()) {
        call.unjoin(p);
      }
      call.destroyNetworkConnection(false);
    }
  }

  protected Exception getExceptionByResponse(SipServletResponse res) {
    if (res == null) {
      return null;
    }
    Exception e = null;
    if (SIPHelper.isBusy(res)) {
      e = new BusyException();
    }
    else if (SIPHelper.isRedirect(res)) {
      e = new RedirectException(res.getHeaders("Contact"));
    }
    else if (SIPHelper.isTimeout(res)) {
      e = new TimeoutException();
    }
    else if (SIPHelper.isDecline(res)) {
      e = new RejectException();
    }
    else {
      e = new RejectException();
    }

    return e;
  }

  protected CallCompleteEvent.Cause getCallCompleteCauseByResponse(SipServletResponse res) {
    CallCompleteEvent.Cause cause = null;
    if (SIPHelper.isBusy(res)) {
      cause = CallCompleteEvent.Cause.BUSY;
    }
    else if (SIPHelper.isRedirect(res)) {
      cause = CallCompleteEvent.Cause.REDIRECT;
    }
    else if (SIPHelper.isTimeout(res)) {
      cause = CallCompleteEvent.Cause.TIMEOUT;
    }
    else if (SIPHelper.isDecline(res)) {
      cause = CallCompleteEvent.Cause.DECLINE;
    }
    else {
      cause = CallCompleteEvent.Cause.ERROR;
    }

    return cause;
  }

  protected JoinCompleteEvent.Cause getJoinCompleteCauseByResponse(SipServletResponse res) {
    JoinCompleteEvent.Cause cause = null;
    if (SIPHelper.isBusy(res)) {
      cause = JoinCompleteEvent.Cause.BUSY;
    }
    else if (SIPHelper.isRedirect(res)) {
      cause = JoinCompleteEvent.Cause.REDIRECT;
    }
    else if (SIPHelper.isTimeout(res)) {
      cause = JoinCompleteEvent.Cause.TIMEOUT;
    }
    else if (SIPHelper.isDecline(res)) {
      cause = JoinCompleteEvent.Cause.REJECT;
    }
    else if (SIPHelper.isNotAcceptableHere(res)) {
      cause = JoinCompleteEvent.Cause.NOT_ACCEPTABLE;
    }
    else {
      cause = JoinCompleteEvent.Cause.ERROR;
    }
    return cause;
  }

  public Cause getCause() {
    return _cause;
  }

  public Exception getException() {
    return _exception;
  }

  public static ExecutionException checkJoinStrategy(final Participant part, final JoinType type, final boolean force)
      throws Exception {
    final Participant[] parts = part.getParticipants();

    if (parts.length > 0) {
      if (type == JoinType.DIRECT || type == JoinType.BRIDGE_EXCLUSIVE) {
        if (force) {
          // unjoin previous joined Participant
          Unjoint unjoint = null;
          if (parts.length > 0) {
            for (Participant participant : parts) {
              unjoint = part.unjoin(participant);
            }
            
            // wait until the unjoin done
            while (part.getParticipants().length > 0) {
              part.wait(2000);
            }
          }
        }
        else {
          // "AlreadyJoined" error
          return new ExecutionException(JoinDelegate.buildAlreadyJoinedExceptionMessage(part), null);
        }
      }
      else { // BRIDGE_SHARED
        if (!force) {
          JoinType checkJoinType = null;
          if (parts.length > 0) {
            for (final Participant participant : parts) {
              checkJoinType = getJoinType(part, participant);
              if (checkJoinType == JoinType.DIRECT || checkJoinType == JoinType.BRIDGE_EXCLUSIVE) {
                // "AlreadyJoined" error
                return new ExecutionException(JoinDelegate.buildAlreadyJoinedExceptionMessage(part), null);
              }
            }
          }
        }
      }
    }

    return null;
  }

  public static ExecutionException checkJoinStrategy(final Participant part, final Participant other,
      final JoinType type, final boolean force) throws Exception {
    final Participant[] parts = part.getParticipants();
    final Participant[] otherParts = other.getParticipants();

    if (parts.length > 0 || otherParts.length > 0) {
      if (type == JoinType.DIRECT || type == JoinType.BRIDGE_EXCLUSIVE) {
        if (force) {
          // unjoin previous joined Participant
          Unjoint unjoint = null;
          if (parts.length > 0) {
            for (Participant participant : parts) {
              unjoint = part.unjoin(participant);
            }
            
            // wait until the unjoin done
            while (part.getParticipants().length > 0) {
              part.wait(2000);
            }
          }
          if (otherParts.length > 0) {
            for (Participant participant : otherParts) {
              unjoint = other.unjoin(participant);
            }
            
            // wait until the unjoin done
            while (other.getParticipants().length > 0) {
              synchronized(other) {
                other.wait(2000);
              }
            }
          }
        }
        else {
          // "AlreadyJoined" error
          return new ExecutionException(parts.length > 0 ? JoinDelegate.buildAlreadyJoinedExceptionMessage(part)
              : JoinDelegate.buildAlreadyJoinedExceptionMessage(other), null);
        }
      }
      else { // BRIDGE_SHARED
        if (!force) {
          JoinType checkJoinType = null;
          if (parts.length > 0) {
            for (final Participant participant : parts) {
              checkJoinType = getJoinType(part, participant);
              if (checkJoinType == JoinType.DIRECT || checkJoinType == JoinType.BRIDGE_EXCLUSIVE) {
                // "AlreadyJoined" error
                return new ExecutionException(JoinDelegate.buildAlreadyJoinedExceptionMessage(part), null);
              }
            }
          }
          if (otherParts.length > 0) {
            for (final Participant participant : otherParts) {
              checkJoinType = getJoinType(other, participant);
              if (checkJoinType == JoinType.DIRECT || checkJoinType == JoinType.BRIDGE_EXCLUSIVE) {
                // "AlreadyJoined" error
                return new ExecutionException(JoinDelegate.buildAlreadyJoinedExceptionMessage(other), null);
              }
            }
          }
        }
      }
    }

    return null;
  }

  protected static JoinType getJoinType(final Participant part, final Participant other) {
    if (part instanceof SIPCallImpl) {
      return ((SIPCallImpl) part).getJoinType(other);
    }
    if (part instanceof MixerImpl) {
      return ((MixerImpl) part).getJoinType(other);
    }
    return null;
  }

  protected static Direction getJoinDirection(final Participant part, final Participant other) {
    if (part instanceof SIPCallImpl) {
      return ((SIPCallImpl) part).getDirection(other);
    }
    if (part instanceof MixerImpl) {
      return ((MixerImpl) part).getDirection(other);
    }
    return null;
  }

  public static void bridgeJoin(final Participant part, final Participant other, final Direction direction)
      throws MsControlException {
    LOG.info(part + " joins to " + other + " in " + direction);

    final Lock lock = JoinLockService.getInstance().get(part, other);
    lock.lock();
    try {
      // check if part and other has been joined
      final Direction oldDirection = getJoinDirection(part, other);
      if (oldDirection != null) {
        if (oldDirection != direction) {
          bridgeUnjoin(part, other);
        }
        else {
          LOG.debug(part + "already joined to " + other + " in " + direction + ",ignore the operation");
          return;
        }
      }

      final Joinable joinable = getJoinable(part);
      final Joinable otherJoinable = getJoinable(other);

      MediaMixer multipleJoiningMixer = null;
      MediaMixer otherMultipleJoiningMixer = null;

      final Participant[] parts = part.getParticipants(Direction.RECV);
      final Participant[] otherParts = other.getParticipants(Direction.RECV);

      final Joinable[] joinees = joinable.getJoinees(Direction.RECV);
      final Joinable[] otherJoinees = otherJoinable.getJoinees(Direction.RECV);

      if (joinees.length == 0 && otherJoinees.length == 0) {
        joinable.join(direction, otherJoinable);
      }
      else {
        if (joinees.length > 0) {
          multipleJoiningMixer = getMultipleJoiningMixer(part, true);
        }
        if (otherJoinees.length > 0) {
          otherMultipleJoiningMixer = getMultipleJoiningMixer(other, true);
        }

        MediaGroup medGro = getMediaService(part);
        MediaGroup otherMedGro = getMediaService(other);

        if (joinees.length > 0) {
          if (medGro == null) {
            sharedJoin(part, multipleJoiningMixer, parts[0], other, otherMultipleJoiningMixer, direction);
          }
          else {
            sharedJoin(part, multipleJoiningMixer, medGro, other, otherMultipleJoiningMixer, direction);
          }
        }
        if (otherJoinees.length > 0) {
          if (otherMedGro == null) {
            sharedJoin(other, otherMultipleJoiningMixer, otherParts[0], part, multipleJoiningMixer, reserve(direction));
          }
          else {
            sharedJoin(other, otherMultipleJoiningMixer, otherMedGro, part, multipleJoiningMixer, reserve(direction));
          }
        }
      }
    }
    finally {
      lock.unlock();
      JoinLockService.getInstance().remove(part.getId());
      JoinLockService.getInstance().remove(other.getId());
    }
  }

  public static void bridgeJoin(final Participant part, final MediaGroup medGro) throws MsControlException {
    LOG.info(part + " joins to " + medGro + " in DUPLEX");

    final Lock lock = JoinLockService.getInstance().get(part);
    lock.lock();
    try {
      final Participant[] parts = part.getParticipants(Direction.RECV);
      if (parts.length == 0) {
        getJoinable(part).join(Direction.DUPLEX, medGro);
      }
      else {
        MediaMixer multipleJoiningMixer = getMultipleJoiningMixer(part, true);
        sharedJoin(part, multipleJoiningMixer, parts[0], medGro);
      }
    }
    finally {
      lock.unlock();
      JoinLockService.getInstance().remove(part.getId());
    }
  }

  public static void bridgeUnjoin(final Participant part, final Participant other) throws MsControlException {
    LOG.info(part + " unjoins from " + other);

    final Lock lock = JoinLockService.getInstance().get(part, other);
    lock.lock();
    try {
      final Joinable joinable = getJoinable(part);
      final Joinable otherJoinable = getJoinable(other);

      if (contains(joinable, otherJoinable, null)) {
        joinable.unjoin(otherJoinable);
      }

      final MediaMixer multipleJoiningMixer = getMultipleJoiningMixer(part, false);
      final MediaMixer otherMultipleJoiningMixer = getMultipleJoiningMixer(other, false);

      if (multipleJoiningMixer != null && contains(multipleJoiningMixer, otherJoinable, null)) {
        multipleJoiningMixer.unjoin(otherJoinable);
        if (multipleJoiningMixer.getJoinees().length == 1) {
          destroyMultipleJoiningMixer(part);
        }
      }
      if (otherMultipleJoiningMixer != null && contains(otherMultipleJoiningMixer, joinable, null)) {
        otherMultipleJoiningMixer.unjoin(joinable);
        if (otherMultipleJoiningMixer.getJoinees().length == 1) {
          destroyMultipleJoiningMixer(other);
        }
      }
    }
    finally {
      lock.unlock();
      JoinLockService.getInstance().remove(part.getId());
      JoinLockService.getInstance().remove(other.getId());
    }
  }

  public static String buildAlreadyJoinedExceptionMessage(final Participant part) {
    final StringBuffer sbuf = new StringBuffer();
    sbuf.append(part + " is already joined.");
    return sbuf.toString();
  }

  /**
   * It's used in the following join scenarion,
   * 
   * <pre>
   * part.join(peer);
   * part.join(other);
   * </pre>
   * 
   * @param part
   *          A participant has previous join in RECV direction.
   * @param multipleJoiningMixer
   *          The MediaMixer of "part" used for SHARED join.
   * @param peer
   *          The Participant connected in RECV direction.
   * @param other
   *          The new Participant to connect.
   * @param otherMultipleJoiningMixer
   *          The MediaMixer of "other" used for SHARED join.
   * @param direction
   *          It indicates direction (DUPLEX, SEND, RECV).
   * @throws MsControlException
   */
  protected static void sharedJoin(final Participant part, final MediaMixer multipleJoiningMixer,
      final Participant peer, final Participant other, final MediaMixer otherMultipleJoiningMixer,
      final Direction direction) throws MsControlException {
    final Joinable joinable = getJoinable(part);
    final Joinable otherJoinable = getJoinable(other);
    if (part instanceof Mixer) {
      //
      // we don't need to rejoin its peer Participant in RECV direction,
      // because Mixer supports "listen to more than one".
      //
      _sharedJoin(joinable, multipleJoiningMixer, null, null, null, otherJoinable, otherMultipleJoiningMixer, direction);
    }
    else {
      final Joinable peerJoinable = getJoinable(peer);
      final MediaMixer peerMultipleJoiningMixer = getMultipleJoiningMixer(peer, false);
      final Direction peerDirection = part.getDirection(peer);
      _sharedJoin(joinable, multipleJoiningMixer, peerJoinable, peerDirection, peerMultipleJoiningMixer, otherJoinable,
          otherMultipleJoiningMixer, direction);
    }
  }

  /**
   * It's used in the following join scenarion,
   * 
   * <pre>
   * part.getMediaService();
   * part.join(other);
   * </pre>
   * 
   * @param part
   *          A participant has previous join in RECV direction.
   * @param multipleJoiningMixer
   *          The MediaMixer of "part" used for SHARED join.
   * @param medGro
   *          The media service of "part".
   * @param other
   *          The new Participant to connect.
   * @param otherMultipleJoiningMixer
   *          The MediaMixer of "other" used for SHARED join.
   * @param direction
   *          It indicates direction (DUPLEX, SEND, RECV).
   * @throws MsControlException
   */
  protected static void sharedJoin(final Participant part, final MediaMixer multipleJoiningMixer,
      final MediaGroup medGro, final Participant other, final MediaMixer otherMultipleJoiningMixer,
      final Direction direction) throws MsControlException {
    final Joinable joinable = getJoinable(part);
    final Joinable otherJoinable = getJoinable(other);
    if (part instanceof Mixer) {
      //
      // we don't need to rejoin its MediaService(MediaGroup),
      // because Mixer supports "listen to more than one".
      //
      _sharedJoin(joinable, multipleJoiningMixer, null, null, null, otherJoinable, otherMultipleJoiningMixer, direction);
    }
    else {
      _sharedJoin(joinable, multipleJoiningMixer, medGro, Direction.DUPLEX, null, otherJoinable,
          otherMultipleJoiningMixer, direction);
    }
  }

  /**
   * It's used in the following join scenarion,
   * 
   * <pre>
   * part.join(peer);
   * part.getMediaService();
   * </pre>
   * 
   * @param part
   *          A participant has previous join in RECV direction.
   * @param multipleJoiningMixer
   *          The MediaMixer of "part" used for SHARED join.
   * @param peer
   *          The Participant connected in RECV direction.
   * @param medGro
   *          The media service of "part".
   * @throws MsControlException
   */
  protected static void sharedJoin(final Participant part, final MediaMixer multipleJoiningMixer,
      final Participant peer, final MediaGroup medGro) throws MsControlException {
    final Joinable joinable = getJoinable(part);
    if (part instanceof Mixer) {
      //
      // we don't need to rejoin its peer Participant,
      // because Mixer supports "listen to more than one".
      //
      _sharedJoin(joinable, multipleJoiningMixer, null, null, null, medGro, null, Direction.DUPLEX);
    }
    else {
      final Joinable peerJoinable = getJoinable(peer);
      final Direction peerDirection = part.getDirection(peer);
      final MediaMixer peerMultipleJoiningMixer = getMultipleJoiningMixer(peer, false);
      _sharedJoin(joinable, multipleJoiningMixer, peerJoinable, peerDirection, peerMultipleJoiningMixer, medGro, null,
          Direction.DUPLEX);
    }
  }

  private static void _sharedJoin(final Joinable joinable, final MediaMixer multipleJoiningMixer,
      final Joinable peerJoinable, final Direction peerDirection, final MediaMixer peerMultipleJoiningMixer,
      final Joinable otherJoinable, final MediaMixer otherMultipleJoiningMixer, final Direction direction)
      throws MsControlException {
    // first rejoin to peer
    if (peerJoinable != null && !peerJoinable.equals(multipleJoiningMixer)) {
      if (contains(joinable, peerJoinable, null)) {
        joinable.unjoin(peerJoinable);
      }
      joinable.join(Direction.RECV, multipleJoiningMixer);
      if (isRecv(peerDirection)) {
        peerJoinable.join(Direction.SEND, multipleJoiningMixer);
      }
      if (isSend(peerDirection)) {
        if (peerMultipleJoiningMixer == null) {
          joinable.join(Direction.SEND, peerJoinable);
        }
        else {
          joinable.join(Direction.SEND, peerMultipleJoiningMixer);
        }
      }
    }

    // then join to other
    if (joinable instanceof MediaMixer) {
      // 1, mx.join(mg);
      // 2, mx1.join(mx2);
      // 3, mx.join(nc); nc has no previous join
      if (otherJoinable instanceof MediaGroup || otherJoinable instanceof MediaMixer
          || otherMultipleJoiningMixer == null) {
        joinable.join(direction, otherJoinable);
      }
      else {
        // 4, mx.join(nc); nc has previous join
        if (isRecv(direction)) {
          otherJoinable.join(Direction.SEND, multipleJoiningMixer);
        }
        if (isSend(direction)) {
          joinable.join(Direction.SEND, otherMultipleJoiningMixer);
        }
      }
    }
    else {
      // 5, other cases
      if (isRecv(direction)) {
        otherJoinable.join(Direction.SEND, multipleJoiningMixer);
      }
      if (isSend(direction)) {
        if (otherMultipleJoiningMixer == null) {
          joinable.join(Direction.SEND, otherJoinable);
        }
        else {
          joinable.join(Direction.SEND, otherMultipleJoiningMixer);
        }
      }
    }
  }

  public static Joinable getJoinable(final Participant part) {
    if (part.getMediaObject() instanceof Joinable) {
      return (Joinable) part.getMediaObject();
    }
    else {
      // invalid MediaObject type
      return null;
    }
  }

  public static MediaGroup getMediaService(final Participant part) throws MsControlException {
    Joinable joinable = getJoinable(part);
    for (Joinable joinee : joinable.getJoinees()) {
      if (joinee instanceof MediaGroup) {
        return (MediaGroup) joinee;
      }
    }
    return null;
  }

  protected static MediaMixer getMultipleJoiningMixer(final Participant part, final boolean createIfNotExisted)
      throws MsControlException {
    if (part instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) part;
      if (createIfNotExisted && call.getMultipleJoiningMixer() == null) {
        call.createMultipleJoiningMixer();
      }
      return call.getMultipleJoiningMixer();
    }
    else if (part instanceof MixerImpl) {
      // MediaMixer supports to listen to more than one resource, so just use
      // MixerImpl's own MediaMixer
      final MixerImpl mixer = (MixerImpl) part;
      return (MediaMixer) mixer.getMediaObject();
    }
    else if (part instanceof ClampDtmfMixerAdapter) {
      final ClampDtmfMixerAdapter adapter = (ClampDtmfMixerAdapter) part;
      return (MediaMixer) adapter.getMixer().getMediaObject();
    }
    return null;
  }

  protected static void destroyMultipleJoiningMixer(final Participant part) {
    try {
      if (part instanceof SIPCallImpl) {
        final SIPCallImpl call = (SIPCallImpl) part;
        call.destroyMultipleJoiningMixer();
      }
    }
    catch (Throwable t) {
      LOG.warn("Exception when destroying multipleJoiningMixer", t);
    }
  }

  public static boolean contains(final Joinable joinable, final Joinable joinee, final Direction direction)
      throws MsControlException {
    if (joinee == null) {
      return false;
    }
    final Joinable[] joinees = direction == null ? joinable.getJoinees() : joinable.getJoinees(direction);
    for (Joinable j : joinees) {
      if (j.equals(joinee)) {
        return true;
      }
    }
    return false;
  }

  public static Direction reserve(Direction direction) {
    if (direction == Direction.RECV) {
      return Direction.SEND;
    }
    else if (direction == Direction.SEND) {
      return Direction.RECV;
    }
    return Direction.DUPLEX;
  }

  public static boolean isRecv(Direction direction) {
    if (direction == Direction.RECV || direction == Direction.DUPLEX) {
      return true;
    }
    return false;
  }

  public static boolean isSend(Direction direction) {
    if (direction == Direction.SEND || direction == Direction.DUPLEX) {
      return true;
    }
    return false;
  }

  // used for remote join
  public void remoteJoinAnswer(byte[] sdp) throws Exception {

  }

  protected void disconnectCalls(final List<SIPCallImpl> calls) {
    ((ExecutionContext) _call1.getApplicationContext()).getExecutor().execute(new InheritLogContextRunnable() {
      @Override
      public void run() {
        for (SIPCallImpl call : calls) {
          try {
            call._joinDelegate = null;
            call.disconnect(false, CallCompleteEvent.Cause.CANCEL, null, null);
          }
          catch (Exception ex) {
            LOG.debug("Exception when disconnecting call " + call);
          }
        }

        calls.clear();
      }

    });
  }

  protected void disconnectCall(final SIPCallImpl call, final boolean fail, final CallCompleteEvent.Cause cause,
      final Exception ex) {
    ((ExecutionContext) _call1.getApplicationContext()).getExecutor().execute(new InheritLogContextRunnable() {
      @Override
      public void run() {
        try {
          call._joinDelegate = null;
          call.disconnect(fail, cause == null ? (ex != null ? CallCompleteEvent.Cause.ERROR
              : CallCompleteEvent.Cause.NEAR_END_DISCONNECT) : cause, ex, null);
        }
        catch (Exception ex) {
          LOG.debug("Exception when disconnecting call " + call);
        }
      }
    });
  }

  protected void failCall(final SIPCallImpl call, final Exception ex) {
    disconnectCall(call, true, null, ex);
  }
}
