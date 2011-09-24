package com.voxeo.moho.presence.impl;

import com.voxeo.moho.presence.PresenceStore;

public class StoreHolder {
  
  private static ThreadLocal<PresenceStore> _presenceStores = new ThreadLocal<PresenceStore>();
  
  public static void setPresenceStore(PresenceStore store) {
    _presenceStores.set(store);
  }
  
  public static PresenceStore getPresenceStore() {
    return _presenceStores.get();
  }
}
