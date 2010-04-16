package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.BusyException;
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

public class AutomaticRedial implements Application {

  OutputCommand _prompt;

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
    _prompt = new OutputCommand(new AudibleResource[] {new TextToSpeechResource(
        "Peer is busy, we will automatically redial for you when peer is available. Please wait.")});
  }

  @State
  public void handleInvite(final InviteEvent event) throws Exception {
    Call call = event.acceptCall();
    try {
      call.join(event.getInvitee(), JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      if (ex.getCause() instanceof BusyException) {
        final Subscription subscribe = event.getInvitee().subscribe(event.getInvitor(), Subscription.Type.DIALOG, 3600,
            this);
        subscribe.setApplicationState("waitNotify");
        subscribe.setAttribute("call", call);
        call.join().get();
        call.getMediaService().prompt(_prompt, null, 30);
      }
      throw ex;
    }
  }

  @State("waitNotify")
  public void notify(final NotifyEvent ev) {
    if (ev.getEventType() == Subscription.Type.DIALOG) {
      if (ev.getResourceState().equalsIgnoreCase("Terminated")) {
        final Subscription s = (Subscription) ev.source;
        final Call call = (Call) s.getAttribute("call");
        final Endpoint address = s.getAddress();
        call.join((CallableEndpoint) address, JoinType.DIRECT, Joinable.Direction.DUPLEX);
      }
    }
  }
}
