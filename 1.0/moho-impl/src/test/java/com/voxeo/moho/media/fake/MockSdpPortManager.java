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
import javax.media.mscontrol.networkconnection.CodecPolicy;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;

public class MockSdpPortManager implements SdpPortManager {

  public List<MediaEventListener<SdpPortManagerEvent>> listeners = new LinkedList<MediaEventListener<SdpPortManagerEvent>>();

  private MediaSession mediaSession;

  private NetworkConnection networkConnection;

  @Override
  public void generateSdpOffer() throws SdpPortManagerException {

  }

  @Override
  public CodecPolicy getCodecPolicy() {
    return null;
  }

  @Override
  public byte[] getMediaServerSessionDescription() throws SdpPortManagerException {
    return null;
  }

  @Override
  public byte[] getUserAgentSessionDescription() throws SdpPortManagerException {
    return null;
  }

  @Override
  public void processSdpAnswer(byte[] arg0) throws SdpPortManagerException {
  }

  @Override
  public void processSdpOffer(byte[] arg0) throws SdpPortManagerException {

  }

  @Override
  public void rejectSdpOffer() throws SdpPortManagerException {

  }

  @Override
  public void setCodecPolicy(CodecPolicy arg0) throws SdpPortManagerException {

  }

  @Override
  final public NetworkConnection getContainer() {
    return networkConnection;
  }

  final public void setNetworkConnection(NetworkConnection m) {
    networkConnection = m;
  }

  @Override
  final public void addListener(MediaEventListener<SdpPortManagerEvent> arg0) {
    if (listeners == null) {
      listeners = new LinkedList<MediaEventListener<SdpPortManagerEvent>>();
    }
    listeners.add(arg0);
  }

  @Override
  final public MediaSession getMediaSession() {
    return mediaSession;
  }

  final public void setMediaSession(MediaSession m) {
    mediaSession = m;
  }

  @Override
  final public void removeListener(MediaEventListener<SdpPortManagerEvent> arg0) {
    listeners.remove(arg0);
  }

}
