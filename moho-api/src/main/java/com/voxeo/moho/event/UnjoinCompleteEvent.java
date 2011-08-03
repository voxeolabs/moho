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

package com.voxeo.moho.event;

import com.voxeo.moho.Participant;

/**
 * This event fires when a unjoin operation is completed.
 * 
 * @author wchen
 */
public interface UnjoinCompleteEvent extends Event<EventSource> {

  public enum Cause {
    DISCONNECT, SUCCESS_UNJOIN, FAIL_UNJOIN, NOT_JOINED, ERROR
  }

  Participant getParticipant();

  Cause getCause();

  Exception getException();
}
