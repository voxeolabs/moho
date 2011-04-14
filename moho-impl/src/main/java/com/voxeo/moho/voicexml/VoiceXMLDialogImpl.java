/**
 * ` * Copyright 2010 Voxeo Corporation Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.voicexml;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.vxml.VxmlDialog;
import javax.media.mscontrol.vxml.VxmlDialogEvent;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.JoinWorker;
import com.voxeo.moho.JoineeData;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.MediaResourceDisconnectEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public class VoiceXMLDialogImpl extends DispatchableEventSource implements Dialog, ParticipantContainer {

  private static final Logger LOG = Logger.getLogger(VoiceXMLDialogImpl.class);

  protected enum DialogState {
    IDLE, PREPARING, PREPARED, STARTED
  };

  protected DialogState _state = DialogState.IDLE;

  protected VoiceXMLEndpoint _address;

  protected Parameters _options;

  protected Map<java.lang.String, java.lang.Object> _sessionVariables;

  protected MediaSession _media;

  protected VxmlDialog _dialog;

  protected FutureTask<Map<String, Object>> _future;

  protected Map<String, Object> _result;

  protected Object _lock = new Object();

  protected JoineeData _joinees = new JoineeData();

  protected VoiceXMLDialogImpl(final ExecutionContext ctx, final VoiceXMLEndpoint address,
      final Map<Object, Object> params) {
    super(ctx);
    try {
      _media = ctx.getMSFactory().createMediaSession();
      _dialog = _media.createVxmlDialog(null);
      _options = _media.createParameters();
      _sessionVariables = new HashMap<String, Object>();
      _dialog.addListener(new VxmlListener());
      _address = address;
      if (params != null) {
        for (final Object key : params.keySet()) {
          if (key instanceof Parameter) {
            _options.put((Parameter) key, params.get(key));
          }
          else {
            _sessionVariables.put(key.toString(), params.get(key));
          }
        }
      }
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
  }

  @Override
  public int hashCode() {
    return _dialog.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof VoiceXMLDialogImpl)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    return _dialog.equals(((VoiceXMLDialogImpl) o).getMediaObject());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(this.getClass().getSimpleName()).append("[").append(_state).append("]")
        .toString();
  }

  @Override
  public Endpoint getAddress() {
    return _address;
  }

  @Override
  public MediaObject getMediaObject() {
    return _dialog;
  }

  @Override
  public void disconnect() {
    try {
      _dialog.terminate(true);
    }
    catch (final Exception e) {
      LOG.warn("Exception when terminate VxmlDialog", e);
    }
    try {
      _dialog.release();
    }
    catch (final Exception e) {
      LOG.warn("Exception when release VxmlDialog", e);
    }
    try {
      _media.release();
    }
    catch (final Exception e) {
      LOG.warn("Exception when release MediaSession", e);
    }
    _media = null;

    this.dispatch(new MediaResourceDisconnectEvent(this));
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
  public void addParticipant(final Participant p, final JoinType type, final Direction direction, Participant realJoined) {
    _joinees.add(p, type, direction, realJoined);
  }

  @Override
  public void removeParticipant(final Participant p) {
    _joinees.remove(p);
  }

  @Override
  public Joint join(final Participant other, final JoinType type, final Direction direction)
      throws IllegalStateException {
    synchronized (_lock) {
      if (_state != DialogState.IDLE) {
        throw new IllegalStateException("Cannot join when the dialog is starting.");
      }
      if (_joinees.contains(other)) {
        return new JointImpl(_context.getExecutor(), new JointImpl.DummyJoinWorker(VoiceXMLDialogImpl.this, other));
      }
    }
    if (other instanceof Call) {
      return other.join(this, type, direction);
    }
    else {
      if (!(other.getMediaObject() instanceof Joinable)) {
        throw new IllegalArgumentException("MediaObject isn't joinable.");
      }
      return new JointImpl(_context.getExecutor(), new JoinWorker() {
        @Override
        public JoinCompleteEvent call() throws Exception {
          JoinCompleteEvent event = null;
          try {
            synchronized (_lock) {
              _dialog.join(direction, (Joinable) other.getMediaObject());
              _joinees.add(other, type, direction);
              ((ParticipantContainer) other).addParticipant(VoiceXMLDialogImpl.this, type, direction, null);
              event = new JoinCompleteEvent(VoiceXMLDialogImpl.this, other, Cause.JOINED);
            }
          }
          catch (final Exception e) {
            event = new JoinCompleteEvent(VoiceXMLDialogImpl.this, other, Cause.ERROR, e);
            throw new MediaException(e);
          }
          finally {
            VoiceXMLDialogImpl.this.dispatch(event);
          }
          return event;
        }

        @Override
        public boolean cancel() {
          return false;
        }
      });
    }
  }

  @Override
  public void unjoin(final Participant p) {
    synchronized (_lock) {
      if (!_joinees.contains(p)) {
        return;
      }
      _joinees.remove(p);
      if (p.getMediaObject() instanceof Joinable) {
        try {
          _dialog.unjoin((Joinable) p.getMediaObject());
        }
        catch (final Exception e) {
          LOG.warn("", e);
        }
      }
    }
    p.unjoin(this);
  }

  public void prepare() {
    synchronized (_lock) {
      if (_state != DialogState.IDLE) {
        throw new IllegalStateException("" + this);
      }
      _dialog.prepare(_address.getDocumentURL(), _options, _sessionVariables);
      setState(DialogState.PREPARING);
    }
  }

  public void start() {
    synchronized (_lock) {
      if (_state != DialogState.PREPARING && _state != DialogState.PREPARED) {
        throw new IllegalStateException("" + this);
      }
      while (_state != DialogState.PREPARED && _state != DialogState.IDLE) {
        try {
          _lock.wait();
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
      if (_state == DialogState.IDLE) {
        throw new IllegalStateException("Error: " + this);
      }
      _future = new FutureTask<Map<String, Object>>(new Callable<Map<String, Object>>() {
        @Override
        public Map<String, Object> call() throws Exception {
          synchronized (_lock) {
            Map<String, Object> retval = _result = null;
            if (_state == DialogState.PREPARED) {
              _dialog.start(_sessionVariables);
              while (_result == null && _state != DialogState.IDLE) {
                _lock.wait();
              }
            }
            retval = _result;
            return retval;
          }
        }

      });
      new Thread(_future).start();
    }
  }

  public void terminate(final boolean immediate) {
    synchronized (_lock) {
      _dialog.terminate(immediate);
      while (_state != DialogState.IDLE) {
        try {
          _lock.wait();
        }
        catch (final InterruptedException e) {
          // ignore
        }
      }
    }
  }

  protected void setState(final DialogState state) {
    synchronized (_lock) {
      _state = state;
    }
  }

  protected class VxmlListener implements MediaEventListener<VxmlDialogEvent> {

    public void onEvent(final VxmlDialogEvent event) {
      if (event.getEventType().equals(VxmlDialogEvent.PREPARED)) {
        synchronized (_lock) {
          if (_state == DialogState.PREPARING) {
            setState(DialogState.PREPARED);
            _lock.notifyAll();
          }
        }
      }
      else if (event.getEventType().equals(VxmlDialogEvent.STARTED)) {
        synchronized (_lock) {
          setState(DialogState.STARTED);
        }
      }
      else if (event.getEventType().equals(VxmlDialogEvent.EXITED)) {
        synchronized (_lock) {
          _result = event.getNameList();
          _lock.notifyAll();
          setState(DialogState.IDLE);
        }
      }
      else if (event.getEventType().equals(VxmlDialogEvent.DISCONNECTION_REQUESTED)
          || event.getEventType().equals(VxmlDialogEvent.ERROR_EVENT)) {
        synchronized (_lock) {
          setState(DialogState.IDLE);
          _lock.notifyAll();
        }
      }
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public Map<String, Object> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public Map<String, Object> get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    return _future.get(timeout, unit);
  }

  @Override
  public boolean isCancelled() {
    return _future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return _future.isDone();
  }
}
