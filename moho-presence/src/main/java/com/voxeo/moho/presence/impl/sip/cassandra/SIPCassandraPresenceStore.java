package com.voxeo.moho.presence.impl.sip.cassandra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.SubscriptionID;
import com.voxeo.moho.presence.impl.AbstractPresenceStore;
import com.voxeo.moho.presence.impl.sip.SIPPresenceStore;
import com.voxeo.moho.presence.sip.EventSoftState;
import com.voxeo.moho.presence.sip.SIPResource;
import com.voxeo.moho.presence.sip.impl.SIPConstans;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;

public class SIPCassandraPresenceStore extends AbstractPresenceStore implements SIPPresenceStore {
  
  private static final ObjectSerializer OBJECT_SERIALIZER = ObjectSerializer.get();
  
  private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();
  
  private static final String DEFAULT_DATABASE_ADDRESS = "localhost:9160";
  
  private static final String DEFAULT_CLUSTER_NAME = "MohoCluster";
  
  private static final String DEFAULT_KEYSPACE_NAME = "Presence";
  
  private static final String SUBSCRIPTION_COLUMN_FAMILY_NAME = "Subscriptions";
  
  private static final String RESOURCE_COLUMN_FAMILY_NAME = "Resources";
  
  private static final String EVENT_SOFT_STATE_COLUMN_FAMILY_NAME = "EventSoftStates";
  
  private static final String PRESENCE_NB_COLUMN_FAMILY_NAME = "PresenceNotifyBody";
  
  private static final String RESOURCE_SUB_COLUMN_FAMILY_NAME = "IdxResourceSubs";

  private Cluster _cluster;
  
  private Keyspace _keyspace;

  @Override
  public void init(Map<String, String> props) {
    String databaseAddress = props.get("databaseAddress");
    String clusterName = props.get("clusterName");
    String keySapceName = props.get("keysapceName");
    
    _cluster = HFactory.getOrCreateCluster(clusterName != null ? clusterName : DEFAULT_CLUSTER_NAME, databaseAddress != null ? databaseAddress : DEFAULT_DATABASE_ADDRESS);
    _keyspace = HFactory.createKeyspace(keySapceName != null ? keySapceName : DEFAULT_KEYSPACE_NAME, _cluster);
  }

  @Override
  public void addSubscription(SubscriptionContext context) {
    insertSubscription(context);
    insertResourceSubRelation(context);
  }

  @Override
  public void updateSubscripton(SubscriptionContext context) {
    insertSubscription(context);
  }
  
