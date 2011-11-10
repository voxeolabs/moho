package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;

public class MixerEndpointImpl implements MixerEndpoint {
  
  private URI _uri;
  
  private MohoRemoteImpl _mohoRemoteImpl;

  private Properties _props = new Properties();
  
  private String _mixerName;
  
  public MixerEndpointImpl(MohoRemoteImpl mohoRemoteImpl, String mixerName) {
    _mohoRemoteImpl = mohoRemoteImpl;
    _mixerName = mixerName;
    _uri = URI.create("mscontrol://" + _mixerName);
  }
  
  @Override
  public String getName() {
    return _uri.toString();
  }

  @Override
  public URI getURI() {
    return _uri;
  }

  @Override
  public Mixer create(Map<Object, Object> params) throws MediaException {
    return new MixerImpl(this, params);
  }

  @Override
  public String getProperty(String key) {
    return _props.getProperty(key);
  }

  @Override
  public String remove(String key) {
    return (String) _props.remove(key);
  }

  @Override
  public void setProperty(String key, String value) {
    _props.setProperty(key, value);
  }

  public String getConferenceName() {
    return _mixerName;
  }

  public MohoRemoteImpl getMohoRemote() {
    return _mohoRemoteImpl;
  }
  
}
