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

import com.voxeo.moho.Application;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.utils.EventListener;

public class ApplicationEventSource extends DispatchableEventSource {

  public ApplicationEventSource(final ExecutionContext context, final Application application) {
    super(context, false);
    if (application instanceof EventListener<?>) {
      addListener((EventListener<?>) application);
    }
    else {
      addObserver(application);
    }
  }
}
