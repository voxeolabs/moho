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

package com.voxeo.moho.conference;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.JoinWorker;
import com.voxeo.moho.Joint;
import com.voxeo.moho.JointImpl;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.UnjointImpl;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.IDGenerator;

public class ConferenceImpl extends MixerImpl implements Conference, ParticipantContainer {

  private static final Logger LOG = Logger.getLogger(ConferenceImpl.class);

  protected String _id;

  protected int _maxSeats;

  protected int _occupiedSeats;

  protected ConferenceController _controller;

  protected Object _lock = new Object();

  protected ConferenceImpl(final ExecutionContext context, final MixerEndpoint address,
      final Map<Object, Object> params, final String id, final int seats, final ConferenceController controller,
      Parameters parameters) {
    super(context, address, params, parameters);
    _id = IDGenerator.generateId(_context, RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE, id);
    _maxSeats = seats;
    _controller = controller;
    
    if (_context != null && getId() != null) {
      ((ApplicationContextImpl) _context).addParticipant(this);
    }
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
        synchronized (_lock) {
          if (_controller != null) {
            _controller.preJoin(other, ConferenceImpl.this);
          }
          final JoinCompleteEvent retval = ConferenceImpl.super.join(other, type, direction).get();
          _occupiedSeats = _occupiedSeats + 1;
          if (_controller != null) {
            _controller.postJoin(other, ConferenceImpl.this);
          }
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
  public Joint join(final Participant other, final JoinType type, final Direction direction, final Properties props)
      throws IllegalStateException {
    checkState();
    return new JointImpl(_context.getExecutor(), new JoinWorker() {

      @Override
      public JoinCompleteEvent call() throws Exception {
        synchronized (_lock) {
          if (_controller != null) {
            _controller.preJoin(other, ConferenceImpl.this);
          }
          final JoinCompleteEvent retval = ConferenceImpl.super.join(other, type, direction, props).get();
          _occupiedSeats = _occupiedSeats + 1;
          if (_controller != null) {
            _controller.postJoin(other, ConferenceImpl.this);
          }
          return retval;
        }
      }

      @Override
      public boolean cancel() {
        return false;
      }
    });

  }

  public MohoUnjoinCompleteEvent doUnjoin(final Participant other, boolean isInitiator) throws Exception {
    synchronized (_lock) {
      MohoUnjoinCompleteEvent event = null;
      try {
        if (_controller != null) {
          _controller.preUnjoin(other, this);
        }
        _occupiedSeats = _occupiedSeats - 1;
        event = super.doUnjoin(other, isInitiator);
      }
      catch (final Exception e) {
        LOG.warn("", e);
        throw e;
      }
      finally {
        if (_controller != null) {
          _controller.postUnjoin(other, this);
        }
      }

      return event;
    }
  }

  @Override
  public Unjoint unjoin(final Participant p) {
    Unjoint task = new UnjointImpl(_context.getExecutor(), new Callable<UnjoinCompleteEvent>() {
      @Override
      public UnjoinCompleteEvent call() throws Exception {
        return doUnjoin(p, true);
      }
    });
    return task;
  }

  @Override
  public int getMaxSeats() {
    return _maxSeats;
  }

  @Override
  public int getOccupiedSeats() {
    return _occupiedSeats;
  }

  @Override
  public ConferenceController getController() {
    return _controller;
  }

  @Override
  public void setController(ConferenceController controller) {
    this._controller = controller;
  }

}
