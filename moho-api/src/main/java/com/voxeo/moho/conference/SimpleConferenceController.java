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

import java.util.concurrent.ExecutionException;

import com.voxeo.moho.Call;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class SimpleConferenceController implements ConferenceController {

  protected OutputCommand _enter;

  protected InputCommand _pass;

  protected OutputCommand _exit;

  protected InputCommand _term;

  protected int _repeat = 0;

  public SimpleConferenceController() {
  }

  public SimpleConferenceController(final AudibleResource enterAnnouncement, final AudibleResource exitAnnoucement,
      final Grammar term) {
    this(enterAnnouncement == null ? null : new OutputCommand(enterAnnouncement), null, 0, term == null ? null
        : new InputCommand(term), exitAnnoucement == null ? null : new OutputCommand(exitAnnoucement));
  }

  /**
   * <p>
   * Construct a ConferenceController to do the following:
   * </p>
   * <ul>
   * <li>Before a {@link Participant Participant} can join the conference, an
   * announcement (<code>enterAnnouncement</code>) is rendered to the
   * {@link Participant Participant} an input is asked. If the input is matched
   * to the <code>pass</code> {@link com.voxeo.moho.input.InputCommand
   * InputCommand}, the {@link Participant Participant} will be allowed joined
   * to the conference, unless the conference is full.</li>
   * <li>During the conference call, if a {@link Participant Participant} has
   * entered an input that matches <code>term</code>
   * {@link com.voxeo.moho.input.InputCommand InputCommand}, an announcement (
   * <code>exitAnnoucement</code>) is rendered to the {@link Participant
   * Participant} and unjoins the {@link Participant Participant} from the
   * conference.
   * </ul>
   * 
   * @param enterAnnoucement
   *          the announcement rendered before joining the conference, unless it
   *          is null.
   * @param pass
   *          the input expected before joining the conference, unless it is
   *          null.
   * @param repeat
   *          number of rounds of asking for input.
   * @param term
   *          the input to indicate unjoining from the conference, unless it is
   *          null.
   * @param exitAnnoucement
   *          the announcement rendered before unjoining the conference unless
   *          it is null.
   */
  public SimpleConferenceController(final OutputCommand enterAnnouncement, final InputCommand pass, final int repeat,
      final InputCommand term, final OutputCommand exitAnnouncement) {
    _enter = enterAnnouncement;
    _exit = exitAnnouncement;
    _term = term;
    _repeat = repeat;
    _pass = pass;
  }

  @Override
  public void preJoin(final Participant p, final Conference f) {
    if (f.getOccupiedSeats() >= f.getMaxSeats()) {
      throw new ConferenceFullException();
    }
    if (p instanceof Call) {
      if (_enter != null || _pass != null) {
        if (p.getMediaObject() == null) {
          try {
            ((Call) p).join().get();
          }
          catch (Exception e) {
            throw new MediaException(e);
          }
        }
        final Prompt<Call> prompt = ((Call) p).prompt(_enter, _pass, _repeat);
        if (_pass != null) {
          try {
            if (!prompt.getInput().get().hasMatch()) {
              throw new ConferencePasswordNoMatchException();
            }
          }
          catch (final InterruptedException e) {
            throw new MediaException(e);
          }
          catch (final ExecutionException e) {
            throw new MediaException(e);
          }
        }
        else {
          try {
            prompt.getOutput().get();
          }
          catch (final Exception e) {
            throw new MediaException(e);
          }
        }
      }
    }
  }

  @Override
  public void postJoin(final Participant p, final Conference f) {
    if (p instanceof Call && _term != null) {
      final Call call = (Call) p;
      final Observer observer = new Observer() {
        @SuppressWarnings("unused")
        @State
        public void handleEvent(final InputCompleteEvent<Call> event) {
          if (event.hasMatch()) {
            f.unjoin(p);
          }
        }
      };
      call.addObserver(observer);
      try {
        call.input(_term);
      }
      catch (final MediaException e) {
        call.removeObserver(observer);
        throw e;
      }
    }
  }

  @Override
  public void preUnjoin(final Participant p, final Conference f) {

  }

  @Override
  public void postUnjoin(final Participant p, final Conference f) {
    if (p instanceof Call && _exit != null) {
      final Call call = (Call) p;
      try {
        call.output(_exit).get();
      }
      catch (final InterruptedException e) {
        throw new MediaException(e);
      }
      catch (final ExecutionException e) {
        throw new MediaException(e);
      }
    }
  }

}
