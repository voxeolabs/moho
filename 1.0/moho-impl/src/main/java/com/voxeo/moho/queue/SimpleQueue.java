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

package com.voxeo.moho.queue;

import java.util.Iterator;
import java.util.Queue;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.DispatchableEventSource;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

/**
 * <p>
 * SimpleQueue allows a {@link com.voxeo.moho.Call Call} to be parked on the
 * queue. While on the queue, the queue will render the audio into to the call.
 * </p>
 * <p>
 * If the audio is shared, each {@link com.voxeo.moho.Call Call} will be
 * hearing the same audio streams at the same time, like radio broadcasting.
 * <p>
 * <p>
 * Otherwise, each {@link com.voxeo.moho.Call Call} will get its own streams of
 * audio.
 * </p>
 * 
 * @author wchen
 */
public class SimpleQueue extends DispatchableEventSource implements CallQueue {
  protected Queue<Call> _queue;

  protected OutputCommand _output;

  protected Mixer _mixer;

  protected boolean _shared;

  /**
   * Construct a Simple Queue
   * 
   * @param ctx
   *          the application context
   * @param queue
   *          the queue implementation
   * @param res
   *          the audio to be rendered
   * @param shared
   *          whether audio streams are shared or not
   */
  public SimpleQueue(final ApplicationContext ctx, final Queue<Call> queue, final AudibleResource res,
      final boolean shared) {
    super((ExecutionContext) ctx);
    _queue = queue;
    _output = new OutputCommand(res);
    _output.setBargein(false);
    _output.setRepeatInterval(Integer.MAX_VALUE);
    _shared = shared;
  }

  @Override
  public boolean isEmpty() {
    return _queue.isEmpty();
  }

  @Override
  public Iterator<Call> iterator() {
    return _queue.iterator();
  }

  @Override
  public boolean offer(final Call e) {
    if (_queue.offer(e)) {
      if (_shared) {
        try {
          e.join(_mixer, JoinType.BRIDGE, Direction.RECV).get();
        }
        catch (final Exception e1) {
          e.join();
        }
      }
      else {
        final MediaService ms = e.getMediaService();
        ms.output(_output);
        // probably need listener here to repeat the output
      }
      dispatch(new EnqueueEvent(this, e));
      return true;
    }
    return false;
  }

  @Override
  public Call peek() {
    return _queue.peek();
  }

  @Override
  public Call poll() {
    final Call c = _queue.poll();
    if (c != null) {
      if (_shared) {
        c.unjoin(_mixer);
      }
      dispatch(new DequeueEvent(this, c));
    }
    return c;
  }

  @Override
  public boolean remove(final Call o) {
    if (_queue.remove(o)) {
      dispatch(new DequeueEvent(this, o));
      return true;
    }
    return false;
  }

  @Override
  public int size() {
    return _queue.size();
  }

  @Override
  public void destroy() {
    for (final Iterator<Call> i = iterator(); i.hasNext();) {
      final Call c = i.next();
      if (_shared) {
        c.unjoin(_mixer);
      }
      c.disconnect();
    }
    if (_mixer != null) {
      // how do we destroy a mixer???
    }
    _queue.clear();
  }

  @Override
  public void init() {
    if (_shared) {
      final MixerEndpoint e = (MixerEndpoint) this.getApplicationContext().getEndpoint(
          MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
      _mixer = e.create(null);
      final MediaService ms = _mixer.getMediaService();
      ms.output(_output);
      // probably put a listener here to repeat the output
    }
  }

}
