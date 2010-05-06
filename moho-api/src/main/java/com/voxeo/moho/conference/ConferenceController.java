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

package com.voxeo.moho.conference;

import com.voxeo.moho.Participant;

/**
 * Control the process when a {@link com.voxeo.moho.Participant Participant}
 * join/unjoin a {@link Conference}. The controller like an interceptor between
 * participant and conference.
 */
public interface ConferenceController {

  /**
   * Invoked for each participant just after invoked the join method on the
   * {@link Conference} to join the conference.
   * 
   * @param p
   *          the participant joining conference.
   * @param f
   *          the conference be joined.
   */
  void preJoin(Participant p, Conference f);

  /**
   * Invoked for each participant just before the invoked join method on the
   * {@link Conference} complete.
   * 
   * @param p
   * @param f
   */
  void postJoin(Participant p, Conference f);

  /**
   * Invoked for each participant just after invoked the unjoin method on the
   * {@link Conference} to unjoin the conference.
   * 
   * @param p
   *          the participant joining conference.
   * @param f
   *          the conference be joined.
   */
  void preUnjoin(Participant p, Conference f);

  /**
   * Invoked for each participant just before the invoked unjoin method on the
   * {@link Conference} complete.
   * 
   * @param p
   *          the participant joining conference.
   * @param f
   *          the conference be joined.
   */
  void postUnjoin(Participant p, Conference f);

}
