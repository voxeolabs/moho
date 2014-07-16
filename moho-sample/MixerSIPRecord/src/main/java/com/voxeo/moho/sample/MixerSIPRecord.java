package com.voxeo.moho.sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.SIPRecordCommand;
import com.voxeo.moho.sip.SIPEndpoint;

public class MixerSIPRecord implements Application {

  private static final Logger LOG = Logger.getLogger(MixerSIPRecord.class);

  ApplicationContext _appContext;

  String srsAddress = "sip:srs@127.0.0.1:6061";

  String srcAddress = "sip:src@127.0.0.1:5060";

  String outgoingcallAddress = "sip:9990043109@sip.tropo.com";

  @Override
  public void init(ApplicationContext ctx) {
    _appContext = ctx;

    if (ctx.getParameter("srcAddress") != null) {
      srcAddress = ctx.getParameter("srcAddress");
    }

    if (ctx.getParameter("srsAddress") != null) {
      srsAddress = ctx.getParameter("srsAddress");
    }

    if (ctx.getParameter("outgoingcallAddress") != null) {
      outgoingcallAddress = ctx.getParameter("outgoingcallAddress");
    }
  }

  @Override
  public void destroy() {

  }

  @State
  public void handleInvite(IncomingCall call) {
    call.addObserver(this);
    call.answer();
    call.output("connecting you");

    try {
      //create the outbound call
      SIPEndpoint outgoingEndpoint = (SIPEndpoint) _appContext.createEndpoint(outgoingcallAddress);
      Call outgoingCall = outgoingEndpoint.createCall(call.getAddress());
      outgoingCall.addObserver(this);

      //create mixer
      MixerEndpoint endpoint = (MixerEndpoint) _appContext.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
      final Mixer mixer = endpoint.create(null);
      mixer.addObserver(this);
      outgoingCall.setAttribute("mixer", mixer);
      outgoingCall.setAttribute("peer", call);
      call.setAttribute("mixer", mixer);
      call.setAttribute("peer", outgoingCall);

      //join incoming call to mixer
      call.join(mixer, JoinType.BRIDGE_SHARED, Direction.DUPLEX).get();

      //join outbound call to mixer.
      outgoingCall.join(mixer, JoinType.BRIDGE_SHARED, Direction.DUPLEX).get();

      //start SIPrec on mixer
      SIPEndpoint srs = (SIPEndpoint) _appContext.createEndpoint(srsAddress);
      SIPEndpoint src = (SIPEndpoint) _appContext.createEndpoint(srcAddress);
      
      // create extended data for the session if you need
      Element extendedData = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      Element ele = DocumentHelper.createElement(DocumentHelper.createQName("ucid",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      ele.setText("0019044B529CA9E6;encoding=hex");
      extendedData.add(ele);
      List<Element> extendedDataList = new LinkedList<Element>();
      extendedDataList.add(extendedData);
      
      // create extended data for participant if you need
      Element callerParticipantExtendedData = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      Element ele3 = DocumentHelper.createElement(DocumentHelper.createQName("callingParty",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      ele3.setText("true");
      callerParticipantExtendedData.add(ele3);
      List<Element> callerParticipantExtendedDataList = new LinkedList<Element>();
      callerParticipantExtendedDataList.add(callerParticipantExtendedData);
      
      Element calleeParticipantExtendedData = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      Element ele4 = DocumentHelper.createElement(DocumentHelper.createQName("callingParty",
          DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
      ele4.setText("false");
      calleeParticipantExtendedData.add(ele4);
      List<Element> calleeParticipantExtendedDataList = new LinkedList<Element>();
      calleeParticipantExtendedDataList.add(calleeParticipantExtendedData);
      
      Map<String, List<Element>> map = new HashMap<String, List<Element>>();
      map.put(call.getId(), callerParticipantExtendedDataList);
      map.put(outgoingCall.getId(), calleeParticipantExtendedDataList);
      
      SIPRecordCommand command = new SIPRecordCommand(srs, src, extendedDataList, map);
      Recording<Mixer> recording = mixer.record(command);

      if (call.getInvitee().getName().equalsIgnoreCase("withpauseresume")) {
        Thread.sleep(10000);

        mixer.output("pausing recording").get();
        recording.pause();
        mixer.output("paused recording");

        Thread.sleep(5000);

        mixer.output("resuming recording").get();
        recording.resume();
        mixer.output("resumed recording");
      }
    }
    catch (InterruptedException e) {
      LOG.error("", e);
    }
    catch (ExecutionException e) {
      LOG.error("", e);
    }
  }

  @State
  public void recordComplete(RecordCompleteEvent event) {
    LOG.debug("==========***********Received record complete event:" + event);
  }

  @State
  public void callcompleteEvent(CallCompleteEvent event) {
    if (event.getSource().getAttribute("peer") != null) {
      try {
        ((Mixer) event.getSource().getAttribute("mixer")).output("peer was hangup, stopping siprecording.").get();
      }
      catch (Exception e) {
        LOG.error("", e);
      }

      ((Mixer) event.getSource().getAttribute("mixer")).disconnect();

      try {
        Call peerCall = ((Call) event.getSource().getAttribute("peer"));
        peerCall.setAttribute("peer", null);
        peerCall.hangup();
      }
      catch (Exception e) {
        LOG.error("", e);
      }
    }
  }
}
