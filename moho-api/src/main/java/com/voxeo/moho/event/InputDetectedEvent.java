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

package com.voxeo.moho.event;

public class InputDetectedEvent extends MediaNotificationEvent {

  private static final long serialVersionUID = -1309218764486052886L;

  protected String _input = null;

  public InputDetectedEvent(final EventSource source, final String input) {
    super(source);
    _input = input;
  }

  public String getInput() {
    return _input;
  }

}
