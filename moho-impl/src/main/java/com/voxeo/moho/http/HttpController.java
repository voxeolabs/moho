package com.voxeo.moho.http;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.imified.IMifiedDriver;
import com.voxeo.moho.spi.HTTPDriver;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SpiFramework;

public class HttpController extends HttpServlet {

  private static final long serialVersionUID = -6982167135072972700L;

  protected HTTPDriver _driver;
  
  protected SpiFramework _framework;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    _framework = (SpiFramework) getServletContext().getAttribute(ApplicationContext.FRAMEWORK);
    if(_framework.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_HTTP) == null) {
      try {
        _framework.registerDriver(ProtocolDriver.PROTOCOL_HTTP, IMifiedDriver.class.getName());
      }
      catch (Exception e) {
        throw new ServletException(e);
      }
    }
    ((ApplicationContextImpl)_framework).setHTTPController(this);
    _driver = (HTTPDriver)_framework.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_HTTP);
    _driver.init(_framework);
  }

  @Override
  public void destroy() {
    super.destroy();
    _driver.destroy();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    _driver.doGet(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    _driver.doPost(req, resp);
  }

}
