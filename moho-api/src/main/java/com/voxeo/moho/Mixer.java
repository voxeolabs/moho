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

import java.util.Properties;

import javax.media.mscontrol.join.Joinable.Direction;

/**
 * This is used to mix multiple {@link com.voxeo.moho.Participant Participant}
 * together. A mixer is also a Participant, so other Participants can be
 * joined/unjoined to a Mixer. The media streams of joined participants are
 * mixed (or add, or sum) into a single stream, then the result stream is sent
 * out to every joined Participant.
 */
public interface Mixer extends MultiStreamParticipant {

  /**
   * Get the media service attached to the Mixer.
   * 
   * @return media service attached to the Mixer
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the Mixer is released.
   */
  MediaService getMediaService();

  /**
   * Connect this Mixer with the specified participant, can specify some
   * properties such as playTones = false
   * 
   * @param other
   *          the other participant to be connected with.
   * @param type
   *          whether the media is bridged or direct between the two
   *          participants
   * @param direction
   *          whether the media is full duplex or half-duplex between the two
   *          participants
   * @param props
   *          specify parameters
   * @throws IllegalStateException
   *           if the participant has been released.
   * @return
   */
  Joint join(Participant other, JoinType type, Direction direction, Properties props);

}
