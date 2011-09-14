package com.voxeo.moho.reg.impl.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.RegisterEvent.Contact;


public class CassandraRegisterStoreTest extends BaseEmbededServerSetupTest {
  
  private static CassandraRegisterStore _cassandraRegisterStore;
  
  @Before
  public void setUp() throws Exception {
    _cassandraRegisterStore = new CassandraRegisterStore();
    HashMap<String, String> config = new HashMap<String, String>();
    config.put("databaseAddress", "localhost:9170");
    _cassandraRegisterStore.init(config);
  }
  
  @Test
  public void testGetContact() {
    MockEndPoint endPoint1 = new MockEndPoint("sip:test1@voxeo.com");
    MockEndPoint endPoint2 = new MockEndPoint("sip:Alex@voxeo.com");
    MockContact mockContact1 = new MockContact("sip:test1@127.0.0.1");
    MockContact mockContact3 = new MockContact("sip:test1@172.21.0.191");
    MockContact mockContact2 = new MockContact("sip:alex@172.21.0.193");

    _cassandraRegisterStore.add(endPoint1, mockContact1);
    _cassandraRegisterStore.add(endPoint1, mockContact3);
    _cassandraRegisterStore.add(endPoint2, mockContact2);

    Contact contact = _cassandraRegisterStore.getContact(endPoint1, new MockEndPoint("sip:test1@127.0.0.1"));
    assertNotNull(contact);
    assertEquals("sip:test1@127.0.0.1", contact.getEndpoint().getURI().toString());
    
    Collection<Contact> contacts = _cassandraRegisterStore.getContacts(endPoint1);
    assertNotNull(contacts);
    assertTrue(contacts.size() == 2);
    Iterator<Contact> iterator = contacts.iterator();
    assertEquals("sip:test1@127.0.0.1", iterator.next().getEndpoint().getURI().toString());
    assertEquals("sip:test1@172.21.0.191", iterator.next().getEndpoint().getURI().toString());
    
    contact = _cassandraRegisterStore.getContact(endPoint2, new MockEndPoint("sip:alex@172.21.0.193"));
    assertNotNull(contact);
    assertEquals("sip:alex@172.21.0.193", contact.getEndpoint().getURI().toString());
  }
  
  @Test
  public void testIsExisting() {
    MockEndPoint endPoint1 = new MockEndPoint("sip:test1@voxeo.com");
    MockEndPoint endPoint2 = new MockEndPoint("sip:Alex@voxeo.com");
    MockContact mockContact1 = new MockContact("sip:test1@127.0.0.1");
    MockContact mockContact3 = new MockContact("sip:test1@172.21.0.191");
    MockContact mockContact2 = new MockContact("sip:alex@172.21.0.193");

    _cassandraRegisterStore.add(endPoint1, mockContact1);
    _cassandraRegisterStore.add(endPoint1, mockContact3);
    _cassandraRegisterStore.add(endPoint2, mockContact2);
    
    assertTrue(_cassandraRegisterStore.isExisting(endPoint1, mockContact1));
    assertTrue(_cassandraRegisterStore.isExisting(endPoint1));
  }
  
  @Test
  public void testRemoveContact() {
    MockEndPoint endPoint1 = new MockEndPoint("sip:test1@voxeo.com");
    MockEndPoint endPoint2 = new MockEndPoint("sip:Alex@voxeo.com");
    MockContact mockContact1 = new MockContact("sip:test1@127.0.0.1");
    MockContact mockContact3 = new MockContact("sip:test1@172.21.0.191");
    MockContact mockContact2 = new MockContact("sip:alex@172.21.0.193");

    _cassandraRegisterStore.add(endPoint1, mockContact1);
    _cassandraRegisterStore.add(endPoint1, mockContact3);
    _cassandraRegisterStore.add(endPoint2, mockContact2);
    
    _cassandraRegisterStore.remove(endPoint1, mockContact1);
    assertFalse(_cassandraRegisterStore.isExisting(endPoint1, mockContact1));
    
    _cassandraRegisterStore.remove(endPoint2);
    assertTrue(_cassandraRegisterStore.getContacts(endPoint2).size() == 0);
  }
  
  @After
  public void tearDown() throws Exception {
    _cassandraRegisterStore.destroy();
    _cassandraRegisterStore = null;
  }
  
  public static class MockEndPoint implements Endpoint {
    URI _uri;
    public MockEndPoint(String uri) {
      _uri = URI.create(uri); 
    }

    @Override
    public String getName() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public URI getURI() {
      return _uri;
    }
  }
  
  public static class MockContact implements Contact {
    private static final long serialVersionUID = 8884928460090599577L;
    String _uri;
    public MockContact(String uri) {
      _uri = uri;
    }
    @Override
    public Endpoint getEndpoint() {
      return new MockEndPoint(_uri);
    }

    @Override
    public int getExpiration() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public boolean isWildCard() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isExpired() {
      // TODO Auto-generated method stub
      return false;
    }
    
  }
}
