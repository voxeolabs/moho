package com.voxeo.moho.textchannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextChannels {

  protected static Map<String, TextChannelProvider> providers = new ConcurrentHashMap<String, TextChannelProvider>();

  public static void registerProvider(TextChannelProvider provider) {
    providers.put(provider.getType(), provider);
  }

  public static void unRegisterProvider(String type) {
    providers.remove(type);
  }

  public static TextChannelProvider getProvider(String type) {
    return providers.get(type);
  }
}
