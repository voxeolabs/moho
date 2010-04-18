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

import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;

import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.event.JoinCompleteEvent;

public class MixerImpl extends DispatchableEventSource implements Mixer, ParticipantContainer {

  protected MixerEndpoint _address;

  protected MediaService _service;

  protected MediaSession _media;

  protected MediaMixer _mixer;

  protected JoineeData _joinees = new JoineeData();

  protected MixerImpl(final ExecutionContext context, final MixerEndpoint address, final Map<Object, Object> params) {
    super(context);
    try {
      MsControlFactory mf = null;
      if (params == null || params.size() == 0) {
        mf = context.getMSFactory();
      }
      else {
        final Driver driver = DriverManager.getDrivers().next();
        final Properties props = new Properties();
        for (final Map.Entry<Object, Object> entry : params.entrySet()) {
          final String key = String.valueOf(entry.getKey());
          final String value = entry.getValue() == null ? "" : entry.getValue().toString();
          props.setProperty(key, value);
        }
        if (props.getProperty(MsControlFactory.MEDIA_SERVER_URI) == null && address != null) {
          props.setProperty(MsControlFactory.MEDIA_SERVER_URI, address.getURI());
        }
        mf = driver.getFactory(props);
      }
      _media = mf.createMediaSession();
      _mixer = _media.createMediaMixer(MediaMixer.AUDIO);
      _address = address;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  @Override
  public int hashCode() {
    return _mixer.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof MixerImpl)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    return _mixer.equals(((MixerImpl) o).getMediaObject());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(MixerImpl.class.getSimpleName()).append("[").append(_mixer).append("]")
        .toString();
  }

  @Override
  public synchronized MediaService getMediaService() throws MediaException, IllegalStateException {
    checkState();
    if (_service == null) {
      try {
        _service = _context.getMediaServiceFactory().create(this, _media);
        _service.getMediaGroup().join(Direction.DUPLEX, _mixer);
        return _service;
      }
      catch (final Exception e) {
        throw new MediaException(e);
      }
    }
    return _service;
  }

  @Override
  public synchronized void disconnect() {
    try {
      _mixer.release();
    }
    catch (final Exception e) {
      ;
    }
    try {
      _media.release();
    }
    catch (final Exception e) {
      ;
    }
    _media = null;
  }

  @Override
  public Endpoint getAddress() {
    return _address;
  }

  @Override
  public Participant[] getParticipants() {
    return _joinees.getJoinees();
  }

  @Override
  public Participant[] getParticipants(final Direction direction) {
    return _joinees.getJoinees(direction);
  }

  @Override
  public void addParticipant(final Participant p, final JoinType type, final Direction direction) {
    _joinees.add(p, type, direction);
  }

  @Override
  public void removeParticipant(final Participant p) {
    _joinees.remove(p);
  }

  @Override
  public Joint join(final Participant other, final JoinType type, final Direction direction)
      throws IllegalStateException {
    synchronized (this) {
      checkState();
      if (_joinees.contains(other)) {
        return new JointImpl(_context.getExecutor(), new JointImpl.DummyJoinWorker(MixerImpl.this, other));
      }
    }
    if (other instanceof Call) {
      return other.join(this, type, direction);
    }
    else {
      if (!(other.getMediaObject() instanceof Joinable)) {
        throw new IllegalArgumentException("MediaObject is't joinable.");
      }
      return new JointImpl(_context.getExecutor(), new JoinWorker() {
        @Override
        public JoinCompleteEvent call() throws Exception {
          try {
            synchronized (MixerImpl.this) {
              _mixer.join(direction, (Joinable) other.getMediaObject());
              _joinees.add(other, type, direction);
              ((ParticipantContainer) other).addParticipant(MixerImpl.this, type, direction);
            }
            return new JoinCompleteEvent(MixerImpl.this, other);
          }
          catch (final Exception e) {
            throw new MediaException(e);
          }
        }

        @Override
        public boolean cancel() {
          return false;
        }
      });
    }
  }

  @Override
  public synchronized void unjoin(final Participant p) {
    if (!_joinees.contains(p)) {
      return;
    }
    _joinees.remove(p);
    if (p.getMediaObject() instanceof Joinable) {
      try {
        _mixer.unjoin((Joinable) p.getMediaObject());
      }
      catch (final Exception e) {
        ;
      }
    }
    p.unjoin(this);
  }

  @Override
  public MediaObject getMediaObject() {
    return _mixer;
  }

  @Override
  public JoinableStream getJoinableStream(final StreamType arg0) throws MediaException, IllegalStateException {
    checkState();
    try {
      return _mixer.getJoinableStream(arg0);
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
  }

  @Override
  public JoinableStream[] getJoinableStreams() throws MediaException, IllegalStateException {
    checkState();
    try {
      return _mixer.getJoinableStreams();
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
  }

  protected void checkState() {
    if (_media == null) {
      throw new IllegalStateException();
    }
  }

}
