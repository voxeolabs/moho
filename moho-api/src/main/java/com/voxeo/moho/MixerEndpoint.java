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

import java.util.Map;

/**
 * The endpoint represent a {@link com.voxeo.moho.Mixer Mixer}.
 */
public interface MixerEndpoint extends Endpoint {

  public static final String DEFAULT_MIXER_ENDPOINT = "mscontrol://DEFAULT_MIXER_ENDPOINT";

  /**
   * Create a mixer based on this endpoint.
   * 
   * @param params
   *          used to create mixer.
   * @return
   * @throws MediaException
   */
  Mixer create(Map<Object, Object> params) throws MediaException;

}
