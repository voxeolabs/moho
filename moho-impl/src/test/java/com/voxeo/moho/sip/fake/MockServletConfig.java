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
  final public String getInitParameter(String arg0) {
    return _ctx.getInitParameter(arg0);
  }

  @Override
  final public Enumeration getInitParameterNames() {
    return _ctx.getInitParameterNames();
  }

  @Override
  final public ServletContext getServletContext() {
    return _ctx;
  }

  @Override
  final public String getServletName() {
    return "test";
  }

}
