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

package com.voxeo.moho.event.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MediaEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.Observer;

public class MockEventSource implements EventSource {

  private List<MediaEvent<?>> receivedEvents;

  final public List<MediaEvent<?>> getReceivedEvents() {
    return receivedEvents;
  }

  @Override
  final public synchronized <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event) {
    return this.dispatch(event, null);
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return null;
  }

  @Override
  public String getApplicationState() {

    return null;
  }

  @Override
  public String getApplicationState(String FSM) {

    return null;
  }

  @Override
  public void removeObserver(Observer listener) {

  }

  @Override
  public void setApplicationState(String state) {

  }

  @Override
  public void setApplicationState(String FSM, String state) {

  }

  @Override
  public String getId() {

    return null;
  }

  @Override
  public Map<String, Object> getAttributeMap() {

    return null;
  }

  @Override
  public void setAttribute(String name, Object value) {

  }

  @Override
  public void addObserver(Observer... observers) {

  }

  @Override
  final synchronized public <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event, Runnable r) {
    if (receivedEvents == null) {
      receivedEvents = new ArrayList<MediaEvent<?>>();
    }

    receivedEvents.add((MediaEvent<?>) event);
    return null;
  }

  @Override
  public <T> T getAttribute(String name) {
    // TODO Auto-generated method stub
    return null;
  }
}
