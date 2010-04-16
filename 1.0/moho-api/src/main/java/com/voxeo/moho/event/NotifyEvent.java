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

import com.voxeo.moho.Subscription;

public abstract class NotifyEvent extends SignalEvent implements ForwardableEvent {

  private static final long serialVersionUID = 1538596617421252364L;

  /** RFC 3265 */
  public enum SubscriptionState {
    ACTIVE, PENDING, TERMINATED
  }

  protected NotifyEvent(final EventSource source) {
    super(source);
  }

  public abstract Subscription.Type getEventType();

  public abstract SubscriptionState getSubscriptionState();

  // TODO the state string for dialog, presence, refer event package.
  public abstract String getResourceState();
}
