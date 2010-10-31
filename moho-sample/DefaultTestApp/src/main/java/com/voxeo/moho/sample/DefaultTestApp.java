package com.voxeo.moho.sample;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.sip.SIPInviteEvent;

public class DefaultTestApp implements Application {

  private Map<String, Endpoint> addresses = new ConcurrentHashMap<String, Endpoint>();

  private Map<String, Call> calls = new ConcurrentHashMap<String, Call>();

  private ApplicationContext _ctx = null;

  public void init(final ApplicationContext ctx) {
    _ctx = ctx;
  }

  @State
  public void handleRegister(final RegisterEvent evt) {
    if (evt.getExpiration() > 0) {
      addresses.put(evt.getEndpoint().getURI(), evt.getContacts()[0]);
    }
    else {
      addresses.remove(evt.getEndpoint().getURI());
    }
    evt.accept();
  }

  @State
  public void handleInvite(final InviteEvent inv) throws Exception {
    final Call call = inv.acceptCall(this);
    if (inv instanceof SIPInviteEvent) {
      call.setAttribute("RemoteContact", ((SIPInviteEvent) inv).getHeader("Contact"));
    }
    call.join().get();
    call.getMediaService().output("Welcome to Voxeo Prism Test Application").get();
    calls.put(call.getAddress().getURI(), call);
    mainMenu(call);
  }

  private void mainMenu(final Call call) throws Exception {
    call.setApplicationState("main-menu");
    final OutputCommand output = new OutputCommand(new TextToSpeechResource(
        "Press or say  1 for testing TTS, Press or say 2 for testing Recording"));
    output.setBargein(true);
    final InputCommand input = new InputCommand(new Grammar[] {new SimpleGrammar("1,2"), new SimpleGrammar("one,two")});
    call.getMediaService().prompt(output, input, 0);
  }

  @State("main-menu")
  public void maiMenuInputComplete(final InputCompleteEvent evt) throws Exception {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getValue().equals("1") || evt.getValue().equalsIgnoreCase("one")) {
          call.setApplicationState("ttsTest");
          Endpoint endpoint = addresses.get(call.getAddress().getURI());
          if (endpoint == null) {
            final String remote = (String) call.getAttribute("RemoteContact");
            if (remote != null) {
              endpoint = _ctx.getEndpoint(remote);
            }
          }
          ((TextableEndpoint) endpoint).sendText((TextableEndpoint) call.getAddress(),
              "Please type your message. Type exit, quit or bye to return to the main menu.");
        }
        else {
          call.setApplicationState("recordTest");
          final URI recordURI = new File(evt.getSource().getApplicationContext().getRealPath(
              call.getAddress().getName() + "_" + new Date().getTime() + "_Recording.au")).toURI();
          call.setAttribute("RecordFileLocation", recordURI);
          final OutputCommand output = new OutputCommand(new TextToSpeechResource(
              "Please record your message after the beep, Press hash to stop record."));
          output.setBargein(true);
          final RecordCommand recordCommand = new RecordCommand(recordURI);
          recordCommand.setPrompt(output);
          call.getMediaService().input("#");
          call.setAttribute("Recording", call.getMediaService().record(recordCommand));
        }
    }
  }

  @State("recordTest")
  public void recordComplete(final InputCompleteEvent evt) throws Exception {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        ((Recording) call.getAttribute("Recording")).stop();
        final AudibleResource[] resources = new AudibleResource[2];
        resources[0] = new TextToSpeechResource("Here is what you said");
        resources[1] = new AudioURIResource(((URI) call.getAttribute("RecordFileLocation")), "");
        final OutputCommand output = new OutputCommand(resources);
        call.getMediaService().output(output).get();
        mainMenu(call);
    }
  }

  @State
  public void handleText(final TextEvent e) throws Exception {
    final Call call = calls.get(e.getSource().getURI());
    final String text = e.getText();
    if (text.equalsIgnoreCase("exit") || text.equalsIgnoreCase("quit") || text.equalsIgnoreCase("bye")) {
      call.getMediaService().output("Now return to main menu.").get();
      mainMenu(call);
    }
    else {
      call.getMediaService().output(text).get();
    }
  }

  @State
  public void handleComplete(final CallCompleteEvent e) {
    calls.remove(((Call) e.source).getAddress().getURI());
    addresses.remove(((Call) e.source).getAddress().getURI());
  }

  @Override
  public void destroy() {
  }
}