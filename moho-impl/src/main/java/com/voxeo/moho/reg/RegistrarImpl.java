package com.voxeo.moho.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.reg.RegisterEvent.Contact;
import com.voxeo.moho.reg.sip.SIPRegisterEventImpl.ContactImpl;
import com.voxeo.moho.sip.SIPRegisterEvent;
import com.voxeo.moho.sip.SIPRegisterEvent.SIPContact;

public class RegistrarImpl implements Registrar, Runnable {
  protected RegistrarStore _store;
  protected Collection<RegistrarController> _controllers = new ArrayList<RegistrarController>();
  protected int _maxExpiration = 60000;
  protected Properties _props;
  protected boolean _running = false;
  protected Thread _runner;

  @Override
  public void doRegister(RegisterEvent event) {
    if (event instanceof SIPRegisterEvent) {
      doSIPRegister((SIPRegisterEvent) event);
    }
    else {
      event.reject(Reason.DECLINE);
    }
  }

  protected boolean isResponsibleFor(SIPRegisterEvent event) {
    // TODO check domains.
    return true;
  }

  protected void doSIPRegister(SIPRegisterEvent event) {
    // TODO: validate Required header
    if (isResponsibleFor(event)) {
      _store.startTx();
      try {
        for (Contact contact : event.getContacts()) {
          if (contact.getExpiration() > _maxExpiration) {
            ((ContactImpl) contact).setExpiration(_maxExpiration);
          }
          if (!contact.isWildCard()) {
            Contact current = _store.getContact(event.getEndpoint(), contact.getEndpoint());
            if (current != null && current instanceof SIPContact) {
              validateContact((SIPContact) contact, (SIPContact) current);
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
      catch (Throwable t) {
        _store.rollbackTx();
        event.reject(Reason.ERROR);
      }
    }
    else {
      // TODO proxy
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

  @Override
  public void run() {
    while (_running) {
      Iterator<Endpoint> i = _store.getEndpoints();
      Endpoint ep = null;
      while ((ep = i.next()) != null && _running) {
        try {
          _store.startTx();
          Collection<Contact> contacts = _store.getContacts(ep);
          for (Contact contact : contacts) {
            if (contact.isExpired()) {
              _store.remove(ep, contact);
            }
          }
          _store.commitTx();
        }
        catch (Throwable t) {
          _store.rollbackTx();
        }
        Thread.yield();
      }
      try {
        Thread.sleep(_maxExpiration);
      }
      catch (Exception e) {
        // ignore
      }
    }
  }

  @Override
  public void init(Properties props) {
    _props = props;

    String storeImpl = props.getProperty(STORE_IMPL);
    if (storeImpl == null) {
      storeImpl = MemoryRegistrarStore.class.getName();
    }
    try {
      _store = (RegistrarStore) Class.forName(storeImpl).newInstance();
      _store.init(props);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalidate Registrar Store implementation: " + e);
    }

    String max = props.getProperty(MAX_EXPIRE);
    if (max != null) {
      this._maxExpiration = Integer.parseInt(max);
    }

    // TODO: get all the domains.

    _running = true;
    _runner = new Thread(this, "Registrar");
    _runner.start();
  }

  @Override
  public void destroy() {
    _running = false;
    _runner.interrupt();
    _store.destroy();
  }

}
