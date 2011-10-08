package com.voxeo.moho.xmpp;

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Element;

import com.voxeo.moho.TextableEndpoint;
import com.voxeo.servlet.xmpp.JID;

public interface XMPPEndpoint extends TextableEndpoint {
  JID getJID();

  void sendRichContent(TextableEndpoint from, List<Element> content, String type) throws IOException;
  
  void sendRichContent(TextableEndpoint from, List<Element> content) throws IOException;
}
