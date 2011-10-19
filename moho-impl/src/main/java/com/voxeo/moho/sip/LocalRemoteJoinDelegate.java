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
import com.voxeo.moho.remotejoin.RemoteParticipant;

public class LocalRemoteJoinDelegate extends JoinDelegate implements MediaEventListener<SdpPortManagerEvent> {

  private static final Logger LOG = Logger.getLogger(LocalRemoteJoinDelegate.class);

  protected Participant _localParticipant;

  protected RemoteParticipantImpl _remoteParticipant;

  protected boolean waitAnswerProcessed;

  protected boolean notifyRemote;

  public LocalRemoteJoinDelegate(final Participant local, final RemoteParticipant remoteParticipant,
      final Direction direction) {
    _localParticipant = local;
    _remoteParticipant = (RemoteParticipantImpl) remoteParticipant;
    _direction = direction;
    _joinType = JoinType.BRIDGE;
  }

  @Override
  protected void doJoin() throws Exception {
    _remoteParticipant.startJoin(_localParticipant, this);

    if (_localParticipant.getMediaObject() == null && _localParticipant instanceof Call) {
      ((Call) _localParticipant).join(Direction.DUPLEX);
      return;
    }
    // 1 create network connection
    this.createNetworkConnection(_localParticipant.getApplicationContext(), _remoteParticipant);

    // 2 join network connection
    ((Joinable) _localParticipant.getMediaObject()).join(_direction, _remoteParticipant.getNetworkConnection());

    // generate sdp offer.
    _remoteParticipant.getNetworkConnection().getSdpPortManager().generateSdpOffer();
  }

  @Override
  public void onEvent(SdpPortManagerEvent event) {
    if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)) {
      if (event.isSuccessful()) {
        waitAnswerProcessed = true;

        final byte[] sdp = event.getMediaServerSdp();
        try {
          ((ApplicationContextImpl) _remoteParticipant.getApplicationContext()).getRemoteCommunication().join(
              _localParticipant.getId(), _remoteParticipant.getId(), sdp);
        }
        catch (final Exception e) {
          LOG.error("", e);
          done(Cause.ERROR, e);
        }
      }
      else {
        Exception ex = new NegotiateException(event);
        done(Cause.ERROR, ex);
      }
    }
    else if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_PROCESSED)) {
      if (event.isSuccessful()) {
        if (waitAnswerProcessed) {
          notifyRemote = true;
          done(JoinCompleteEvent.Cause.JOINED, null);
          return;
        }
      }

      Exception ex = new NegotiateException(event);
      notifyRemote = true;
      done(Cause.ERROR, ex);
    }
  }

  public void remoteJoinAnswer(byte[] sdp) throws Exception {
    try {
      _remoteParticipant.getNetworkConnection().getSdpPortManager().processSdpAnswer(sdp);
    }
    catch (Exception e) {
      LOG.error("Error when doing remote join:" + e.getMessage(), e);
      done(Cause.ERROR, e);
      throw e;
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
  public synchronized void done(final Cause cause, Exception exception) {
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
