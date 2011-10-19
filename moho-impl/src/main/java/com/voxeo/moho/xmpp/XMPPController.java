package com.voxeo.moho.xmpp;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.spi.XMPPDriver;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.XmppServlet;

public class XMPPController extends XmppServlet {

  private static final long serialVersionUID = -1802430322022324158L;

  protected XMPPDriver _driver;
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    SpiFramework framework = (SpiFramework) getServletContext().getAttribute(ApplicationContext.FRAMEWORK);
    if(framework.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_XMPP) == null) {
      try {
        framework.registerDriver(ProtocolDriver.PROTOCOL_XMPP, XMPPDriverImpl.class.getName());
      }
      catch (Exception e) {
        throw new ServletException(e);
      }
    }
    ((ApplicationContextImpl)framework).setXMPPController(this);
    _driver = (XMPPDriver)framework.getDriverByProtocolFamily(ProtocolDriver.PROTOCOL_XMPP);
    _driver.init(framework);
  }
  
  @Override
  public void destroy() {
    super.destroy();
    _driver.destroy();
  }
  
  @Override
  protected void doIQRequest(IQRequest req) throws ServletException, IOException {
    _driver.doIQRequest(req);
  }
  
  @Override
  protected void doIQResponse(IQResponse resp) throws ServletException, IOException {
    _driver.doIQResponse(resp);
  }
  
  @Override
  protected void doPresence(PresenceMessage req) throws ServletException, IOException {
    _driver.doPresence(req);
  }
  
  @Override
  protected void doMessage(InstantMessage req) throws ServletException, IOException {
    _driver.doMessage(req);
  }
}
