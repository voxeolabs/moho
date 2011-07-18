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

import java.net.URI;

public class AudioURIResource extends AudibleResource {

  protected URI _uri;

  protected TextToSpeechResource _alternative;

  public AudioURIResource(final URI uri) {
    _uri = uri;
  }

  public void setMedia(final URI uri) {
    _uri = uri;
  }

  public void setAlternative(final TextToSpeechResource text) {
    _alternative = text;
  }

  public TextToSpeechResource getAlternative() {
    return _alternative;
  }

  @Override
  public URI toURI() {
    if (_uri != null) {
      return _uri;
    }
    return getAlternative().toURI();
  }
}
