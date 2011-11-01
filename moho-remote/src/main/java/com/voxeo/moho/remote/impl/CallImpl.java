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

package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mediagroup.MediaGroup;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.core.AnsweredEvent;
import com.rayo.core.CallRejectReason;
import com.rayo.core.DtmfEvent;
import com.rayo.core.EndEvent;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.RingingEvent;
import com.rayo.core.UnjoinedEvent;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.OffHoldEvent;
import com.rayo.core.verb.OnHoldEvent;
import com.rayo.core.verb.Record;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbRef;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.DispatchableEventSource;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoEarlyMediaEvent;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.CallEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.RequestEvent;
import com.voxeo.moho.event.ResponseEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.remote.impl.event.MohoHangupEventImpl;
import com.voxeo.moho.remote.impl.media.InputImpl;
import com.voxeo.moho.remote.impl.media.OutputImpl;
import com.voxeo.moho.remote.impl.media.PromptImpl;
import com.voxeo.moho.remote.impl.media.RecordingImpl;

public abstract class CallImpl extends DispatchableEventSource implements Call, RayoListener {

  private static final Logger LOG = Logger.getLogger(CallImpl.class);

  protected CallableEndpoint _caller;

  protected CallableEndpoint _callee;

  protected Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();

  protected List<Call> _peers = new ArrayList<Call>(0);

  protected JoineeData _joinees = new JoineeData();

  protected MohoRemoteImpl _mohoRemote;

  protected State _state;

  protected Map<String, String> _headers;

  protected Map<String, RayoListener> _componentListeners = new ConcurrentHashMap<String, RayoListener>();

  protected Map<String, JointImpl> _joints = new ConcurrentHashMap<String, JointImpl>();

  protected Map<String, UnJointImpl> _unjoints = new ConcurrentHashMap<String, UnJointImpl>();

  protected boolean _isMuted;

  protected boolean _isHold;
  
  protected ReadWriteLock joinLock = new ReentrantReadWriteLock(); 

