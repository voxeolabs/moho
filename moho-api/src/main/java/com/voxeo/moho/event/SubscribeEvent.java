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

package com.voxeo.moho.event;

import java.io.Serializable;

import com.voxeo.moho.Framework;
import com.voxeo.moho.spi.ExecutionContext;

/**
 * This event is fired when a subscribe request is received.
 * 
 * @author wchen
 * 
 */
public interface SubscribeEvent extends RequestEvent<Framework>, RedirectableEvent, ProxyableEvent {
  public interface SubscriptionContext extends Serializable {
    String getSubscriber();
    String getSubscribee();
    Object getId();
    void setExecutionContext(ExecutionContext context);
  }
  
  SubscriptionContext getSubscription();
}
