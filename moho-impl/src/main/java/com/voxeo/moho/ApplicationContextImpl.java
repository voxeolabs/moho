/**
 * Copyright 2010-2011 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;

import org.apache.log4j.Logger;

import com.voxeo.moho.conference.ConferenceDriverImpl;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.media.GenericMediaServiceFactory;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.sip.SIPDriverImpl;
import com.voxeo.moho.spi.ConferenceDriver;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.util.Utils.DaemonThreadFactory;
import com.voxeo.moho.utils.EventListener;
import com.voxeo.moho.voicexml.VoiceXMLDriverImpl;

public class ApplicationContextImpl extends DispatchableEventSource implements ExecutionContext, SpiFramework{

  private static final Logger LOG = Logger.getLogger(ApplicationContextImpl.class);
  
  protected Map<String, ProtocolDriver> _driversByProtocol = new HashMap<String, ProtocolDriver>();
  
  protected Map<String, ProtocolDriver> _driversBySchema = new HashMap<String, ProtocolDriver>();

  protected SipServlet _sip;

  protected HttpServlet _http;  
  
  protected Application _application;

  protected MsControlFactory _mcFactory;
  
  protected MediaServiceFactory _msFactory;

  protected ConferenceManager _confMgr;

  protected SipFactory _sipFactory;

  protected SdpFactory _sdpFactory;

  protected Map<String, Call> _calls = new ConcurrentHashMap<String, Call>();

  protected Map<String, String> _parameters = new ConcurrentHashMap<String, String>();

  protected ServletContext _servletContext;

  protected ThreadPoolExecutor _executor;

  @SuppressWarnings("unchecked")
  public ApplicationContextImpl(final Application app, final MsControlFactory mc, final SipServlet servlet) {
    super();
    _context = this;
    _application = app;
    _mcFactory = mc;
    _sip = servlet;
    _servletContext = _sip.getServletContext();
    _sipFactory = (SipFactory) _servletContext.getAttribute(SipServlet.SIP_FACTORY);
    _sdpFactory = (SdpFactory) _servletContext.getAttribute("javax.servlet.sdp.SdpFactory");
    final Enumeration<String> e = _servletContext.getInitParameterNames();
    while (e.hasMoreElements()) {
      final String name = e.nextElement();
      final String value = _servletContext.getInitParameter(name);
      setParameter(name, value);
    }
    Class<? extends MediaDialect> mediaDialectClass = com.voxeo.moho.media.dialect.GenericDialect.class;
    final String mediaDialectClassName = getParameter("mediaDialectClass");
    final MediaDialect mediaDialect = null;
    try {
      if (mediaDialectClassName != null) {
        mediaDialectClass = (Class<? extends MediaDialect>) Class.forName(mediaDialectClassName);
      }
      mediaDialectClass.newInstance();
    }
    catch(Exception ex) {
      LOG.error("Moho is unable to create media dialect (" + mediaDialectClassName + ")", ex);
    }
    LOG.info("Moho is creating media service with dialect (" + mediaDialect + ").");
    _msFactory = new GenericMediaServiceFactory(mediaDialect);

    try {
      registerDriver(ProtocolDriver.PROTOCOL_SIP, SIPDriverImpl.class.getName());
      registerDriver(ProtocolDriver.PROTOCOL_VXML, VoiceXMLDriverImpl.class.getName());
      registerDriver(ProtocolDriver.PROTOCOL_CONF, ConferenceDriverImpl.class.getName());
    }
    catch (Exception ex) {
      LOG.error("Moho is unable to register drivers: " + ex, ex);
    }
    for (String name : getProtocolFamilies()) {
      ProtocolDriver d = getDriverByProtocolFamily(name);
      LOG.info("Moho is initializing driver[" + d + "]");
      d.init(this);
    }
    
    ConferenceDriver cd = (ConferenceDriver)getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_CONF);
    setConferenceManager(cd.getManager());

    getServletContext().setAttribute(ApplicationContext.APPLICATION, app);
    getServletContext().setAttribute(ApplicationContext.APPLICATION_CONTEXT, this);
    getServletContext().setAttribute(ApplicationContext.FRAMEWORK, this);
    
    if (app instanceof EventListener<?>) {
      addListener((EventListener<?>)app);
    }
    else {
      addObserver(app);
    }
    
    int eventDispatcherThreadPoolSize = 50;
    final String eventDipatcherThreadPoolSizePara = getParameter("eventDispatcherThreadPoolSize");
    if (eventDipatcherThreadPoolSizePara != null) {
      eventDispatcherThreadPoolSize = Integer.valueOf(eventDipatcherThreadPoolSizePara);
    }
    LOG.info("Moho is creating event dispatcher with " + eventDispatcherThreadPoolSize + " threads.");
    _executor = new ThreadPoolExecutor(eventDispatcherThreadPoolSize, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
    _dispatcher.setExecutor(_executor, false);
  }

  @Override
  public Application getApplication() {
    return _application;
  }

  private Endpoint getEndpoint(final String addr, String type) {
    if (addr == null) {
      throw new IllegalArgumentException("argument is null");
    }
//      if (addr.startsWith("sip:") || addr.startsWith("sips:") || addr.startsWith("<sip:") || addr.startsWith("<sips:")) {
//        return new SIPEndpointImpl(this, _sipFactory.createAddress(addr));
//      }
//      else if (addr.startsWith("mscontrol://")) {
//        return new MixerEndpointImpl(this, addr);
//      }
//      else if (addr.startsWith("file://") || addr.startsWith("http://") || addr.startsWith("https://")
//          || addr.startsWith("ftp://")) {
//        return new VoiceXMLEndpointImpl(this, addr);
//      }
//      else if (addr.startsWith("tel:") || addr.startsWith("fax:") || addr.startsWith("<tel:")
//          || addr.startsWith("<fax:")) {
//        return new SIPEndpointImpl(this, _sipFactory.createAddress(addr));
//      }
//      else if (type != null && TextChannels.getProvider(type) != null) {
//        return TextChannels.getProvider(type).createEndpoint(addr, this);
//      }
      String schema = addr.split(":")[0];
      if (schema == null || schema.trim().length() == 0) {
        throw new IllegalArgumentException("Address must be in the form of URL or <URL>.");
      }
      ProtocolDriver driver = getDriverByEndpointSechma(schema);
      if (driver == null) {
        throw new IllegalArgumentException("No suitable driver for this address format.");
      }
      return driver.createEndpoint(addr);
  }

  @Override
  public Endpoint createEndpoint(String endpoint, String type) {
    return getEndpoint(endpoint, type);
  }

  @Override
  public Endpoint createEndpoint(String endpoint) {
    return getEndpoint(endpoint, null);
  }

  @Override
  public MsControlFactory getMSFactory() {
    return _mcFactory;
  }

  @Override
  public SipFactory getSipFactory() {
    return _sipFactory;
  }

  @Override
  public ConferenceManager getConferenceManager() {
    return _confMgr;
  }

  public void setConferenceManager(ConferenceManager conferenceManager) {
    _confMgr = conferenceManager;
  }

  @Override
  public Executor getExecutor() {
    return _executor;
  }

  @Override
  public Call getCall(final String cid) {
    return _calls.get(cid);
  }

  @Override
  public void addCall(final Call call) {
    _calls.put(call.getId(), call);
  }

  @Override
  public void removeCall(final String id) {
    _calls.remove(id);
  }

  @Override
  public String getParameter(final String name) {
    return _parameters.get(name);
  }

  @Override
  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(_parameters);
  }

  public void setParameter(final String name, final String value) {
    _parameters.put(name, value);
  }

  @Override
  public SdpFactory getSdpFactory() {
    return _sdpFactory;
  }

  @Override
  public ServletContext getServletContext() {
    return _servletContext;
  }

  @Override
  public String getRealPath(final String path) {
    return getServletContext().getRealPath(path);
  }

  @Override
  public void destroy() {
    getApplication().destroy();
    _executor.shutdown();
  }
  
  @Override
  public MediaServiceFactory getMediaServiceFactory() {
    return _msFactory;
  }

  @Override
  public SpiFramework getFramework() {
    return this;
  }

  @Override
  public void registerDriver(String protocol, String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    ProtocolDriver driver = createProvider(className);
    registerDriver(driver);
  }
  
  protected void registerDriver(ProtocolDriver driver) {
    String protocol = driver.getProtocolFamily();
    _driversByProtocol.put(protocol, driver);
    for(String schema : driver.getEndpointSchemas()) {
      _driversBySchema.put(schema, driver);
    }
  }
  
  @SuppressWarnings("rawtypes")
  private ProtocolDriver createProvider(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class clz = null;
    try {
      clz = this.getClass().getClassLoader().loadClass(name);
    }
    catch (final Throwable t) {
      clz = Thread.currentThread().getContextClassLoader().loadClass(name);
    }
    return (ProtocolDriver) clz.newInstance();
  }

  @Override
  public String[] getProtocolFamilies() {
    Set<String> s = _driversByProtocol.keySet();
    return s.toArray(new String[s.size()]);
  }

  @Override
  public String[] getEndpointSchemas() {
    Set<String> s = _driversBySchema.keySet();
    return s.toArray(new String[s.size()]);
  }

  @Override
  public ProtocolDriver getDriverByProtocolFamily(String protocol) {
    return _driversByProtocol.get(protocol);
  }

  @Override
  public ProtocolDriver getDriverByEndpointSechma(String schema) {
    return _driversBySchema.get(schema);
  }

  @Override
  public ExecutionContext getExecutionContext() {
    return _context;
  }

  @Override
  public SipServlet getSIPController() {
    return _sip;
  }
  
  public void setSIPController(SipServlet sip) {
    _sip = sip;
  }

  @Override
  public HttpServlet getHTTPController() {
    return _http;
  }
  
  public void setHTTPController(HttpServlet http) {
    _http = http;
  }

}
