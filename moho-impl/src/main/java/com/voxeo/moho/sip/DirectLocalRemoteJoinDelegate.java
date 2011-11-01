package com.voxeo.moho.sip;

import java.io.IOException;
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
import com.voxeo.moho.remotejoin.RemoteParticipant;

public class DirectLocalRemoteJoinDelegate extends JoinDelegate {

  private static final Logger LOG = Logger.getLogger(DirectLocalRemoteJoinDelegate.class);

  protected SIPCallImpl _localParticipant;

  protected RemoteParticipantImpl _remoteParticipant;

  protected boolean notifyRemote;

  public DirectLocalRemoteJoinDelegate(final SIPCallImpl local, final RemoteParticipant remoteParticipant,
      final Direction direction) {
    _localParticipant = local;
    _remoteParticipant = (RemoteParticipantImpl) remoteParticipant;
    _direction = direction;
    _joinType = JoinType.DIRECT;
  }

  @Override
  public void doJoin() throws Exception {
    _remoteParticipant.startJoin(_localParticipant, this);
    ((ParticipantContainer) _localParticipant).startJoin(_remoteParticipant, this);
    
    byte[] sdp = _localParticipant.getJoinSDP();

    if (sdp != null) {
      try {
        ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication().join(
            _localParticipant.getId(), _remoteParticipant.getId(), _joinType, sdp);
      }
      catch (final Exception e) {
        LOG.error("", e);
        done(Cause.ERROR, e);
      }
    }
  }

  @Override
  protected void doInviteResponse(SipServletResponse res, SIPCallImpl call, Map<String, String> headers)
      throws Exception {
    if (SIPHelper.isErrorResponse(res)) {
      done(getJoinCompleteCauseByResponse(res), getExceptionByResponse(res));
    }
    else {
      if (SIPHelper.isSuccessResponse(res)) {
        try {
          ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication()
              .join(_localParticipant.getId(), _remoteParticipant.getId(), _joinType,
                  SIPHelper.getRawContentWOException(res));
        }
        catch (final Exception e) {
          LOG.error("", e);
          done(Cause.ERROR, e);
        }
      }
    }
  }

  @Override
  protected void doAck(SipServletRequest req, SIPCallImpl call) throws Exception {
    // TODO only for not answered incoming call, do nothing now.
  }

  public void remoteJoinAnswer(byte[] sdp) throws IOException {
    try {
      _localParticipant.processSDPAnswer(sdp);
    }
    catch (IOException ex) {
      LOG.error("", ex);
      done(Cause.ERROR, ex);
      throw ex;
    }

    notifyRemote = true;
    done(Cause.JOINED, null);
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
        exception, true);
    if (cause == Cause.JOINED) {
      ((ParticipantContainer) _localParticipant).addParticipant(_remoteParticipant, _joinType, _direction, null);
    }
    _remoteParticipant.joinDone(notifyRemote);

    _settableJoint.done(joinCompleteEvent);

    _localParticipant.dispatch(joinCompleteEvent);
    done = true;
  }
}
