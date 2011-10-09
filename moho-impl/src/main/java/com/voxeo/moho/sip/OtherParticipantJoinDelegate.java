package com.voxeo.moho.sip;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.MohoJoinCompleteEvent;

public class OtherParticipantJoinDelegate extends JoinDelegate {

  protected Participant otherParticipant;

  protected OtherParticipantJoinDelegate(final SIPCallImpl call1, final Participant participant,
      final Direction direction) {
    _call1 = call1;
    otherParticipant = participant;
    _direction = direction;
    _joinType = JoinType.BRIDGE;
  }

  @Override
  protected void doJoin() throws Exception {
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
