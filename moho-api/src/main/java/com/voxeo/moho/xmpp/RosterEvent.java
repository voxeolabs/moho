package com.voxeo.moho.xmpp;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


public interface RosterEvent extends XMPPIQEvent {
  
  String ELEMENT_ITEM = "item";
  
  public static enum XmppSubscription {
    NONE, BOTH, FROM, TO, REMOVE
  }

  public static enum Ask {
    NONE, SUBSCRIBE, UNSUBSCRIBE
  }

  public static interface RosterItem extends Serializable {
    Ask getAsk();

    void setAsk(Ask ask);
    
    void setPreApproved(boolean approved);
    
    boolean getPreApproved();

    String getName();
    
    void setName(String name);

    String getJID();

    XmppSubscription getSubscription();

    void setSubscription(XmppSubscription sub);

    List<String> getGroups();
    
    void addGroupName(String group);
  }
  
  Collection<RosterItem> getItems();
  
  RosterItem getItem(String jid);
}
