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

public class RecordCompleteEvent extends MediaCompleteEvent {

  private static final long serialVersionUID = -8757723338851029875L;

  public enum Cause {
    TIMEOUT, ERROR, SILENCE, UNKNOWN, CANCEL, INI_TIMEOUT,
  }

  protected Cause _cause;

  protected long _duration;

  public RecordCompleteEvent(final EventSource source, final Cause cause, long duration) {
    super(source);
    _cause = cause;
    _duration = duration;
  }

  public Cause getCause() {
    return _cause;
  }

  /**
   * Returns the length of the recording, in milliseconds. This length does not
   * include any omitted silence.
   * 
   * @return the length of the recording, in milliseconds.
   */
  public long getDuration() {
    return _duration;
  }
}
