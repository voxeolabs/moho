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

import java.util.Map;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

/**
 * This interface marks an event that can be redirected to a 
 * different {@link com.voxeo.moho.Endpoint, Endpoint}.
 * 
 * @author wchen
 *
 */
public interface RedirectableEvent {
  /**
   * @return true if this event has been redirected.
   */
  boolean isRedirected();

  /**
   * Redirect this event to another {@link com.voxeo.moho.Endpoint Endpoint}.
   * @param other the other {@link com.voxeo.moho.Endpoint Endpoint}
   * @throws SignalException when signaling error occurs.
   */
  void redirect(final Endpoint other) throws SignalException;

  /**
   * Redirect this event to another {@link com.voxeo.moho.Endpoint Endpoint}
   * with additional protocol specific headers.
   * @param other the other {@link com.voxeo.moho.Endpoint Endpoint}
   * @param headers the protocol specific headers.
   * @throws SignalException when signaling error occurs.
   */
  void redirect(Endpoint other, Map<String, String> headers) throws SignalException;

}
