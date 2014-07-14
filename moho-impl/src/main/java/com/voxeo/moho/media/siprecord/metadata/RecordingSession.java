package com.voxeo.moho.media.siprecord.metadata;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;

public class RecordingSession {
  private static final Logger LOG = Logger.getLogger(RecordingSession.class);

  private Map<CommunicationSession, Association> communicationSessions = new HashMap<CommunicationSession, Association>();

  private Date startTime;

  private Date stopTime;

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getStopTime() {
    return stopTime;
  }

  public void setStopTime(Date stopTime) {
    this.stopTime = stopTime;
  }

  public void assotiateCommunicationSession(CommunicationSession cs) {
    communicationSessions.put(cs, new Association(new Date()));
  }

  public void disassotiateCommunicationSession(CommunicationSession cs) {
    Association asso = communicationSessions.get(cs);

    if (asso != null) {
      asso.setDisassotiateTimestamp(new Date());
    }
    else {
      LOG.warn("No this CommunicationSession in RecordingSession. cs:" + cs);
    }
  }

  public Map<CommunicationSession, Association> getCommunicationSessions() {
    return communicationSessions;
  }

  public String generateMetadataSnapshot() {
    Document document = DocumentHelper.createDocument();
    Namespace namespace = DocumentHelper.createNamespace("", "urn:ietf:params:xml:ns:recording");

    Element rootElement = DocumentHelper.createElement(DocumentHelper.createQName("recording", namespace));
    document.setRootElement(rootElement);

    Element dataMode = DocumentHelper.createElement(DocumentHelper.createQName("dataMode", namespace));
    dataMode.add(DocumentHelper.createText("complete"));
    rootElement.add(dataMode);

    // communication session
    for (Entry<CommunicationSession, Association> entry : communicationSessions.entrySet()) {
      CommunicationSession communicationSession = entry.getKey();

      Element sessionElement = DocumentHelper.createElement(DocumentHelper.createQName("session", namespace));
      sessionElement.addAttribute("id", communicationSession.getId());

      Element associateTime = DocumentHelper.createElement(DocumentHelper.createQName("associate-time", namespace));
      associateTime.add(DocumentHelper.createText(dateFormat.format(entry.getValue().getAssotiateTimestamp())));
      sessionElement.add(associateTime);

      if (entry.getValue().getDisassotiateTimestamp() != null) {
        Element disassociateTime = DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
            namespace));
        disassociateTime.add(DocumentHelper.createText(dateFormat.format(entry.getValue().getDisassotiateTimestamp())));
        sessionElement.add(disassociateTime);
      }

      if(communicationSession.getExtendedDatas() != null) {
        for (Element extendData : communicationSession.getExtendedDatas()) {
          sessionElement.add(extendData);
        }
      }

      rootElement.add(sessionElement);

