package com.voxeo.moho.presence.xmpp.impl;

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.presence.xmpp.RosterService;
import com.voxeo.moho.presence.xmpp.XMPPPresenceService;
import com.voxeo.moho.xmpp.RosterEvent.Ask;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.moho.xmpp.RosterItemImpl;
import com.voxeo.moho.xmpp.RosterPush;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppSession;

public class RosterServiceImpl implements RosterService {
  
  private static final Logger LOG = Logger.getLogger(XMPPPresenceServiceImpl.class);

  private XMPPPresenceService _presenceService;
  
  public RosterServiceImpl(XMPPPresenceService xmppPresenceService) {
    _presenceService = xmppPresenceService;
  }

  @Override
  public Roster getRoster(JID from) {
    return getRoster(from.toString());
  }

  @Override
  public Roster getRoster(String from) {
    Roster roster = getStore().getRoster(from.toString());
    if (roster == null) {
      roster = new RosterImpl(from);
      getStore().addRoster(roster);
    }
    return roster;
  }

  @Override
  public void addRosterItem(String user, RosterItem item) {
    Roster roster = getStore().getRoster(user);
    if (roster == null) {
      roster = new RosterImpl(user);
    }
    roster.addItem(item);
    getStore().addRoster(roster);
    // send a roster push containing the new roster
    // item to all of the user's interested resources
    pushRosterItem(_presenceService.getXmppFactory().createJID(user), item);
  }
  
  public RosterPush createRosterPush(JID from, RosterItem item) {
    return null;
  }
  
  //TODO refactor to RosterPUSH
  private void pushRosterItem(JID from, RosterItem item) {
    try {
      Element element = createRosterItemElement(item);
      List<XmppSession> sessions = _presenceService.getSessionUtil().getSessions(from);
      for (XmppSession session : sessions) {
        if (_presenceService.isResourceInterested(from)) {
          session.createIQ(from.toString(), "set", element).send();
        }
      }
    }
    catch (Exception e) {
      LOG.error("Error do roster push", e);
    }
  }

  private XMPPPresenceStore getStore() {
    return (XMPPPresenceStore) StoreHolder.getPresenceStore();
  }

  @Override
  public void removeRosterItem(String user, RosterItem item) {
    Roster roster = getStore().getRoster(user);
    roster.removeItem(item);
    getStore().addRoster(roster);
    pushRosterItem(_presenceService.getXmppFactory().createJID(user), item);
  }

  @Override
  public Element createRosterItemElement(RosterItem... items) throws DOMException, ParserConfigurationException {
    Element results = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        .createElementNS("jabber:iq:roster", "query");
    if (items != null) {
      for (RosterItem item : items) {
        Element itemElem = results.getOwnerDocument().createElement("item");
        itemElem.setAttribute("jid", item.getJID().toString());
        if (item.getSubscription() != null) {
          itemElem.setAttribute("subscription", item.getSubscription().toString().toLowerCase());
        }
        if (item.getName() != null) {
          itemElem.setAttribute("name", item.getName());
        }
        for (String group : item.getGroups()) {
          Element groupElem = itemElem.getOwnerDocument().createElement("group");
          groupElem.setTextContent(group);
          itemElem.appendChild(groupElem);
        }
        if (item.getAsk() != Ask.NONE) {
          itemElem.setAttribute("ask", item.getAsk().toString().toLowerCase());
        }
        results.appendChild(itemElem);
      }
    }
    return results;
  }

  @Override
  public RosterItem getRosterItem(String owner, String to) {
    Roster roster = getRoster(owner);
    RosterItem item = roster.getItem(to);
    if (item == null) {
      item = new RosterItemImpl(to);
      roster.addItem(item);
      getStore().addRoster(roster);
    }
    return item;
  }

  @Override
  public RosterItem getRosterItem(JID owner, JID to) {
    return getRosterItem(owner.toString(), to.toString());
  }

  public RosterItem createRosterItem(String to) {
    return new RosterItemImpl(to);
  }
  
  public RosterItem createRosterItem(JID to) {
    return createRosterItem(to.toString());
  }
  
  @Override
  public void addRosterItem(JID user, RosterItem item) {
    addRosterItem(user.toString(), item);
  }
}
