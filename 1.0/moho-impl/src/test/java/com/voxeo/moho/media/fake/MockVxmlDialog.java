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

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.vxml.VxmlDialog;
import javax.media.mscontrol.vxml.VxmlDialogEvent;

public class MockVxmlDialog implements VxmlDialog {

  public List<MediaEventListener<VxmlDialogEvent>> listeners;

  public MockMediaSession session;

  @Override
  public void acceptEvent(String arg0, Map<String, Object> arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void prepare(URL arg0, Parameters arg1, Map<String, Object> arg2) {
    // TODO Auto-generated method stub

  }

  @Override
  public void prepare(String arg0, Parameters arg1, Map<String, Object> arg2) {
    // TODO Auto-generated method stub

  }

  @Override
  public void start(Map<String, Object> arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void terminate(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public Joinable[] getJoinables() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Joinable[] getJoinees() throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Joinable[] getJoinees(Direction arg0) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void join(Direction arg0, Joinable arg1) throws MsControlException {
    // TODO Auto-generated method stub

  }

  @Override
  public void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) throws MsControlException {
    // TODO Auto-generated method stub

  }

  @Override
  public void unjoin(Joinable arg0) throws MsControlException {
    // TODO Auto-generated method stub

  }

  @Override
  public void unjoinInitiate(Joinable arg0, Serializable arg1) throws MsControlException {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListener(JoinEventListener arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public MediaSession getMediaSession() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeListener(JoinEventListener arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public Parameters createParameters() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterator<MediaObject> getMediaObjects() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Parameters getParameters(Parameter[] arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public URI getURI() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void release() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setParameters(Parameters arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  final public void addListener(MediaEventListener<VxmlDialogEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<VxmlDialogEvent>>();
    }
    listeners.add(arg0);
  }

  @Override
  final public void removeListener(MediaEventListener<VxmlDialogEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<VxmlDialogEvent>>();
    }
    listeners.remove(arg0);
  }

}
