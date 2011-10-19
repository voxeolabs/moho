package com.voxeo.moho.sip;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Call;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.MohoJoinCompleteEvent;

public class RemoteLocalJoinDelegate extends JoinDelegate implements MediaEventListener<SdpPortManagerEvent> {
  private static final Logger LOG = Logger.getLogger(LocalRemoteJoinDelegate.class);

  protected RemoteParticipantImpl _remoteParticipant;

  protected Participant _localParticipant;

  protected byte[] sdpOffer;

  protected boolean waitAnswerProcessed = false;

  protected boolean notifyRemote;

  public RemoteLocalJoinDelegate(final RemoteParticipantImpl remoteParticipant, final Participant localParticipant,
      final Direction direction, byte[] offer) {
    _localParticipant = localParticipant;
    _remoteParticipant = (RemoteParticipantImpl) remoteParticipant;
    // TODO set direction.
    _direction = Direction.DUPLEX;
    _joinType = JoinType.BRIDGE;
    sdpOffer = offer;
  }

  @Override
  public void doJoin() throws Exception {
    try {
      _remoteParticipant.startJoin(_localParticipant, this);
      ((ParticipantContainer) _localParticipant).startJoin(_remoteParticipant, this);

      if (_localParticipant.getMediaObject() == null && _localParticipant instanceof Call) {
        notifyRemote = true;
        ((Call) _localParticipant).join(Direction.DUPLEX);
        return;
      }

      // 1 create network connection
      this.createNetworkConnection(_localParticipant.getApplicationContext(), _remoteParticipant);

      // 2 join network connection
      ((Joinable) _localParticipant.getMediaObject()).join(_direction, _remoteParticipant.getNetworkConnection());

      // process sdp offer.
      _remoteParticipant.getNetworkConnection().getSdpPortManager().processSdpOffer(sdpOffer);
    }
    catch (Exception ex) {
      done(Cause.ERROR, ex);
      throw ex;
    }
  }

  @Override
  public void onEvent(SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
      if (event.isSuccessful()) {
        final byte[] sdp = event.getMediaServerSdp();
        try {
          ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication().joinAnswer(
              _remoteParticipant.getId(), _localParticipant.getRemoteAddress(), sdp);
        }
        catch (final Exception e) {
          LOG.error("", e);
          notifyRemote = false;
          done(Cause.ERROR, e);
        }
      }
      else {
        Exception ex = new NegotiateException(event);
        notifyRemote = true;
        done(Cause.ERROR, ex);
      }
    }
    else {
      Exception ex = new NegotiateException(event);
      notifyRemote = true;
      done(Cause.ERROR, ex);
    }
  }

  protected synchronized void createNetworkConnection(ApplicationContext context,
      RemoteParticipantImpl remoteParticipant) throws MsControlException {
    MsControlFactory mf = context.getMSFactory();

    remoteParticipant.setMediaSession(mf.createMediaSession());

    NetworkConnection network = remoteParticipant.getMediaSession().createNetworkConnection(NetworkConnection.BASIC,
        null);
    remoteParticipant.setNetworkConnection(network);
    network.getSdpPortManager().addListener(this);
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
