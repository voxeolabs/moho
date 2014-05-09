package com.voxeo.moho.media.record;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipURI;

import org.dom4j.Element;

import com.voxeo.moho.sip.SIPEndpoint;

/**
 * This is used for SIP Recording
 * 
 * @author zhuwillie
 */
public class SIPRecordCommand extends RecordCommand {

  // URI of SRS
  private SipURI _siprecServer;

  private SipURI _srcURI;

  // TODO should use W3C dom?
  // the key of the map is ID of moho call, value is the XML element application
  // want put to metadata as subelement of the <participant> element
  private Map<String, List<Element>> _participantExtendedMetadata;

  private List<Element> _extendedDatat;

  private SIPRecordCommand(URI recorduri) {
    super(recorduri);
  }

  public SIPRecordCommand(SIPEndpoint siprecServer, SIPEndpoint srcURI, List<Element> extendedDatat,
      Map<String, List<Element>> participantExtendedMetadata) {
    super(null);
    _siprecServer = siprecServer.getSipURI();
    _srcURI = srcURI.getSipURI();
    _extendedDatat = extendedDatat;
    _participantExtendedMetadata = participantExtendedMetadata;
  }

  public SipURI getSiprecServer() {
    return _siprecServer;
  }

  public SipURI getSiprecSrcURI() {
    return _srcURI;
  }

  public void set_srcURI(SipURI _srcURI) {
    this._srcURI = _srcURI;
  }

  public Map<String, List<Element>> getParticipantExtendedMetadata() {
    return _participantExtendedMetadata;
  }

  public List<Element> getExtendedDatat() {
    return _extendedDatat;
  }
}
