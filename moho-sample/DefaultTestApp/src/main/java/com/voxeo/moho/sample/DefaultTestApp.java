package com.voxeo.moho.sample;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipServletRequest;

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

  public void init(ApplicationContext ctx) {
    _ctx = ctx;
  }

  @State
  public void register(final RegisterEvent ev) {
    if (ev.getExpiration() > 0) {
      addresses.put(ev.getEndpoint().getURI().toLowerCase(), ev.getContacts()[0]);
    }
    else {
      addresses.remove(ev.getEndpoint().getURI().toLowerCase());
    }
    ev.accept();
  }

  @State
  public void handleInvite(final InviteEvent inv) {
    final Call call = inv.acceptCall(this);

    try {
      SipServletRequest req = ((SIPInviteEvent) inv).getSipRequest();
      call.setAttribute("RemoteContact", req.getAddressHeader("Contact").getURI());
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      call.join().get();
      call.getMediaService().output("Welcome to Voxeo Prism Test Application").get();
      calls.put(inv.getInvitor().getURI().toLowerCase(), call);

      mainMenu(call);
    }
    catch (Exception ex) {
      call.disconnect();
      calls.remove(call);
      ex.printStackTrace();
    }
  }

  private void mainMenu(Call call) throws Exception {
    call.setApplicationState("main-menu");

    OutputCommand output = new OutputCommand(new TextToSpeechResource(
        "Press or say  1 for testing TTS, Press or say 2 for testing Recording"));
    output.setBargein(true);

    InputCommand input = new InputCommand(new Grammar[] {new SimpleGrammar("1,2"), new SimpleGrammar("one,two")});

    call.getMediaService().prompt(output, input, 0);
  }

  @State("main-menu")
  public void maiMenuInputComplete(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getValue().equals("1") || evt.getValue().equalsIgnoreCase("one")) {
          call.setApplicationState("ttsTest");

          try {
            TextableEndpoint endpoint = null;
            if (addresses.get(call.getAddress().getURI().toLowerCase()) != null) {
              endpoint = (TextableEndpoint) addresses.get(call.getAddress().getURI().toLowerCase());
            }
            else {
              endpoint = (TextableEndpoint) _ctx.getEndpoint(((javax.servlet.sip.URI) call
                  .getAttribute("RemoteContact")).toString());
            }

            endpoint.sendText((TextableEndpoint) call.getAddress(),
                "Please type your message. Type exit, quit or bye to return to the main menu.");
          }
          catch (Exception e) {
            call.disconnect();
            calls.remove(call);
            e.printStackTrace();
          }

        }
        else {
          call.setApplicationState("recordTest");

          final String path = evt.getSource().getApplicationContext().getServletContext().getRealPath(
              call.getAddress().getName() + "_" + new Date().getTime() + "_Recording.au");

          URI recordURI = new File(path).toURI();
          call.setAttribute("RecordFileLocation", recordURI);

          RecordCommand recordCommand = new RecordCommand(recordURI);

          OutputCommand output = new OutputCommand(new TextToSpeechResource(
              "Please record your message after the beep, Press hash to stop record."));
          output.setBargein(true);
          recordCommand.setPrompt(output);

          call.getMediaService().input("#");

          Recording recording = call.getMediaService().record(recordCommand);
          call.setAttribute("Recording", recording);
        }
        break;
    }
  }

  @State("recordTest")
  public void recordComplete(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        ((Recording) call.getAttribute("Recording")).stop();

        AudibleResource[] resources = new AudibleResource[2];
        resources[0] = new TextToSpeechResource("Here is what you said");
        resources[1] = new AudioURIResource(((URI) call.getAttribute("RecordFileLocation")), "");

        OutputCommand output = new OutputCommand(resources);
        try {
          call.getMediaService().output(output).get();
          mainMenu(call);
        }
        catch (Exception e) {
          call.disconnect();
          calls.remove(call);
          e.printStackTrace();
        }
        break;
    }
  }

  @State
  public void handleText(final TextEvent e) {
    Call call = calls.get(e.getSource().getURI().toLowerCase());
    String text = e.getText();
    try {
      if (text.equalsIgnoreCase("exit") || text.equalsIgnoreCase("quit") || text.equalsIgnoreCase("bye")) {
        call.getMediaService().output("Now return to main menu.").get();
        mainMenu(call);
      }
      else {
        call.getMediaService().output(text).get();
      }
    }
    catch (Exception ex) {
      call.disconnect();
      calls.remove(call);
      ex.printStackTrace();
    }
  }
  
  @State
  public void handleComplete(final CallCompleteEvent e) {
    calls.remove(((Call) e.source).getAddress().getURI().toLowerCase());
    addresses.remove(((Call) e.source).getAddress().getURI().toLowerCase());
  }

  @Override
  public void destroy() {
  }
}