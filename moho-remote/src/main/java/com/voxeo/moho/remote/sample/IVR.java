package com.voxeo.moho.remote.sample;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class IVR implements Observer {
  
  public static void main(String[] args) {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new IVR());
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost");
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

    call.setApplicationState("menu-level-1");

    OutputCommand output = new OutputCommand(
        "1 for sales, 2 for support, this is the prompt that should be interruptable."
//        "ftp://public:public@172.21.0.83/jsr309test/audio/jsr309-TCK-media/Numbers_1-5_40s.au"
        );
    output.setBargeinType(BargeinType.ANY);

    InputCommand input = new InputCommand(new SimpleGrammar("1,2"));
    input.setTerminator('#');
    call.prompt(output, input, 0);
  }

  @State("menu-level-1")
  public void menu1(final InputCompleteEvent<Call> evt) {
    System.out.println(evt.getCause());
    switch (evt.getCause()) {
      case MATCH:
        final Call call = evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-level-2-1");
          call.prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        else {
          call.setApplicationState("menu-level-2-2");
          call.prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        break;
    }
  }

  @State("menu-level-2-1")
  public void menu21(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call =  evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-sales");
          call.prompt("thank you for calling sipmethod sales", null, 0);
          call.hangup();
        }
        else {
          call.setApplicationState("menu-prophecy-sales");
          call.prompt("thank you for calling prophecy sales", null, 0);
          call.hangup();
        }
        break;
    }
  }

  @State("menu-level-2-2")
  public void menu22(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-support");
          call.prompt("thank you for calling sipmethod support", null, 0);
          call.hangup();
        }
        else {
          call.setApplicationState("menu-prophecy-support");
          call.prompt("thank you for calling prophecy support", null, 0);
          call.hangup();
        }
        break;
    }
  }
}
