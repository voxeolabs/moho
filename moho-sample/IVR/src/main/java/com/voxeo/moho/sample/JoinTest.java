package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.RingEvent;

public class JoinTest implements Application {
  private static final Logger LOG = Logger.getLogger(JoinTest.class);

  ApplicationContext _ctx;

  @Override
  public void init(ApplicationContext ctx) {
    _ctx = ctx;
  }

  @Override
  public void destroy() {

  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.answer();
    call.output("hello");
    call.addObserver(this);

    call.setApplicationState("personal-call");
    CallableEndpoint endpoint2 = (CallableEndpoint) _ctx.createEndpoint("sip:sipuserf@127.0.0.1:64536");
    Call call2 = endpoint2.createCall("sip:martin@example.com");

    try {
      call2.join(call, JoinType.BRIDGE, Direction.DUPLEX);
    }
    catch (Exception ex) {
      LOG.error("Error", ex);
    }
  }

  @State
  public void handleInvite(final JoinCompleteEvent event) throws Exception {
    System.out.println(event.getCause() + ".." + event.getParticipant());
  }

  @State
  public void handleInvite(final RingEvent event) throws Exception {
    System.out.println("Received ring event" + event);
  }
}
