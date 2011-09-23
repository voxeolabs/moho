package com.voxeo.moho.presence.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.StoreRetrieveListener;

public abstract class AbstractPresenceStore implements PresenceStore {
  
  private Map<Class<?>, StoreRetrieveListener<?>> _listeners = new ConcurrentHashMap<Class<?>, StoreRetrieveListener<?>>();

  @Override
  public <T> void addRetrieveListener(Class<?> clazz, StoreRetrieveListener<T> listener) {
    _listeners.put(clazz, listener);
  }

  @Override
  public <T> void removeRetrieveListener(Class<?> clazz) {
    _listeners.remove(clazz);
  }

  protected <T> void triggerRetrieveListener(Class<?> clazz, T resource) {
    @SuppressWarnings("unchecked")
    StoreRetrieveListener<T> retrieveListener = (StoreRetrieveListener<T>) _listeners.get(clazz);
    if (retrieveListener != null) {
      retrieveListener.onRetrieve(resource);
    }
  }
}
