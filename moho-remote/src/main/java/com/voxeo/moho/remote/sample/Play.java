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

import java.net.URI;
import java.util.concurrent.ExecutionException;

import com.rayo.core.verb.Output;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class Play implements Observer {

  public static void main(String[] args) throws Exception {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new Play());
   // mohoRemote.connect(new SimpleAuthenticateCallbackImpl("mpermar", "xxxx", "", "voxeo"), "jabber.org", "telefonica115.orl.voxeo.net");
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost", "localhost");
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
		//call.output("hello world. how are you. we are going to stop you").get();
    	call.output("Hello").get();
    	call.output(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
	} catch (Exception e) {
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
