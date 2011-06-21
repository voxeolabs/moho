package com.voxeo.moho.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.RegisterEvent.Contact;
import com.voxeo.moho.sip.SIPRegisterEvent;
import com.voxeo.moho.sip.SIPRegisterEvent.SIPContact;

public class RegistrarImpl implements Registrar {
  protected RegistrarStore _store;
  protected Collection<RegistrarController> _controllers = new ArrayList<RegistrarController>();
  int _defaultExpiration = 60000;
  protected Properties _props;
  
  public RegistrarImpl(Properties props) {
    this(null, props);
  }
  
  public RegistrarImpl(RegistrarStore store, Properties props) {
    if (store == null) {
      store = new MemoryRegistrarStore();
    }
    _store = store;
    _props = props;
    //TODO
    // default expiration;
    // domains;
  }

  @Override
  public void doRegister(RegisterEvent event) {
    if (event instanceof SIPRegisterEvent) {
      doSIPRegister((SIPRegisterEvent)event);
    }
    else {
      event.reject(Reason.DECLINE);
    }
  }
  
  protected void doSIPRegister(SIPRegisterEvent event) {
    _store.startTx();
    try {
      for (Contact contact : event.getContacts()) {
        if (!contact.isWildCard()) {
          Contact current = _store.getContact(event.getEndpoint(), contact.getEndpoint());
          if (current != null && current instanceof SIPContact) {
            validateContact((SIPContact)contact, (SIPContact)current);
            if (contact.getExpiration() != 0) {
              _store.update(event.getEndpoint(), contact);
            }
            else {
              _store.remove(event.getEndpoint(), contact);
            }
          }
          else {
            _store.add(event.getEndpoint(), contact);
          }
        }
        else if (contact.getExpiration() == 0) {
          _store.remove(event.getEndpoint());
        }
      }
      _store.commitTx();
      event.accept();
    }
    catch(Throwable t) {
      _store.rollbackTx();
      event.reject(Reason.ERROR);
    }
  }
  
  protected void validateContact(SIPContact newC, SIPContact currentC) {
    if (newC.getCallID() != currentC.getCallID()) {
      throw new IllegalArgumentException("Same contact can not be registered with different Call-ID.");
    }
    if (newC.getCSeq() < currentC.getCSeq()) {
      throw new IllegalArgumentException("Same contact can not be registered out of sequence.");
    }
  }

  @Override
  public Collection<Contact> getContacts(Endpoint aor) {
    return _store.getContacts(aor);
  }

  @Override
  public void addController(RegistrarController controller) {
    _controllers.add(controller);
  }

  @Override
  public void removeController(RegistrarController controller) {
    _controllers.remove(controller);
  }

  @Override
  public Iterator<RegistrarController> getControllers() {
    return _controllers.iterator();
  }

}
