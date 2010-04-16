package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.NotifyEvent;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class PresenceSubscribe implements Application {

  OutputCommand _prompt;

  CallableEndpoint _ep1;

  Endpoint _ep2;

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {
    _prompt = new OutputCommand(new AudibleResource[] {new TextToSpeechResource(
        "Peer is not available now, we will automatically redial for you when peer is available. Please wait.")});
    _ep1 = (CallableEndpoint) ctx.getEndpoint(ctx.getParameter("party1"));
    _ep2 = ctx.getEndpoint(ctx.getParameter("party2"));
  }

  @State
  public void handleInvite(final InviteEvent e) throws Exception {
    final Call call = e.acceptCall(this);
    call.join().get();
    call.getMediaService().prompt(_prompt, null, 30);
    final Subscription sub = _ep1.subscribe(_ep2, Subscription.Type.PRESENCE, 3600, this);
    sub.setAttribute("call", call);
  }

  @State
  public void handleNotify(final NotifyEvent notify) {
    if (notify.getEventType() == Subscription.Type.PRESENCE && notify.getResourceState().equalsIgnoreCase("open")) {
      final Subscription s = (Subscription) notify.source;
      final Call call = (Call) s.getAttribute("call");
      final Endpoint address = s.getAddress();
      call.join((CallableEndpoint) address, JoinType.DIRECT, Joinable.Direction.DUPLEX);
    }
  }
}
