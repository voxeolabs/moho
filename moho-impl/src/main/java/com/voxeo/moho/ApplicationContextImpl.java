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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import org.apache.http.impl.client.DefaultHttpClient;

import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.conference.ConferenceMangerImpl;
import com.voxeo.moho.media.GenericMediaServiceFactory;
import com.voxeo.moho.sip.SIPEndpointImpl;
import com.voxeo.moho.text.imified.ImifiedEndpointImpl;
import com.voxeo.moho.util.Utils.DaemonThreadFactory;
import com.voxeo.moho.voicexml.VoiceXMLEndpointImpl;

public class ApplicationContextImpl extends AttributeStoreImpl implements ExecutionContext {

  protected Application _application;

  protected MsControlFactory _mcFactory;

  protected ConferenceManager _confMgr;

  protected SipFactory _sipFactory;

  protected SdpFactory _sdpFactory;

  protected MediaServiceFactory _msFactory;

  protected String _controller;

  protected Map<String, Call> _calls;

  protected Map<String, String> _parameters;

  protected String _imifiedApiURL;

  protected DefaultHttpClient _httpClient;

  protected ServletContext _servletContext;

  protected ThreadPoolExecutor _executor;

  public ApplicationContextImpl(final Application app, final MsControlFactory mc, final SipFactory sip,
      final SdpFactory sdp, final String controller, final ServletContext servletContext, final int threadPoolSize) {
    _application = app;
    _mcFactory = mc;
    _sipFactory = sip;
    _sdpFactory = sdp;
    _controller = controller;
    _confMgr = new ConferenceMangerImpl(this);
    _calls = new ConcurrentHashMap<String, Call>();
    _msFactory = new GenericMediaServiceFactory();
    _parameters = new ConcurrentHashMap<String, String>();
    _servletContext = servletContext;

    _executor = new ThreadPoolExecutor(threadPoolSize, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
  }

  @Override
  public Application getApplication() {
    return _application;
  }

  @Override
  public Endpoint getEndpoint(final String addr) {
    if (addr == null) {
      throw new IllegalArgumentException("argument is null");
    }
    try {
      if (addr.startsWith("sip:") || addr.startsWith("sips:") || addr.startsWith("<sip:") || addr.startsWith("<sips:")) {
        return new SIPEndpointImpl(this, _sipFactory.createAddress(addr));
      }
      else if (addr.startsWith("mscontrol://")) {
        return new MixerEndpointImpl(this, addr);
      }
      else if (addr.startsWith("file://") || addr.startsWith("http://") || addr.startsWith("https://")
          || addr.startsWith("ftp://")) {
        return new VoiceXMLEndpointImpl(this, addr);
      }
      else if (addr.startsWith("im:")) {
        return new ImifiedEndpointImpl(this, addr.substring(addr.indexOf(":") + 1));
      }
      else {
        throw new IllegalArgumentException("Unsupported format: " + addr);
      }
    }
    catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public MsControlFactory getMSFactory() {
    return _mcFactory;
  }

  @Override
  public SipFactory getSipFactory() {
    return _sipFactory;
  }

  @Override
  public ConferenceManager getConferenceManager() {
    return _confMgr;
  }

  @Override
  public Executor getExecutor() {
    return _executor;
  }

  @Override
  public Call getCall(final String cid) {
    return _calls.get(cid);
  }

  @Override
  public void addCall(final Call call) {
    _calls.put(call.getId(), call);
  }

  @Override
  public void removeCall(final String id) {
    _calls.remove(id);
  }

  @Override
  public String getParameter(final String name) {
    return _parameters.get(name);
  }

  @Override
  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(_parameters);
  }

  public String getController() {
    return _controller;
  }

  public void setParameter(final String name, final String value) {
    _parameters.put(name, value);
  }

  @Override
  public SdpFactory getSdpFactory() {
    return _sdpFactory;
  }

  @Override
  public MediaServiceFactory getMediaServiceFactory() {
    return _msFactory;
  }

  public String getImifiedApiURL() {
    return _imifiedApiURL;
  }

  public void setImifiedApiURL(final String imifiedApiURL) {
    this._imifiedApiURL = imifiedApiURL;
  }

  public DefaultHttpClient getHttpClient() {
    return _httpClient;
  }

  public void setHttpClient(final DefaultHttpClient httpClient) {
    _httpClient = httpClient;
  }

  @Override
  public ServletContext getServletContext() {
    return _servletContext;
  }

  public void destroy() {
    getApplication().destroy();
    _executor.shutdown();
  }
}
