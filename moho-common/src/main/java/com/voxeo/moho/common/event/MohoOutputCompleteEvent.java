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

package com.voxeo.moho.common.event;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.MediaOperation;

public class MohoOutputCompleteEvent<T extends EventSource> extends MohoMediaCompleteEvent<T> implements
    OutputCompleteEvent<T> {

  protected Cause _cause;

  protected String _errorText;

  public MohoOutputCompleteEvent(T source, Cause cause, MediaOperation mediaOperation) {
    super(source, mediaOperation);
    _cause = cause;
    _mediaOperation = mediaOperation;
  }

  public MohoOutputCompleteEvent(T source, Cause cause, String errorText, MediaOperation mediaOperation) {
    this(source, cause, mediaOperation);
    _errorText = errorText;
  }

  @Override
  public Cause getCause() {
    return _cause;
  }

  @Override
  public String getErrorText() {
    return _errorText;
  }
}
