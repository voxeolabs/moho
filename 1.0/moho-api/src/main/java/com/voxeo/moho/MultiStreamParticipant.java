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

package com.voxeo.moho;

import javax.media.mscontrol.join.JoinableStream;

/**
 * A type of Participant which can have multiple media streams
 * 
 * @author wchen
 */
public interface MultiStreamParticipant extends Participant {

  /**
   * @param value
   *          Identifies a type of media, like audio, video
   * @return a JoinableStream, referencing the media of the given type; It can
   *         be used to restrict a join to this specific stream. null if the
   *         container does not support this type of media.
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           or the call isn't answered.
   */
  JoinableStream getJoinableStream(JoinableStream.StreamType value);

  /**
   * @return an array of all existing streams. The array may contain more than
   *         one stream of each type (e.g. two audio, one video).
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           or the call isn't answered.
   */
  JoinableStream[] getJoinableStreams();
}
