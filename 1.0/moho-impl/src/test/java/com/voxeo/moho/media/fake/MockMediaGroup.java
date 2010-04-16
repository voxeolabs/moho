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

package com.voxeo.moho.media.fake;

import java.io.Serializable;
import java.net.URI;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

public class MockMediaGroup implements MediaGroup {

  private static final long serialVersionUID = 8990490136717109437L;

  MockMediaSession session;

  public Parameters settedParameters;

  @Override
  public Player getPlayer() throws MsControlException {

    return null;
  }

  @Override
  public Recorder getRecorder() throws MsControlException {

    return null;
  }

  @Override
  public SignalDetector getSignalDetector() throws MsControlException {

    return null;
  }

  @Override
  public SignalGenerator getSignalGenerator() throws MsControlException {

    return null;
  }

  @Override
  public void stop() {

  }

  @Override
  public JoinableStream getJoinableStream(StreamType arg0) throws MsControlException {

    return null;
  }

  @Override
  public JoinableStream[] getJoinableStreams() throws MsControlException {

    return null;
  }

  @Override
  public Joinable[] getJoinees() throws MsControlException {

    return null;
  }

  @Override
  public Joinable[] getJoinees(Direction arg0) throws MsControlException {

    return null;
  }

  @Override
  public void join(Direction arg0, Joinable arg1) throws MsControlException {

  }

  @Override
  public void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) throws MsControlException {

  }

  @Override
  public void unjoin(Joinable arg0) throws MsControlException {

  }

  @Override
  public void unjoinInitiate(Joinable arg0, Serializable arg1) throws MsControlException {

  }

  @Override
  public void addListener(JoinEventListener arg0) {

  }

  @Override
  final public MediaSession getMediaSession() {
    return session;
  }

  final public void setMediaSession(MockMediaSession theSession) {
    session = theSession;
  }

  @Override
  public void removeListener(JoinEventListener arg0) {

  }

  @Override
  public void confirm() throws MsControlException {

  }

  @Override
  public MediaConfig getConfig() {

    return null;
  }

  @Override
  public <R> R getResource(Class<R> arg0) throws MsControlException {

    return null;
  }

  @Override
  public void triggerAction(Action arg0) {

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
  final public Parameters getParameters(Parameter[] arg0) {

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
  final public void setParameters(Parameters arg0) {
    settedParameters = arg0;
  }

  @Override
  public void addListener(AllocationEventListener arg0) {

  }

  @Override
  public void removeListener(AllocationEventListener arg0) {

  }

}
