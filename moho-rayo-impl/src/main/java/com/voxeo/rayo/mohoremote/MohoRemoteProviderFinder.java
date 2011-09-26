package com.voxeo.rayo.mohoremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

class MohoRemoteProviderFinder {
  protected static final Logger LOG = Logger.getLogger(MohoRemoteProviderFinder.class);

  @SuppressWarnings("rawtypes")
  public static Object findJarServiceProvider(String factoryId) throws Exception {

    String serviceId = "META-INF/services/" + factoryId;
    InputStream is = null;

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null) {
      is = cl.getResourceAsStream(serviceId);

      if (is == null) {
        cl = MohoRemoteProviderFinder.class.getClassLoader();
        is = cl.getResourceAsStream(serviceId);
      }
    }
    else {
      cl = MohoRemoteProviderFinder.class.getClassLoader();
      is = cl.getResourceAsStream(serviceId);
    }

    if (is == null) {
      // No provider found
      LOG.error("can't find provider " + serviceId);
      return null;
    }

    LOG.info("found jar resource=" + serviceId);

    BufferedReader rd;
    try {
      rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    }
    catch (java.io.UnsupportedEncodingException e) {
      rd = new BufferedReader(new InputStreamReader(is));
    }

    String factoryClassName = null;
    try {
      factoryClassName = rd.readLine();
      rd.close();
    }
    catch (IOException x) {
      LOG.error("Got exception when looking for provider " + serviceId, x);
      return null;
    }

    if (factoryClassName != null && !"".equals(factoryClassName)) {
      LOG.info("found provider in resource, value=" + factoryClassName);

      Class providerClass = cl.loadClass(factoryClassName);
      Object instance = providerClass.newInstance();

      return instance;
    }

    // No provider found
    return null;
  }
}
