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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.core.verb.SeekCommand;
import com.rayo.core.verb.SeekCommand.Direction;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.core.verb.VerbRef;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.common.event.MohoOutputCompleteEvent;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.remote.impl.CallImpl;
import com.voxeo.moho.remote.impl.JID;
import com.voxeo.moho.remote.impl.RayoListener;

//TODO exception and IQ error handling
public class OutputImpl<T extends EventSource> implements Output<T>, RayoListener {
  private static final Logger LOG = Logger.getLogger(OutputImpl.class);

  protected SettableResultFuture<OutputCompleteEvent<T>> _future = new SettableResultFuture<OutputCompleteEvent<T>>();

  final Lock lock = new ReentrantLock();

  protected VerbRef _verbRef;

  protected CallImpl _call;
  
  protected OutputCommand _next;

  protected T _todo;

  protected boolean paused;

  public OutputImpl(final VerbRef verbRef, final OutputCommand next, final CallImpl call, T todo) {
    _verbRef = verbRef;
    _call = call;
    _next = next;
    _todo = todo;
    _call.addComponentListener(_verbRef.getVerbId(), this);
  }

  public OutputImpl(final VerbRef verbRef, final CallImpl call, T todo) {
    this(verbRef, null, call, todo);
  }

  @SuppressWarnings("unchecked")
  protected void done(final OutputCompleteEvent<T> event) {
    _call.removeComponentListener(_verbRef.getVerbId());
    if (_next == null) {
      _future.setResult(event);
    }
    else {
      //avoid dead wait on output
      _call.getMohoRemote().getExecutor().execute(new Runnable() {
        @Override
        public void run() {
          OutputImpl<T> outputImpl = ((OutputImpl<T>) _call.output(_next));
          _verbRef = outputImpl._verbRef;
          _next = outputImpl._next;
          _call.addComponentListener(_verbRef.getVerbId(), OutputImpl.this);
        }
      });
    }
  }

  protected void done(final MediaException exception) {
    _future.setException(exception);
    _call.removeComponentListener(_verbRef.getVerbId());
  }

  @Override
  public void pause() {
    if (!_future.isDone() && !paused) {
      try {
        IQ iq = _call.getMohoRemote().getRayoClient().pause(_verbRef);
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
        IQ iq = _call.getMohoRemote().getRayoClient().resume(_verbRef);
        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          throw new MediaException(error.getCondition() + error.getText());
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
          throw new MediaException(error.getCondition() + error.getText());
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }
    }
  }

  @Override
  public synchronized void jump(final int index) {
    // TODO rayo client doesn't support this.
  }

  @Override
  public synchronized void move(final boolean direction, final long time) {
    if (!_future.isDone()) {
      try {
        SeekCommand command = new SeekCommand();
        if (direction) {
          command.setDirection(Direction.FORWARD);
          command.setAmount((int) (time / 1000));
        }
        else {
          command.setDirection(Direction.BACK);
          command.setAmount((int) (time / 1000));
        }

        _call.getMohoRemote().getRayoClient().seek(_verbRef, command);
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }
    }
  }

  @Override
  public void speed(final boolean upOrDown) {
    if (!_future.isDone()) {
      try {
        IQ iq = null;
        if (upOrDown) {
          iq = _call.getMohoRemote().getRayoClient().speedUp(_verbRef);
        }
        else {
          iq = _call.getMohoRemote().getRayoClient().speedDown(_verbRef);
        }
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
  public void volume(final boolean upOrDown) {
    if (!_future.isDone()) {
      try {
        IQ iq = null;
        if (upOrDown) {
          iq = _call.getMohoRemote().getRayoClient().volumeUp(_verbRef);
        }
        else {
          iq = _call.getMohoRemote().getRayoClient().volumeDown(_verbRef);
        }
        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          throw new MediaException(error.getCondition() + error.getText());
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
  public OutputCompleteEvent<T> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public OutputCompleteEvent<T> get(final long timeout, final TimeUnit unit) throws InterruptedException,
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

  @Override
  public void onRayoEvent(JID from, Presence presence) {
	  LOG.debug("OutputImpl Recived presence, processing:" + presence);
    Object obj = presence.getExtension().getObject();

    if (obj instanceof com.rayo.core.verb.VerbCompleteEvent) {
      com.rayo.core.verb.VerbCompleteEvent event = (com.rayo.core.verb.VerbCompleteEvent) obj;

      MohoOutputCompleteEvent<T> mohoEvent = new MohoOutputCompleteEvent<T>(_todo,
          getMohoOutputCompleteReasonByRayoReason(event.getReason()), event.getErrorText(), this);

      this.done(mohoEvent);
      if (_next == null) {
        _call.dispatch(mohoEvent);
      }
    }
    else {
      LOG.error("OutputImpl Can't process presence:" + presence);
    }
  }

  @Override
  public void onRayoCommandResult(JID from, IQ iq) {
    // DO nothing.
  }

  protected OutputCompleteEvent.Cause getMohoOutputCompleteReasonByRayoReason(VerbCompleteReason reason) {
    if (reason instanceof com.rayo.core.verb.OutputCompleteEvent.Reason) {
      if (reason == com.rayo.core.verb.OutputCompleteEvent.Reason.SUCCESS) {
        return OutputCompleteEvent.Cause.END;
      }
    }
    else if (reason instanceof com.rayo.core.verb.VerbCompleteEvent.Reason) {
      if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.HANGUP) {
        return OutputCompleteEvent.Cause.DISCONNECT;
      }
      else if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.STOP) {
        return OutputCompleteEvent.Cause.CANCEL;
      }
      else if (reason == com.rayo.core.verb.VerbCompleteEvent.Reason.ERROR) {
        return OutputCompleteEvent.Cause.ERROR;
      }
    }

    return OutputCompleteEvent.Cause.ERROR;
  }

  public String getVerbId() {

    return _verbRef.getVerbId();
  }
}
