package com.voxeo.moho;

public class Configuration {
  private static boolean _earlyMediaWithout100rel = false;

  public static boolean isEarlyMediaWithout100rel() {
    return _earlyMediaWithout100rel;
  }

  public static void setEarlyMediaWithout100rel(boolean earlyMediaWithout100rel) {
    _earlyMediaWithout100rel = earlyMediaWithout100rel;
  }
}