  private void insertSubscription(SubscriptionContext context) {
    Mutator<Object> mutator = getObjectMutator();
    mutator.addInsertion(context.getId(), SUBSCRIPTION_COLUMN_FAMILY_NAME, HFactory.createColumn("value", context, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }
  
  private Mutator<String> getStringMutator() {
    return HFactory.createMutator(_keyspace, STRING_SERIALIZER);
  }
  
  private Mutator<Object> getObjectMutator() {
    return HFactory.createMutator(_keyspace, OBJECT_SERIALIZER);
  }

  @Override
  public void removeSubscripton(SubscriptionContext context) {
    Mutator<Object> mutator = getObjectMutator();
    mutator.addDeletion(context.getId(), SUBSCRIPTION_COLUMN_FAMILY_NAME);
    mutator.execute();
    removeResourceSubRelation(context);
  }

  @Override
  public boolean isSubscriptionExist(SubscriptionContext context) {
    return querySubscription(context.getId()) != null;
  }

  @Override
  public Resource getResource(String resourceUri, String eventName) {
    HColumn<String, Object> queryResource = queryResource(resourceUri, eventName);
    if (queryResource != null) {
      Resource retv = (Resource) queryResource.getValue();
      triggerRetrieveListener(Resource.class, retv);
      return retv;
    }
    return null;
  }

  @Override
  public void addResource(Resource resource) {
    insertResource(resource);
  }
  
  private void insertResource(Resource resource) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(resource.getUri(), RESOURCE_COLUMN_FAMILY_NAME, HFactory.createColumn(((SIPResource)resource).getEventName(), resource, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }

  @Override
  public void updateResource(Resource resource) {
    insertResource(resource);
  }

  @Override
  public NotifyBody getNotifyBody(String resourceUri, String eventName, String notifyBodyType) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(eventName)) {
      HColumn<String, Object> queryNotifyBody = queryNotifyBody(resourceUri, notifyBodyType);
      return (NotifyBody) (queryNotifyBody != null ? queryNotifyBody.getValue() : null);
    }
    throw new IllegalArgumentException("Can't find notify body for event[" + eventName + "]");
  }

  @Override
  public void startTx() {
    //TODO
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
    _cluster.getConnectionManager().shutdown();
    HFactory.shutdownCluster(_cluster);
    _keyspace = null;
    _cluster = null;
  }

  public EventSoftState getEventSoftState(String resourceUri, String entityTag) {
    HColumn<String, Object> eventSoftState = queryEventSoftState(resourceUri, entityTag);
    return (EventSoftState) (eventSoftState == null ? null : eventSoftState.getValue());
  }

  public boolean removeEventSoftState(EventSoftState state) {
    Mutator<String> mutator = getStringMutator();
    mutator.addDeletion(state.getResourceURL(), SUBSCRIPTION_COLUMN_FAMILY_NAME);
    mutator.execute();
    return true;
  }

  public void addEventSoftState(EventSoftState state) {
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(state.getResourceURL(), EVENT_SOFT_STATE_COLUMN_FAMILY_NAME, HFactory.createColumn(state.getEntityTag(), state, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }

  public void addNotifyBody(String resourceUri, String eventName, String notifyBodyType, NotifyBody notifyBody) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(eventName)) {
      Mutator<String> mutator = getStringMutator();
      mutator.addInsertion(resourceUri, PRESENCE_NB_COLUMN_FAMILY_NAME, HFactory.createColumn(
          notifyBodyType, notifyBody, STRING_SERIALIZER, OBJECT_SERIALIZER));
      mutator.execute();
    }
    else {
      throw new IllegalArgumentException("Can't save notify body for event[" + eventName + "]");
    }
  }

  public List<SubscriptionID> getSubscriptions(String resourceUri, String event, String notifyBodyType) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(event)) {
      HColumn<String, Object> resourceSubs = queryResourceSubs(resourceUri, notifyBodyType);
      if (resourceSubs != null) {
        return (List<SubscriptionID>) resourceSubs.getValue();
      }
      return Collections.emptyList();
    }
    else {
      throw new IllegalArgumentException("Can't get subscriptions for event[" + event + "]");
    }
  }
  
  private void insertResourceSubRelation(SubscriptionContext context) {
    String notifyBodyType = ((SIPSubscriptionContext) context).getNotifyBodyType();
    HColumn<String, Object> resourceSubs = queryResourceSubs(context.getSubscribee(), notifyBodyType);
    List<SubscriptionID> idList = null;
    if (resourceSubs != null) {
      idList = (List<SubscriptionID>) resourceSubs.getValue();
    }
    else {
      idList = new ArrayList<SubscriptionID>();
    }
    idList.add((SubscriptionID) context.getId());
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(context.getSubscribee(), RESOURCE_SUB_COLUMN_FAMILY_NAME, HFactory.createColumn(notifyBodyType, idList, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }
  
  private void removeResourceSubRelation(SubscriptionContext context) {
    String notifyBodyType = ((SIPSubscriptionContext) context).getNotifyBodyType();
    HColumn<String, Object> resourceSubs = queryResourceSubs(context.getSubscribee(), notifyBodyType);
    List<SubscriptionID> idList = null;
    if (resourceSubs != null) {
      idList = (List<SubscriptionID>) resourceSubs.getValue();
      idList.remove((SubscriptionID) context.getId());
    }
    else {
      return;
    }
    Mutator<String> mutator = getStringMutator();
    mutator.addInsertion(context.getSubscribee(), RESOURCE_SUB_COLUMN_FAMILY_NAME, HFactory.createColumn(notifyBodyType, idList, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }

  public SubscriptionContext getSubscription(SubscriptionID subId) {
    HColumn<String, Object> result = querySubscription(subId);
    if (result == null) {
      return null;
    }
    SubscriptionContext retv = (SubscriptionContext) result.getValue();
    triggerRetrieveListener(SubscriptionContext.class, retv);
    return retv;
  }
  
  private HColumn<String, Object> queryResourceSubs(String resourceKey, String notifybodyType) {
    return queryResultByStringKey(resourceKey, RESOURCE_SUB_COLUMN_FAMILY_NAME, notifybodyType);
  }
  
  private HColumn<String, Object> queryEventSoftState(String resourceKey, String entifyName) {
    return queryResultByStringKey(resourceKey, EVENT_SOFT_STATE_COLUMN_FAMILY_NAME, entifyName);
  }
  
  private HColumn<String, Object> queryNotifyBody(String resourceKey, String notifyBodyType) {
    return queryResultByStringKey(resourceKey, PRESENCE_NB_COLUMN_FAMILY_NAME, notifyBodyType);
  }
  
  private HColumn<String, Object> queryResource(String resourceKey, String eventName) {
    return queryResultByStringKey(resourceKey, RESOURCE_COLUMN_FAMILY_NAME, eventName);
  }
  
  private HColumn<String, Object> querySubscription(Object id) {
    return queryResultByObjectKey(id, SUBSCRIPTION_COLUMN_FAMILY_NAME, "value");
  }

  private HColumn<String, Object> queryResultByObjectKey(Object key, String columnFamily, String columnName) {
    ColumnQuery<Object, String, Object> createColumnQuery = HFactory.createColumnQuery(_keyspace, OBJECT_SERIALIZER, STRING_SERIALIZER, OBJECT_SERIALIZER);
    createColumnQuery.setColumnFamily(columnFamily);
    createColumnQuery.setKey(key);
    createColumnQuery.setName(columnName);
    
    QueryResult<HColumn<String, Object>> queryResult = createColumnQuery.execute();
    return queryResult.get();
  }
  
  private HColumn<String, Object> queryResultByStringKey(String key, String columnFamily, String columnName) {
    ColumnQuery<String, String, Object> createColumnQuery = HFactory.createColumnQuery(_keyspace, STRING_SERIALIZER, STRING_SERIALIZER, OBJECT_SERIALIZER);
    createColumnQuery.setColumnFamily(columnFamily);
    createColumnQuery.setKey(key);
    createColumnQuery.setName(columnName);
    
    QueryResult<HColumn<String, Object>> queryResult = createColumnQuery.execute();
    return queryResult.get();
  }
}
