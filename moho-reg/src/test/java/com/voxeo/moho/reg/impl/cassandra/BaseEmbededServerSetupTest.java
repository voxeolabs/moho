package com.voxeo.moho.reg.impl.cassandra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.connection.HConnectionManager;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.testutils.EmbeddedServerHelper;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.KSMetaData;
import org.apache.cassandra.db.ColumnFamilyType;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.TimeUUIDType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.cassandra.thrift.IndexType;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Base class for test cases that need access to EmbeddedServerHelper
 *
 * @author Nate McCall (nate@vervewireless.com)
 *
 */
public abstract class BaseEmbededServerSetupTest {
  private static Logger log = LoggerFactory.getLogger(BaseEmbededServerSetupTest.class);
  private static EmbeddedServerHelper embedded;

  protected HConnectionManager connectionManager;
  protected CassandraHostConfigurator cassandraHostConfigurator;
  protected String clusterName = "TestCluster";

  /**
   * Set embedded cassandra up and spawn it in a new thread.
   *
   * @throws TTransportException
   * @throws IOException
   * @throws InterruptedException
   */
  @BeforeClass
  public static void setup() throws Exception {
    log.info("in setup of BaseEmbedded.Test");
    embedded = new EmbeddedServerHelper();
    loadSchema();
    embedded.setup();
  }

  protected static void loadSchema() {
    try {
      for (KSMetaData ksm : getSchemaDefinition()) {
        for (CFMetaData cfm : ksm.cfMetaData().values())
          CFMetaData.map(cfm);
        DatabaseDescriptor.setTableDefinition(ksm, DatabaseDescriptor
            .getDefsVersion());
      }
    } catch (ConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  protected static Collection<KSMetaData> getSchemaDefinition() {
    List<KSMetaData> schema = new ArrayList<KSMetaData>();

    // A whole bucket of shorthand
    String ks1 = "Registrar";

    Class<? extends AbstractReplicationStrategy> simple = SimpleStrategy.class;
    Map<String, String> opts = new HashMap<String, String>();
    opts.put("replication_factor", Integer.toString(1));
    
    ColumnFamilyType st = ColumnFamilyType.Standard;
    ColumnFamilyType su = ColumnFamilyType.Super;
    AbstractType bytes = BytesType.instance;
    
    List<AbstractType> subComparators = new ArrayList<AbstractType>();
    subComparators.add(BytesType.instance);
    subComparators.add(TimeUUIDType.instance);
    subComparators.add(IntegerType.instance);
    
    // Keyspace 1
    schema.add(new KSMetaData(
        ks1,
        simple,
        opts,

        // Column Families
        standardCFMD(ks1, "Bindings")));

    return schema;
  }

  protected static CFMetaData compositeCFMD(String ksName, String cfName, AbstractType... types) {
    try {
      return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard, CompositeType.getInstance(Arrays.asList(types)), null);      
    } catch (ConfigurationException e) {
      
    }
    return null;
  }
  
  protected static CFMetaData standardCFMD(String ksName, String cfName) {
    return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard,
        BytesType.instance, null).keyCacheSize(0);
  }

  protected static CFMetaData superCFMD(String ksName, String cfName,
      AbstractType subcc) {
    return new CFMetaData(ksName, cfName, ColumnFamilyType.Super,
        BytesType.instance, subcc).keyCacheSize(0);
  }

  protected static CFMetaData indexCFMD(String ksName, String cfName, final Boolean withIdxType)
  {
      return standardCFMD(ksName, cfName)
              .columnMetadata(new HashMap<ByteBuffer, ColumnDefinition>()
                  {{
                      ByteBuffer cName = ByteBuffer.wrap("birthyear".getBytes(Charsets.UTF_8));
                      IndexType keys = withIdxType ? IndexType.KEYS : null;
                      put(cName, new ColumnDefinition(cName, LongType.instance, keys, null));
                  }});
  }

  protected static CFMetaData jdbcCFMD(String ksName, String cfName,
      AbstractType comp) {
    return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard, comp, null)
        .defaultValidator(comp);
  }
  
  protected static CFMetaData cqlTestCf(String ksName, String cfName,
      AbstractType comp) {
    return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard, comp, null)
        .keyValidator(UTF8Type.instance).columnMetadata(new HashMap<ByteBuffer, ColumnDefinition>()
            {{
              ByteBuffer cName = ByteBuffer.wrap("birthyear".getBytes(Charsets.UTF_8));
              put(cName, new ColumnDefinition(cName, LongType.instance, null, null));
          }});
  }
  
  @AfterClass
  public static void teardown() throws Exception {
    embedded.teardown();
    embedded = null;
  }


  protected void setupClient() {
    cassandraHostConfigurator = new CassandraHostConfigurator("127.0.0.1:9170");
    connectionManager = new HConnectionManager(clusterName,cassandraHostConfigurator);
  }
}
