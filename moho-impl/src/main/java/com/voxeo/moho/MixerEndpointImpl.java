/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.MsControlFactory;

import com.voxeo.moho.spi.ExecutionContext;

public class MixerEndpointImpl implements MixerEndpoint {

  protected ExecutionContext _context;

  protected URI _uri;

  protected Map<String, String> _parameters = new HashMap<String, String>();

  protected Properties _props = new Properties();

  public MixerEndpointImpl(final ExecutionContext ctx, final String uri) {
    _context = ctx;
    if (uri == null || uri.equals(MixerEndpoint.DEFAULT_MIXER_ENDPOINT)) {
      String resultUri = ctx.getMSFactory().getProperties().getProperty(MsControlFactory.MEDIA_SERVER_URI);
      if(resultUri != null) {
        _uri = URI.create(resultUri);
      }
    }
  }

  @Override
  public int hashCode() {
    return _uri.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof MixerEndpoint)) {
      return false;
    }
    return _uri.equals(((MixerEndpoint) o).getURI());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(MixerEndpointImpl.class.getSimpleName()).append("[").append(_uri).append("]")
        .toString();
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
  public Mixer create(final Map<Object, Object> params) throws MediaException {
    return new MixerImpl(_context, this, params, null);
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

}
