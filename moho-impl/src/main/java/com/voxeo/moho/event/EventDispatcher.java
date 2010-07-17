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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.voxeo.utils.EnumEvent;
import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;
import com.voxeo.utils.SynchronousExecutor;

public class EventDispatcher {

  private static final Logger log = Logger.getLogger(EventDispatcher.class);

  private ConcurrentHashMap<Class<?>, List<Object>> clazzListeners = new ConcurrentHashMap<Class<?>, List<Object>>();

  private ConcurrentHashMap<Object, List<Object>> enumListeners = new ConcurrentHashMap<Object, List<Object>>();

  private ConcurrentHashMap<Object, List<Object>> lifecycleObjectMap = new ConcurrentHashMap<Object, List<Object>>();

  private Executor executor = SynchronousExecutor.get();

  private Lock lifecycleLock = new ReentrantLock();

  private Queue<FutureTask<?>> _queue = new LinkedList<FutureTask<?>>();

  private boolean processorRunning = false;

  public EventDispatcher() {
  }

  public EventDispatcher(final Executor executor) {
    this.executor = executor;
  }

  public void addListener(final Class<?> eventClazz, final EventListener<?> listener) {
    List<Object> list = clazzListeners.get(eventClazz);
    if (list == null) {
      list = new CopyOnWriteArrayList<Object>();
      final List<Object> existing = clazzListeners.putIfAbsent(eventClazz, list);
      if (existing != null) {
        existing.add(listener);
      }
      else {
        list.add(listener);
      }
    }
    else {
      list.add(listener);
    }
  }

  public <E extends Enum<E>> void addListener(final E type, final EventListener<? extends EnumEvent<?, E>> listener) {
    List<Object> list = enumListeners.get(type);
    if (list == null) {
      list = new CopyOnWriteArrayList<Object>();
      final List<Object> existing = enumListeners.putIfAbsent(type, list);
      if (existing != null) {
        existing.add(listener);
      }
      else {
        list.add(listener);
      }
    }
    else {
      list.add(listener);
    }
  }

  /**
   * @param lifecycleObject
   *          - This is a way of grouping listeners together, for convenient
   *          removal later. Listeners added with this method can be removed
   *          later in one shot by calling removeListenersForLifecycleObject()
   */
  public void addListener(final Object lifecycleObject, final Class<?> eventClazz, final EventListener<?> listener) {
    lifecycleLock.lock();
    try {
      addToLifecycleMap(lifecycleObject, listener);
      addListener(eventClazz, listener);
    }
    finally {
      lifecycleLock.unlock();
    }
  }

  /**
   * @param lifecycleObject
   *          - This is a way of grouping listeners together, for convenient
   *          removal later. Listeners added with this method can be removed
   *          later in one shot by calling removeListenersForLifecycleObject()
   */
  public <E extends Enum<E>> void addListener(final Object lifecycleObject, final E type,
      final EventListener<? extends EnumEvent<?, E>> listener) {
    lifecycleLock.lock();
    try {
      addToLifecycleMap(lifecycleObject, listener);
      addListener(type, listener);
    }
    finally {
      lifecycleLock.unlock();
    }
  }

  private void addToLifecycleMap(final Object lifecycleObject, final EventListener<?> listener) {
    List<Object> list = lifecycleObjectMap.get(lifecycleObject);
    if (list == null) {
      list = new CopyOnWriteArrayList<Object>();
      final List<Object> existing = lifecycleObjectMap.putIfAbsent(lifecycleObject, list);
      if (existing != null) {
        existing.add(listener);
      }
      else {
        list.add(listener);
      }
    }
    else {
      list.add(listener);
    }
  }

  /**
   * @param lifecycleObject
   *          - Removes all listeners that were added with
   *          {@link #addListener(Object, Enum, EventListener)} or
   *          {@link #addListener(Object, Class, EventListener)}
   */
  public void removeListenersForLifecycleObject(final Object lifecycleObject) {
    lifecycleLock.lock();
    try {
      if (lifecycleObjectMap != null) {
        final List<Object> list = lifecycleObjectMap.remove(lifecycleObject);
        if (list != null) {
          for (final Object o : list) {
            removeListener((EventListener<?>) o);
          }
        }
      }
    }
    finally {
      lifecycleLock.unlock();
    }
  }

  public void removeListener(final EventListener<?> listener) {
    for (final List<Object> list : clazzListeners.values()) {
      list.remove(listener);
    }
    for (final List<Object> list : enumListeners.values()) {
      list.remove(listener);
    }
  }

  public <S, T extends Event<S>> Future<T> fire(final T event) {
    return fire(event, false);
  }

  public <S, T extends Event<S>> Future<T> fire(final T event, final boolean narrowType) {
    return fire(event, narrowType, null);
  }

  public <S, T extends Event<S>> Future<T> fire(final T event, final boolean narrowType, final Runnable afterExec) {

    final FutureTask<T> task = new FutureTask<T>(new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        if (log.isTraceEnabled()) {
          log.trace("Firing event :" + event);
        }
        Class<? extends Event> clazz = event.getClass();
        do {
          final List<Object> list = clazzListeners.get(clazz);
          if (list != null) {
            for (final Object listener : list) {
              ((EventListener<T>) listener).onEvent(event);
            }
          }
          clazz = (Class<? extends Event>) clazz.getSuperclass();
        }
        while (narrowType && !clazz.equals(Object.class));

        if (event instanceof EnumEvent) {
          final EnumEvent<S, ? extends Enum<?>> enumEvent = (EnumEvent<S, ? extends Enum<?>>) event;
          final List<Object> list = enumListeners.get(enumEvent.type);
          if (list != null) {
            for (final Object listener : list) {
              ((EventListener<T>) listener).onEvent(event);
            }
          }
        }

        if (afterExec != null) {
          afterExec.run();
        }

      }
    }, event);

    synchronized (_queue) {
      boolean excuteProcessor = false;
      _queue.offer(task);

      if (!processorRunning) {
        processorRunning = true;
        excuteProcessor = true;
      }

      if (excuteProcessor) {
        executor.execute(new TaskProcessor());
      }
    }

    return task;
  }

  private class TaskProcessor implements Runnable {
    public void run() {
      while (true) {
        FutureTask<?> task = null;
        synchronized (_queue) {
          task = _queue.poll();
          if (task == null) {
            processorRunning = false;
            break;
          }
        }

        task.run();
      }
    }
  }

  public void setExecutor(final Executor executor) {
    this.executor = executor;
  }
}
