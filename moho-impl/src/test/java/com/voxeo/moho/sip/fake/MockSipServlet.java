package com.voxeo.moho.sip.fake;

import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
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
  }
  
  public ServletContext getServletContext() {
    return _ctx;
  }
  
}
