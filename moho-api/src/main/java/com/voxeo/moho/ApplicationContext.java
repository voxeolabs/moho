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

import javax.servlet.ServletContext;

import com.voxeo.moho.conference.ConferenceManager;

/**
 * <p>
 * ApplicationContext gives an application the.
 * </p>
 * <ul>
 * <li>access to application configuration parameters</li>
 * <li>storage for application runtime attribute data</li>
 * <li>object factory for endpoint</li>
 * <li>access to the conference manager</li>
 * </ul>
 * 
 * @author wchen
 */
public interface ApplicationContext extends AttributeStore, ParameterStore {

  String APPLICATION = "com.voxeo.moho.application";

  String APPLICATION_CONTEXT = "com.voxeo.moho.application.context";

  @Deprecated
  Endpoint getEndpoint(String addr);
  
  Endpoint createEndpoint(String addr);

  Application getApplication();

  ConferenceManager getConferenceManager();

  MediaServiceFactory getMediaServiceFactory();

  ServletContext getServletContext();

  String getRealPath(String path);

}
