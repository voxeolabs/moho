/**
 * Copyright 2010-2011 Voxeo Corporation
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

import java.util.Collection;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.services.Service;
import com.voxeo.servlet.xmpp.XmppFactory;

/**
 * <p>
 * ApplicationContext gives an application the.
 * </p>
 * <ul>
 * <li>access to application configuration parameters</li>
 * <li>storage for application runtime attribute data</li>
 * <li>object factory for @{link Endpoint}</li>
 * <li>access to the conference manager</li>
 * </ul>
 * 
 * @author wchen
 */
public interface ApplicationContext extends AttributeStore, ParameterStore {

  String APPLICATION = "com.voxeo.moho.application";

  String APPLICATION_CONTEXT = "com.voxeo.moho.application.context";

  String FRAMEWORK = "com.voxeo.moho.framework";

  /**
   * @return the underlying Java Media Control factory.
   */
  MsControlFactory getMSFactory();

  /**
   * @return the underlying SIP factory.
   */
  SipFactory getSipFactory();

  /**
   * @return the uderlying SDP factory.
   */
  SdpFactory getSdpFactory();
  
  /**
   * @return the uderlying XMPP factory.
   */
  XmppFactory getXmppFactory();

  /**
   * @param addr
   *          the address of an endpoint
   * @return the parsed {@link Endpoint Endpoint} object
   */
  Endpoint createEndpoint(String addr);

  /**
   * @param addr
   *          the address of an endpoint
   * @return the parsed {@link Endpoint Endpoint} object
   */
  Endpoint createEndpoint(String addr, String type);

  Application getApplication();

  ConferenceManager getConferenceManager();

  ServletContext getServletContext();

  String getRealPath(String path);

  <T extends Service> T getService(Class<T> def);

  <T extends Service> Collection<T> listServices();

  <T extends Service> boolean containsService(Class<T> def);
}
