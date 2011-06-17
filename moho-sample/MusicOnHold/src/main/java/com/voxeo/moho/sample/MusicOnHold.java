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

import java.net.URI;
import java.net.URISyntaxException;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.State;
import com.voxeo.moho.event.ReInviteEvent;

public class MusicOnHold implements Application {

  URI _media;

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {
    try {
      _media = new URI(ctx.getParameter("MediaLocation"));
    }
    catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @State
  public void handleInvite(final IncomingCall call) {
    call.addObserver(this);
    call.accept();
    final Call outgoingCall = call.getInvitee().call(call.getInvitor());
    outgoingCall.addObserver(this);
    outgoingCall.setSupervised(true);
    call.setSupervised(true);
    call.join(outgoingCall, JoinType.BRIDGE, Joinable.Direction.DUPLEX);
  }

  @State
  public void holdMusic(final ReInviteEvent ev) throws SignalException {
    final Call c = ev.getSource();
    ev.accept();
    if (ev.isHold()) {
      c.getPeers()[0].prompt(_media, null, 0);
    }
    else {
      c.join(c.getPeers()[0], JoinType.BRIDGE, Joinable.Direction.DUPLEX);
    }
  }

}
