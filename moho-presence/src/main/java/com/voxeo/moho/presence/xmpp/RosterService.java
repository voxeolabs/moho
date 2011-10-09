package com.voxeo.moho.presence.xmpp;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.servlet.xmpp.JID;

public interface RosterService {
  Roster getRoster(JID from);
  
  Roster getRoster(String from);
  
  RosterItem getRosterItem(String owner, String to);
  
  RosterItem getRosterItem(JID owner, JID to);
  
  RosterItem createRosterItem(String to);
  
  RosterItem createRosterItem(JID to);

  void addRosterItem(String user, RosterItem item);
  
  void addRosterItem(JID user, RosterItem item);
  
  void removeRosterItem(String user, RosterItem item);
  
  Element createRosterItemElement(RosterItem... items) throws DOMException, ParserConfigurationException;
}
