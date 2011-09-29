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

package com.voxeo.moho.remote.impl.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.voxeo.moho.State;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.utils.EventListener;

public class AutowiredEventTarget {

  private static final Logger log = Logger.getLogger(AutowiredEventTarget.class);

  public static final String DEFAULT_FSM = "com.voxeo.moho.event.fsm";

  public static final String ANY_STATE = "com.voxeo.moho.event.anystate";

  protected final Map<String, String> _definedStates = new HashMap<String, String>();

  protected Method _method;

  protected Object _observer;

  protected EventListener<Event<? extends EventSource>> _listener;

  AutowiredEventTarget(final Method method, final Object observer) {
    final State anno = method.getAnnotation(State.class);
    if (anno != null) {
      final String[] states = anno.value();
      for (final String state : states) {
        final String[] values = state.split("=");
        if (values.length == 1) {
          if (values[0].trim().length() == 0) {
            values[0] = ANY_STATE;
          }
          _definedStates.put(DEFAULT_FSM, values[0]);
        }
        else if (values.length == 2) {
          if (values[1].trim().length() == 0) {
            values[1] = ANY_STATE;
          }
          _definedStates.put(values[0], values[1]);
        }
        else {
          // log error
        }
      }
    }
    else {
      _definedStates.put(DEFAULT_FSM, ANY_STATE);
    }
    _method = method;
    _observer = observer;
  }

  AutowiredEventTarget(final String[][] states, final EventListener<Event<? extends EventSource>> listener) {
    for (final String[] s : states) {
      _definedStates.put(s[0], s[1]);
    }
    _listener = listener;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof AutowiredEventTarget)) {
      return false;
    }
    return this._method.equals(((AutowiredEventTarget) o)._method);
  }

  Object getObserver() {
    return _observer;
  }

  EventListener<Event<? extends EventSource>> getListener() {
    return _listener;
  }

  boolean invoke(final Event<? extends EventSource> event) throws Exception {
    for (final Map.Entry<String, String> entry : _definedStates.entrySet()) {
      final String defined = entry.getValue();
      if (defined != ANY_STATE) {
        if (!defined.equals(event.getSource().getApplicationState(entry.getKey()))) {
          return false;
        }
      }
    }
    if (_observer != null && _method != null) {
      final boolean accessible = _method.isAccessible();
      try {
        _method.setAccessible(true);
        _method.invoke(_observer, new Object[] {event});
      }
      catch (final Exception e) {
        log.error("Got Exception when invoking Application.", e);
        if (e instanceof InvocationTargetException
            && ((InvocationTargetException) e).getTargetException() instanceof Exception) {
          throw (Exception) ((InvocationTargetException) e).getTargetException();
        }
      }
      finally {
        _method.setAccessible(accessible);
      }
    }
    if (_listener != null) {
      try {
        _listener.onEvent(event);
      }
      catch (final Exception e) {
        log.error("", e);
        throw e;
      }
    }
    return true;
  }
}
