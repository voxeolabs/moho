package com.voxeo.moho.xmpp;

import com.voxeo.moho.TextableEndpoint;
import com.voxeo.servlet.xmpp.JID;

public interface XMPPEndpoint extends TextableEndpoint {
  JID getJID();
}
