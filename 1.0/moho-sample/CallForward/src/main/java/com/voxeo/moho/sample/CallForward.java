package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.sip.SIPEndpoint;

public class CallForward implements Application {

  ApplicationContext _ctx;

  SIPEndpoint _target;

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {
    _ctx = ctx;
    _target = (SIPEndpoint) _ctx.getEndpoint(ctx.getParameter("target"));
  }

  @State
  public void handleInvite(final InviteEvent e) {
    final Call call = e.acceptCall(this);
    try {
      call.join(e.getInvitee(), JoinType.DIRECT, Joinable.Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      call.join(_target, JoinType.DIRECT, Joinable.Direction.DUPLEX);
    }
  }
}
