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

package com.voxeo.moho;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;

import org.apache.log4j.Logger;

import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.AutowiredEventTarget;
import com.voxeo.moho.event.CallEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventDispatcher;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MohoEarlyMediaEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RequestEvent;
import com.voxeo.moho.event.ResponseEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.remote.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.RemoteJoinDriver;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.moho.util.Utils;
import com.voxeo.moho.utils.EventListener;

public abstract class CallImpl implements Call {

  private static final Logger LOG = Logger.getLogger(CallImpl.class);

  protected String _id;

  protected Map<String, String> _states = new ConcurrentHashMap<String, String>();

  protected ExecutionContext _context;

  protected EventDispatcher _dispatcher = new EventDispatcher();

  protected ConcurrentHashMap<Observer, AutowiredEventListener> _observers = new ConcurrentHashMap<Observer, AutowiredEventListener>();

  protected CallableEndpoint _caller;

  protected CallableEndpoint _callee;

  protected Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();

  protected boolean _isSupervised;

  protected List<Call> _peers = new ArrayList<Call>(0);

  protected CallImpl(ExecutionContext context) {
    _context = context;
    _dispatcher.setExecutor(getThreadPool(), true);
    String uid = UUID.randomUUID().toString();
    String rawid = ((RemoteJoinDriver) _context.getFramework().getDriverByProtocolFamily(
        RemoteJoinDriver.PROTOCOL_REMOTEJOIN)).getRemoteAddress(RemoteParticipant.RemoteParticipant_TYPE_CALL, uid);
    int a = 0;
    if ((a = (rawid.length() * 2) % 3) != 0) {
      if (a == 1) {
        rawid = rawid.concat("a");
      }
      else {
        rawid = rawid.concat("ab");
      }
    }
    _id = ParticipantIDParser.encode(rawid);

    context.addCall(this);
  }

  @Override
  public Output<Call> output(String text) throws MediaException {
    return getMediaService().output(text);
  }

  @Override
  public Output<Call> output(URI media) throws MediaException {
    return getMediaService().output(media);
  }

  @Override
  public Output<Call> output(OutputCommand output) throws MediaException {
    return getMediaService().output(output);
  }

  @Override
  public Prompt<Call> prompt(String text, String grammar, int repeat) throws MediaException {
    return getMediaService().prompt(text, grammar, repeat);
  }

  @Override
  public Prompt<Call> prompt(URI media, String grammar, int repeat) throws MediaException {
    return getMediaService().prompt(media, grammar, repeat);
  }

  @Override
  public Prompt<Call> prompt(OutputCommand output, InputCommand input, int repeat) throws MediaException {
    return getMediaService().prompt(output, input, repeat);
  }

  @Override
  public Input<Call> input(String grammar) throws MediaException {
    return getMediaService().input(grammar);
  }

  @Override
  public Input<Call> input(InputCommand input) throws MediaException {
    return getMediaService().input(input);
  }

  @Override
  public Recording<Call> record(URI recording) throws MediaException {
    return getMediaService().record(recording);
  }

  @Override
  public Recording<Call> record(RecordCommand command) throws MediaException {
    return getMediaService().record(command);
  }

  @Override
  public MediaGroup getMediaGroup() {
    return getMediaService().getMediaGroup();
  }

  /**
   * return the media service attached to the call
   * 
   * @param reinvite
   *          whether Moho Framework should automatically re-invites the call to
   *          {@link Participant.JoinType#BRIDGE Bridge} mode if the call is
   *          currently joined in {@link Participant.JoinType#DIRECT Direct}
   *          mode.
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           but reinvite is false or if the call is not answered.
   */
  protected abstract MediaService<Call> getMediaService(boolean reinvite);

  /**
   * return the media service attached to the call. Equivalent of
   * {@link #getMediaService(boolean) getMediaService(true)}.
   * 
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           but reinvite is false or if the call is not answered.
   */
  protected MediaService<Call> getMediaService() {
    return getMediaService(true);
  }

  @Override
  public void hangup() {
    hangup(null);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void addObserver(final Observer... observers) {
    if (observers != null) {
      for (final Observer observer : observers) {
        if (observer instanceof EventListener) {
          EventListener l = (EventListener) observer;
          Class claz = Utils.getGenericType(observer);
          if (claz == null) {
            claz = Event.class;
          }
          _dispatcher.addListener(claz, l);
        }
        else {
          final AutowiredEventListener autowire = new AutowiredEventListener(observer);
          if (_observers.putIfAbsent(observer, autowire) == null) {
            _dispatcher.addListener(Event.class, autowire);
          }
        }
      }
    }
  }

  // public void removeListener(final EventListener<?> listener) {
  // _dispatcher.removeListener(listener);
  // }

  @Override
  public void removeObserver(final Observer listener) {
    final AutowiredEventListener autowiredEventListener = _observers.remove(listener);
    if (autowiredEventListener != null) {
      _dispatcher.removeListener(autowiredEventListener);
    }
  }

  public <S extends EventSource, T extends Event<S>> Future<T> internalDispatch(final T event) {
    return _dispatcher.fire(event, true, null);
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
    return _dispatcher.fire(event, true, afterExec);
  }

  @Override
  public String getId() {
    return _id;
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return _context;
  }

  @Override
  public String getApplicationState() {
    return _states.get(AutowiredEventTarget.DEFAULT_FSM);
  }

  @Override
  public void setApplicationState(final String state) {
    _states.put(AutowiredEventTarget.DEFAULT_FSM, state);
  }

  public String getApplicationState(final String FSM) {
    return _states.get(FSM);
  }

  @Override
  public void setApplicationState(final String FSM, final String state) {
    _states.put(FSM, state);
  }

  protected Executor getThreadPool() {
    return _context.getExecutor();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(final String name) {
    if (name == null) {
      return null;
    }
    return (T) _attributes.get(name);
  }

  @Override
  public Map<String, Object> getAttributeMap() {
    return new HashMap<String, Object>(_attributes);
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    if (value == null) {
      _attributes.remove(name);
    }
    else {
      _attributes.put(name, value);
    }
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
    Future<T> retval = null;
    if (!(event instanceof CallEvent) && !(event instanceof RequestEvent) && !(event instanceof ResponseEvent)) {
      retval = this.internalDispatch(event);
    }
    else {
      final Runnable acceptor = new Runnable() {
        @Override
        public void run() {
          if (event instanceof EarlyMediaEvent) {
            if (!((MohoEarlyMediaEvent) event).isProcessed() && !((MohoEarlyMediaEvent) event).isAsync()) {
              try {
                ((EarlyMediaEvent) event).reject(null);
              }
              catch (final SignalException e) {
                LOG.warn(e);
              }
            }
          }

          else if (event instanceof AcceptableEvent) {
            if (!((AcceptableEvent) event).isAccepted() && !((AcceptableEvent) event).isRejected()
                && !((AcceptableEvent) event).isAsync()) {
              try {
                ((AcceptableEvent) event).accept();
              }
              catch (final SignalException e) {
                LOG.warn(e);
              }
            }
          }

        }
      };
      retval = this.dispatch(event, acceptor);
    }
    return retval;
  }

  @Override
  public Call[] getPeers() {
    synchronized (_peers) {
      return _peers.toArray(new CallImpl[_peers.size()]);
    }
  }

  @Override
  public Joint join() {
    return join(Joinable.Direction.DUPLEX);
  }

  @Override
  public Joint join(final CallableEndpoint other, final JoinType type, final Direction direction) {
    return join(other, type, direction, null);
  }

}
