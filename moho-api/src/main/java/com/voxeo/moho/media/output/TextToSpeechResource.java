/**
 * Copyright 2010-2011 Voxeo Corporation
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
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.log4j.Logger;

public class TextToSpeechResource extends AudibleResource {

  private static final Logger LOG = Logger.getLogger(TextToSpeechResource.class);

  protected String _text;

  protected Locale _locale;

  protected URI _uri;

  public TextToSpeechResource(final String text) {
    setText(text, Locale.getDefault());
  }

  public TextToSpeechResource(final String text, final Locale locale) {
    setText(text, locale);
  }

  public String getText() {
    return _text;
  }

  public void setText(final String text, final Locale locale) {
    _text = text;
    _locale = locale;
    try {
      _uri = URI.create("data:"
          + URLEncoder.encode("application/ssml+xml," + "<?xml version=\"1.0\"?>" + "<speak>" + "<voice>" + getText()
              + "</voice>" + "</speak>", "UTF-8"));
    }
    catch (final UnsupportedEncodingException e) {
      LOG.error("Exception when create URI from text", e);
    }
  }

  public Locale getLocale() {
    return _locale;
  }

  @Override
  public URI toURI() {
    return _uri;
  }
}
