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

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerEvent;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.conference.ConferenceController;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.conference.SimpleConferenceController;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.spi.ExecutionContext;

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
  public void handleInvite(final IncomingCall call) throws Exception {
    call.addObserver(this);
    call.accept();
    call.setSupervised(true);
    MixerEndpoint end = (MixerEndpoint) _ctx.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
    end.setProperty("playTones", "true");

    Properties p = new Properties();
    p.setProperty("playTones", "false");

    if (conference == null) {
      Parameters parameters = ((ExecutionContext) _ctx).getMSFactory().createParameters();
      parameters.put(MediaMixer.ENABLED_EVENTS, new EventType[]{MixerEvent.ACTIVE_INPUTS_CHANGED});

      conference = _manager.createConference(end, null, call.getInvitee().getName(), Integer.MAX_VALUE, _controller,
          parameters);
      conference.addObserver(this);
    }

    conference.join(call, JoinType.BRIDGE, Direction.DUPLEX, p).get();

    call.setAttribute("conference", conference);
  }

  @State
  public void handleActiveSpeaker(final ActiveSpeakerEvent env) throws Exception {
    env.getActiveSpeakers();

    conference.output("active speaker event received.");
  }

  @State
  public void handleCallComplete(final CallCompleteEvent env) throws Exception {
    Call call = env.getSource();
    Conference conference = (Conference) call.getAttribute("conference");

    conference.output(
        "participant left conference. there is " + conference.getParticipants().length + " participants now.");
  }
}
