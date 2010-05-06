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

import java.util.LinkedList;
import java.util.List;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.resource.RTC;

public class MockSignalDetector implements SignalDetector {
  public List<MediaEventListener<SignalDetectorEvent>> listeners;

  public MediaGroup mediaGroup;

  public MockMediaSession session;

  @Override
  public void flushBuffer() throws MsControlException {

  }

  @Override
  public void receiveSignals(int arg0, Parameter[] arg1, RTC[] arg2, Parameters arg3) throws MsControlException {

  }

  @Override
  public void stop() {

  }

  @Override
  final public MediaGroup getContainer() {
    return mediaGroup;
  }

  final public void setContainer(MediaGroup theContainer) {
    mediaGroup = (MockMediaGroup) theContainer;
  }

  @Override
  final public void addListener(MediaEventListener<SignalDetectorEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<SignalDetectorEvent>>();
    }
    listeners.add(arg0);
  }

  @Override
  final public MediaSession getMediaSession() {
    return session;
  }

  final public void setMediaSession(MediaSession theSession) {
    session = (MockMediaSession) theSession;
  }

  @Override
  final public void removeListener(MediaEventListener<SignalDetectorEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<SignalDetectorEvent>>();
    }
    listeners.remove(arg0);
  }

}
