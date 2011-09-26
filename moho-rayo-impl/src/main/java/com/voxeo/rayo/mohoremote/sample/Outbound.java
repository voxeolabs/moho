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

package com.voxeo.rayo.mohoremote.sample;

import java.net.URI;
import java.util.Timer;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.rayo.mohoremote.MohoRemote;
import com.voxeo.rayo.mohoremote.MohoRemoteFactory;

public class Outbound implements Observer {

  CallableEndpoint _party1;

  CallableEndpoint _party2;

  CallableEndpoint _local;

  Timer _timer;

  public static void main(String[] args) {
    MohoRemoteFactory mohoRemoteFactory = MohoRemoteFactory.newInstance();
    MohoRemote mohoRemote = mohoRemoteFactory.newMohoRemote();
    mohoRemote.addObserver(new Outbound());

    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost");

    CallableEndpoint endpoint = mohoRemote.createEndpoint(URI.create("sip:prism@127.0.0.1:56368"));

    Call call = endpoint.createCall("sip:mohosample@example.com");
    call.addObserver(new Outbound());
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final AnsweredEvent<Call> event) {
    event.getSource().output("Hello world");
  }

}
