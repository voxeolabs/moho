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
  private SipURI _srs;

  private SipURI _src;

  // TODO should use W3C dom?
  // the key of the map is ID of moho call, value is the XML element application
  // want put to metadata as subelement of the <participant> element
  private Map<String, List<Element>> _participantExtendedMetadata;

  private List<Element> _extendedDatat;

  private SIPRecordCommand(URI recorduri) {
    super(recorduri);
  }

  public SIPRecordCommand(SIPEndpoint siprecSRS, SIPEndpoint siprecSRC) {
    this(siprecSRS, siprecSRC, null, null);
  }

  public SIPRecordCommand(SIPEndpoint siprecSRS, SIPEndpoint siprecSRC, List<Element> extendedDatat,
      Map<String, List<Element>> participantExtendedMetadata) {
    super(null);
    _srs = siprecSRS.getSipURI();
    _src = siprecSRC.getSipURI();
    _extendedDatat = extendedDatat;
    _participantExtendedMetadata = participantExtendedMetadata;
  }

  public SipURI getSiprecServer() {
    return _srs;
  }

  public SipURI getSiprecSrcURI() {
    return _src;
  }

  public Map<String, List<Element>> getParticipantExtendedMetadata() {
    return _participantExtendedMetadata;
  }

  public List<Element> getExtendedDatat() {
    return _extendedDatat;
  }
}
