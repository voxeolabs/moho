package com.voxeo.moho.sip.fake;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {
  ServletContext _ctx; 
  
  public MockServletConfig(ServletContext ctx) {
    _ctx = ctx;
  }

  @Override
  public String getInitParameter(String arg0) {
    return _ctx.getInitParameter(arg0);
  }

  @Override
  public Enumeration getInitParameterNames() {
    return _ctx.getInitParameterNames();
  }

  @Override
  public ServletContext getServletContext() {
    return _ctx;
  }

  @Override
  public String getServletName() {
    return "test";
  }

}
