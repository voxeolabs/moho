/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.conference;

import com.voxeo.moho.MixerException;

/**
 * General exception related to conference operation.
 */
public class ConferenceException extends MixerException {

  private static final long serialVersionUID = 2648829831220562680L;

  public ConferenceException() {
  }

  public ConferenceException(String message) {
    super(message);
  }

  public ConferenceException(Throwable cause) {
    super(cause);
  }

  public ConferenceException(String message, Throwable cause) {
    super(message, cause);
  }

}
