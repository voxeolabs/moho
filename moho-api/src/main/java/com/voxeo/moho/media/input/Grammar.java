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

import com.voxeo.moho.media.MediaResource;

public abstract class Grammar implements MediaResource {

  protected String _text = null;

  public static Grammar create(final String grammar) {
    if (grammar.startsWith("#JSGF")) {
      return new JSGFGrammar(grammar);
    }
    else {
      return new SimpleGrammar(grammar);
    }
  }

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

  protected Grammar() {

  }

  protected Grammar(final String text) {
    _text = text;
  }

  public String toText() {
    return _text;
  }

}
