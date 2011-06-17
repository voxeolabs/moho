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

import java.io.File;
import java.net.URI;
import java.util.Date;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.HangupEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.RecordCommand;

public class MidCallRecord implements Application {

  @Override
  public void destroy() {
  }

  @Override
  public void init(final ApplicationContext ctx) {
  }

  @State
  public void handleInvite(final IncomingCall partyA) throws Exception {
    partyA.addObserver(this);
    partyA.accept();
    partyA.setSupervised(true);

    final Call partyB = partyA.getInvitee().call(partyA.getInvitor());
    partyB.addObserver(this);
    partyB.setSupervised(true);

    partyA.join(partyB, JoinType.BRIDGE, Direction.DUPLEX).get();

    partyA.setApplicationState("waitForInput");
    partyA.input("*");
  }

  @State
  public void handleDisconnect(final HangupEvent event) {
      final Call call = event.getSource();

      final Call[] peers = call.getPeers();

      if (peers != null && peers.length > 0) {
        peers[0].unjoin(call);
        peers[0].output("Hello, The peer disconnect.");
      }
  }

  @State("waitForInput")
  public void inputComplete(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final URI media = new File(evt.getSource().getApplicationContext().getRealPath(
            evt.getSource().getId() + "_" + new Date().getTime() + "_recording.au")).toURI();
        final RecordCommand command = new RecordCommand(media);
        command.setInitialTimeout(20 * 1000);
        command.setFinalTimeout(60 * 1000);
        command.setStartBeep(false);
        final Recording<Call> recording = evt.getSource().record(command);
        evt.getSource().setAttribute("Recording", recording);
        evt.getSource().setApplicationState("waitStop");

        evt.getSource().input("1");
    }
  }

  @State("waitStop")
  public void waitStop(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Recording<Call> recording = (Recording<Call>) evt.getSource().getAttribute("Recording");
        if (recording != null) {
          recording.stop();
        }
    }
  }

}
