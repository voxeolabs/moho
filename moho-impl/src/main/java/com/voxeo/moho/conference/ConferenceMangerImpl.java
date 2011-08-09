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

package com.voxeo.moho.conference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.media.mscontrol.Parameters;

import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.spi.ExecutionContext;

public class ConferenceMangerImpl implements ConferenceManager {

  protected ExecutionContext _context;

  protected Map<String, Conference> _conferences = new HashMap<String, Conference>();

  public ConferenceMangerImpl() {
    super();
  }

  public ConferenceMangerImpl(final ExecutionContext context) {
    _context = context;
  }

  @Override
  public Conference createConference(final String id, final int seats, Parameters parameters) {
    return this.createConference(id, seats, new SimpleConferenceController(), parameters);
  }

  @Override
  public Conference createConference(final String id, final int seats, final ConferenceController controller,
      Parameters parameters) {
    return this.createConference((MixerEndpoint) _context.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT), null,
        id, seats, controller, parameters);
  }

  @Override
  public Conference createConference(final MixerEndpoint mxier, final Map<Object, Object> mixerParams, final String id,
      final int seats, final ConferenceController controller, Parameters parameters) {
    synchronized (_conferences) {
      Conference retval = getConference(id);
      if (retval == null) {
        retval = new ConferenceImpl(_context, mxier, mixerParams, id, seats, controller, parameters);
        _conferences.put(id, retval);
      }
      return retval;
    }
  }

  @Override
  public Conference getConference(final String id) {
    synchronized (_conferences) {
      return _conferences.get(id);
    }
  }

  @Override
  public void removeConference(final String id) {
    Conference conf = null;
    synchronized (_conferences) {
      conf = _conferences.remove(id);
    }
    if (conf != null) {
      conf.disconnect();
    }
  }

  @Override
  public Set<String> getConferences() {
    synchronized (_conferences) {
      return Collections.unmodifiableSet(_conferences.keySet());
    }
  }

  @Override
  public void removeAllConferences() {
    for (final String id : getConferences()) {
      removeConference(id);
    }
  }

  @Override
  public void init(ExecutionContext context, Map<String, String> properties) {
    _context = context;
  }

  @Override
  public void destroy() {
    Collection<Conference> conferences = _conferences.values();

    for (Conference conf : conferences) {
      conf.disconnect();
    }
  }

  @Override
  public String getName() {
    return ConferenceManager.class.getName();
  }
}
