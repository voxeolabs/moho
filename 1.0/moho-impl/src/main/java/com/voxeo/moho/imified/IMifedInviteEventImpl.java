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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventState;
import com.voxeo.moho.event.Observer;
import com.voxeo.utils.EventListener;

public class IMifedInviteEventImpl extends IMifiedInviteEvent implements HttpServletRequest {
  HttpServletRequest _request;

  HttpServletResponse _response;

  IMifedInviteEventImpl(final HttpServletRequest request, final HttpServletResponse response) {
    _request = request;
    _response = response;
  }

  @Override
  public CallableEndpoint getInvitee() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Endpoint getInvitor() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EventState getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void redirect(final Endpoint other) throws SignalException {
    // NOP
  }

  @Override
  public void redirect(final Endpoint other, final Map<String, String> headers) throws SignalException {
    // NOP
  }

  @Override
  public void reject(final Reason reason) throws SignalException {
    _response.setStatus(HttpServletResponse.SC_OK);
    try {
      _response.getWriter().write(reason.toString());
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }

  }

  @Override
  public void reject(final Reason reason, final Map<String, String> headers) throws SignalException {
    if (headers != null) {
      for (final Map.Entry<String, String> entry : headers.entrySet()) {
        _response.addHeader(entry.getKey(), entry.getValue());
      }
    }
    reject(reason);
  }

  @Override
  public String getAuthType() {
    return _request.getAuthType();
  }

  @Override
  public String getContextPath() {
    return _request.getContextPath();
  }

  @Override
  public Cookie[] getCookies() {
    return _request.getCookies();
  }

  @Override
  public long getDateHeader(final String arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getHeader(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<?> getHeaderNames() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<?> getHeaders(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getIntHeader(final String arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getMethod() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPathInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPathTranslated() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getQueryString() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRemoteUser() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRequestURI() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getServletPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HttpSession getSession() {
    return _request.getSession();
  }

  @Override
  public HttpSession getSession(final boolean arg0) {
    return _request.getSession(arg0);
  }

  @Override
  public Principal getUserPrincipal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isUserInRole(final String arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Object getAttribute(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<?> getAttributeNames() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getContentLength() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLocalAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLocalName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getLocalPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Locale getLocale() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<?> getLocales() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getParameter(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<?, ?> getParameterMap() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<?> getParameterNames() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getParameterValues(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getProtocol() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRealPath(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRemoteAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRemoteHost() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getRemotePort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(final String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getScheme() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getServerName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getServerPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean isSecure() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void removeAttribute(final String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAttribute(final String arg0, final Object arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setCharacterEncoding(final String arg0) throws UnsupportedEncodingException {
    // TODO Auto-generated method stub

  }

  @Override
  public HttpServletRequest getHttpRequest() {
    return this;
  }

  @Override
  public String getMessage() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {

  }

  @Override
  public void accept() throws SignalException, IllegalStateException {
  }

  @Override
  public Call acceptCall(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, IllegalStateException {
    return null;
  }

  @Override
  public Call acceptCall(final Map<String, String> headers, final Observer... observer) throws SignalException,
      IllegalStateException {
    return null;
  }

  @Override
  public Call acceptCallWithEarlyMedia(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, MediaException, IllegalStateException {
    return null;
  }

  @Override
  public Call acceptCallWithEarlyMedia(final Map<String, String> headers, final Observer... observers)
      throws SignalException, MediaException, IllegalStateException {
    return null;
  }
}
