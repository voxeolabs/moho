/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.voicexml;

import java.util.Map;
import java.util.concurrent.Future;

import com.voxeo.moho.Participant;

/**
 * Dialog represents a dialog between a {@link com.voxeo.moho.Call Call} and a
 * VoiceXML browser. Since a dialog might take a long time to complete,
 * Dialog is both a {@link java.util.concurrent.Future Future} and 
 * {@link com.voxeo.moho.event.EventSource EventSource}, which allows the application to
 * pull/wait on the final outcome as well as listening on the events.
 * 
 * @author wchen
 */
public interface Dialog extends Participant, Future<Map<String, Object>> {

  /**
   * prepare the VoiceXML dialog
   */
  void prepare();

  /**
   * start the VoiceXML dialog
   */
  void start();

  /**
   * terminate the VoiceXML dialog
   * @param immediate whether to terminate immediately.
   */
  void terminate(final boolean immediate);

}