      // participants
      for (Entry<ParticipantMetadata, Association> participantEntry : communicationSession.getParticipants().entrySet()) {
        ParticipantMetadata participant = participantEntry.getKey();

        Element participantElement = DocumentHelper.createElement(DocumentHelper.createQName("participant", namespace));
        participantElement.addAttribute("id", participant.getId());
        participantElement.addAttribute("session", communicationSession.getId());

        // name aor list
        for (Entry<URI, String> nameAorEntry : participant.getNameAors().entrySet()) {
          Element nameIDElement = DocumentHelper.createElement(DocumentHelper.createQName("nameID", namespace));
          nameIDElement.addAttribute("aor", nameAorEntry.getKey().toString());

          Element nameElement = DocumentHelper.createElement(DocumentHelper.createQName("name", namespace));
          nameElement.addText(nameAorEntry.getValue());

          nameIDElement.add(nameElement);

          participantElement.add(nameIDElement);
        }

        // send/receive stream
        for (Entry<MediaStream, Association> sendStreamEntry : participant.getSendStreams().entrySet()) {
          Element sendStreamElement = DocumentHelper.createElement(DocumentHelper.createQName("send", namespace));
          sendStreamElement.addText(sendStreamEntry.getKey().getId());

          participantElement.add(sendStreamElement);
        }

        for (Entry<MediaStream, Association> recvStreamEntry : participant.getReceiveStreams().entrySet()) {
          Element recvStreamElement = DocumentHelper.createElement(DocumentHelper.createQName("recv", namespace));
          recvStreamElement.addText(recvStreamEntry.getKey().getId());

          participantElement.add(recvStreamElement);
        }

        // association time
        Element participantAssociateTime = DocumentHelper.createElement(DocumentHelper.createQName("associate-time",
            namespace));
        participantAssociateTime.add(DocumentHelper.createText(dateFormat.format(participantEntry.getValue()
            .getAssotiateTimestamp())));
        participantElement.add(participantAssociateTime);

        if (participantEntry.getValue().getDisassotiateTimestamp() != null) {
          Element disassociateTime = DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
              namespace));
          disassociateTime.add(DocumentHelper.createText(dateFormat.format(participantEntry.getValue()
              .getDisassotiateTimestamp())));
          participantElement.add(disassociateTime);
        }

        // extensiondata
        if(participant.getExtendedDatas() != null) {
          for (Element extendData : participant.getExtendedDatas()) {
            participantElement.add(extendData);
          }
        }

        rootElement.add(participantElement);
      }

      // streams
      for (Entry<MediaStream, Association> streamEntry : communicationSession.getMediaStreams().entrySet()) {
        MediaStream mediaStream = streamEntry.getKey();

        Element streamElement = DocumentHelper.createElement(DocumentHelper.createQName("stream", namespace));
        streamElement.addAttribute("id", mediaStream.getId());
        streamElement.addAttribute("session", communicationSession.getId());

        Element labelElement = DocumentHelper.createElement(DocumentHelper.createQName("label", namespace));
        labelElement.addText(mediaStream.getLabel());

        streamElement.add(labelElement);

        // association time
        Element streamAssociateTime = DocumentHelper.createElement(DocumentHelper.createQName("associate-time",
            namespace));
        streamAssociateTime.add(DocumentHelper.createText(dateFormat.format(streamEntry.getValue()
            .getAssotiateTimestamp())));
        streamElement.add(streamAssociateTime);

        if (streamEntry.getValue().getDisassotiateTimestamp() != null) {
          Element disassociateTime = DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
              namespace));
          disassociateTime.add(DocumentHelper.createText(dateFormat.format(streamEntry.getValue()
              .getDisassotiateTimestamp())));
          streamElement.add(disassociateTime);
        }

        rootElement.add(streamElement);
      }
    }

    return document.asXML();
  }
  
  public String generateMetadataSnapshot_Draft15() {
    Document document = DocumentHelper.createDocument();
    Namespace namespace = DocumentHelper.createNamespace("", "urn:ietf:params:xml:ns:recording:1");

    Element rootElement = DocumentHelper.createElement(DocumentHelper.createQName("recording", namespace));
    document.setRootElement(rootElement);

    Element dataMode = DocumentHelper.createElement(DocumentHelper.createQName("dataMode", namespace));
    dataMode.add(DocumentHelper.createText("complete"));
    rootElement.add(dataMode);

    // communication session
    for (Entry<CommunicationSession, Association> entry : communicationSessions.entrySet()) {
      CommunicationSession communicationSession = entry.getKey();

      Element sessionElement = DocumentHelper.createElement(DocumentHelper.createQName("session", namespace));
      sessionElement.addAttribute("session_id", communicationSession.getId());

      if (communicationSession.getExtendedDatas() != null) {
        for (Element extendData : communicationSession.getExtendedDatas()) {
          sessionElement.add(extendData);
        }
      }

      rootElement.add(sessionElement);

      Element sessionrecordingassocElement = DocumentHelper.createElement(DocumentHelper.createQName(
          "sessionrecordingassoc", namespace));
      sessionrecordingassocElement.addAttribute("session_id", communicationSession.getId());

      Element associateTime = DocumentHelper.createElement(DocumentHelper.createQName("associate-time", namespace));
      associateTime.add(DocumentHelper.createText(dateFormat.format(entry.getValue().getAssotiateTimestamp())));
      sessionrecordingassocElement.add(associateTime);

      if (entry.getValue().getDisassotiateTimestamp() != null) {
        Element disassociateTime = DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
            namespace));
        disassociateTime.add(DocumentHelper.createText(dateFormat.format(entry.getValue().getDisassotiateTimestamp())));
        sessionrecordingassocElement.add(disassociateTime);
      }
      rootElement.add(sessionrecordingassocElement);

      // participants
      for (Entry<ParticipantMetadata, Association> participantEntry : communicationSession.getParticipants().entrySet()) {
        ParticipantMetadata participant = participantEntry.getKey();

        Element participantElement = DocumentHelper.createElement(DocumentHelper.createQName("participant", namespace));
        participantElement.addAttribute("participant_id", participant.getId());

        // name aor list
        for (Entry<URI, String> nameAorEntry : participant.getNameAors().entrySet()) {
          Element nameIDElement = DocumentHelper.createElement(DocumentHelper.createQName("nameID", namespace));
          nameIDElement.addAttribute("aor", nameAorEntry.getKey().toString());

          Element nameElement = DocumentHelper.createElement(DocumentHelper.createQName("name", namespace));
          nameElement.addText(nameAorEntry.getValue());

          nameIDElement.add(nameElement);

          participantElement.add(nameIDElement);
        }

        // extensiondata
        if (participant.getExtendedDatas() != null) {
          for (Element extendData : participant.getExtendedDatas()) {
            participantElement.add(extendData);
          }
        }

        rootElement.add(participantElement);

        Element participantsessionassocElement = DocumentHelper.createElement(DocumentHelper.createQName(
            "participantsessionassoc", namespace));
        participantsessionassocElement.addAttribute("participant_id", participant.getId());
        participantsessionassocElement.addAttribute("session_id", communicationSession.getId());

        // association time
        Element participantAssociateTime = DocumentHelper.createElement(DocumentHelper.createQName("associate-time",
            namespace));
        participantAssociateTime.add(DocumentHelper.createText(dateFormat.format(participantEntry.getValue()
            .getAssotiateTimestamp())));
        participantsessionassocElement.add(participantAssociateTime);

        if (participantEntry.getValue().getDisassotiateTimestamp() != null) {
          Element disassociateTime = DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
              namespace));
          disassociateTime.add(DocumentHelper.createText(dateFormat.format(participantEntry.getValue()
              .getDisassotiateTimestamp())));
          participantsessionassocElement.add(disassociateTime);
        }
        
        rootElement.add(participantsessionassocElement);

        Element participantstreamassocElement = DocumentHelper.createElement(DocumentHelper.createQName(
            "participantstreamassoc", namespace));
        participantstreamassocElement.addAttribute("participant_id", participant.getId());
        // send/receive stream
        for (Entry<MediaStream, Association> sendStreamEntry : participant.getSendStreams().entrySet()) {
          Element sendStreamElement = DocumentHelper.createElement(DocumentHelper.createQName("send", namespace));
          sendStreamElement.addText(sendStreamEntry.getKey().getId());

          participantstreamassocElement.add(sendStreamElement);
        }

        for (Entry<MediaStream, Association> recvStreamEntry : participant.getReceiveStreams().entrySet()) {
          Element recvStreamElement = DocumentHelper.createElement(DocumentHelper.createQName("recv", namespace));
          recvStreamElement.addText(recvStreamEntry.getKey().getId());

          participantstreamassocElement.add(recvStreamElement);
        }
        rootElement.add(participantstreamassocElement);
        // // association time
        // Element streamAssociateTime =
        // DocumentHelper.createElement(DocumentHelper.createQName("associate-time",
        // namespace));
        // streamAssociateTime.add(DocumentHelper.createText(dateFormat.format(streamEntry.getValue()
        // .getAssotiateTimestamp())));
        // participantstreamassocElement.add(streamAssociateTime);
        //
        // if (streamEntry.getValue().getDisassotiateTimestamp() != null) {
        // Element disassociateTime =
        // DocumentHelper.createElement(DocumentHelper.createQName("disassociate-time",
        // namespace));
        // disassociateTime.add(DocumentHelper.createText(dateFormat.format(streamEntry.getValue()
        // .getDisassotiateTimestamp())));
        // participantstreamassocElement.add(disassociateTime);
        // }
      }

      // streams
      for (Entry<MediaStream, Association> streamEntry : communicationSession.getMediaStreams().entrySet()) {
        MediaStream mediaStream = streamEntry.getKey();

        Element streamElement = DocumentHelper.createElement(DocumentHelper.createQName("stream", namespace));
        streamElement.addAttribute("stream_id", mediaStream.getId());
        streamElement.addAttribute("session_id", communicationSession.getId());

        Element labelElement = DocumentHelper.createElement(DocumentHelper.createQName("label", namespace));
        labelElement.addText(mediaStream.getLabel());

        streamElement.add(labelElement);

        rootElement.add(streamElement);
      }
    }

    return document.asXML();
  }

  public static void main(String[] args) throws Exception {
    RecordingSession rs = new RecordingSession();
    CommunicationSession cs = new CommunicationSession();
    Element extendedData = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    Element ele = DocumentHelper.createElement(DocumentHelper.createQName("ucid",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    ele.setText("0019044B529CA9E6;encoding=hex");

    extendedData.add(ele);
    Element ele2 = DocumentHelper.createElement(DocumentHelper.createQName("callerOrig",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    ele2.setText("true");
    extendedData.add(ele2);
    List<Element> aa = new LinkedList<Element>();
    aa.add(extendedData);
    cs.setExtendedDatas(aa);
    MediaStream stream1 = new MediaStream();
    stream1.setLabel("1");
    MediaStream stream2 = new MediaStream();
    stream2.setLabel("2");

    ParticipantMetadata part1 = new ParticipantMetadata();
    part1.associateSendStream(stream1);
    part1.associateReceiveStream(stream2);
    Map<URI, String> nameAors = new HashMap<URI, String>();
    nameAors.put(URI.create("sip:usea@test"), "iamusera");
    part1.setNameAors(nameAors);

    Element extendedData1 = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    Element ele3 = DocumentHelper.createElement(DocumentHelper.createQName("callingParty",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    ele3.setText("true");
    extendedData1.add(ele3);
    part1.addExtendedData(extendedData1);

    ParticipantMetadata part2 = new ParticipantMetadata();
    part2.associateSendStream(stream2);
    part2.associateReceiveStream(stream1);
    Map<URI, String> nameAors2 = new HashMap<URI, String>();
    nameAors2.put(URI.create("sip:useb@test"), "iamuserb");
    part2.setNameAors(nameAors2);

    Element extendedData2 = DocumentHelper.createElement(DocumentHelper.createQName("extensiondata",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    Element ele4 = DocumentHelper.createElement(DocumentHelper.createQName("callingParty",
        DocumentHelper.createNamespace("apkt", "http:/acmepacket.com/siprec/extensiondata")));
    ele4.setText("false");
    extendedData2.add(ele4);
    part2.addExtendedData(extendedData2);

    cs.associateParticipant(part1);
    cs.associateParticipant(part2);

    cs.associateMediaStream(stream1);
    cs.associateMediaStream(stream2);

    rs.assotiateCommunicationSession(cs);
    
    Thread.sleep(10000);

    Map<CommunicationSession, Association> communicationSessions = rs.getCommunicationSessions();
    for (CommunicationSession communicationSession : communicationSessions.keySet()) {
      rs.disassotiateCommunicationSession(communicationSession);

      Map<ParticipantMetadata, Association> participants = communicationSession.getParticipants();
      for (ParticipantMetadata participant : participants.keySet()) {
        communicationSession.disassociateParticipant(participant);
      }

      Map<MediaStream, Association> mediaStreams = communicationSession.getMediaStreams();
      for (MediaStream mediaStream : mediaStreams.keySet()) {
        communicationSession.disassociateMediaStream(mediaStream);
      }
    }

    
    // System.out.print(DocumentHelper.createElement("test").asXML());
    System.out.println(rs.generateMetadataSnapshot_Draft15());
    char a = 9;
    System.out.println(a);
  }
}
