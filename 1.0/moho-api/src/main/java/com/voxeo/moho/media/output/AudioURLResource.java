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

package com.voxeo.moho.media.output;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

public class AudioURLResource extends AudibleResource {

  protected URL _url;

  protected URI _uri;

  protected String _type;

  protected TextToSpeechResource _alternative;

  public AudioURLResource(final URL url, final String type) {
    setMedia(url, type);
  }

  public URL getMedia() {
    return _url;
  }

  public String getType() {
    return _type;
  }

  public void setMedia(final URL url, final String type) {
    _url = url;
    _type = type;
    makeURI();
  }

  public void setAlternative(final TextToSpeechResource text) {
    _alternative = text;
    makeURI();
  }

  public TextToSpeechResource getAlternative() {
    return _alternative;
  }

  @Override
  public URI toURI() {
    return _uri;
  }

  protected void makeURI() {
    try {
      _uri = _url.toURI();
    }
    catch (final URISyntaxException e) {
      try {
        _uri = URI.create("data:"
            + URLEncoder.encode("application/ssml+xml," + "<?xml version=\"1.0\"?>" + "<speak>" + "<voice>"
                + getAlternative() + "</voice>" + "</speak>", "UTF-8"));
      }
      catch (final UnsupportedEncodingException e1) {
        // ignore -- shounldn't happen
      }
    }
  }
}
