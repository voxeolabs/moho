package com.voxeo.moho.remote.sample;

import java.net.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class Record implements Observer {
  
  private static final URI RECORD_URI = URI.create("ftp://public:public@172.21.0.83/jsr309test/audio/jsr309-TCK-media/moho.au");
  
  public static void main(String[] args) throws Exception {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new Record());
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost");
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final IncomingCall call) {
    call.answer();
    call.addObserver(this);
    RecordCommand recordCommand = new RecordCommand(RECORD_URI);
    recordCommand.setMaxDuration(10*1000);
    call.record(recordCommand);
  }

  @State
  public void handleRecordComplete(final RecordCompleteEvent<Call> event) {
    System.out.println(event.getCause());
    if (event.getErrorText() != null) {
      event.getSource().output(event.getErrorText());
    }
    Output<Call> output = event.getSource().output("Your recording is:");
    //FIXME the second will stop the first if we do not wait for the completeEvent
    try {
      if (output.get() != null) {
        event.getSource().output(RECORD_URI);
      }
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  @State
  public void handleOutputComplete(final OutputCompleteEvent<Call> event) {
    System.out.println(event.getCause());
  }

  @State
  public void handleCallComplete(final CallCompleteEvent event) {
    System.out.print(event.getCause());
  }
}
