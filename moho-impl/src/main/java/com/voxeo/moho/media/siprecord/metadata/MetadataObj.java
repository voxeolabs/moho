package com.voxeo.moho.media.siprecord.metadata;

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.voxeo.moho.utils.Identifiable;

public class MetadataObj implements Identifiable<String> {
  private static final Logger LOG = Logger.getLogger(MetadataObj.class);

  protected String id;

  public MetadataObj() {
    super();
    try {
      String uuid = UUID.randomUUID().toString();
      id = new String(Base64.encodeBase64(uuid.getBytes("iso8859-1")), "iso8859-1");
    }
    catch (Exception ex) {
      LOG.error("Exception when generating ID.", ex);
    }
  }

  @Override
  public String getId() {
    return id;
  }

}
