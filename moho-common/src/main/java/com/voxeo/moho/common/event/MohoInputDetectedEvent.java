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
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class MohoInputDetectedEvent<T extends EventSource> extends MohoMediaNotificationEvent<T> implements InputDetectedEvent<T> {

  protected String _input = null;

  public MohoInputDetectedEvent(final T source, final String input) {
    super(source);
    _input = input;
  }

  @Override
  public String getInput() {
    return _input;
  }

  @Override
	public boolean isEndOfSpeech() {
		// TODO Auto-generated method stub
		return false;
	}
  
  @Override
	public boolean isStartOfSpeech() {
		// TODO Auto-generated method stub
		return false;
	}
  
  @Override
	public Signal getSignal() {
		// TODO Auto-generated method stub
		return null;
	}
}
