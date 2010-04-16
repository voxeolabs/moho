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

package com.voxeo.moho.imified;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IMiiedController extends HttpServlet {

  private static final long serialVersionUID = -8375162918116822683L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    Request req = new Request();
    req._routekey = request.getParameter("routekey");
    req._botkey = request.getParameter("botkey");
    req._msg = request.getParameter("msg");
    req._userkey = request.getParameter("userkey");
    req._network = request.getParameter("network");
    if (req._userkey == null) {
      req._user = request.getParameter("user");
    }
  }

  class Request {
    String _routekey;
    String _botkey;
    String _msg;
    String _userkey;
    String _network;
    String _user;
  }
}
