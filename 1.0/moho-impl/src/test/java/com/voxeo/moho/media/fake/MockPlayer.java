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

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.resource.RTC;

public class MockPlayer implements Player {

  public List<MediaEventListener<PlayerEvent>> listeners;

  private MockMediaSession session;

  private MockMediaGroup mediaGroup;

  private URI[] uris;

  private RTC[] rtcs;

  final public void setUris(URI[] uris) {
    this.uris = uris;
  }

  final public void setRtcs(RTC[] rtcs) {
    this.rtcs = rtcs;
  }

  @Override
  public void play(URI[] arg0, RTC[] arg1, Parameters arg2) throws MsControlException {

  }

  @Override
  public void play(URI arg0, RTC[] arg1, Parameters arg2) throws MsControlException {

  }

  @Override
  public void stop(boolean arg0) {

  }

  @Override
  final public MediaGroup getContainer() {
    return mediaGroup;
  }

  final public void setContainer(MediaGroup theContainer) {
    mediaGroup = (MockMediaGroup) theContainer;
  }

  @Override
  final public void addListener(MediaEventListener<PlayerEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<PlayerEvent>>();
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
  final public void removeListener(MediaEventListener<PlayerEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<PlayerEvent>>();
    }
    listeners.remove(arg0);
  }
}
