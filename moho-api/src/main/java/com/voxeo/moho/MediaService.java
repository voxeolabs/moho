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

import java.net.URI;

import javax.media.mscontrol.mediagroup.MediaGroup;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;

/**
 * This interface encapsulates media functions.
 * 
 * @author wchen
 */
public interface MediaService<T extends EventSource> {

  /**
   * Render and output the text to the call to which this service is attached.
   * If the current media channel is audio, text-to-speech will be performed. If
   * the current media channel is instant messaging, text message will be sent.
   * 
   * @param text
   *          the text to be rendered and output.
   * @return an output {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Output<T> output(String text) throws MediaException;

  /**
   * Render and output the resource to the call to which this service is
   * attached. If the current media channel is audio, the resource will be
   * rendered into audio based on the negotiated codec. If the current media
   * channel is instant messaging, URI itself will be sent.
   * 
   * @param media
   *          the resource to be rendered and output.
   * @return an output {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Output<T> output(URI media) throws MediaException;

  /**
   * Render and output content based on the {@link OutputCommand OutputCommand}
   * to the call which the service is attached.
   * 
   * @param output
   *          the {@link OutputCommand OutputCommand}.
   * @return an output {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Output<T> output(OutputCommand output) throws MediaException;

  /**
   * Equivalent of {@link #output(String) output(text)} and
   * {@link #input(String) input(grammar)}.
   * 
   * @param text
   *          the text to be rendered and output
   * @param grammar
   *          the grammar for recognizing input
   * @param repeat
   *          how many times to repeat the output until an recognized input is
   *          received.
   * @return a prompt {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Prompt<T> prompt(String text, String grammar, int repeat) throws MediaException;

  /**
   * Equivalent of {@link #output(String) output(text)} and
   * {@link #input(String) input(grammar)}.
   * 
   * @param media
   *          the media to be rendered and output
   * @param grammar
   *          the grammar for recognizing input
   * @param repeat
   *          how many times to repeat the output until an recognized input is
   *          received.
   * @return a Prompt {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Prompt<T> prompt(URI media, String grammar, int repeat) throws MediaException;

  /**
   * Equivalent of {@link #output(OutputCommand) output(output)} and
   * {@link #input(InputCommand) input(input)}.
   * 
   * @param output
   *          the output command
   * @param input
   *          the input command
   * @param repeat
   *          how many times to repeat the output until an recognized input is
   *          received.
   * @return a Prompt {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Prompt<T> prompt(OutputCommand output, InputCommand input, int repeat) throws MediaException;

  /**
   * Waits for the input from the call to which this service is attached for
   * recognition based on grammar.
   * 
   * @param grammar
   *          the recognition grammar
   * @return an input {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Input<T> input(String grammar) throws MediaException;

  /**
   * Waits for the input from the call to which this service is attached, based
   * on InputCommand.
   * 
   * @param input
   *          the {@link InputCommand InputCommand}
   * @return an input {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Input<T> input(InputCommand input) throws MediaException;

  /**
   * records the call from the call to which this service is attached.
   * 
   * @param recording
   *          the URI where to save the recording
   * @return an recording {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Recording<T> record(URI recording) throws MediaException;

  /**
   * records the call from the call to which this service is attached.
   * 
   * @param command
   * @return an recording {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Recording<T> record(RecordCommand command) throws MediaException;

  /**
   * return the underlying {@link javax.media.mscontrol.mediagroup.MediaGroup
   * MediaGroup}
   */
  MediaGroup getMediaGroup();

}
