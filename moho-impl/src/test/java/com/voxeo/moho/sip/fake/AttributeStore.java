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

package com.voxeo.moho.sip.fake;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeStore {

  private Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();

  final public Object getAttribute(String s) throws NullPointerException, IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    return _attributes.get(s);
  }

  final public Enumeration<String> getAttributeNames() throws IllegalStateException {

    return null;
  }

  final public void removeAttribute(String s) throws IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.remove(s);
  }

  final public void setAttribute(String s, Object obj) throws NullPointerException, IllegalStateException {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.put(s, obj);
  }
}
