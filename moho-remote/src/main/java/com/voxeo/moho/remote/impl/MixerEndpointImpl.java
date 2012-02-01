package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;

public class MixerEndpointImpl implements MixerEndpoint {

  private MohoRemoteImpl _mohoRemoteImpl;

  private Properties _props = new Properties();

  public MixerEndpointImpl(MohoRemoteImpl mohoRemoteImpl) {
    _mohoRemoteImpl = mohoRemoteImpl;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public URI getURI() {
    return null;
  }

  @Override
  public Mixer create(String name, Map<Object, Object> params) throws MediaException {
    _mohoRemoteImpl.getParticipantsLock().lock();
    try {
      if (name != null) {
        Mixer mixer = _mohoRemoteImpl.getMixerByName(name);
        if (mixer != null) {
          return mixer;
        }
      }

      return new MixerImpl(this, name, params);
    }
    finally {
      _mohoRemoteImpl.getParticipantsLock().unlock();
    }
  }

  @Override
  public Mixer create(Map<Object, Object> params) throws MediaException {
    return this.create(null, params);
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

  public MohoRemoteImpl getMohoRemote() {
    return _mohoRemoteImpl;
  }
}
