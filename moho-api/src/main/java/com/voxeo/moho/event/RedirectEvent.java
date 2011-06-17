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

import com.voxeo.moho.Endpoint;

/**
 * This event is fired when a redirect response is received.
 * 
 * @author wchen
 *
 * @param <T>
 */
public interface RedirectEvent<T extends EventSource> extends ResponseEvent<T>, AcceptableEvent {
  /**
   * @return true if the target is moved permanently
   */
  boolean isPermanent();
  
  /**
   * @return the new location of the target. The first one will be returned if there are multiple new locatins.
   */
  Endpoint getEndpoint();
  
  /**
   * @return all the new locations of the target
   */
  Endpoint[] getEndpoints();
  
  /**
   * @return true if this redirect has been processed.
   */
  boolean isProcessed();
  
}
