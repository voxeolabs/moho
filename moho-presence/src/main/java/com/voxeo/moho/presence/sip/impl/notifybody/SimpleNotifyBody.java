package com.voxeo.moho.presence.sip.impl.notifybody;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.sip.impl.SIPConstans;
import com.voxeo.moho.presence.sip.impl.Utils;

public abstract class SimpleNotifyBody implements NotifyBody {
  
  private static final long serialVersionUID = -2385136651272331085L;

  protected static final Logger LOG = Logger.getLogger(SimpleNotifyBody.class);

  protected String _content;
  
  protected transient Document _document;
  
  public SimpleNotifyBody(String encoding, byte[] content) {
    if (encoding == null) {
      encoding = SIPConstans.ENCODING_UTF_8;
    }
    String xml;
    try {
      xml = new String(content, encoding);
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Error converting to JDocument." + e.getMessage());
    }
    _content = xml;
  }

  public SimpleNotifyBody(String xml) {
    _content = xml;
  }

  public String getEncoding() {
    return SIPConstans.ENCODING_UTF_8;
  }

  public String getContent() {
    return _content;
  }

  /**
   * NOTE: since the m_document is transient, so it is the developer's
   * responsibility to call this method to update m_xmlDoc after the content of
   * the m_document is changed.
   * 
   * @param doc
   */
  public void setDocument(Document doc) {
    _document = doc;
    _content = new XMLOutputter().outputString(_document);
  }

  /**
   * @return the m_document
   */
  public Document getDocument() {
    if (_document == null) {
      try {
        _document = Utils.getDocument(_content);
      }
      catch (Exception e) {
        LOG.error("Error converting to JDOM Object : " + _content, e);
      }
    }
    return _document;
  }

  public Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      LOG.error("Cone error", e);
    }
    return null;
  }
}