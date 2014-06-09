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

import java.io.File;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.voxeo.moho.common.event.DispatchableEventSource;
import com.voxeo.moho.common.util.NetworkUtils;
import com.voxeo.moho.common.util.Utils.DaemonThreadFactory;
import com.voxeo.moho.conference.ConferenceDriverImpl;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.remote.network.RemoteCommunicationImpl;
import com.voxeo.moho.services.Service;
import com.voxeo.moho.sip.RemoteParticipantImpl;
import com.voxeo.moho.sip.SIPDriverImpl;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.common.util.InheritLogContextThreadPoolExecutor;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.moho.util.SDPUtils;
import com.voxeo.moho.utils.EventListener;
import com.voxeo.moho.voicexml.VoiceXMLDriverImpl;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;

public class ApplicationContextImpl extends DispatchableEventSource implements ExecutionContext, SpiFramework {

  private static final Logger LOG = Logger.getLogger(ApplicationContextImpl.class);

  protected Map<String, ProtocolDriver> _driversByProtocol = new HashMap<String, ProtocolDriver>();

  protected Map<String, ProtocolDriver> _driversBySchema = new HashMap<String, ProtocolDriver>();

  protected SipServlet _sip;

  protected HttpServlet _http;

  protected XmppServlet _xmpp;

  protected Application _application;

  protected MsControlFactory _mcFactory;

  protected MediaServiceFactory _msFactory;

  protected ConferenceManager _confMgr;

  protected SipFactory _sipFactory;

  protected SdpFactory _sdpFactory;

  protected XmppFactory _xmppFactory;

  protected Map<String, String> _parameters = new ConcurrentHashMap<String, String>();

  protected ServletContext _servletContext;

  protected InheritLogContextThreadPoolExecutor _executor;
  
  protected ScheduledThreadPoolExecutor _scheduledEcutor;

  protected org.springframework.context.support.AbstractApplicationContext _springContext;

  protected org.springframework.context.support.AbstractApplicationContext _appSpringContext;

  protected Map<String, Participant> _participants = new ConcurrentHashMap<String, Participant>();

  // remote join
  protected Registry _registry = null;

  protected RemoteCommunicationImpl _remoteCommunication;

  protected String _remoteCommunicationRMIAddress;

  protected int _remoteCommunicationPort = 4231;

  protected String _remoteCommunicationAddress = NetworkUtils.getLocalAddress().toString();

  protected String _remoteObject;

  protected String _schema = "moho";

  protected Map<String, Mixer> _mixerNameMap = new ConcurrentHashMap<String, Mixer>();

  private MediaDialect _dialect;

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
    _xmppFactory = (XmppFactory) _servletContext.getAttribute(XmppServlet.XMPP_FACTORY);

    String serviceContextFilePath = "WEB-INF/service-context.xml";

    final Enumeration<String> e = servlet.getInitParameterNames();
    while (e.hasMoreElements()) {
      final String name = e.nextElement();
      final String value = servlet.getInitParameter(name);
      setParameter(name, value);

      if (name.equalsIgnoreCase("service-context-file")) {
        serviceContextFilePath = value;
      }
    }

    try {
      String realPath = servlet.getServletContext().getRealPath(serviceContextFilePath);
      File file = new File(realPath);

      if (file.exists()) {
        _appSpringContext = new FileSystemXmlApplicationContext("file:" + realPath);
      }
    }
    catch (Exception ex) {
      LOG.warn("Error when loading service-context-file at:" + serviceContextFilePath, ex);
    }

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

    getServletContext().setAttribute(ApplicationContext.APPLICATION, app);
    getServletContext().setAttribute(ApplicationContext.APPLICATION_CONTEXT, this);
    getServletContext().setAttribute(ApplicationContext.FRAMEWORK, this);

    if (app instanceof EventListener<?>) {
      addListener((EventListener<?>) app);
    }
    else {
      addObserver(app);
    }

    Class<? extends MediaDialect> mediaDialectClass = com.voxeo.moho.media.dialect.GenericDialect.class;
    final String mediaDialectClassName = getParameters().get("mediaDialectClass");
    try {
      if (mediaDialectClassName != null) {
        mediaDialectClass = (Class<? extends MediaDialect>) Class.forName(mediaDialectClassName);
      }
      _dialect = mediaDialectClass.newInstance();
      LOG.info("Moho is creating media service with dialect (" + _dialect + ").");
    }
    catch (Exception ex) {
      LOG.error("Moho is unable to create media dialect (" + mediaDialectClassName + ")", ex);
    }

