package com.voxeo.moho.util;

import java.util.Collection;
import java.util.Map;

public class Checker {

  public static boolean isEmpty(final Collection<?> collection) {
    return collection == null || collection.size() == 0;
  }

  public static boolean isEmpty(final Map<?, ?> map) {
    return map == null || map.size() == 0;
  }

  public static boolean isEmpty(final Object[] array) {
    return array == null || array.length == 0;
  }

  public static boolean isEmpty(final String s) {
    return StringUtils.isEmpty(s);
  }

  public static boolean isEquals(final Object o1, final Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

}
