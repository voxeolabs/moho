package com.voxeo.moho.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant;

public class JoinLockService {

  private static final Logger LOG = Logger.getLogger(JoinLockService.class);

  private static JoinLockService INSTANCE = new JoinLockService();

  protected final Map<String, LockImpl> _m = new HashMap<String, LockImpl>();

  public static JoinLockService getInstance() {
    return INSTANCE;
  }

  public synchronized Lock get(final Participant part, final Participant other) {
    final List<String> ids1 = new ArrayList<String>();
    ids1.add(part.getId());
    Participant[] joinees = part.getParticipants();
    if (joinees != null && joinees.length > 0) {
      for (Participant p : joinees) {
        ids1.add(p.getId());
      }
    }
    if (other != null) {
      ids1.add(other.getId());
      joinees = other.getParticipants();
      if (joinees != null && joinees.length > 0) {
        for (Participant p : joinees) {
          ids1.add(p.getId());
        }
      }
    }

    final List<String> ids2 = new ArrayList<String>();
    ids2.add(part.getId());
    if (other != null) {
      ids2.add(other.getId());
    }

    return get(ids1, ids2);
  }

  public synchronized Lock get(final Participant part) {
    return get(part, null);
  }

  protected synchronized Lock get(final List<String> ids1, final List<String> ids2) {
    Lock retval = null;

    // find the lock
    for (final String id : ids1) {
      if (_m.containsKey(id)) {
        retval = _m.get(id).getLock();
        break;
      }
    }

    // increase the lock counter
    LockImpl lock;
    for (final String id : ids2) {
      lock = _m.get(id);
      if (lock == null) {
        if (retval == null) {
          lock = new LockImpl();
          retval = lock.getLock();
        }
        else {
          lock = new LockImpl(retval);
        }
        _m.put(id, lock);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Added [" + id + ", " + lock + "]");
        }
      }
      else {
        lock.getCounter().incrementAndGet();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updated [" + id + ", " + lock + "]");
        }
      }
    }

    return retval;
  }

  public synchronized void remove(final String id) {
    // find the lock
    LockImpl lock = _m.get(id);

    // decrease the lock counter
    if (lock != null && lock.getCounter().decrementAndGet() < 1) {
      _m.remove(id);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Removed [" + id + ", " + lock + "]");
      }
    }
  }

  protected class LockImpl {

    private final Lock _lock;

    private final AtomicInteger _counter;

    public LockImpl() {
      _lock = new ReentrantLock();
      _counter = new AtomicInteger(1);
    }

    public LockImpl(final Lock lock) {
      _lock = lock;
      _counter = new AtomicInteger(1);
    }

    public Lock getLock() {
      return _lock;
    }

    public AtomicInteger getCounter() {
      return _counter;
    }

    @Override
    public String toString() {
      return _lock.toString() + "[" + _counter + "]";
    }
  }

}
