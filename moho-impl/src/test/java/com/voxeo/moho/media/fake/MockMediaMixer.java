package com.voxeo.moho.media.fake;

import java.net.URI;
import java.util.Iterator;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerAdapter;
import javax.media.mscontrol.mixer.MixerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

public class MockMediaMixer extends MockJoinable implements MediaMixer {

  @Override
  public JoinableStream getJoinableStream(StreamType arg0) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JoinableStream[] getJoinableStreams() throws MsControlException {
    // TODO Auto-generated method stub
    return null;
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
  public void confirm() throws MsControlException {
    // TODO Auto-generated method stub

  }

  @Override
  public MediaConfig getConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R> R getResource(Class<R> arg0) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void triggerAction(Action arg0) {
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
  public void addListener(AllocationEventListener arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(AllocationEventListener arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListener(MediaEventListener<MixerEvent> arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(MediaEventListener<MixerEvent> arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public MixerAdapter createMixerAdapter(Configuration<MixerAdapter> arg0) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MixerAdapter createMixerAdapter(Configuration<MixerAdapter> arg0, Parameters arg1) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MixerAdapter createMixerAdapter(MediaConfig arg0, Parameters arg1) throws MsControlException {
    // TODO Auto-generated method stub
    return null;
  }
}
