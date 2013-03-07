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

package com.voxeo.moho.event;

import java.util.Map;

import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

/**
 * If the {@link com.voxeo.moho.Call Call} is in the supervised mode,
 * this event is fired when some input -- DTMF or speech -- is detected
 * to give the application greater control of the {@link com.voxeo.moho.media.Input Input}.
 * 
 * @author wchen
 *
 */
public interface InputDetectedEvent<T extends EventSource> extends MediaNotificationEvent<T> {

  /**
   * @return the concept of voice or DTMF inputs
   */
  String getConcept();

  /**
   * @return the full semantic interpretation of voice or DTMF input
   */
  String getInterpretation();

  /**
   * @return the ASR engine's confidence in the voice or DTMF input
   */
  float getConfidence();

  /**
   * @return the raw NLSML result returned from the underlying MRCP engine
   */
  String getNlsml();

  /**
   * @return the tag returned from the lower JSR-309 level
   */
  String getTag();

  /**
   * @return the type of the recognized voice or DTMF input
   */
  InputMode getInputMode();

  /**
   * get the semantic interpretation result slots.
   * 
   * @return semantic interpretation result slots for voice or DTMF input
   */
  Map<String, String> getSISlots();

  /**
   * @return the recognized voice or DTMF input
   */
  String getInput();

  /**
   * @return START-OF-SPEECH is detected
   */
  boolean isStartOfSpeech();

  /**
   * @return END-OF-SPEECH is detected
   */
  boolean isEndOfSpeech();

  /**
   * @return the recognized signal
   */
  Signal getSignal();

}