    int eventDispatcherCorePoolSize = getParameterValue("eventDispatcherThreadPoolSize", 50);
    int eventDispatcherMaxPoolSize = getParameterValue("eventDispatcherMaxThreadPoolSize", Integer.MAX_VALUE);
    int eventDispatcherThreadTimeout = getParameterValue("eventDispatcherThreadTimeout", 60);
    
    
    LOG.info("Moho is creating event dispatcher with " + eventDispatcherCorePoolSize + " core threads. Max threads: " 
    		+ eventDispatcherMaxPoolSize + ". Thread timeout: " + eventDispatcherThreadTimeout);
    _executor = new InheritLogContextThreadPoolExecutor(eventDispatcherCorePoolSize, eventDispatcherMaxPoolSize, eventDispatcherThreadTimeout, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
    
    _scheduledEcutor = new ScheduledThreadPoolExecutor(10, new DaemonThreadFactory("MohoContext"));
    _dispatcher.setExecutor(_executor, false);

    _springContext = new ClassPathXmlApplicationContext("classpath:moho-service-context.xml");
    Collection<Service> beans = null;
    if (_appSpringContext != null) {
      beans = _appSpringContext.getBeansOfType(Service.class).values();
      for (Service service : beans) {
        try {
          service.init(this, getParameters());
        }
        catch (Exception e1) {
          LOG.error("Error when initialize service" + service, e1);
        }
      }
    }

    beans = _springContext.getBeansOfType(Service.class).values();
    for (Service service : beans) {
      try {
        service.init(this, getParameters());
      }
      catch (Exception e1) {
        LOG.error("Error when initialize service" + service, e1);
      }
    }

    _msFactory = this.getService(MediaServiceFactory.class);
    _confMgr = this.getService(ConferenceManager.class);
    
    if (getParameter("remoteCommunicationAddress") != null) {
      _remoteCommunicationAddress = getParameter("remoteCommunicationAddress");
      LOG.debug("Using remoteCommunicationAddress configuration:" + getParameter("remoteCommunicationAddress"));
    }
    else {
      if (_remoteCommunicationAddress.startsWith("/")) {
        _remoteCommunicationAddress = _remoteCommunicationAddress.substring(1);
      }
      LOG.debug("No remoteCommunicationAddress configuration, using the default:" + _remoteCommunicationAddress);
    }

    SDPUtils.init(this);
//    try {
//      if (getParameter("remoteCommunicationPort") != null) {
//        try {
//          _remoteCommunicationPort = Integer.valueOf(getParameter("remoteCommunicationPort"));
//        }
//        catch (NumberFormatException ex) {
//          LOG.warn("Wrong remoteCommunicationPort configuration:" + getParameter("remoteCommunicationPort")
//              + ", using the default:" + _remoteCommunicationPort);
//        }
//      }
//
//      if (getParameter("remoteCommunicationAddress") != null) {
//        _remoteCommunicationAddress = getParameter("remoteCommunicationAddress");
//        LOG.debug("Using remoteCommunicationAddress configuration:" + getParameter("remoteCommunicationAddress"));
//      }
//      else {
//        LOG.debug("No remoteCommunicationAddress configuration, using the default:" + _remoteCommunicationAddress);
//      }
//
//      if (_remoteCommunicationAddress.startsWith("/")) {
//        _remoteCommunicationAddress = _remoteCommunicationAddress.substring(1);
//      }
//      _remoteObject = "RemoteCommunication";
//      _remoteCommunication = new RemoteCommunicationImpl(this);
//      _registry = LocateRegistry.createRegistry(4231);
//      RemoteCommunication stub = (RemoteCommunication) UnicastRemoteObject.exportObject(_remoteCommunication, 0);
//      _registry.rebind(_remoteObject, stub);
//      _remoteCommunicationRMIAddress = "rmi://" + _remoteCommunicationAddress + ":" + _remoteCommunicationPort + "/"
//          + _remoteObject;
//    }
//    catch (RemoteException ex) {
//      LOG.error("Error when initialize remote communication", ex);
//    }
  }

private int getParameterValue(String param, int defaultValue) {
	
	int intValue = defaultValue;
    final String paramValue = getParameter(param);
    if (paramValue != null) {
      intValue = Integer.valueOf(paramValue);
    }
	return intValue;
}

  @Override
  public Application getApplication() {
    return _application;
  }

