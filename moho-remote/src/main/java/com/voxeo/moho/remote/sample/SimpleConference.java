package com.voxeo.moho.remote.sample;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class SimpleConference implements Observer {

  static MohoRemoteImpl mohoRemote;
  
  public static void main(String[] args) {
    mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new SimpleConference());
    mohoRemote.connect("usera", "1", "", "voxeo", "localhost", null);
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.addObserver(this);
    call.answer();
    Output<Call> output = call.output("welcome to conference 1234, joining");
    output.get();
    MixerEndpoint endpoint = mohoRemote.createEndpoint("1234");
    
    Mixer mixer = endpoint.create(null);
    mixer.addObserver(this);
    Joint join = mixer.join(call, JoinType.BRIDGE, Direction.DUPLEX);
    Thread.sleep(2000);
//    mixer.unjoin(call);
    mixer.output("sound from heaven");
  }

  @State
  public void handleJoinComplete(final JoinCompleteEvent evt) throws Exception {
    System.out.println(evt);
  }
  
  @State
  public void handleActiveSpeaker(final ActiveSpeakerEvent env) throws Exception {
    env.getActiveSpeakers();

    //conference.output("active speaker event received.");
  }
  
  

  @State
  public void handleCallComplete(final CallCompleteEvent env) throws Exception {
    Call call = env.getSource();
    Conference conference = (Conference) call.getAttribute("conference");

    conference.output(
        "participant left conference. there is " + conference.getParticipants().length + " participants now.");
  }
}
