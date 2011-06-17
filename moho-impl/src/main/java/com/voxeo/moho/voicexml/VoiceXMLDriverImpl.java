package com.voxeo.moho.voicexml;

import java.net.MalformedURLException;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.spi.VoiceXMLDriver;

public class VoiceXMLDriverImpl implements VoiceXMLDriver {
  protected static final String[] SCHEMAS = new String[]{"file", "http", "https", "ftp", "ftps"};
  protected SpiFramework _framework;
  
  public void init(SpiFramework framework) {
    _framework = framework;
  }
  
  @Override
  public String getProtocolFamily() {
    return PROTOCOL_VXML;
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
    try {
      return new VoiceXMLEndpointImpl(_framework.getExecutionContext(), addr);
    }
    catch(MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void destroy() {
  }

}
