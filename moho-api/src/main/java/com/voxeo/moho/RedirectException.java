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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Thrown when the called Endpoint return a redirect signal.
 */
public class RedirectException extends SignalException {

  private static final long serialVersionUID = -8620383625996859747L;

  protected List<String> _targets = new LinkedList<String>();

  public RedirectException(final ListIterator<String> targets) {
    while (targets.hasNext()) {
      _targets.add(targets.next());
    }
  }

  public String getTarget() {
    if (_targets.size() > 0) {
      _targets.get(0);
    }
    return null;
  }

  public List<String> getTargets() {
    return _targets;
  }
}
