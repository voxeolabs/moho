package com.voxeo.moho.remote;

import org.apache.log4j.Logger;

@Deprecated
public abstract class MohoRemoteFactory {
  protected static final Logger LOG = Logger.getLogger(MohoRemoteFactory.class);

  public static String SERVICE_ID = "com.voxeo.rayo.mohoremote.MohoRemoteFactory";

  public static MohoRemoteFactory newInstance() {
    try {
      return (MohoRemoteFactory) MohoRemoteProviderFinder.findJarServiceProvider(SERVICE_ID);
    }
    catch (Exception ex) {
      LOG.error("Exception when looking for service provider " + SERVICE_ID, ex);
    }
    return null;
  }

  public abstract MohoRemote newMohoRemote();
}
