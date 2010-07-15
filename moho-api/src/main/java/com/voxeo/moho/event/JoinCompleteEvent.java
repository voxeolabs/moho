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

package com.voxeo.moho.event;

import com.voxeo.moho.Participant;
import com.voxeo.utils.Event;

public class JoinCompleteEvent extends Event<EventSource> {

  public enum Cause {

    JOINED,

    BUSY, TIMEOUT, REJECT, REDIRECT,

    ERROR,

    CANCELED, DISCONNECTED
  }

  protected Participant _participant;

  protected Cause _cause;

  protected Exception _exception;

  public JoinCompleteEvent(final EventSource source, final Participant p, final Cause cause) {
    super(source);
    _participant = p;
    _cause = cause;
  }

  public JoinCompleteEvent(final EventSource source, final Participant p, final Cause cause, final Exception e) {
    super(source);
    _participant = p;
    _cause = cause;
  }

  public EventSource getSource() {
    return source;
  }

  public Participant getParticipant() {
    return _participant;
  }

  public Cause getCause() {
    return _cause;
  }

  public Exception getException() {
    return _exception;
  }

}
