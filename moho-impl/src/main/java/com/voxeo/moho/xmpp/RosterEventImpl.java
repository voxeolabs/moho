package com.voxeo.moho.xmpp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.voxeo.moho.Framework;
import com.voxeo.moho.util.XmlUtils;
import com.voxeo.servlet.xmpp.IQRequest;

public class RosterEventImpl extends XMPPIQEventImpl implements RosterEvent {

  private Map<String, RosterItem> _items = new HashMap<String, RosterEvent.RosterItem>();

  public RosterEventImpl(Framework framework, IQRequest request) {
    super(framework, request);
    List<Element> childElements = XmlUtils.getChildElements(request.getElement(), ELEMENT_ITEM, false);
    for (Element elm : childElements) {
      String jid = elm.getAttribute("jid");
      RosterItemImpl rosterItem = new RosterItemImpl(jid);
      _items.put(jid, rosterItem);
      if (XmlUtils.getAttributeValue(elm, "subscription") != null) {
        rosterItem.setSubscription(XmppSubscription.valueOf(elm.getAttribute("subscription").toUpperCase()));
      }
      for (Element group : XmlUtils.getChildElements(elm, "group", false)) {
        rosterItem.addGroupName(XmlUtils.getTextContent(group));
      }
      if (XmlUtils.getAttributeValue(elm, "name") != null) {
        rosterItem.setName(XmlUtils.getAttributeValue(elm, "name"));
      }
      if (XmlUtils.getAttributeValue(elm, "ask") != null) {
        rosterItem.setAsk(Ask.valueOf(elm.getAttribute("ask").toUpperCase()));
      }
    }
  }

  @Override
  public Collection<RosterItem> getItems() {
    return _items.values();
  }

  @Override
  public RosterItem getItem(String jid) {
    return _items.get(jid);
  }
}
