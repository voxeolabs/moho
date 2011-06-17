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
package com.voxeo.moho.spi;

import java.io.IOException;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public interface SIPDriver extends ProtocolDriver {

  void init(SpiFramework app, SipFactory sipF, SdpFactory sdpF, MsControlFactory mf, SipServlet servlet);

  void destroy();

  void doRequest(final SipServletRequest req) throws ServletException, IOException;

  void doResponse(final SipServletResponse res) throws ServletException, IOException;
}
