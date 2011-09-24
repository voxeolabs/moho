package com.voxeo.moho.presence;

public interface StoreRetrieveListener<T> {
  void onRetrieve(T resource);
}
