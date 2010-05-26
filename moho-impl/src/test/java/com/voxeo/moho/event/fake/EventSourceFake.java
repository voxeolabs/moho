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
import java.util.UUID;
import java.util.concurrent.Future;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MediaEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;

public class EventSourceFake implements EventSource {

  private List<MediaEvent> receivedEvents;

  private String id = UUID.randomUUID().toString();

  final public List<MediaEvent> getReceivedEvents() {
    return receivedEvents;
  }

  @Override
  public void addListener(final EventListener<?> listener) {

  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListener(final Class<E> type, final T listener) {

  }

  @Override
  public void addListeners(final EventListener<?>... listener) {

  }

  @Override
  public <E extends Event<?>, T extends EventListener<E>> void addListeners(final Class<E> type, final T... listener) {

  }

  @Override
  public <S, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
    return dispatch(event);
  }

  @Override
  final public <S, T extends Event<S>> Future<T> dispatch(final T event) {
    if (receivedEvents == null) {
      receivedEvents = new ArrayList<MediaEvent>();
    }

    receivedEvents.add((MediaEvent) event);
    return null;
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
  public String getApplicationState(final String FSM) {

    return null;
  }

  @Override
  public void removeListener(final EventListener<?> listener) {

  }

  @Override
  public void removeObserver(final Observer listener) {

  }

  @Override
  public void setApplicationState(final String state) {

  }

  @Override
  public void setApplicationState(final String FSM, final String state) {

  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Object getAttribute(final String name) {

    return null;
  }

  @Override
  public Map<String, Object> getAttributeMap() {

    return null;
  }

  @Override
  public void setAttribute(final String name, final Object value) {

  }

  @Override
  public void addObserver(final Observer observer) {

  }

  @Override
  public void addObservers(final Observer... observers) {

  }
}
