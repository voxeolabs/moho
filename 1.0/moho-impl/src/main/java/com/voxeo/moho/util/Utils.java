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

package com.voxeo.moho.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ThreadFactory;

import com.voxeo.utils.EventListener;

public class Utils {

  public static Class getGenericType(final Object o) {
    for (Class clz = o.getClass(); clz != null && !clz.equals(Object.class); clz = clz.getSuperclass()) {
      for (final Type type : clz.getGenericInterfaces()) {
        if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class
            && ((ParameterizedType) type).getRawType().equals(EventListener.class)) {
          Type argument = ((ParameterizedType) type).getActualTypeArguments()[0];
          while (argument instanceof ParameterizedType) {
            argument = ((ParameterizedType) argument).getRawType();
          }
          if (argument instanceof Class) {
            return (Class) argument;
          }
        }
      }
    }
    return null;
  }

  public static class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable r) {
      final Thread t = new Thread(r);
      t.setDaemon(true);
      return t;
    }

  }

}
