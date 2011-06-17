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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.AttributeStoreImpl;
import com.voxeo.moho.ExceptionHandler;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.Utils;
import com.voxeo.moho.utils.EventListener;

/**
 * DispatchableEventSource supports dispatching events to different targets on a
 * single threaded event queue The target can be either an EventListener or a
 * event method in an Observer. The event method in an Observer is a public
 * method with a single parameter whose type is one of the event.
 */
public class DispatchableEventSource extends AttributeStoreImpl implements EventSource {

  protected String _id;

  protected Map<String, String> _states = new ConcurrentHashMap<String, String>();

  protected ExecutionContext _context;

  protected EventDispatcher _dispatcher = new EventDispatcher();

  protected ConcurrentHashMap<Observer, AutowiredEventListener> _observers = new ConcurrentHashMap<Observer, AutowiredEventListener>();

  protected DispatchableEventSource() {
    _id = UUID.randomUUID().toString(); // TODO: better one?    
  }
  
  public DispatchableEventSource(final ExecutionContext applicationContext) {
    this(applicationContext, true);
  }

  public DispatchableEventSource(final ExecutionContext applicationContext, boolean orderedDispatch) {
    this();
    _context = applicationContext;
    _dispatcher.setExecutor(getThreadPool(), orderedDispatch);
  }

  // Event Handling
  // =================================================================

  public void addListener(final EventListener<?> listener) {
    if (listener != null) {
      _dispatcher.addListener(MohoEvent.class, listener);
    }
  }

  public void addListeners(final EventListener<?>... listeners) {
    if (listeners != null) {
      for (final EventListener<?> listener : listeners) {
        this.addListener(listener);
      }
    }
  }

  public <E extends MohoEvent<?>, T extends EventListener<E>> void addListener(final Class<E> type, final T listener) {
    if (listener != null) {
      _dispatcher.addListener(type, listener);
    }
  }

  public <E extends MohoEvent<?>, T extends EventListener<E>> void addListeners(final Class<E> type, final T... listeners) {
    if (listeners != null) {
      for (final T listener : listeners) {
        this.addListener(type, listener);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public void addObserver(final Observer observer) {
    if (observer != null) {
      if (observer instanceof EventListener) {
        EventListener<?> l = (EventListener<?>) observer;
        Class claz = Utils.getGenericType(observer);
        if (claz == null) {
          claz = Event.class;
        }
        _dispatcher.addListener(claz, l);
      }
      else {
        final AutowiredEventListener autowire = new AutowiredEventListener(observer);
        if (_observers.putIfAbsent(observer, autowire) == null) {
          _dispatcher.addListener(MohoEvent.class, autowire);
        }
      }
    }
  }

  @Override
  public void addObserver(final Observer... observers) {
    if (observers != null) {
      for (final Observer o : observers) {
        addObserver(o);
      }
    }
  }

  public void removeListener(final EventListener<?> listener) {
    _dispatcher.removeListener(listener);
  }

  @Override
  public void removeObserver(final Observer listener) {
    if (listener instanceof EventListener) {
      _dispatcher.removeListener((EventListener<?>) listener);
    }
    else {
      final AutowiredEventListener autowiredEventListener = _observers.remove(listener);
      if (autowiredEventListener != null) {
        _dispatcher.removeListener(autowiredEventListener);
      }
    }
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
    return _dispatcher.fire(event, true, null);
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
    return _dispatcher.fire(event, true, afterExec);
  }

  // PROPERTIES
  // =================================================================

  public String getId() {
    return _id;
  }

  public ApplicationContext getApplicationContext() {
    return _context;
  }

  // States
  // ==================================================================
  @Override
  public String getApplicationState() {
    return _states.get(AutowiredEventTarget.DEFAULT_FSM);
  }

  @Override
  public void setApplicationState(final String state) {
    _states.put(AutowiredEventTarget.DEFAULT_FSM, state);
  }

  public String getApplicationState(final String FSM) {
    return _states.get(FSM);
  }

  @Override
  public void setApplicationState(final String FSM, final String state) {
    _states.put(FSM, state);
  }

  protected Executor getThreadPool() {
    return _context.getExecutor();
  }

  @Override
  public void addExceptionHandler(ExceptionHandler... handlers) {
    _dispatcher.addExceptionHandler(handlers);
  }

}
