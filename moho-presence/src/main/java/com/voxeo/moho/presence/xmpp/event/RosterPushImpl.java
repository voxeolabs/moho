package com.voxeo.moho.presence.xmpp.event;

import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.presence.xmpp.XMPPPresenceService;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.moho.xmpp.RosterPush;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppSession;

public class RosterPushImpl extends DispatchableEventSource implements RosterPush {
  
  private static final Logger LOG = Logger.getLogger(RosterPushImpl.class);
  
  private final XMPPPresenceService _presenceService;
  
  private final JID _from;
  
  private final RosterItem _item;
  

  public RosterPushImpl(XMPPPresenceService presenceService, JID from, RosterItem item) {
    _presenceService = presenceService;
    _from = from;
    _item = item;
  }

  public JID getFrom() {
    return _from;
  }

  public RosterItem getItem() {
    return _item;
  }


  @Override
  public void send() {
    try {
      Element element = _presenceService.getRosterService().createRosterItemElement(_item);
      List<XmppSession> sessions = _presenceService.getSessionUtil().getSessions(_from);
      for (XmppSession session : sessions) {
        if (_presenceService.isResourceInterested(_from)) {
          session.createIQ(_from.toString(), "set", element).send();
        }
      }
    }
    catch (Exception e) {
      LOG.error("Error do roster push", e);
    }
  }

}
