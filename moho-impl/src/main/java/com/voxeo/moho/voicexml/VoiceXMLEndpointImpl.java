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

package com.voxeo.moho.voicexml;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import com.voxeo.moho.ExecutionContext;

public class VoiceXMLEndpointImpl implements VoiceXMLEndpoint {

  protected ExecutionContext _context;

  protected URL _document;

  public VoiceXMLEndpointImpl(final ExecutionContext ctx, final String document) throws MalformedURLException {
    _context = ctx;
    _document = new URL(document);
  }

  @Override
  public int hashCode() {
    return _document.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof VoiceXMLEndpointImpl)) {
      return false;
    }
    return _document.equals(((VoiceXMLEndpointImpl) o).getDocumentURL());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(VoiceXMLEndpointImpl.class.getSimpleName()).append("[").append(_document).append(
        "]").toString();
  }

  @Override
  public URL getDocumentURL() {
    return _document;
  }

  @Override
  public String getName() {
    return _document.toString();
  }

  @Override
  public URI getURI() {
    try {
      return _document.toURI();
    }
    catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public Dialog create(final Map<Object, Object> params) {
    return new VoiceXMLDialogImpl(_context, this, params);
  }
}
