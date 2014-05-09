package com.voxeo.moho.media.siprecord.metadata;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

public class CommunicationSession extends MetadataObj {

  private Map<ParticipantMetadata, Association> participants = new HashMap<ParticipantMetadata, Association>();

  private Map<MediaStream, Association> mediaStreams = new HashMap<MediaStream, Association>();

  private List<Element> extendedDatas = new LinkedList<Element>();

  public void associateParticipant(ParticipantMetadata cs) {
    participants.put(cs, new Association(new Date()));
  }

  public void disassociateParticipant(ParticipantMetadata cs) {
    participants.get(cs).setDisassotiateTimestamp(new Date());
  }

  public void associateMediaStream(MediaStream stream) {
    mediaStreams.put(stream, new Association(new Date()));
  }

  public void disassociateMediaStream(MediaStream stream) {
    mediaStreams.get(stream).setDisassotiateTimestamp(new Date());
  }

  public Map<ParticipantMetadata, Association> getParticipants() {
    return participants;
  }

  public Map<MediaStream, Association> getMediaStreams() {
    return mediaStreams;
  }

  public List<Element> getExtendedDatas() {
    return extendedDatas;
  }

  public void setExtendedDatas(List<Element> extendedDatas) {
    this.extendedDatas = extendedDatas;
  }

  public void addExtendedData(Element extendedData) {
    this.extendedDatas.add(extendedData);
  }
}
