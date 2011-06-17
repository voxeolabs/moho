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

import java.util.concurrent.Future;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.AttributeStore;
import com.voxeo.moho.utils.Identifiable;

/**
 * <p>
 * An EventSource is an object that can generate {@link Event
 * Event} in Moho. Applications can set application defined state on EventSource
 * by calling {@link #setApplicationState(String) setApplicationState(String)}
 * for single state or {@link #setApplicationState(String,String)
 * setApplicationState(String)} for multiple concurrent states.
 * </p>
 * <p>
 * Please note certain EventSources, such as {@link com.voxeo.moho.Call Call},
 * have system defined states besides application defined states. Event dispatch
 * from EventSource is always based on application defined state.
 * </p>
 * <p>
 * {@link Observer Observer} can be added to an EventSource to listen for events.
 * </p>
 * 
 * @author wchen
 */
public interface EventSource extends Identifiable<String>, AttributeStore {

  /**
   * @return the application state set by {@link #setApplicationState(String)
   *         setApplicationState}
   */
  String getApplicationState();

  /**
   * @param FSM
   *          the name of the concurrent state machine.
   * @return the application state of one of the state machines by
   *         {@link #setApplicationState(String, String) setApplicationState}
   */
  String getApplicationState(String FSM);

  /**
   * set the application state
   * 
   * @param state
   *          the state name
   */
  void setApplicationState(String state);

  /**
   * set the application state of a particular state machine .
   * 
   * @param FSM
   *          the name of the state machine
   * @param state
   *          the state name in FSM state
   */
  void setApplicationState(String FSM, String state);

  /**
   * @return the application context of this application
   */
  ApplicationContext getApplicationContext();

  /**
   * Add an event observers to this event source. If the same observers has been
   * added, it is a NOP.
   * 
   * @param observers
   *          the event observers to be added.
   */
  void addObserver(Observer... observers);

  /**
   * remove the application listener to this event source. if the listener has
   * not been added before, it is a NOP.
   * 
   * @param listener
   *          the event listener to be added
   */
  void removeObserver(Observer listener);

  /**
   * Dispatch an event to this event source.
   * 
   * @param event
   *          the event to be dispatched.
   * @return a @{link java.util.concurrent.Future} for the dispatching.
   */
  <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event);

  /**
   * Dispatch an event to this event source.
   * 
   * @param event
   *          the event to be dispatched.
   * @return a @{link java.util.concurrent.Future} for the dispatching.
   */
  <S extends EventSource, T extends Event<S>> Future<T> dispatch(T event, Runnable afterExec);
}
