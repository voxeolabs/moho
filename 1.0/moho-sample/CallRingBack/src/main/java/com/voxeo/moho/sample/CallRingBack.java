package com.voxeo.moho.sample;

import java.net.MalformedURLException;
import java.net.URL;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;

public class CallRingBack implements Application {

  URL _media;

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {
    try {
      String mediaLocation = ctx.getParameter("MediaLocation");
      if (mediaLocation == null) {
        throw new IllegalArgumentException();
      }
      _media = new URL(mediaLocation);
    }
    catch (final MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @State
  public void handleInvite(final InviteEvent e) {
    final Call call = e.acceptCallWithEarlyMedia(this);
    call.getMediaService().prompt(_media, null, 30);
    call.join(e.getInvitee(), JoinType.BRIDGE, Joinable.Direction.DUPLEX);
  }
}