  protected CallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller, CallableEndpoint callee,
      Map<String, String> headers) {
    _mohoRemote = mohoRemote;
    _dispatcher.setExecutor(_mohoRemote.getExecutor(), true);
    _caller = caller;
    _callee = callee;
    _id = callID;
    if (_id != null) {
      _mohoRemote.addCall(this);
    }
    _headers = headers;
  }

  @Override
  public Output<Call> output(String text) throws MediaException {
    OutputImpl<Call> output = null;
    try {
      VerbRef verbRef = _mohoRemote.getRayoClient().output(text, this.getId());

      output = new OutputImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return output;
  }

  @Override
  public Output<Call> output(URI media) throws MediaException {
    OutputImpl<Call> output = null;
    try {
      VerbRef verbRef = _mohoRemote.getRayoClient().output(media, this.getId());

      output = new OutputImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return output;
  }

  @Override
  public Output<Call> output(OutputCommand output) throws MediaException {
    OutputImpl<Call> outputFuture = null;
    try {
      com.rayo.core.verb.Output rayoOutput = new com.rayo.core.verb.Output();
      if (output.getStartingOffset() > 0) {
        rayoOutput.setStartOffset(Duration.standardSeconds(output.getStartingOffset() / 1000));
      }
      if (output.isStartInPausedMode()) {
        rayoOutput.setStartPaused(true);
      }
      if (output.getRepeatInterval() > 0) {
        rayoOutput.setRepeatInterval(Duration.standardSeconds(output.getRepeatInterval() / 1000));
      }
      if (output.getRepeatTimes() > 0) {
        rayoOutput.setRepeatTimes(output.getRepeatTimes());
      }
      if (output.getMaxtime() > 0) {
        rayoOutput.setMaxTime(Duration.standardSeconds(output.getMaxtime() / 1000));
      }
      if (output.getVoiceName() != null) {
        rayoOutput.setVoice(output.getVoiceName());
      }
      VerbRef verbRef = null;
      OutputCommand next = null;
      if (output.getAudibleResources() != null && output.getAudibleResources().length > 0) {
        AudibleResource ar = output.getAudibleResources()[0];
        if (ar instanceof TextToSpeechResource) {
          rayoOutput.setPrompt(new Ssml(((TextToSpeechResource) ar).getText()));
        }
        else  {
          verbRef = _mohoRemote.getRayoClient().output(ar.toURI(), this.getId());
        }
        
        if (output.getAudibleResources().length > 1) {
          next = (OutputCommand) output.clone();
          next.setAudibleResource(Arrays.copyOfRange(output.getAudibleResources(), 1, output.getAudibleResources().length));
        }
      }
      if (verbRef == null) {
        verbRef = _mohoRemote.getRayoClient().output(rayoOutput, this.getId());
      }

      outputFuture = new OutputImpl<Call>(verbRef, next, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return outputFuture;
  }

  @Override
  public Prompt<Call> prompt(String text, String grammar, int repeat) throws MediaException {
    final OutputCommand output = text == null ? null : new OutputCommand(new TextToSpeechResource(text));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @Override
  public Prompt<Call> prompt(URI media, String grammar, int repeat) throws MediaException {
    final OutputCommand output = media == null ? null : new OutputCommand(new AudioURIResource(media));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @Override
  public Prompt<Call> prompt(OutputCommand output, InputCommand input, int repeat) throws MediaException {
    PromptImpl<Call> prompt = new PromptImpl<Call>(_mohoRemote.getExecutor());
    if (output != null) {
      for (int i = 0; i < repeat + 1; i++) {
        prompt.setOutput(output(output));
      }
    }
    if (input != null) {
      prompt.setInput(input(input));
    }

    return prompt;
  }

  @Override
  public Input<Call> input(String grammar) throws MediaException {
    InputImpl<Call> input = null;
    try {
      Choices choice = new Choices();
      choice.setContent(grammar);
      choice.setContentType(Choices.VOXEO_GRAMMAR);

      List<Choices> list = new ArrayList<Choices>(1);
      list.add(choice);
      com.rayo.core.verb.Input command = new com.rayo.core.verb.Input();
      command.setCallId(this.getId());
      command.setGrammars(list);

      VerbRef verbRef = _mohoRemote.getRayoClient().input(command, this.getId());
      input = new InputImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return input;
  }

  @Override
  public Input<Call> input(InputCommand inputCommand) throws MediaException {
    InputImpl<Call> input = null;
    try {
      Grammar[] grammars = inputCommand.getGrammars();
      List<Choices> list = new ArrayList<Choices>(grammars.length);
      for (Grammar grammar : grammars) {
        Choices choice = new Choices();
        if (grammar.getText() != null) {
          choice.setContent(grammar.getText());
          choice.setContentType(Choices.VOXEO_GRAMMAR);
        }
        else {
          choice.setUri(grammar.getUri());
        }
        list.add(choice);
      }

      com.rayo.core.verb.Input command = new com.rayo.core.verb.Input();
      command.setCallId(this.getId());
      command.setGrammars(list);
      if (inputCommand.getInitialTimeout() > 0) {
        command.setInitialTimeout(Duration.standardSeconds(inputCommand.getInitialTimeout() / 1000));
      }
      if (inputCommand.getInterDigitsTimeout() > 0) {
        command.setInterDigitTimeout(Duration.standardSeconds(inputCommand.getInterDigitsTimeout()));
      }
      if (inputCommand.getTerminator() != null) {
        command.setTerminator(inputCommand.getTerminator());
      }
      if (inputCommand.getMinConfidence() > 0) {
        command.setMinConfidence(inputCommand.getMinConfidence());
      }
      if (inputCommand.getRecognizer() != null) {
        command.setRecognizer(inputCommand.getRecognizer());
      }
      if (inputCommand.getInputMode() != null) {
        if (inputCommand.getInputMode() == com.voxeo.moho.media.InputMode.DTMF) {
          command.setMode(com.rayo.core.verb.InputMode.DTMF);
        }
        else if (inputCommand.getInputMode() == com.voxeo.moho.media.InputMode.SPEECH) {
          command.setMode(com.rayo.core.verb.InputMode.VOICE);
        }
        else {
          command.setMode(com.rayo.core.verb.InputMode.ANY);
        }
      }
      if (inputCommand.getSensitivity() > 0) {
        command.setSensitivity(inputCommand.getSensitivity());
      }

      VerbRef verbRef = _mohoRemote.getRayoClient().input(command, this.getId());
      input = new InputImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return input;
  }

  @Override
  public Recording<Call> record(URI recordURI) throws MediaException {
    Recording<Call> recording = null;
    try {
      Record record = new Record();
      record.setTo(recordURI);
      record.setCallId(this.getId());
      VerbRef verbRef = _mohoRemote.getRayoClient().record(record, this.getId());

      recording = new RecordingImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return recording;
  }

  @Override
  public Recording<Call> record(RecordCommand command) throws MediaException {
    Recording<Call> recording = null;
    try {
      Record record = new Record();
      record.setTo(command.getRecordURI());
      record.setCallId(this.getId());

      if (command.getFinalTimeout() > 0) {
        record.setFinalTimeout(Duration.standardSeconds(command.getFinalTimeout() / 1000));
      }
      if (command.getFileFormat() != null) {
        record.setFormat(command.getFileFormat().toString());
      }
      if (command.getMaxDuration() > 0) {
        record.setMaxDuration(Duration.standardSeconds(command.getMaxDuration() / 1000));
      }
      if (command.isStartBeep()) {
        record.setStartBeep(true);
      }
      if (command.isStartInPausedMode()) {
        record.setStartPaused(true);
      }
      if (command.getInitialTimeout() > 0) {
        record.setInitialTimeout(Duration.standardSeconds(command.getInitialTimeout() / 1000));
      }
      VerbRef verbRef = _mohoRemote.getRayoClient().record(record, this.getId());

      recording = new RecordingImpl<Call>(verbRef, this, this);
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MediaException(e);
    }
    return recording;
  }

  @Override
  public MediaGroup getMediaGroup() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public void hangup() {
    hangup(null);
  }

  @Override
  public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
    Future<T> retval = null;
    if (!(event instanceof CallEvent) && !(event instanceof RequestEvent) && !(event instanceof ResponseEvent)) {
      retval = super.dispatch(event);
    }
    else {
      final Runnable acceptor = new Runnable() {
        @Override
        public void run() {
          if (event instanceof EarlyMediaEvent) {
            if (!((MohoEarlyMediaEvent) event).isProcessed()) {
              try {
                ((EarlyMediaEvent) event).reject(null);
              }
              catch (final SignalException e) {
                LOG.warn(e);
              }
            }
          }

          else if (event instanceof AcceptableEvent) {
            if (!((AcceptableEvent) event).isAccepted() && !((AcceptableEvent) event).isRejected()) {
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
      retval = super.dispatch(event, acceptor);
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

  @Override
  public Joint join(CallableEndpoint other, JoinType type, Direction direction, Map<String, String> headers) {
    Call outboundCall = other.createCall(getAddress(), headers);
    return this.join(outboundCall, type, direction);
  }

  @Override
  public Joint join(Participant other, JoinType type, Direction direction) {

	  return join(other, type, Boolean.TRUE, direction);
  }

  @Override
  public Unjoint unjoin(Participant other) {
    if (!_joinees.contains(other)) {
      throw new IllegalStateException("Not joined.");
    }
    UnJointImpl unJoint = null;
    try {
      JoinDestinationType type = null;
      if (other instanceof Call) {
        type = JoinDestinationType.CALL;
      }
      else {
        type = JoinDestinationType.MIXER;
      }
      unJoint = new UnJointImpl(this);
      _unjoints.put(other.getId(), unJoint);
      IQ iq = _mohoRemote.getRayoClient().unjoin(other.getId(), type, this.getId());

      if (iq.isError()) {
        _unjoints.remove(other.getId());
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      
    }
    catch (XmppException e) {
      _unjoints.remove(other.getId());
      LOG.error("", e);
      throw new SignalException("", e);
    }
    return unJoint;
  }

  @Override
  public Endpoint getInvitor() {
    return _caller;
  }

  @Override
  public CallableEndpoint getInvitee() {
    return _callee;
  }

  @Override
  public State getCallState() {
    return _state;
  }

  @Override
  public void mute() {
    if (_isMuted) {
      return;
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().mute(this.getId());
      if (iq.isError()) {
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isMuted = true;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException("", e);
    }
  }

  @Override
  public void unmute() {
    if (!_isMuted) {
      throw new IllegalStateException("This call hasn't been muted");
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().unmute(this.getId());
      if (iq.isError()) {
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isMuted = false;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException("", e);
    }
  }

  @Override
  public synchronized void hold() {
    if (_isHold) {
      return;
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().hold(this.getId());
      if (iq.isError()) {
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isHold = true;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException("", e);
    }
  }

  @Override
  public void unhold() {
    if (!_isHold) {
      throw new IllegalStateException("This call hasn't been hold");
    }
    try {
      IQ iq = _mohoRemote.getRayoClient().unhold(this.getId());
      if (iq.isError()) {
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        _isHold = false;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException("", e);
    }
  }

  @Override
  public void hangup(Map<String, String> headers) {
    if (_state == Call.State.ACCEPTED || _state == Call.State.CONNECTED || _state == Call.State.INPROGRESS) {
      try {
        HangupCommand command = new HangupCommand();
        command.setCallId(this.getId());
        command.setHeaders(headers);

        IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());

        if (iq.isError()) {
          com.rayo.client.xmpp.stanza.Error error = iq.getError();
          throw new SignalException(error.getCondition() + error.getText());
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new SignalException("", e);
      }
    }

    cleanUp();
  }

  @Override
  public boolean isHold() {
    return _isHold;
  }

  @Override
  public boolean isMute() {
    return _isMuted;
  }

  @Override
  public JoinableStream getJoinableStream(StreamType value) {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public JoinableStream[] getJoinableStreams() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public Endpoint getAddress() {
    return _callee;
  }

  @Override
  public String getRemoteAddress() {
    return _caller.getURI().toString();
  }

  @Override
  public Participant[] getParticipants() {
    return _joinees.getJoinees();
  }

  @Override
  public Participant[] getParticipants(Direction direction) {
    return _joinees.getJoinees(direction);
  }

  @Override
  public void disconnect() {
    this.hangup();
  }

  protected void cleanUp() {
    _attributes.clear();
    _peers.clear();
    _joinees.clear();

    _headers = null;

    // TODO
    // Commenting this as otherwise complete events for active verbs will not make it to the client
    //_componentListeners.clear();

    Collection<JointImpl> joints = _joints.values();
    for (JointImpl joint : joints) {
      joint.done(new SignalException("Call disconnect."));
    }
    _joints.clear();

    Collection<UnJointImpl> unjoints = _unjoints.values();
    for (UnJointImpl unjoint : unjoints) {
      unjoint.done(new SignalException("Call disconnect."));
    }
    _unjoints.clear();
  }

  @Override
  public MediaObject getMediaObject() {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public String getHeader(String name) {
    return _headers.get(name);
  }

  @Override
  public ListIterator<String> getHeaders(String name) {
    List<String> list = new ArrayList<String>();
    String value = _headers.get(name);
    list.add(value);
    return list.listIterator();
  }

  @Override
  public Iterator<String> getHeaderNames() {
    return CollectionUtils.unmodifiableCollection(_headers.values()).iterator();
  }

  @Override
  public void onRayoEvent(JID from, Presence presence) {
    if (from.getResource() != null) {
      RayoListener listener = _componentListeners.get(from.getResource());
      if (listener != null) {
    	  listener.onRayoEvent(from, presence);
      }
    }
    else {
      Object object = presence.getExtension().getObject();
      if (object instanceof EndEvent) {
        EndEvent event = (EndEvent) object;
        EndEvent.Reason rayoReason = event.getReason();
        if (rayoReason == EndEvent.Reason.HANGUP) {
          MohoHangupEventImpl mohoEvent = new MohoHangupEventImpl(this);
          this.dispatch(mohoEvent);
        }
        MohoCallCompleteEvent mohoEvent = new MohoCallCompleteEvent(this,
            getMohoReasonByRayoEndEventReason(event.getReason()), null, event.getHeaders());
        this.dispatch(mohoEvent);

        _state = State.DISCONNECTED;
        cleanUp();
      }
      else if (object instanceof DtmfEvent) {
        DtmfEvent event = (DtmfEvent) object;
        MohoInputDetectedEvent<Call> mohoEvent = new MohoInputDetectedEvent<Call>(this, event.getSignal());
        this.dispatch(mohoEvent);
      }
      else if (object instanceof JoinedEvent) {

    	  Lock lock = joinLock.readLock();
		  lock.lock();
    	  try {
	        JoinedEvent event = (JoinedEvent) object;
	        MohoJoinCompleteEvent mohoEvent = null;
	        String id = event.getTo();
	        JoinDestinationType type = event.getType();
	        JointImpl joint = _joints.remove(id);
	        if (type == JoinDestinationType.CALL) {
	          Call peer = (Call) this.getMohoRemote().getParticipant(id);
	          _joinees.add(peer, joint.getType(), joint.getDirection());
	          _peers.add(peer);
	          mohoEvent = new MohoJoinCompleteEvent(this, peer, JoinCompleteEvent.Cause.JOINED, true);
	        }
	        else {
	          // TODO mixer unjoin.
	        }
	        this.dispatch(mohoEvent);
    	  } finally {
    		  lock.unlock();
    	  }
      }

      else if (object instanceof UnjoinedEvent) {
        UnjoinedEvent event = (UnjoinedEvent) object;
        MohoUnjoinCompleteEvent mohoEvent = null;
        String id = event.getFrom();
        JoinDestinationType type = event.getType();
        _unjoints.remove(id);
        if (type == JoinDestinationType.CALL) {
          Call peer = (Call) _mohoRemote.getParticipant(id);
          _joinees.remove(peer);
          _peers.remove(peer);
          mohoEvent = new MohoUnjoinCompleteEvent(this, peer, UnjoinCompleteEvent.Cause.SUCCESS_UNJOIN, true);
          this.dispatch(mohoEvent);
        }
        else {
          // TODO mixer unjoin.
        }
      }
      else if (object instanceof OffHoldEvent) {
        // TODO for conference
      }
      else if (object instanceof OnHoldEvent) {
        // TODO for conference
      }  else if (object instanceof AnsweredEvent) {
    	  // TODO for answered
      }
      else if (object instanceof RingingEvent) {
    	  // TODO for answered
      }
      else {
        LOG.error("Can't process presence:" + presence);
      }
    }
  }

  @Override
  public void onRayoCommandResult(JID from, IQ iq) {
    if (from.getResource() != null) {
      RayoListener listener = _componentListeners.get(from.getResource());
      if (listener != null) {
    	  listener.onRayoCommandResult(from, iq);
      }
    }
    else {

    }
  }

  protected CallRejectReason getRayoCallRejectReasonByMohoReason(AcceptableEvent.Reason reason) {
    switch (reason) {
      case DECLINE:
        return CallRejectReason.DECLINE;
      case BUSY:
        return CallRejectReason.BUSY;
      case ERROR:
        return CallRejectReason.ERROR;
      default:
        return CallRejectReason.ERROR;
    }
  }

  protected CallCompleteEvent.Cause getMohoReasonByRayoEndEventReason(EndEvent.Reason reason) {
    switch (reason) {
      case HANGUP:
        return CallCompleteEvent.Cause.DISCONNECT;
      case TIMEOUT:
        return CallCompleteEvent.Cause.TIMEOUT;
      case BUSY:
        return CallCompleteEvent.Cause.BUSY;
      case REJECT:
        return CallCompleteEvent.Cause.DECLINE;
      case REDIRECT:
        return CallCompleteEvent.Cause.REDIRECT;
      case ERROR:
        return CallCompleteEvent.Cause.ERROR;
      default:
        return CallCompleteEvent.Cause.ERROR;
    }
  }

  public MohoRemoteImpl getMohoRemote() {
    return _mohoRemote;
  }

  public void removeComponentListener(String id) {
    _componentListeners.remove(id);
  }

  public void addComponentListener(String id, RayoListener listener) {
    _componentListeners.put(id, listener);
  }

  public abstract void startJoin() throws XmppException;

  @Override
  public Joint join(Participant other, JoinType type, boolean force, Direction direction) {

	    JointImpl joint = null;
	    Lock lock = joinLock.writeLock(); 
	    try {
	    	lock.lock();
	      this.startJoin();
	      // TODO make a parent class implement participant.
	      ((CallImpl) other).startJoin();

	      JoinCommand command = new JoinCommand();
	      command.setCallId(this.getId());
	      command.setTo(other.getId());
	      command.setMedia(type);
	      command.setDirection(direction);
	      command.setForce(force);
	      JoinDestinationType destinationType = null;
	      if (other instanceof Call) {
	        destinationType = JoinDestinationType.CALL;
	      }
	      else {
	        destinationType = JoinDestinationType.MIXER;
	      }
	      command.setType(destinationType);
	      
	      joint = new JointImpl(this, type, direction);
	      _joints.put(other.getId(), joint);
	      ((CallImpl) other)._joints.put(this.getId(), joint);
	      
	      IQ iq = _mohoRemote.getRayoClient().join(command, this.getId());

	      if (iq.isError()) {
	          _joints.remove(other.getId());
	          ((CallImpl) other)._joints.remove(this.getId());
	        com.rayo.client.xmpp.stanza.Error error = iq.getError();
	        throw new SignalException(error.getCondition() + error.getText());
	      }
	    }
	    catch (XmppException e) {
	        _joints.remove(other.getId());
	        ((CallImpl) other)._joints.remove(this.getId());
	      LOG.error("", e);
	      throw new SignalException("", e);
	    } finally {
	    	lock.unlock();
	    }
	    return joint;
  }
  
  @Override
  public JoinType getJoinType(Participant participant) {
    return _joinees.getJoinType(participant);
  }
}
