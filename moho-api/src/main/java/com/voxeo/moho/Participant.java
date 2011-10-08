/**
 * Copyright 2010-2011 Voxeo Corporation Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.event.EventSource;

/**
 * This interface represents an abstract party involved in a conversation.
 * 
 * @author wchen
 */
public interface Participant extends EventSource {

  public enum JoinType {
    /** media is bridged at the media server */
    BRIDGE, // BRIDGE_EXCLUSIVE_KEEP

    BRIDGE_EXCLUSIVE_REPLACE,

    BRIDGE_SHARED,

    /** media is connected between two endpoints */
    DIRECT;

    public static boolean isBridge(JoinType type) {
      if (type != null && (type.equals(BRIDGE) || type.equals(BRIDGE_EXCLUSIVE_REPLACE) || type.equals(BRIDGE_SHARED))) {
        return true;
      }
      return false;
    }
  }

  /**
   * @return the address of this participant
   */
  Endpoint getAddress();

  /**
   * Connect this participant with the specified participant.
   * 
   * @param other
   *          the other participant to be connected with.
   * @param type
   *          whether the media is bridged or direct between the two
   *          participants
   * @param direction
   *          whether the media is full duplex or half-duplex between the two
   *          participants
   * @throws IllegalStateException
   *           if the participant has been released.
   */
  Joint join(Participant other, JoinType type, Direction direction);

  /**
   * Disconnect from the specified participant. If the specific participant is
   * the last joinee, this participant will be either left on the media server
   * or no media connection at all, depending on the previous media path.
   * 
   * @param other
   *          the specified participant to be unjoined.
   */
  Unjoint unjoin(Participant other);

  /**
   * @return the array of current joined participants.
   */
  Participant[] getParticipants();

  /**
   * @param direction
   *          the join direction of the participants
   * @return the array of the current joined participants in the specified
   *         direction.
   */
  Participant[] getParticipants(Joinable.Direction direction);

  /**
   * disconnect this participant.
   */
  void disconnect();

  /**
   * @return the underlying {@link javax.media.mscontrol.MediaObject
   *         MediaObject}.
   */
  MediaObject getMediaObject();

  /**
   * get remote address used for remote join
   * 
   * @return remote address
   */
  String getRemoteAddress();
}
