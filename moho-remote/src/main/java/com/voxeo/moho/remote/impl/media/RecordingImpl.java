/**
 * Copyright 2010-2011 Voxeo Corporation
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

package com.voxeo.moho.remote.impl.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.core.verb.VerbRef;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.common.event.MohoRecordCompleteEvent;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.remote.impl.CallImpl;
import com.voxeo.moho.remote.impl.JID;
import com.voxeo.moho.remote.impl.RayoListener;

//TODO exception and IQ error handling
public class RecordingImpl<T extends EventSource> implements Recording<T>, RayoListener {
  private static final Logger LOG = Logger.getLogger(RecordingImpl.class);

  protected SettableResultFuture<RecordCompleteEvent<T>> _future = new SettableResultFuture<RecordCompleteEvent<T>>();

  protected VerbRef _verbRef;

  protected CallImpl _call;

  protected T _todo;

  protected boolean paused;

  public RecordingImpl(final VerbRef verbRef, final CallImpl call, T todo) {
    _verbRef = verbRef;
    _call = call;
    _todo = todo;
    _call.addComponentListener(_verbRef.getVerbId(), this);
  }

  protected void done(final RecordCompleteEvent<T> event) {
    _future.setResult(event);
    _call.removeComponentListener(_verbRef.getVerbId());
  }

  protected void done(final MediaException exception) {
    _future.setException(exception);
    _call.removeComponentListener(_verbRef.getVerbId());
  }

  @Override
  public void pause() {
    if (!_future.isDone() && !paused) {
      try {
        IQ iq = _call.getMohoRemote().getRayoClient().pauseRecord(_verbRef);
        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          throw new MediaException(error.getCondition() + error.getText());
        }
        else {
          paused = true;
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }
    }
  }

  @Override
  public void resume() {
    if (!_future.isDone() && paused) {
      try {
        IQ iq = _call.getMohoRemote().getRayoClient().resumeRecord(_verbRef);
        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          LOG.error(error.getCondition() + error.getText());
        }
        else {
          paused = false;
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }
    }
  }

  @Override
  public void stop() {
    if (!_future.isDone()) {
      try {
        IQ iq = _call.getMohoRemote().getRayoClient().stop(_verbRef);
        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          LOG.error(error.getCondition() + error.getText());
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public RecordCompleteEvent<T> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public RecordCompleteEvent<T> get(final long timeout, final TimeUnit unit) throws InterruptedException,
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

  public synchronized boolean isPending() {
    return !_future.isDone();
  }

  @Override
  public void onRayoEvent(JID from, Presence presence) {
    Object obj = presence.getExtension().getObject();

    if (obj instanceof com.rayo.core.verb.VerbCompleteEvent) {
      com.rayo.core.verb.VerbCompleteEvent event = (com.rayo.core.verb.VerbCompleteEvent) obj;

      long duration = 0;
      if (event instanceof com.rayo.core.verb.RecordCompleteEvent) {
        duration = ((com.rayo.core.verb.RecordCompleteEvent) event).getDuration().getMillis();
      }
      MohoRecordCompleteEvent<T> mohoEvent = new MohoRecordCompleteEvent<T>(_todo,
          getMohoOutputCompleteReasonByRayoReason(event.getReason()), duration, event.getErrorText(), this);

      this.done(mohoEvent);
      _call.dispatch(mohoEvent);
    }
    else {
      LOG.error("RecordingImpl Can't process presence:" + presence);
    }

  }

  protected RecordCompleteEvent.Cause getMohoOutputCompleteReasonByRayoReason(VerbCompleteReason reason) {
    if (reason instanceof com.rayo.core.verb.OutputCompleteEvent.Reason) {
      if (reason == com.rayo.core.verb.OutputCompleteEvent.Reason.SUCCESS) {
        return RecordCompleteEvent.Cause.SILENCE;
      }
    }
    else if (reason instanceof com.rayo.core.verb.VerbCompleteEvent.Reason) {
      if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.HANGUP) {
        return RecordCompleteEvent.Cause.DISCONNECT;
      }
      else if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.STOP) {
        return RecordCompleteEvent.Cause.CANCEL;
      }
      else if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.ERROR) {
        return RecordCompleteEvent.Cause.ERROR;
      }
    }

    return RecordCompleteEvent.Cause.ERROR;
  }

  @Override
  public void onRayoCommandResult(JID from, IQ iq) {
    // DO nothing.
  }

  public String getVerbId() {

    return _verbRef.getVerbId();
  }
}
