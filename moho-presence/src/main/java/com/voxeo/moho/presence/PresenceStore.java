package com.voxeo.moho.presence;

import java.util.Map;

public interface PresenceStore {
  
  void init(Map<String, String> props);
  
  void startTx();
  
  void commitTx();
  
  void rollbackTx();
  
  void destroy();
  
  <T> void addRetrieveListener(Class<?> clazz, StoreRetrieveListener<T> listener);
  
  <T> void removeRetrieveListener(Class<?> clazz);
}
