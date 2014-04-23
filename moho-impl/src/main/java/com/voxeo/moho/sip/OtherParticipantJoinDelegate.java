package com.voxeo.moho.sip;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class OtherParticipantJoinDelegate extends JoinDelegate {
  
  private static final Logger LOG = Logger.getLogger(OtherParticipantJoinDelegate.class);

  protected Participant otherParticipant;

  protected OtherParticipantJoinDelegate(final SIPCallImpl call1, final Participant participant, final JoinType type,
      final Direction direction) {
    _call1 = call1;
    otherParticipant = participant;
    _direction = direction;
    _joinType = type;
  }

  @Override
  public void doJoin() throws Exception {
    super.doJoin();
    _call1.unlinkDirectlyPeer();
    if (_call1.getMediaObject() == null) {
      _call1.join(Direction.DUPLEX);
      return;
    }

    JoinDelegate.bridgeJoin(_call1, otherParticipant, _direction);

    if (otherParticipant instanceof MixerImpl.ClampDtmfMixerAdapter) {
      MixerImpl.ClampDtmfMixerAdapter adapter = (MixerImpl.ClampDtmfMixerAdapter) otherParticipant;

      _call1.addParticipant(adapter.getMixer(), _joinType, _direction, adapter);
      ((ParticipantContainer) otherParticipant).addParticipant(_call1, _joinType, _direction, adapter);
    }
    else {
      _call1.addParticipant(otherParticipant, _joinType, _direction, null);
      ((ParticipantContainer) otherParticipant).addParticipant(_call1, _joinType, _direction, null);
    }

    done(Cause.JOINED, null);
  }

  public synchronized void done(final Cause cause, Exception exception) {
    if (done) {
      return;
    }
    if (exception != null) {
      LOG.error("Join complete in error cause:" + cause + " for joinDelegate" + this, exception);
    }
    else {
      LOG.debug("Join complete with cause:" + cause + " for joinDelegate" + this);
    }

    _cause = cause;
    _exception = exception;

    _call1.joinDone(otherParticipant, this);
    JoinCompleteEvent joinCompleteEvent = new MohoJoinCompleteEvent(_call1, otherParticipant, cause, exception, true);
    _call1.dispatch(joinCompleteEvent);

    JoinCompleteEvent peerJoinCompleteEvent = new MohoJoinCompleteEvent(otherParticipant, _call1, cause, exception,
        false);
    otherParticipant.dispatch(peerJoinCompleteEvent);

    _settableJoint.done(joinCompleteEvent);
    done = true;
  }
}
