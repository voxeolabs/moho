package com.voxeo.moho.conference;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.MixerEndpointImpl;
import com.voxeo.moho.spi.ConferenceDriver;
import com.voxeo.moho.spi.SpiFramework;

public class ConferenceDriverImpl implements ConferenceDriver {
  protected static final String[] SCHEMAS= new String[]{"mscontrol"};
  
  SpiFramework _framework;
  ConferenceManager _mgr;
  
  public void init(SpiFramework framework) {
    _framework = framework;
    _mgr = new ConferenceMangerImpl(framework.getExecutionContext());
  }

  @Override
  public String getProtocolFamily() {
    return PROTOCOL_CONF;
  }

  @Override
  public String[] getEndpointSchemas() {
    return SCHEMAS;
  }

  @Override
  public SpiFramework getFramework() {
    return _framework;
  }

  @Override
  public Endpoint createEndpoint(String addr) {
    return new MixerEndpointImpl(_framework.getExecutionContext(), addr);  
  }

  @Override
  public void destroy() {
  }

  @Override
  public ConferenceManager getManager() {
    return _mgr;
  }

}
