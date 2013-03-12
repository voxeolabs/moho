/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.remote.sample;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class AskSensory implements Observer {

  public static void main(String[] args) throws Exception {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new AskSensory());
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "ec2-75-101-235-124.compute-1.amazonaws.com", "ec2-75-101-235-124.compute-1.amazonaws.com");
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final IncomingCall call) throws UnsupportedEncodingException {
    call.answer();
    call.addObserver(this);


    URI grammarUri = URI.create("data:" + URLEncoder.encode("application/x-builtin," + "builtin:helloBlueGenie", "UTF-8"));
    Grammar grammar = new Grammar(grammarUri);
    
    InputCommand command = new InputCommand(grammar);
    command.setRecognizer("en-gb");
    
//    String grammarUri = "1";
//    Grammar grammar = new Grammar("application/grammar+voxeo",grammarUri);
//    command.setRecognizer("de-de");
    call.input(command);
  }

  @State
  public void handleInputComplete(final InputCompleteEvent<Call> event) {
    System.out.println(event.getCause());
  }

  @State
  public void handleCallComplete(final CallCompleteEvent event) {
    System.out.print(event.getCause());
  }
}