/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.remote.sample;

import java.net.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class PlayMultiple implements Observer {

  public static void main(String[] args) throws Exception {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new PlayMultiple());
    mohoRemote.connect("usera", "1", "", "voxeo", "localhost", "localhost");
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

    try {
      OutputCommand oc = new OutputCommand(new AudibleResource[] {new TextToSpeechResource("hello, this is first sentence"),
          new TextToSpeechResource("hi, this is the second test for the text to speech resource test1 test2 test3 test4 test5")});
//      OutputCommand oc = new OutputCommand(new AudibleResource[] {new TextToSpeechResource("hello, this is first sentence"),
//          new AudioURIResource(URI.create("ftp://public:public@172.21.0.83/jsr309test/audio/jsr309-TCK-media/Numbers_1-5_40s.au"))});
      Output<Call> output = call.output(oc);
      Thread.sleep(2000);
      output.pause();
      Thread.sleep(2000);
      output.resume();
      Thread.sleep(2000);
      output.pause();
      Thread.sleep(2000);
      output.resume();
      Thread.sleep(5000);
      output.stop();
      output.get();
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
