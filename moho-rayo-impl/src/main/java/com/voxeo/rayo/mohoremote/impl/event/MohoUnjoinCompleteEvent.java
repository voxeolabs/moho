/**
 * Copyright 2010-2011 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.rayo.mohoremote.impl.event;

import com.voxeo.moho.Participant;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.UnjoinCompleteEvent;

public class MohoUnjoinCompleteEvent extends MohoEvent<EventSource> implements UnjoinCompleteEvent {

  protected boolean _initiator;

  protected Participant _participant;

  protected Cause _cause;

  protected Exception _exception;

  public MohoUnjoinCompleteEvent(final EventSource source, final Participant p, final Cause cause, boolean initiator) {
    super(source);
    _participant = p;
    _cause = cause;
    _initiator = initiator;
  }

  public MohoUnjoinCompleteEvent(final EventSource source, final Participant p, final Cause cause, final Exception e,
      boolean initiator) {
    super(source);
    _participant = p;
    _cause = cause;
    _exception = e;
    _initiator = initiator;
  }

  @Override
  public Participant getParticipant() {
    return _participant;
  }

  @Override
  public Cause getCause() {
    return _cause;
  }

  @Override
  public Exception getException() {
    return _exception;
  }

  @Override
  public boolean isInitiator() {
    return _initiator;
  }

}
