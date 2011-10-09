package com.voxeo.moho.presence.impl.xmpp.cassandra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.impl.AbstractCassandraPresenceStore;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.presence.xmpp.XMPPPresenceResource;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;

public class XMPPCassandraPresenceStore extends AbstractCassandraPresenceStore implements XMPPPresenceStore {

  private static final String DEFAULT_KEYSPACE_NAME = "XmppPresence";

  //key-to, columnName-from, coloumnValue-XmppPendingNotification
  private static final String NOTIFICATION_COLUMN_FAMILY_NAME = "Notifications";

  //key-fullJID, columnName-"value", coloumnValue-XMPPPresenceResource
  private static final String RESOURCE_COLUMN_FAMILY_NAME = "Resources";

  // key-userName, columnName-"value", coloumnValue-Roster
  private static final String ROSTER_COLUMN_FAMILY_NAME = "Rosters";

  // key-bareJID, columnName-fullJID, coloumnValue-null
  private static final String BARE_JID_RESOURCE_COLUMN_FAMILY_NAME = "IdxBareJIDResources";

  @Override
  public void init(Map<String, String> props) {
    super.init(props);
  }

  @Override
  public void startTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void commitTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void rollbackTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public XMPPPresenceResource getResource(String jid) {
    HColumn<String, Object> resultByStringKey = queryResultByStringKey(jid, RESOURCE_COLUMN_FAMILY_NAME, "value");
    if (resultByStringKey != null) {
      XMPPPresenceResource resource = (XMPPPresenceResource) resultByStringKey.getValue();
      triggerRetrieveListener(Resource.class, resource);
      return resource;
    }
    return null;
  }

  @Override
  public List<XMPPPresenceResource> getResourceByBareID(String jid) {
    List<XMPPPresenceResource> retv = new ArrayList<XMPPPresenceResource>();
    QueryResult<ColumnSlice<String, Object>> queryRowByStringKey = queryRowByStringKey(jid, BARE_JID_RESOURCE_COLUMN_FAMILY_NAME);
    if (queryRowByStringKey != null) {
      for (HColumn<String, Object> hc : queryRowByStringKey.get().getColumns()) {
        XMPPPresenceResource resource = getResource(hc.getName());
        if (resource != null) {
          retv.add(resource);
        }
      }
    }
    return retv;
  }

  @Override
  public void removeResource(XMPPPresenceResource resource) {
    Mutator<String> mutator = getStringMutator();
    mutator.addDeletion(resource.getUri(), RESOURCE_COLUMN_FAMILY_NAME);
    mutator.execute();
    removeBareResources(resource.getUri(), resource.getBareJID());
  }

  @Override
  public void addResource(XMPPPresenceResource resource) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(resource.getUri(), RESOURCE_COLUMN_FAMILY_NAME, HFactory.createColumn("value", resource, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
    addToBareResources(resource.getUri(), resource.getBareJID());
  }
  
  private void removeBareResources(String fullJID, String bareJID) {
    Mutator<String> mutator = getStringMutator();
    mutator.addDeletion(bareJID, BARE_JID_RESOURCE_COLUMN_FAMILY_NAME, fullJID, STRING_SERIALIZER);
    mutator.execute();
  }
  
  private void addToBareResources(String fullJID, String bareJID) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(bareJID, BARE_JID_RESOURCE_COLUMN_FAMILY_NAME, HFactory.createColumn(fullJID, "", STRING_SERIALIZER, STRING_SERIALIZER));
    mutator.execute();
  }

  @Override
  public Collection<XmppPendingNotification> getNotifyByTo(String jid) {
    Collection<XmppPendingNotification> retv = new HashSet<XmppPendingNotification>();
    QueryResult<ColumnSlice<String, Object>> queryRowByStringKey = queryRowByStringKey(jid, NOTIFICATION_COLUMN_FAMILY_NAME);
    if (queryRowByStringKey != null) {
      List<HColumn<String, Object>> columns = queryRowByStringKey.get().getColumns();
      for (HColumn<String, Object> hc : columns) {
        XmppPendingNotification notify = (XmppPendingNotification) hc.getValue();
        triggerRetrieveListener(XmppPendingNotification.class, notify);
        retv.add(notify);
      }
    }
    return retv;
  }

  @Override
  public XmppPendingNotification getNotification(String from, String to) {
    HColumn<String, Object> queryResultByStringKey = queryResultByStringKey(to, NOTIFICATION_COLUMN_FAMILY_NAME, from);
    if (queryResultByStringKey != null) {
      XmppPendingNotification notify = (XmppPendingNotification) queryResultByStringKey.getValue();
      triggerRetrieveListener(XmppPendingNotification.class, notify);
      return notify;
    }
    return null;
  }

  @Override
  public void addNotification(XmppPendingNotification notifcation) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(notifcation.getTo(), NOTIFICATION_COLUMN_FAMILY_NAME, HFactory.createColumn(notifcation.getFrom(), notifcation, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }

  @Override
  public void removeNotification(XmppPendingNotification notifcation) {
    Mutator<String> mutator = getStringMutator();
    mutator.addDeletion(notifcation.getTo(), NOTIFICATION_COLUMN_FAMILY_NAME, notifcation.getFrom(), STRING_SERIALIZER);
    mutator.execute();
  }

  @Override
  public Roster getRoster(String user) {
    HColumn<String, Object> queryResultByStringKey = queryResultByStringKey(user, ROSTER_COLUMN_FAMILY_NAME, "value");
    if (queryResultByStringKey != null) {
      return (Roster) queryResultByStringKey.getValue();
    }
    return null;
  }

  @Override
  public void addRoster(Roster roster) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(roster.getOwner(), ROSTER_COLUMN_FAMILY_NAME, HFactory.createColumn("value", roster, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }

  @Override
  public void removeRoster(Roster roster) {
    Mutator<String> mutator = getStringMutator();
    mutator.addDeletion(roster.getOwner(), ROSTER_COLUMN_FAMILY_NAME);
    mutator.execute();
  }

  @Override
  protected String getDefaultKeyspaceName() {
    return DEFAULT_KEYSPACE_NAME;
  }
}
