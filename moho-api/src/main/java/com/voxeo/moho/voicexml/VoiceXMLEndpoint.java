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

package com.voxeo.moho.voicexml;

import java.net.URL;
import java.util.Map;

import com.voxeo.moho.Endpoint;

/**
 * This represents the address of a VoiceXML application, based on RFC 5552.
 * 
 * @author wchen
 */
public interface VoiceXMLEndpoint extends Endpoint {

  /**
   * @return the URL of the VoiceXML file
   */
  URL getDocumentURL();

  /**
   * Create a VoiceXML {@link Dialog Dialog} instance
   * @param params parameters to be passed to the VoiceXML browser
   * @return the instance of dialog
   */
  Dialog create(Map<Object, Object> params);

}
