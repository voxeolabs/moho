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
import java.util.Date;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.DisconnectEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.RecordCommand;

public class MidCallRecord implements Application {

  String _mediaLocation;

  @Override
  public void destroy() {
  }

  @Override
  public void init(final ApplicationContext ctx) {
    _mediaLocation = ctx.getParameter("MediaLocation");
    if (_mediaLocation == null) {
      throw new IllegalArgumentException();
    }
  }

  @State
  public void handleInvite(final InviteEvent event) throws Exception {
    final Call partyA = event.acceptCall(this);
    partyA.setSupervised(true);

    Call partyB = event.getInvitee().call(event.getInvitor(), null, this);
    partyB.setSupervised(true);

    partyA.join(partyB, JoinType.BRIDGE, Direction.DUPLEX).get();

    // partyA.join(event.getInvitee(), JoinType.BRIDGE, Direction.DUPLEX).get();

    final MediaService mg = partyA.getMediaService(true);
    partyA.setApplicationState("waitForInput");
    mg.input("* *");
  }

  @State
  public void handleDisconnect(final DisconnectEvent event) {
    if (event.source instanceof Call) {
      Call call = (Call) event.source;

      Call peer = call.getPeers()[0];
      peer.unjoin(call);

      peer.getMediaService().output("Hello, The peer disconnect.");
    }
  }

  @State("waitForInput")
  public void inputComplete(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        URI media = null;
        try {
          media = new URI(_mediaLocation + System.getProperty("file.separator") + new Date().getTime()
              + "_recording.au");

          RecordCommand command = new RecordCommand(media);
          command.setInitialTimeout(20 * 1000);
          command.setFinalTimeout(60 * 1000);
          Recording recording = ((Call) evt.getSource()).getMediaService().record(command);
          evt.getSource().setAttribute("Recording", recording);
          evt.getSource().setApplicationState("waitStop");

          MediaService mg = ((Call) evt.getSource()).getMediaService();
          mg.input("1");
        }
        catch (URISyntaxException e) {
          System.out.print("can't record, URISyntaxException, please configure MediaLocation parameter correctly.");
        }
    }
  }

  @State("waitStop")
  public void waitStop(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        Recording recording = (Recording) evt.getSource().getAttribute("Recording");
        if (recording != null) {
          recording.stop();
        }
    }
  }

}
