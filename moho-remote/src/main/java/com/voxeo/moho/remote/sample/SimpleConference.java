package com.voxeo.moho.remote.sample;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class SimpleConference implements Observer {

  static MohoRemoteImpl mohoRemote;

  static Mixer mixer;

  protected Collection<IncomingCall> participants = new HashSet<IncomingCall>();

  public static void main(String[] args) throws Exception {
    mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new SimpleConference());
    mohoRemote.connect("usera", "1", "", "voxeo", "localhost", "localhost");

    // create mixer
    MixerEndpoint mixerEndpoint = mohoRemote.createMixerEndpoint();
    mixer = mixerEndpoint.create("1234", null);

    // create agent and join to mixer
    Endpoint agentEndpoint = mohoRemote.createEndpoint(URI.create("sip:sipuserf@127.0.0.1:40000"));
    Call agent = ((CallableEndpoint) agentEndpoint).createCall("sip:conference@test");
    agent.join(mixer, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX).get();

    // create coach, join to mixer, join to agent.
    Endpoint coachEndpoint = mohoRemote.createEndpoint(URI.create("sip:sipuserf@127.0.0.1:40000"));
    Call coach = ((CallableEndpoint) coachEndpoint).createCall("sip:conference@test");
    coach.join(mixer, JoinType.BRIDGE_SHARED, false, Direction.RECV).get();
    coach.join(agent, JoinType.BRIDGE_SHARED, false, Direction.SEND).get();

    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.answer(this);
    call.output("welcome to conference 1234, joining").get();
    mixer.join(call, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
  }

  @State
  public void handleJoinComplete(final JoinCompleteEvent evt) throws Exception {
    if (evt.getCause() == JoinCompleteEvent.Cause.JOINED) {
      participants.add((IncomingCall) evt.getSource());
      mixer.output("Participant joined, there are " + participants.size() + " participants now.");
    }
  }

  @State
  public void handleUnJoinComplete(final UnjoinCompleteEvent evt) throws Exception {
    if (evt.getParticipant() == mixer) {
      mixer.output("Participant unjoined, there are " + participants.size() + " participants now.");
    }
  }
}