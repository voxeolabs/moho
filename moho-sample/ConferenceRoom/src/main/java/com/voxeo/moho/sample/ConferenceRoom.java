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

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.conference.ConferenceController;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.conference.SimpleConferenceController;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class ConferenceRoom implements Application {

  private ConferenceManager _manager;

  ConferenceController _controller;

  @Override
  public void destroy() {
    _controller = null;
    _manager.removeAllConferences();
    _manager = null;
  }

  @Override
  public void init(final ApplicationContext ctx) {
    _manager = ctx.getConferenceManager();
    _controller = new SimpleConferenceController(new TextToSpeechResource("Hello"), new TextToSpeechResource("Bye"),
        new SimpleGrammar("#"));
  }

  @State
  public void handleInvite(final InviteEvent inv) {
    final Call call = inv.acceptCall();
    final Conference conf = _manager.createConference(inv.getInvitee().getName(), Integer.MAX_VALUE, _controller, null);
    try {
      conf.join(call, JoinType.BRIDGE, Direction.DUPLEX).get();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
