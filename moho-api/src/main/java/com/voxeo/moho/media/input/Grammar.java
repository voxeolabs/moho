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

package com.voxeo.moho.media.input;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import com.voxeo.moho.media.MediaResource;

public class Grammar implements MediaResource {

  private static final Logger LOG = Logger.getLogger(JSGFGrammar.class);

  protected URI _uri;
  protected String _text = null;
  protected String _contentType = null;

  public Grammar() {}

  protected Grammar(final String contentType, final String contents) {
    _contentType = contentType;
    _text = contents;
  }

  protected Grammar(final URI uri) {
      _uri = uri;
  }

  public String getText() {
    return _text;
  }

  public URI getUri() {
    return _uri;
  }

  public String getContentType() {
    return _contentType;
  }

  @SuppressWarnings("deprecation")
  public URI toURI() {
    if(_uri == null) {
        try {
            return URI.create("data:" + URLEncoder.encode(getContentType() + "," + getText(), "UTF-8"));
        }
        catch (final UnsupportedEncodingException e) {
            LOG.warn("", e);
            return URI.create("data:" + URLEncoder.encode(getContentType() + "," + getText()));
        }
    }
    else {
        return _uri;
    }
  }

  /**
  * @deprecated Use the Grammar class constructor instead
  */
  public static Grammar create(final String grammar) {
    if (grammar.startsWith("#JSGF")) {
      return new JSGFGrammar(grammar);
    }
    else {
      return new SimpleGrammar(grammar);
    }
  }

  /**
   * @deprecated Use the Grammar class constructor instead
   */
  public static Grammar create(Reader reader) throws IOException {
    StringBuffer sb = new StringBuffer();
    char[] charbuf = new char[1024];
    int readLength = 0;
    while ((readLength = reader.read(charbuf)) > 0) {
      sb.append(charbuf, 0, readLength);
    }
      
    String grammar = sb.toString();
      
    if (grammar.startsWith("#JSGF")) {
      return new JSGFGrammar(grammar);
    }
    else {
      return new SimpleGrammar(grammar);
    }
  }
  
}
