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

package com.voxeo.moho.remote.impl.event;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.Recording;

public class MohoRecordCompleteEvent<T extends EventSource> extends MohoMediaCompleteEvent<T> implements
    RecordCompleteEvent<T> {
  protected Cause _cause;

  protected long _duration;

  protected String _errorText;

  public MohoRecordCompleteEvent(final T source, final Cause cause, long duration, Recording<T> mediaOperation) {
    super(source, mediaOperation);
    _cause = cause;
    _duration = duration;
  }

  public MohoRecordCompleteEvent(final T source, final Cause cause, long duration, String errorText,
      Recording<T> mediaOperation) {
    this(source, cause, duration, mediaOperation);
    _errorText = errorText;
  }

  @Override
  public Cause getCause() {
    return _cause;
  }

  @Override
  public long getDuration() {
    return _duration;
  }

  @Override
  public String getErrorText() {
    return _errorText;
  }
}