  private Endpoint getEndpoint(final String addr, String type) {
    if (addr == null) {
      throw new IllegalArgumentException("argument is null");
    }
    if(!addr.contains(":")){
      throw new IllegalArgumentException("Address must be in the form of URL or <URL>. [" + addr + "] is an invalid address");
    }
    String schema = addr.split(":")[0];
    if (schema == null || schema.trim().length() == 0) {
      throw new IllegalArgumentException("Address not prepended with schema. [" + addr + "]");
    }
    ProtocolDriver driver = getDriverByEndpointSechma(schema);
    if (driver == null) {
      throw new IllegalArgumentException("No suitable driver for this address format. [" + addr  + "]");
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
    Participant p = _participants.get(cid);
    if (p != null && p instanceof Call) {
      return (Call) p;
    }
    return null;
  }

  @Override
  public void addCall(final Call call) {
    _participants.put(call.getId(), call);
  }

  @Override
  public void removeCall(final String id) {
    _participants.remove(id);
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

    Collection<ProtocolDriver> drivers = _driversByProtocol.values();
    for (ProtocolDriver driver : drivers) {
      driver.destroy();
    }

    Collection<Service> beans = _springContext.getBeansOfType(Service.class).values();
    for (Service service : beans) {
      service.destroy();
    }
    

//    try {
//      UnicastRemoteObject.unexportObject(_remoteCommunication, true);
//      UnicastRemoteObject.unexportObject(_registry, true);
//    }
//    catch (NoSuchObjectException e) {
//      LOG.warn("", e);
//    }
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
  public void registerDriver(String protocol, String className) throws ClassNotFoundException, InstantiationException,
      IllegalAccessException {
    ProtocolDriver driver = createProvider(className);
    registerDriver(driver);
  }

  protected void registerDriver(ProtocolDriver driver) {
    String protocol = driver.getProtocolFamily();
    _driversByProtocol.put(protocol, driver);
    for (String schema : driver.getEndpointSchemas()) {
      _driversBySchema.put(schema, driver);
    }
  }

  @SuppressWarnings("rawtypes")
  private ProtocolDriver createProvider(String name) throws ClassNotFoundException, InstantiationException,
      IllegalAccessException {
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

  @Override
  public <T extends Service> T getService(Class<T> def) {
    return find(def);
  }

  private <T> T find(Class<T> clazz) {
    Collection<T> beans = null;

    if (_appSpringContext != null) {
      beans = _appSpringContext.getBeansOfType(clazz).values();

      if (!beans.isEmpty()) {
        return beans.iterator().next();
      }
    }

    beans = _springContext.getBeansOfType(clazz).values();

    if (!beans.isEmpty()) {
      return beans.iterator().next();
    }

    return null;
  }

  @Override
  public <T extends Service> Collection<T> listServices() {
    Collection<T> ret = new ArrayList<T>();

    if (_appSpringContext != null) {
      ret.addAll((Collection<T>) _appSpringContext.getBeansOfType(Service.class).values());
    }

    ret.addAll((Collection<T>) _springContext.getBeansOfType(Service.class).values());

    return ret;
  }

  @Override
  public <T extends Service> boolean containsService(Class<T> def) {
    return this.find(def) != null;
  }

  @Override
  public XmppServlet getXMPPController() {
    return _xmpp;
  }

  public void setXMPPController(XmppServlet xmpp) {
    _xmpp = xmpp;
  }

  @Override
  public XmppFactory getXmppFactory() {
    return _xmppFactory;
  }

  @Override
  public Participant getParticipant(String id) {
    String ip = ParticipantIDParser.getIpAddress(id);
    if (ip.equalsIgnoreCase(_remoteCommunicationAddress)) {
      return _participants.get(id);
    }
    else {
      return new RemoteParticipantImpl(this, id);
    }
  }

  public void addParticipant(Participant participant) {
    _participants.put(participant.getId(), participant);

    if (participant instanceof Mixer) {
      String name = ((Mixer) participant).getName();
      if (name != null) {
        _mixerNameMap.put(name, ((Mixer) participant));
      }
    }
  }

  public void removeParticipant(String id) {
    Participant participant = _participants.remove(id);

    if (participant instanceof Mixer) {
      String name = ((Mixer) participant).getName();
      if (name != null) {
        _mixerNameMap.remove(name);
      }
    }
  }

  public Mixer getMixerByName(String name) {
    return _mixerNameMap.get(name);
  }

  public String generateID(String type, String id) {
    // TODO ADDRESS
    String compsitePart = null;
    if(_remoteCommunicationAddress.startsWith("/")){
      compsitePart = ":/" + _remoteCommunicationAddress;
    }
    else{
      compsitePart = "://" + _remoteCommunicationAddress;
    }
    String remoteAddress = _schema + compsitePart + ":" + _remoteCommunicationPort + "/" + type
        + "/" + id;
    return remoteAddress;
  }

  public RemoteCommunicationImpl getRemoteCommunication() {
    return _remoteCommunication;
  }

  public String getRemoteCommunicationRMIAddress() {
    return _remoteCommunicationRMIAddress;
  }

  public int getRemoteCommunicationPort() {
    return _remoteCommunicationPort;
  }

  public String getRemoteCommunicationAddress() {
    return _remoteCommunicationAddress;
  }

  public MediaDialect getDialect() {
    return _dialect;
  }

  public ScheduledThreadPoolExecutor getScheduledEcutor() {
    return _scheduledEcutor;
  }
}
