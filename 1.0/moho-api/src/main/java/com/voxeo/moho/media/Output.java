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

package com.voxeo.moho.media;

import com.voxeo.moho.event.OutputCompleteEvent;

/**
 * Output is a {@link java.util.Future Future} that holds the result of an 
 * {@link com.voxeo.moho.media.output.OutputCommand OutputCommand}.
 * 
 * @author wchen
 *
 */
public interface Output extends MediaOperation<OutputCompleteEvent> {

  /**
   * forward or rewind the output
   * @param direction forward if true, otherwise rewind
   * @param time the time period to be forwarded or rewinded.
   */
  void move(boolean direction, int time);

  /**
   * Jump the output to a specified the position
   * @param index ??
   */
  void jump(int index);

  /**
   * render the output in a faster or slower speed.
   * @param upOrDown faster if true, otherwise slower.
   */
  void speed(boolean upOrDown);

  /**
   * render the output in a higher or lower volume.
   * @param upOrDown higher if true, otherwise lower.
   */
  void volume(boolean upOrDown);

  /**
   * pause the output
   */
  void pause();

  /**
   * resume the output
   */
  void resume();

}
