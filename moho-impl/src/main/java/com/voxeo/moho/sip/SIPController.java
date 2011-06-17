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
package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.media.mscontrol.spi.PropertyInfo;
import javax.sdp.SdpFactory;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.conference.ConferenceDriverImpl;
import com.voxeo.moho.event.ApplicationEventSource;
import com.voxeo.moho.media.GenericMediaServiceFactory;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.spi.ConferenceDriver;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SIPDriver;
import com.voxeo.moho.voicexml.VoiceXMLDriverImpl;

public class SIPController extends SipServlet {

  private static final long serialVersionUID = -6039446683694940149L;

  private static final Logger LOG = Logger.getLogger(SIPController.class);

  protected ApplicationEventSource _app = null;

  protected String _applicationClass = null;
  
  protected SIPDriver _driver;

  @SuppressWarnings("unchecked")
  @Override
  public void init() {
    try {
      _applicationClass = getInitParameter("ApplicationClass");
      if (_applicationClass == null) {
        throw new IllegalArgumentException("Cannot found the application implementation class in this Moho application.");
      }
      LOG.info("Moho application:" + _applicationClass);
      final Application app = createApplicationInstance();

      SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
      SdpFactory sdpFactory = (SdpFactory) getServletContext().getAttribute("javax.servlet.sdp.SdpFactory");

      if (sdpFactory == null) {
        LOG.warn("Unable to get SdpFactory, some function, such as call hold unhold mute unmute, is unavailable:");
      }

      // msctrl.min.threadpool , msctrl.max.threadpool used to set thread pool
      // size of 309
      final Properties p = new Properties();
      final Driver driver = DriverManager.getDrivers().next();
      if (driver.getFactoryPropertyInfo() != null) {
        for (final PropertyInfo info : driver.getFactoryPropertyInfo()) {
          String value = getInitParameter(info.name);
          if (value == null) {
            value = info.defaultValue;
          }
          if (value != null) {
            LOG.info("Moho using property for msFactory:" + info.name + ":" + value);
            p.setProperty(info.name, value);
          }
        }
      }

      MsControlFactory mscFactory = driver.getFactory(p);

      int eventDispatcherThreadPoolSize = 50;
      final String eventDipatcherThreadPoolSizePara = getInitParameter("eventDispatcherThreadPoolSize");
      if (eventDipatcherThreadPoolSizePara != null) {
        eventDispatcherThreadPoolSize = Integer.valueOf(eventDipatcherThreadPoolSizePara);
      }
      LOG.info("Moho using eventDipatcherThreadPoolSize:" + eventDispatcherThreadPoolSize);

      Class<? extends MediaDialect> mediaDialectClass = com.voxeo.moho.media.dialect.GenericDialect.class;
      final String mediaDialectClassName = getInitParameter("mediaDialectClass");
      if (mediaDialectClassName != null) {
        mediaDialectClass = (Class<? extends MediaDialect>) Class.forName(mediaDialectClassName);
      }
      final MediaDialect mediaDialect = mediaDialectClass.newInstance();

      final ApplicationContextImpl ctx = new ApplicationContextImpl(app, mscFactory, sipFactory, sdpFactory, getServletConfig().getServletName(), this.getServletContext(),
          eventDispatcherThreadPoolSize);

      ctx.setMediaServiceFactory(new GenericMediaServiceFactory(mediaDialect));

      final Enumeration<String> e = getInitParameterNames();
      while (e.hasMoreElements()) {
        final String name = e.nextElement();
        final String value = getInitParameter(name);
        ctx.setParameter(name, value);
      }

      _app = new ApplicationEventSource(ctx, app);
      _app.setSIPController(this);
      _app.registerDriver(ProtocolDriver.PROTOCOL_SIP, SIPDriverImpl.class.getName());
      _app.registerDriver(ProtocolDriver.PROTOCOL_VXML, VoiceXMLDriverImpl.class.getName());
      _app.registerDriver(ProtocolDriver.PROTOCOL_CONF, ConferenceDriverImpl.class.getName());
      for (String name : _app.getProtocolFamilies()) {
        ProtocolDriver d = _app.getDriverByProtocolFamily(name);
        d.init(_app);
      }
      ctx.setFramework(_app);
      getServletContext().setAttribute(ApplicationContext.APPLICATION, app);
      getServletContext().setAttribute(ApplicationContext.APPLICATION_CONTEXT, ctx);
      getServletContext().setAttribute(ApplicationContext.FRAMEWORK, _app);
      _driver = (SIPDriver)_app.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_SIP);
      ConferenceDriver cd = (ConferenceDriver)_app.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_CONF);
      ctx.setConferenceManager(cd.getManager());
      app.init(ctx);
    }
    catch (final Throwable t) {
      LOG.error("Unable to initialize Moho:", t);
      throw new RuntimeException(t);
    }
  }

  @SuppressWarnings("rawtypes")
  private Application createApplicationInstance() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class clz = null;
    try {
      clz = this.getClass().getClassLoader().loadClass(_applicationClass);
    }
    catch (final Throwable t) {
      clz = Thread.currentThread().getContextClassLoader().loadClass(_applicationClass);
    }
    final Application app = (Application) clz.newInstance();
    return app;
  }

  @Override
  public void destroy() {
    try {
      _driver.destroy();
    }
    catch (final Throwable t) {
      LOG.error("Unable to dispose Moho:", t);
      throw new RuntimeException(t);
    }
  }

  @Override
  protected void doRequest(final SipServletRequest req) throws ServletException, IOException {
    _driver.doRequest(req);
 }

  @Override
  protected void doResponse(final SipServletResponse res) throws ServletException, IOException {
    _driver.doResponse(res);
  }
}
