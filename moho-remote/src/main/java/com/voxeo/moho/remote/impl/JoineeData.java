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

package com.voxeo.moho.remote.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;

public class JoineeData {

  protected Map<Participant, JoinData> _joinees = new HashMap<Participant, JoinData>(0);

  public synchronized void add(final Participant p, final JoinType type, final Direction direction) {
    final JoinData datum = _joinees.get(p);
    if (datum == null) {
      _joinees.put(p, new JoinData(p, direction, type));
    }
    else {
      datum.setDirection(direction);
      datum.setType(type);
      _joinees.put(p, datum);
    }
  }

  public synchronized void add(final Participant p, final JoinType type, final Direction direction,
      Participant realJoined) {
    final JoinData datum = _joinees.get(p);
    if (datum == null) {
      _joinees.put(p, new JoinData(p, direction, type, realJoined));
    }
    else {
      datum.setDirection(direction);
      datum.setType(type);
      datum.setRealJoined(realJoined);
      _joinees.put(p, datum);
    }
  }

  public synchronized JoinData remove(final Participant p) {
    return _joinees.remove(p);
  }

  public synchronized void clear() {
    _joinees.clear();
  }

  public synchronized boolean contains(final Participant p) {
    return _joinees.containsKey(p);
  }

  public synchronized Participant[] getJoinees() {
    if (_joinees.size() == 0) {
      return new Participant[0];
    }
    return _joinees.keySet().toArray(new Participant[_joinees.size()]);
  }

  public synchronized Participant[] getJoinees(final Direction direction) {
    if (_joinees.size() == 0) {
      return new Participant[0];
    }
    final List<Participant> list = new ArrayList<Participant>(_joinees.size());
    for (final JoinData info : _joinees.values()) {
      if (direction.equals(info.getDirection())) {
        list.add(info.getParticipant());
      }
    }
    return list.toArray(new Participant[list.size()]);
  }

  public synchronized JoinType getJoinType(final Participant p) {
    JoinData data = _joinees.get(p);
    if (data != null) {
      return data.getType();
    }
    return null;
  }
}
