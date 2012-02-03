package com.voxeo.moho.sip;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class DirectRemoteLocalJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectRemoteLocalJoinDelegate.class);

  protected RemoteParticipantImpl _remoteParticipant;

  protected SIPCallImpl _localParticipant;

  protected byte[] _sdpOffer;

  protected boolean notifyRemote;

  public DirectRemoteLocalJoinDelegate(final RemoteParticipantImpl remoteParticipant,
      final SIPCallImpl localParticipant, final Direction direction, byte[] offer) {
    _localParticipant = localParticipant;
    _remoteParticipant = (RemoteParticipantImpl) remoteParticipant;
    _direction = direction;
    _joinType = JoinType.DIRECT;
    _sdpOffer = offer;
  }

  @Override
  public void doJoin() throws Exception {
    _remoteParticipant.startJoin(_localParticipant, this);
    ((ParticipantContainer) _localParticipant).startJoin(_remoteParticipant, this);

    final byte[] answerSDP = _localParticipant.processSDPOffer(_sdpOffer);

    if (answerSDP != null) {
      // avoid dead lock.
      Runnable run = new Runnable() {
        @Override
        public void run() {
          try {
            ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication().joinAnswer(
                _remoteParticipant.getId(), _localParticipant.getRemoteAddress(), answerSDP);
          }
          catch (final Exception e) {
            LOG.error("", e);
            notifyRemote = true;
            done(Cause.ERROR, e);
          }
        }
      };

      ((ApplicationContextImpl) _localParticipant.getApplicationContext()).getExecutor().execute(run);
    }
  }

  @Override
  protected void doInviteResponse(final SipServletResponse res, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (SIPHelper.isErrorResponse(res)) {
      notifyRemote = true;
      done(getJoinCompleteCauseByResponse(res), getExceptionByResponse(res));
    }
    else {
      if (SIPHelper.isSuccessResponse(res)) {
        ((ApplicationContextImpl) _localParticipant.getApplicationContext()).getExecutor().execute(new Runnable() {
          @Override
          public void run() {
            try {
              ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication()
                  .joinAnswer(_remoteParticipant.getId(), _localParticipant.getId(),
                      SIPHelper.getRawContentWOException(res));
            }
            catch (final Exception e) {
              LOG.error("", e);
              notifyRemote = true;
              done(Cause.ERROR, e);
            }
          }
        });
      }
    }
  }

  @Override
  protected void doAck(SipServletRequest req, SIPCallImpl call) throws Exception {
    // TODO only for not answered incoming call, do nothing now.
  }

  @Override
  public synchronized void done(Cause cause, Exception exception) {
    if (done) {
      return;
    }

    _cause = cause;
    _exception = exception;

    ((ParticipantContainer) _localParticipant).joinDone(_remoteParticipant, this);

    JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(_localParticipant, _remoteParticipant, cause,
        exception, false);
    if (cause == Cause.JOINED) {
      ((ParticipantContainer) _localParticipant).addParticipant(_remoteParticipant, _joinType, _direction, null);
    }
    _remoteParticipant.joinDone(notifyRemote);

    if (_settableJoint != null) {
      _settableJoint.done(joinCompleteEvent);
    }
    _localParticipant.dispatch(joinCompleteEvent);
    done = true;
  }
}
