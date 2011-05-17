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

import java.util.Properties;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.conference.ConferenceController;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.conference.SimpleConferenceController;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class ConferenceRoom implements Application {

  private ConferenceManager _manager;

  ConferenceController _controller;

  ApplicationContext _ctx;

  Conference conference;

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
    _ctx = ctx;
  }

  @State
  public void handleInvite(final Call inv) throws Exception {
    final Call call = inv.acceptCall(this);
    MixerEndpoint end = (MixerEndpoint) _ctx.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
    end.setProperty("playTones", "true");

    Properties p = new Properties();
    p.setProperty("playTones", "false");

    if (conference == null) {
      conference = _manager.createConference(end, null, inv.getInvitee().getName(), Integer.MAX_VALUE, _controller,
          null);
    }

    conference.join(call, JoinType.BRIDGE, Direction.DUPLEX, p).get();

    call.setAttribute("conference", conference);
  }

  @State
  public void handleCallComplete(final CallCompleteEvent env) throws Exception {
    Call call = env.getSource();
    Conference conference = (Conference) call.getAttribute("conference");

    conference.getMediaService().output(
        "participant left conference. there is " + conference.getParticipants().length + " participants now.");
  }
}
