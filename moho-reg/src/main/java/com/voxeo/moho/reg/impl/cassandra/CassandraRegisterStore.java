package com.voxeo.moho.reg.impl.cassandra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
import me.prettyprint.hector.api.query.CountQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.RegisterEvent.Contact;

public class CassandraRegisterStore extends NoSqlDatabaseRegisterStore {
  
  private static final ObjectSerializer OBJECT_SERIALIZER = ObjectSerializer.get();
  
  private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();
  
  private static final String DEFAULT_DATABASE_ADDRESS = "localhost:9160";
  
  private static final String DEFAULT_CLUSTER_NAME = "RegistrarCluster";
  
  private static final String DEFAULT_KEYSPACE_NAME = "Registrar";
  
  private static final String DEFAULT_COLUMN_FAMILY_NAME = "Bindings";

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
  public void add(Endpoint addr, Contact contact) {
    insertContact(addr, contact);
  }

  @Override
  public void update(Endpoint addr, Contact contact) {
    insertContact(addr, contact);
  }
  
  private Mutator<String> getMutator() {
    return HFactory.createMutator(_keyspace, STRING_SERIALIZER);
  }

  private void insertContact(Endpoint addr, Contact contact) {
    Mutator<String> mutator = getMutator();
    mutator.addInsertion(getCleanUri(addr), DEFAULT_COLUMN_FAMILY_NAME, HFactory.createColumn(contact.getEndpoint().getURI().toString(), contact, STRING_SERIALIZER, OBJECT_SERIALIZER));
    mutator.execute();
  }
  
  @Override
  public void remove(Endpoint addr, Contact contact) {
    Mutator<String> mutator = getMutator();
    mutator.addDeletion(getCleanUri(addr), DEFAULT_COLUMN_FAMILY_NAME, contact.getEndpoint().getURI().toString(), STRING_SERIALIZER);
    mutator.execute();
  }

  @Override
  public void remove(Endpoint addr) {
    Mutator<String> mutator = getMutator();
    mutator.addDeletion(getCleanUri(addr), DEFAULT_COLUMN_FAMILY_NAME);
    mutator.execute();
  }

  @Override
  public Collection<Contact> getContacts(Endpoint addr) {
    Collection<Contact> contacts = new ArrayList<Contact>();
    List<HColumn<String, Object>> columns = querySliceResult(addr).getColumns();
    for (HColumn<String, Object> hcolumn : columns) {
      contacts.add((Contact) hcolumn.getValue());
    }
    return contacts;
  }

  @Override
  public Iterator<Endpoint> getEndpoints() {
    return null;
  }

  @Override
  public Contact getContact(Endpoint addr, Endpoint contact) {
    HColumn<String, Object> result = queryResult(addr, contact);
    if (result == null) {
      return null;
    }
    return (Contact) result.getValue();
  }

  @Override
  public boolean isExisting(Endpoint addr, Contact contact) {
//    CountQuery<String, Object> query = HFactory.createCountQuery(_keyspace, STRING_SERIALIZER, OBJECT_SERIALIZER);
//    query.setColumnFamily(DEFAULT_COLUMN_FAMILY_NAME);
//    query.setKey(getCleanUri(addr));
//    query.setRange(contact.getEndpoint().getURI().toString(), contact.getEndpoint().getURI().toString(), 1);
//    QueryResult<Integer> execute = query.execute();
//    return execute.get().intValue() > 0;
    return queryResult(addr, contact.getEndpoint()) != null;
  }
  
  private HColumn<String, Object> queryResult(Endpoint addr, Endpoint contact) {
    ColumnQuery<String, String, Object> createColumnQuery = HFactory.createColumnQuery(_keyspace, STRING_SERIALIZER, STRING_SERIALIZER, OBJECT_SERIALIZER);
    createColumnQuery.setColumnFamily(DEFAULT_COLUMN_FAMILY_NAME);
    createColumnQuery.setKey(getCleanUri(addr));
    createColumnQuery.setName(contact.getURI().toString());
    
    QueryResult<HColumn<String, Object>> queryResult = createColumnQuery.execute();
    return queryResult.get();
  }
  
  private String getCleanUri(Endpoint addr) {
    String aor = addr.getURI().toString();
    return deparameterize(aor);
  }

  private String deparameterize(String aor) {
    return aor;
  }

  @Override
  public boolean isExisting(Endpoint addr) {
    return querySliceResult(addr).getColumns().size() > 0;
  }
  
  private ColumnSlice<String, Object> querySliceResult(Endpoint addr) {
    SliceQuery<String, String, Object> createSliceQuery = HFactory.createSliceQuery(_keyspace, STRING_SERIALIZER, STRING_SERIALIZER, OBJECT_SERIALIZER);
    createSliceQuery.setColumnFamily(DEFAULT_COLUMN_FAMILY_NAME);
    createSliceQuery.setKey(getCleanUri(addr));
    createSliceQuery.setColumnNames("", "");
    createSliceQuery.setRange("", "", false, Integer.MAX_VALUE);
    QueryResult<ColumnSlice<String, Object>> execute = createSliceQuery.execute();
    ColumnSlice<String, Object> columnSlice = execute.get();
    return columnSlice;
  }

  @Override
  public void destroy() {
    _cluster.getConnectionManager().shutdown();
    HFactory.shutdownCluster(_cluster);
    _keyspace = null;
    _cluster = null;
  }
}
