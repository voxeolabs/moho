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

import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;

/**
 * This provides the access to all the media functions.
 * 
 * @author wchen
 */
public interface MediaService {

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
  Output output(String text);

  /**
   * Render and output the resource to the call to which this service is
   * attached. If the current media channel is audio, the resource will be
   * rendered into audio based on the neogiated codec. If the current media
   * channel is instant messaging, URI itself will be sent.
   * 
   * @param media
   *          the resource to be rendered and output.
   * @return an output {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Output output(URI media);

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
  Output output(OutputCommand output);

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
  Prompt prompt(String text, String grammar, int repeat);

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
  Prompt prompt(URI media, String grammar, int repeat);

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
  Prompt prompt(OutputCommand output, InputCommand input, int repeat);

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
  Input input(String grammar);

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
  Input input(InputCommand input);

  /**
   * records the call from the call to which this service is attached.
   * 
   * @param recording
   *          the URI where to save the recording
   * @return an recording {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Recording record(URI recording);

  /**
   * records the call from the call to which this service is attached.
   * 
   * @param command
   * @return an recording {@link java.util.concurrent.Future Future}.
   * @throws MediaException
   *           when there is media server error.
   */
  Recording record(RecordCommand command);

  /**
   * return the underlying {@link javax.media.mscontrol.mediagroup.MediaGroup
   * MediaGroup}
   */
  MediaGroup getMediaGroup();

}
