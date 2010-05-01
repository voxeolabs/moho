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

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.NotifyEvent;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class PresenceSubscribe implements Application {

  OutputCommand _prompt;

  CallableEndpoint _ep1;

  Endpoint _ep2;

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {
    _prompt = new OutputCommand(new AudibleResource[] {new TextToSpeechResource(
        "Peer is not available now, we will automatically redial for you when peer is available. Please wait.")});
    _ep1 = (CallableEndpoint) ctx.getEndpoint(ctx.getParameter("party1"));
    _ep2 = ctx.getEndpoint(ctx.getParameter("party2"));
  }

  @State
  public void handleInvite(final InviteEvent e) throws Exception {
    final Call call = e.acceptCall(this);
    call.join().get();
    call.getMediaService().prompt(_prompt, null, 30);
    final Subscription sub = _ep1.subscribe(_ep2, Subscription.Type.PRESENCE, 3600, this);
    sub.setAttribute("call", call);
  }

  @State
  public void handleNotify(final NotifyEvent notify) {
    if (notify.getEventType() == Subscription.Type.PRESENCE && notify.getResourceState().equalsIgnoreCase("open")) {
      final Subscription s = (Subscription) notify.source;
      notify.accept();
      final Call call = (Call) s.getAttribute("call");
      final Endpoint address = s.getAddress();
      call.join((CallableEndpoint) address, JoinType.DIRECT, Joinable.Direction.DUPLEX);
    }
  }
}
