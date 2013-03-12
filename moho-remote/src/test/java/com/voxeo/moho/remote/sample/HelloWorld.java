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
import java.util.concurrent.ExecutionException;

import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class HelloWorld implements Observer {

  public static void main(String[] args) throws Exception {
      
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new HelloWorld());
    mohoRemote.connect("usera", "1", "", "voxeo", "127.0.0.1", "127.0.0.1");

    new Object().wait();
  }

  @State
  public void handleInvite(final IncomingCall call) throws UnsupportedEncodingException, MediaException, InterruptedException, ExecutionException {
    call.answer();
    call.addObserver(this);
    call.output("hey, this is a new call").get();
    InputCompleteEvent<Call> result = call.input(new InputCommand(new SimpleGrammar("red,blue,green"))).get();
    call.output("you said " + result.getInterpretation()).get();
  }

}