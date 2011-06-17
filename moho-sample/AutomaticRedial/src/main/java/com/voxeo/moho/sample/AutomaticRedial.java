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

package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.BusyException;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.NotifyEvent;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class AutomaticRedial implements Application {

  OutputCommand _prompt;

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
    _prompt = new OutputCommand(new AudibleResource[] {new TextToSpeechResource(
        "Peer is busy, we will automatically redial for you when peer is available. Please wait.")});
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.accept();
    try {
      call.join(call.getInvitee(), JoinType.DIRECT, Direction.DUPLEX).get();
    }
    catch (final Exception ex) {
      if (ex.getCause() instanceof BusyException) {
        final Subscription subscribe = call.getInvitee().subscribe(call.getInvitor(), Subscription.Type.DIALOG, 3600);
        subscribe.addObserver(this);
        subscribe.setApplicationState("waitNotify");
        subscribe.setAttribute("call", call);
        subscribe.subscribe();
        call.join().get();
        call.prompt(_prompt, null, 30);
      }
      throw ex;
    }
  }

  @State("waitNotify")
  public void notify(final NotifyEvent<Subscription> ev) {
    if (ev.getEventType() == Subscription.Type.DIALOG) {
      if (ev.getResourceState().equalsIgnoreCase("Terminated")) {
        final Subscription s = ev.getSource();
        final Call call = (Call) s.getAttribute("call");
        final Endpoint address = s.getAddress();
        call.join((CallableEndpoint) address, JoinType.DIRECT, Joinable.Direction.DUPLEX);
      }
    }
  }
}
