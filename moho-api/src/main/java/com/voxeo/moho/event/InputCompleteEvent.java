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
 * This event is fired when an {@link com.voxeo.moho.media.Input Input} is
 * completed.
 * 
 * @author wchen
 */
public interface InputCompleteEvent<T extends EventSource> extends MediaCompleteEvent<T> {

  public enum Cause {
    /** the input is terminated because the initial silence is too long */
    INI_TIMEOUT,
    /** the input is terminated because the INTER_SIG_TIMEOUT_EXCEEDED */
    IS_TIMEOUT,
    /** the input is terminated by exceeding its max time allowed */
    MAX_TIMEOUT,
    /** the input is terminated by unknown error */
    ERROR,
    /** the input is canceled */
    CANCEL,
    /** the input is completed without a match */
    NO_MATCH,
    /** the input is completed with a match */
    MATCH,
    /** the input is terminated because of the first occurrence of speech **/
    START_OF_SPEECH,
    /** the input is terminated because of the first consecutive speech ends **/
    END_OF_SPEECH,
    /** the input is terminated because the source is disconnected */
    DISCONNECT,
    /** No input has been detected before the MAX_SILENCE timeout popped. **/
    MAX_SILENCE_TIMEOUT_EXPIRED,
    /**
     * The maximum amount of speech has been detected before the END_OF_SPEECH
     * event popped.
     **/
    _MAX_SPEECH_DETECTED, UNKNOWN
  }

  /**
   * @return the concept of voice or DTMF inputs
   */
  String getConcept();

  /**
   * @return the full semantic interpretation of voice or DTMF input
   */
  String getInterpretation();

  /**
   * @return the recognized voice or DTMF input before interpretation
   */
  String getUtterance();

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
   * @return the cause of the completion
   */
  Cause getCause();

  boolean hasMatch();

  /**
   * @return the type of the recognized voice or DTMF input
   */
  InputMode getInputMode();

  /**
   * @return the default semantic interpretation of voice or DTMF input
   */
  String getValue();

  /**
   * get error description text.
   * 
   * @return
   */
  String getErrorText();

  /**
   * get the semantic interpretation result slots.
   * 
   * @return semantic interpretation result slots for voice or DTMF input
   */
  Map<String, String> getSISlots();

  /**
   * @return the recognized signal
   */
  Signal getSignal();
}
