package com.voxeo.moho.reg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.reg.RegisterEvent.Contact;

public class MemoryRegistrarStore implements RegistrarStore {
  HashMap<Endpoint, HashMap<Endpoint, Contact>> _tables = new HashMap<Endpoint, HashMap<Endpoint, Contact>>(); 
  ThreadLocal<Tx> _tx;
  
  class Tx{
    HashMap<Endpoint, HashMap<Endpoint, Contact>> _mods;
    
    Tx() {
      _mods = new HashMap<Endpoint, HashMap<Endpoint, Contact>>();
    }
    
    synchronized void commit() {
      synchronized(_tables) {
        for(Endpoint ep : _mods.keySet()) {
          HashMap<Endpoint, Contact> news = _mods.get(ep);
          HashMap<Endpoint, Contact> olds = _tables.get(ep);
          if (olds != null) {
            olds.clear();
            for(Endpoint epp : news.keySet()) {
              olds.put(epp, news.get(epp));
            }
          }
          else {
            _tables.put(ep, news);
          }
        }
        _mods.clear();
      }
    }
    
    synchronized void rollback() {
      _mods.clear();
    }
    
    synchronized void addContact(Endpoint addr, Contact contact) {
      Map<Endpoint, Contact> contacts = get(addr);
      contacts.put(contact.getEndpoint(), contact);
    }
    
    synchronized void updateContact(Endpoint addr, Contact contact) {
      Map<Endpoint, Contact> contacts = get(addr);
      contacts.put(contact.getEndpoint(), contact);
    }
    
    synchronized void removeContact(Endpoint addr, Contact contact) {
      Map<Endpoint, Contact> contacts = get(addr);
      contacts.remove(contact.getEndpoint());
    }
    
    synchronized void removeContacts(Endpoint addr) {
      Map<Endpoint, Contact> contacts = get(addr);
      contacts.clear();
    }
    
    synchronized Collection<Contact> getContacts(Endpoint addr) {
      Map<Endpoint, Contact> contacts = get(addr);
      return contacts.values();
    }
    
    synchronized Contact getContact(Endpoint aor, Endpoint addr) {
      Map<Endpoint, Contact> contacts = get(addr);
      return contacts.get(addr);
    }

    @SuppressWarnings("unchecked")
    synchronized Map<Endpoint, Contact> get(Endpoint endpoint) {
      HashMap<Endpoint, Contact> contacts = _mods.get(endpoint);
      if (contacts == null) {
        contacts = new HashMap<Endpoint, Contact>();
        synchronized(_tables) {
          HashMap<Endpoint, Contact> existings = _tables.get(endpoint);
          if (existings != null) {
            contacts = (HashMap<Endpoint, Contact>)existings.clone();
          }
          else {
            contacts = existings;
          }
        }
        _mods.put(endpoint, contacts);
      }
      return contacts;
    }
  }
  
  @Override
  public void startTx() {
    if (_tx.get() != null) {
      throw new IllegalStateException("There is already an open transaction on this thread[" + Thread.currentThread()+"]");
    }
    _tx.set(new Tx());
  }

  @Override
  public void commitTx() {
    getTx().commit();
  }

  @Override
  public void rollbackTx() {
    getTx().rollback();
  }

  Tx getTx()  {
    Tx tx = _tx.get();
    if (tx == null) {
      throw new IllegalStateException("No transaction has been started on this thread[" + Thread.currentThread()+"]");
    }
    return tx;
  }
  
  @Override
  public void add(Endpoint addr, Contact contact) {
    getTx().addContact(addr, contact);
  }

  @Override
  public void update(Endpoint addr, Contact contact) {
    getTx().updateContact(addr, contact);
  }

  @Override
  public void remove(Endpoint addr, Contact contact) {
    getTx().removeContact(addr, contact);
  }

  @Override
  public void remove(Endpoint endpoint) {
    getTx().removeContacts(endpoint);
  }

  @Override
  public Collection<Contact> getContacts(Endpoint endpoint) {
    return getTx().getContacts(endpoint);
  }
  
  @Override
  public Contact getContact(Endpoint aor, Endpoint addr) {
    return getTx().getContact(aor, addr);
  }

  @Override
  public Iterator<Endpoint> getEndpoints() {
    synchronized(_tables) {
      Set<Endpoint> keys = _tables.keySet();
      Set<Endpoint> copies = new HashSet<Endpoint>();
      copies.addAll(keys);
      return copies.iterator();
    }
  }

  @Override
  public boolean isExisting(Endpoint addr, Contact contact) {
     synchronized(_tables) {
       Map<Endpoint, Contact> contacts = _tables.get(addr);
       if (contacts == null) {
         return false;
       }
       Contact c = contacts.get(contact.getEndpoint());
       if (c == null) {
         return false;
       }
       return true;
     }
  }

  @Override
  public boolean isExisting(Endpoint endpoint) {
    synchronized(_tables) {
      Map<Endpoint, Contact> contacts = _tables.get(endpoint);
      if (contacts == null) {
        return false;
      }
      return true;
    }
  }

  @Override
  public void init(Map<String, String> props) {
  }

  @Override
  public void destroy() {
  }
}
