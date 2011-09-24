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

package com.voxeo.rayo.mohoremote.impl.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.utils.EventListener;

public class Utils {

  public static Class<?> getGenericType(final Object o) {
    for (Class<?> clz = o.getClass(); clz != null && !clz.equals(Object.class); clz = clz.getSuperclass()) {
      for (final Type type : clz.getGenericInterfaces()) {
        if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class
            && ((ParameterizedType) type).getRawType().equals(EventListener.class)) {
          Type argument = ((ParameterizedType) type).getActualTypeArguments()[0];
          while (argument instanceof ParameterizedType) {
            argument = ((ParameterizedType) argument).getRawType();
          }
          if (argument instanceof Class) {
            return (Class<?>) argument;
          }
        }
      }
    }

    for (Class<?> clz = o.getClass(); clz != null && !clz.equals(Object.class); clz = clz.getSuperclass()) {
      Type type = clz.getGenericSuperclass();
      if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class) {
        Type argument = ((ParameterizedType) type).getActualTypeArguments()[0];
        while (argument instanceof ParameterizedType) {
          argument = ((ParameterizedType) argument).getRawType();
        }
        if (argument instanceof Class && Event.class.isAssignableFrom((Class) argument)) {
          return (Class<?>) argument;
        }
      }
    }

    return null;
  }

  public static Class<?> getEventType(Class<?> clazz) {
    do {
      for (Class<?> intf : clazz.getInterfaces()) {
        if (Event.class.isAssignableFrom(intf)) {
          return intf;
        }
      }
      clazz = clazz.getSuperclass();
    }
    while (clazz != null);
    return null;
  }

  public static class DaemonThreadFactory implements ThreadFactory {
    private ThreadGroup group;

    private AtomicInteger id = new AtomicInteger(0);

    public DaemonThreadFactory(String groupName) {
      group = new ThreadGroup(groupName);
    }

    @Override
    public Thread newThread(final Runnable r) {
      final Thread t = new Thread(group, r, "MOHOREMOTE-" + id.getAndIncrement());
      t.setDaemon(true);
      return t;
    }

  }

}
