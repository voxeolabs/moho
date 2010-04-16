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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.voxeo.moho.State;
import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;

/**
 * Takes an <code>Object</code> as an event target. Each method for event
 * processing must have the following pattern
 * 
 * <pre>
 *   &#064;State mystate
 *   public void any_method_name(EventObject event) {...}
 * </pre>
 * 
 * Please note application should use a subclass of Event in practice, such as
 * ... This class will inspect the methods and invoke the method based on
 * signature, state annotation, and event object type.
 * <ul>
 * <li>the method signature must be
 * 
 * <pre>
 * public void any_method_name(EventObject)
 * </pre>
 * 
 * </li>
 * <li>the method must have a State annotation</li>
 * <li>the state specified in the State annotation must match the current state
 * of the source</li>
 * <li>the event type of the argument must be one of the super types of the
 * event.
 * <li>
 * </ul>
 * TODO: if there are multiple matches, should we invoke the most specific one
 * or all of them?
 * 
 * @author wchen
 */
public class AutowiredEventListener implements EventListener<Event<EventSource>> {

  private static final Logger log = Logger.getLogger(AutowiredEventListener.class);

  protected Object _target;

  protected ConcurrentMap<Class<Event<EventSource>>, List<AutowiredEventTarget>> _listeners = new ConcurrentHashMap<Class<Event<EventSource>>, List<AutowiredEventTarget>>();

  /**
   * Takes a target object on which to invoke event handlers
   * 
   * @param target
   */
  @SuppressWarnings("unchecked")
  public AutowiredEventListener(final Object target) {

    _target = target;
    if (target == null) {
      return;
    }
    final Method[] methods = target.getClass().getMethods();
    for (final Method m : methods) {
      if (!Modifier.isPublic(m.getModifiers())) {
        continue; // method must be public
      }
      if (m.getAnnotation(State.class) == null) {
        continue; // method must have state annotation.);
      }
      final Class<?>[] types = m.getParameterTypes();
      if (types.length != 1 || !Event.class.isAssignableFrom(types[0])) {
        continue; // method must have one parameter taking a subtype of Event
      }
      final Class<Event<EventSource>> eventType = (Class<Event<EventSource>>) types[0];
      final AutowiredEventTarget autowiredEventTarget = new AutowiredEventTarget(m, target);
      addTarget(eventType, autowiredEventTarget);
    }
  }

  private void addTarget(final Class<Event<EventSource>> eventType, final AutowiredEventTarget target) {
    List<AutowiredEventTarget> targets = _listeners.get(eventType);
    if (targets == null) {
      targets = new ArrayList<AutowiredEventTarget>();
      _listeners.put(eventType, targets);
    }
    for (final AutowiredEventTarget et : targets) {
      if (et.equals(target)) {
        return;
      }
    }
    targets.add(target);
  }

  @SuppressWarnings("unchecked")
  public void onEvent(final Event<EventSource> event) {
    try {
      Class<? extends Event<EventSource>> clz = (Class<? extends Event<EventSource>>) event.getClass();
      do {
        final List<AutowiredEventTarget> targets = _listeners.get(clz);
        if (targets != null) {
          for (final AutowiredEventTarget target : targets) {
            if (target.invoke(event)) {
              break;
            }
          }
        }
        clz = (Class<? extends Event<EventSource>>) clz.getSuperclass();
      }
      while (clz != null && !clz.equals(Object.class));
    }
    catch (final Throwable t) {
      log.error("", t);
    }
  }

  public Object getTarget() {
    return _target;
  }

}
