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

import com.voxeo.moho.Mixer;

/**
 * Represent a real conference that other {@link com.voxeo.moho.Participant
 * Participant} can <code>join/unjoin</code>. Every Participant that joined a
 * conference receive medias from all other participants in the same conference.
 */
public interface Conference extends Mixer {

    /**
     * Get the max capacity of this conference.
     * 
     * @return number of seats in this conference.
     */
    int getMaxSeats();

    /**
     * Get the number of seats that has been occupied in this conference.
     * 
     * @return number of seats that has been occupied in this conference.
     */
    int getOccupiedSeats();

    public ConferenceController getController();

    public void setController(ConferenceController controller);

}
