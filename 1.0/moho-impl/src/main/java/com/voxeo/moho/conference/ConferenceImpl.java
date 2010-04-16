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

package com.voxeo.moho.conference;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.JoinWorker;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.JoinCompleteEvent;

public class ConferenceImpl extends MixerImpl implements Conference {

  protected String _id;

  protected int _maxSeats;

  protected int _occupiedSeats;

  protected ConferenceController _controller;

  protected ConferenceImpl(final ExecutionContext context, final MixerEndpoint address,
      final Map<Object, Object> params, final String id, final int seats, final ConferenceController controller) {
    super(context, address, params);
    _id = id;
    _maxSeats = seats;
    _controller = controller;
  }

  @Override
  public String getId() {
    return _id;
  }

  @Override
  public Joint join(final Participant other, final JoinType type, final Direction direction)
      throws IllegalStateException {
    checkState();
    return new JointImpl(_context.getExecutor(), new JoinWorker() {
      @Override
      public JoinCompleteEvent call() throws Exception {
        synchronized (ConferenceImpl.this) {
          _controller.preJoin(other, ConferenceImpl.this);
          final JoinCompleteEvent retval = ConferenceImpl.super.join(other, type, direction).get();
          _occupiedSeats = _occupiedSeats + 1;
          _controller.postJoin(other, ConferenceImpl.this);
          return retval;
        }
      }

      @Override
      public boolean cancel() {
        return false;
      }
    });

  }

  @Override
  public synchronized void unjoin(final Participant other) {
    if (!_joinees.contains(other)) {
      return;
    }
    try {
      _controller.preUnjoin(other, this);
      _occupiedSeats = _occupiedSeats - 1;
      super.unjoin(other);
      _controller.postUnjoin(other, this);
    }
    catch (final Exception e) {
      ;
    }
  }

  @Override
  public int getMaxSeats() {
    return _maxSeats;
  }

  @Override
  public int getOccupiedSeats() {
    return _occupiedSeats;
  }

}
