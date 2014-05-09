package com.voxeo.moho.media.siprecord.metadata;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhuwillie
 */
public class MetadataUtils {

  static AtomicLong labelValue = new AtomicLong(1);

  public static String generateLabelValue() {
    return String.valueOf(labelValue.getAndIncrement());
  }
}
