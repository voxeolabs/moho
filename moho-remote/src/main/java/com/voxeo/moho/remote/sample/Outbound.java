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

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class Outbound implements Observer {

  public static void main(String[] args) {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new Outbound());

    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost", "localhost");

    CallableEndpoint endpoint = (CallableEndpoint) mohoRemote.createEndpoint(URI.create("sip:prism@127.0.0.1:5678"));

    Call call = endpoint.createCall("sip:mohosample@example.com");
    call.addObserver(new Outbound());
    call.join();
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleJoinComplete(final JoinCompleteEvent event) {
    ((Call) event.getSource()).output("join complete");
  }
}
