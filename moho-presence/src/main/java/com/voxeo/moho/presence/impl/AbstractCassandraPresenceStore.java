package com.voxeo.moho.presence.impl;

import java.util.Map;

import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public abstract class AbstractCassandraPresenceStore extends AbstractPresenceStore {

  protected static final ObjectSerializer OBJECT_SERIALIZER = ObjectSerializer.get();

  protected static final StringSerializer STRING_SERIALIZER = StringSerializer.get();

  protected static final String DEFAULT_DATABASE_ADDRESS = "localhost:9160";

  protected static final String DEFAULT_CLUSTER_NAME = "MohoCluster";

  protected Cluster _cluster;

  protected Keyspace _keyspace;

  @Override
  public void init(Map<String, String> props) {
    String databaseAddress = props.get("databaseAddress");
    String clusterName = props.get("clusterName");
    String keySapceName = props.get("keysapceName");

    _cluster = HFactory.getOrCreateCluster(clusterName != null ? clusterName : DEFAULT_CLUSTER_NAME,
        databaseAddress != null ? databaseAddress : DEFAULT_DATABASE_ADDRESS);
    _keyspace = HFactory.createKeyspace(keySapceName != null ? keySapceName : getDefaultKeyspaceName(), _cluster);
  }
  
  protected abstract String getDefaultKeyspaceName();

  @Override
  public void destroy() {
    _cluster.getConnectionManager().shutdown();
    HFactory.shutdownCluster(_cluster);
    _keyspace = null;
    _cluster = null;
  }

  protected HColumn<String, Object> queryResultByObjectKey(Object key, String columnFamily, String columnName) {
    ColumnQuery<Object, String, Object> createColumnQuery = HFactory.createColumnQuery(_keyspace, OBJECT_SERIALIZER,
        STRING_SERIALIZER, OBJECT_SERIALIZER);
    createColumnQuery.setColumnFamily(columnFamily);
    createColumnQuery.setKey(key);
    createColumnQuery.setName(columnName);

    QueryResult<HColumn<String, Object>> queryResult = createColumnQuery.execute();
    return queryResult.get();
  }

  protected HColumn<String, Object> queryResultByStringKey(String key, String columnFamily, String columnName) {
    ColumnQuery<String, String, Object> createColumnQuery = HFactory.createColumnQuery(_keyspace, STRING_SERIALIZER,
        STRING_SERIALIZER, OBJECT_SERIALIZER);
    createColumnQuery.setColumnFamily(columnFamily);
    createColumnQuery.setKey(key);
    createColumnQuery.setName(columnName);

    QueryResult<HColumn<String, Object>> queryResult = createColumnQuery.execute();
    return queryResult.get();
  }
  
  protected QueryResult<ColumnSlice<String, Object>> queryRowByStringKey(String key, String columnFamily) {
    SliceQuery<String, String, Object> createSliceQuery = HFactory.createSliceQuery(_keyspace, STRING_SERIALIZER, STRING_SERIALIZER, OBJECT_SERIALIZER);
    createSliceQuery.setColumnFamily(columnFamily);
    createSliceQuery.setKey(key);
    createSliceQuery.setRange(null, null, false, 100);
    return createSliceQuery.execute();
  }

  protected Mutator<String> getStringMutator() {
    return HFactory.createMutator(_keyspace, STRING_SERIALIZER);
  }

  protected Mutator<Object> getObjectMutator() {
    return HFactory.createMutator(_keyspace, OBJECT_SERIALIZER);
  }
}
