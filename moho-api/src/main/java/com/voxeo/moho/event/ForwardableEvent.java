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

import java.util.Map;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public interface ForwardableEvent {

  public enum ForwardableEventState implements EventState {
    FORWARDED;
  }

  void forwardTo(final Call call) throws SignalException, IllegalStateException;

  void forwardTo(final Call call, final Map<String, String> headers) throws SignalException, IllegalStateException;

  void forwardTo(final Endpoint endpoint) throws SignalException, IllegalStateException;

  void forwardTo(final Endpoint endpoint, final Map<String, String> headers) throws SignalException,
      IllegalStateException;

}
