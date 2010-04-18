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

package com.voxeo.moho;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeStoreImpl implements AttributeStore {

  private Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();

  @Override
  public Object getAttribute(final String name) {
    if (name == null) {
      return null;
    }
    return _attributes.get(name);
  }

  @Override
  public Map<String, Object> getAttributeMap() {
    return new HashMap<String, Object>(_attributes);
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    if (value == null) {
      _attributes.remove(name);
    }
    else {
      _attributes.put(name, value);
    }
  }

}
