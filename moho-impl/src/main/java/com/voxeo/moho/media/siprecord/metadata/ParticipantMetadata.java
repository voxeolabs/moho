package com.voxeo.moho.media.siprecord.metadata;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

public class ParticipantMetadata extends MetadataObj {

  private Map<MediaStream, Association> sendStreams = new HashMap<MediaStream, Association>();

  private Map<MediaStream, Association> receiveStreams = new HashMap<MediaStream, Association>();

  private List<Element> extendedDatas = new LinkedList<Element>();

  private Map<URI, String> nameAors = new HashMap<URI, String>();

  public void associateSendStream(MediaStream stream) {
    sendStreams.put(stream, new Association(new Date()));
  }

  public void disassociateSendStream(MediaStream stream) {
    sendStreams.get(stream).setDisassotiateTimestamp(new Date());
  }

  public void associateReceiveStream(MediaStream stream) {
    receiveStreams.put(stream, new Association(new Date()));
  }

  public void disassociateReceiveStream(MediaStream stream) {
    receiveStreams.get(stream).setDisassotiateTimestamp(new Date());
  }

  public Map<MediaStream, Association> getSendStreams() {
    return sendStreams;
  }

  public Map<MediaStream, Association> getReceiveStreams() {
    return receiveStreams;
  }

  public Map<URI, String> getNameAors() {
    return nameAors;
  }

  public void setNameAors(Map<URI, String> nameAors) {
    this.nameAors = nameAors;
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
