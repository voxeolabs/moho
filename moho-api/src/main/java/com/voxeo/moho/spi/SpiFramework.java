package com.voxeo.moho.spi;

import javax.servlet.http.HttpServlet;
import javax.servlet.sip.SipServlet;

import com.voxeo.moho.Application;
import com.voxeo.moho.Framework;
import com.voxeo.servlet.xmpp.XmppServlet;

public interface SpiFramework extends Framework {
  void registerDriver(String protocol, String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException;
  String[] getProtocolFamilies();
  String[] getEndpointSchemas();
  ProtocolDriver getDriverByProtocolFamily(String protocol);
  ProtocolDriver getDriverByEndpointSechma(String schema);
  Application getApplication();
  ExecutionContext getExecutionContext();
  SipServlet getSIPController();
  HttpServlet getHTTPController();
  XmppServlet getXMPPController();
}
