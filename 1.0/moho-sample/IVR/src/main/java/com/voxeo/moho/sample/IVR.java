package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InviteEvent;

public class IVR implements Application {

  @Override
  public void init(final ApplicationContext ctx) {
  }

  @Override
  public void destroy() {
  }

  @State
  public void handleInvite(final InviteEvent inv) throws Exception {
    final Call call = inv.acceptCall(this);
    call.join().get();
    call.setApplicationState("menu-level-1");
    final MediaService mg = call.getMediaService(false);
    mg.prompt("1 for sales, 2 for support", "1,2", 0);
  }

  @State("menu-level-1")
  public void menu1(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-level-2-1");
          call.getMediaService(false).prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        else {
          call.setApplicationState("menu-level-2-2");
          call.getMediaService(false).prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        break;
    }
  }

  @State("menu-level-2-1")
  public void menu21(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-sales");
          call.getMediaService(false).prompt("thank you for calling sipmethod sales", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-sales");
          call.getMediaService(false).prompt("thank you for calling prophecy sales", null, 0);
        }
        break;
    }
  }

  @State("menu-level-2-2")
  public void menu22(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-support");
          call.getMediaService(false).prompt("thank you for calling sipmethod support", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-support");
          call.getMediaService(false).prompt("thank you for calling prophecy support", null, 0);
        }
        break;
    }
  }
}
