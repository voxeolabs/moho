package com.voxeo.moho.sample;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.media.Recording;

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
    final Call call = event.acceptCall(this);
    call.join(event.getInvitee(), JoinType.BRIDGE, Direction.DUPLEX).get();
    final MediaService mg = call.getMediaService(true);
    call.setApplicationState("waitForInput");
    mg.input("**");
  }

  @State("waitForInput")
  public void inputComplete(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        URL media = null;
        try {
          media = new URL(_mediaLocation + System.getProperty("file.separator")
              + ((Call) evt.getSource()).getAddress().getURI() + "_" + new Date().getTime() + "_recording.au");

          Recording recording = ((Call) evt.getSource()).getMediaService().record(media);
          evt.getSource().setAttribute("Recording", recording);
          evt.getSource().setApplicationState("waitStop");

          MediaService mg = ((Call) evt.getSource()).getMediaService();
          mg.input("1");
        }
        catch (MalformedURLException e) {
          System.out.print("can't record, MalformedURLException, please configure MediaLocation parameter correctly.");
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
