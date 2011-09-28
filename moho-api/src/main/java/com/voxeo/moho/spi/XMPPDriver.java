package com.voxeo.moho.spi;

import java.io.IOException;

import javax.servlet.ServletException;

import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.PresenceMessage;

public interface XMPPDriver extends ProtocolDriver {
  void doMessage(InstantMessage req) throws ServletException, IOException;

  void doPresence(PresenceMessage req) throws ServletException, IOException;

  void doIQRequest(IQRequest req) throws ServletException, IOException;

  void doIQResponse(IQResponse resp) throws ServletException, IOException;
}
