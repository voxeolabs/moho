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
import java.util.Properties;

import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.media.mscontrol.spi.PropertyInfo;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SIPDriver;

public class SIPController extends SipServlet {

  private static final long serialVersionUID = -6039446683694940149L;

  private static final Logger LOG = Logger.getLogger(SIPController.class);

  protected ApplicationContextImpl _ctx = null;

  protected String _applicationClass = null;

  protected SIPDriver _driver;

  @Override
  public void init() {
    try {
      _applicationClass = getInitParameter("ApplicationClass");
      if (_applicationClass == null) {
        throw new IllegalArgumentException(
            "Cannot found the application implementation class in this Moho application.");
      }
      LOG.info("Moho application:" + _applicationClass);
      final Application app = createApplicationInstance();

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

      _ctx = new ApplicationContextImpl(app, mscFactory, this);

      _driver = (SIPDriver)_ctx.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_SIP);
      app.init(_ctx);
    }
    catch (final Throwable t) {
      LOG.error("Unable to initialize Moho:", t);
      throw new RuntimeException(t);
    }
  }

  @SuppressWarnings("rawtypes")
  private Application createApplicationInstance() throws ClassNotFoundException, InstantiationException,
      IllegalAccessException {
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
      _ctx.destroy();
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
