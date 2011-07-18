package com.voxeo.moho.sip.fake;

import javax.sdp.SdpFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;

import org.jmock.Mockery;

public class MockSipServlet extends SipServlet {
  Mockery _mockery;
  ServletContext _ctx;
  
  public MockSipServlet(Mockery mockery) {
    _mockery = mockery;
    _ctx = new MockServletContext();
    _ctx.setAttribute(SIP_FACTORY, _mockery.mock(SipFactory.class));
    _ctx.setAttribute("javax.servlet.sdp.SdpFactory", _mockery.mock(SdpFactory.class));
    try {
      init(new MockServletConfig(_ctx));
    }
    catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public ServletContext getServletContext() {
    return _ctx;
  }
  
}
