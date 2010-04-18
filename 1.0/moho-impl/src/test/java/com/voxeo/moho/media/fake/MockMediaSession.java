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

package com.voxeo.moho.media.fake;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.vxml.VxmlDialog;

public class MockMediaSession implements MediaSession {

  private Map<String, Object> _attributes;

  @Override
  public MediaGroup createMediaGroup(Configuration<MediaGroup> arg0) throws MsControlException {
    // TODO
    return null;
  }

  @Override
  final public MediaGroup createMediaGroup(Configuration<MediaGroup> arg0, Parameters arg1) throws MsControlException {
    return createMediaGroup(null);
  }

  @Override
  final public MediaGroup createMediaGroup(MediaConfig arg0, Parameters arg1) throws MsControlException {
    return createMediaGroup(null);
  }

  @Override
  public MediaMixer createMediaMixer(Configuration<MediaMixer> arg0) throws MsControlException {

    return null;
  }

  @Override
  public MediaMixer createMediaMixer(Configuration<MediaMixer> arg0, Parameters arg1) throws MsControlException {

    return null;
  }

  @Override
  public MediaMixer createMediaMixer(MediaConfig arg0, Parameters arg1) throws MsControlException {

    return null;
  }

  @Override
  public NetworkConnection createNetworkConnection(Configuration<NetworkConnection> arg0) throws MsControlException {

    return null;
  }

  @Override
  public NetworkConnection createNetworkConnection(Configuration<NetworkConnection> arg0, Parameters arg1)
      throws MsControlException {

    return null;
  }

  @Override
  public NetworkConnection createNetworkConnection(MediaConfig arg0, Parameters arg1) throws MsControlException {

    return null;
  }

  @Override
  public VxmlDialog createVxmlDialog(Parameters arg0) throws MsControlException {

    return null;
  }

  @Override
  final public Object getAttribute(String arg0) {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    return _attributes.get(arg0);
  }

  @Override
  public Iterator<String> getAttributeNames() {

    return null;
  }

  @Override
  final public void removeAttribute(String arg0) {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.remove(arg0);
  }

  @Override
  final public void setAttribute(String arg0, Object arg1) {
    if (_attributes == null) {
      _attributes = new ConcurrentHashMap<String, Object>();
    }
    _attributes.put(arg0, arg1);
  }

  @Override
  public Parameters createParameters() {

    return null;
  }

  @Override
  public Iterator<MediaObject> getMediaObjects() {

    return null;
  }

  @Override
  public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0) {

    return null;
  }

  @Override
  public Parameters getParameters(Parameter[] arg0) {

    return null;
  }

  @Override
  public URI getURI() {

    return null;
  }

  @Override
  public void release() {

  }

  @Override
  public void setParameters(Parameters arg0) {

  }
}
