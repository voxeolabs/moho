package com.voxeo.moho.xmpp;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.JID;

public class XMPPEndpointImpl implements XMPPEndpoint {
  
  protected ExecutionContext _ctx;
  
  protected JID _address;
  
  public XMPPEndpointImpl(ExecutionContext context, JID address) {
    _ctx = context;
    _address = address;
  }

  @Override
  public void sendText(TextableEndpoint from, String text) throws IOException {
    sendText(from, text, null);
  }

  @Override
  public void sendText(TextableEndpoint from, String text, String type) throws IOException {
    try {
      Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
      .createElement("body");
      element.setTextContent(text);
      InstantMessage instantMessage = _ctx.getXmppFactory().createMessage(((XMPPEndpointImpl)from).getJID(), _address, type == null ? "chat" : type, element);
      instantMessage.send();
    }
    catch (Throwable e) {
      throw new IOException(e);
    }
  }
  
  public void sendRichContent(TextableEndpoint from, List<Element> content, String type) throws IOException {
    try {
      InstantMessage instantMessage = _ctx.getXmppFactory().createMessage(((XMPPEndpointImpl)from).getJID(), _address, type == null ? "chat" : type, content.toArray(new Element[content.size()]));
      instantMessage.send();
    }
    catch (Throwable e) {
      throw new IOException(e);
    }
  }
  
  @Override
  public void sendRichContent(TextableEndpoint from, List<Element> content) throws IOException {
    sendRichContent(from, content, null);
  }

  @Override
  public String getName() {
    return _address.toString();
  }

  @Override
  public URI getURI() {
    return null;
  }

  @Override
  public JID getJID() {
    return _address;
  }

}
