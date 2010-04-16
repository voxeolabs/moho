package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.ReferEvent;

/**
 * Transfer: unattended example. consume REFER request, use 3PCC signaling. this
 * function can also be implemented by just forwarding the refer request to the
 * peer. using ReferEvent.
 */
public class Transfer implements Application {

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {

  }

  @State
  public void handleInvite(final InviteEvent e) {
    final Call call = e.acceptCall(this);
    call.setSupervised(true);
    final Call outgoingCall = e.getInvitee().call(e.getInvitor(), null, this);
    outgoingCall.setSupervised(true);
    call.join(outgoingCall, JoinType.DIRECT, Joinable.Direction.DUPLEX);
    outgoingCall.setApplicationState("wait");
  }

  @State("wait")
  public void transfer(final ReferEvent ev) throws SignalException {
    // you can do your own things here. if you need not do this, you can just
    // remove this method, so you need not set the call in 'supervised' mode in
    // the above handleInvite method.
  }

}
