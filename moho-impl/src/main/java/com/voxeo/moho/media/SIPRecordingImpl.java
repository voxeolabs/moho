package com.voxeo.moho.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.MohoRecordCompleteEvent;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.sip.SipRecordingCall;

public class SIPRecordingImpl<T extends EventSource> implements Recording<T>, Observer {

  private static final Logger LOG = Logger.getLogger(SIPRecordingImpl.class);

  private SipRecordingCall _sipRecordingCall;

  private MixerImpl _mixer;

  protected SettableResultFuture<RecordCompleteEvent<T>> _future = new SettableResultFuture<RecordCompleteEvent<T>>();

  protected Long startTimestamp;

  protected Long pauseTimestamp;

  protected Long totalPauseDuration = 0l;

  protected Exception exception;

  public SIPRecordingImpl(SipRecordingCall sipRecordingCall, MixerImpl mixer) {
    super();
    this._sipRecordingCall = sipRecordingCall;
    _mixer = mixer;
    _sipRecordingCall.addObserver(this);
  }

  public void start() {
    try {
      LOG.debug("SIPRecording joining siprecording call:" + _sipRecordingCall.toString());
      JoinCompleteEvent joinEvent = _sipRecordingCall.join().get();
      if (joinEvent.getCause() != JoinCompleteEvent.Cause.JOINED) {
        throw new SignalException("Exception when starting SIPRecording.", joinEvent.getException());
      }

      LOG.debug("SIPRecording joining siprecording call to mixer.");
      _sipRecordingCall.join(_mixer, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
      startTimestamp = System.currentTimeMillis();
      LOG.debug("SIPRecording started.");
    }
    catch (InterruptedException e) {
      LOG.error("Exception when starting SIPRecording.", e);
      exception = e;
      throw new SignalException(e);
    }
    catch (ExecutionException e) {
      LOG.error("Exception when starting SIPRecording.", e);
      exception = e;
      throw new SignalException(e.getCause());
    }
    finally {
      if (exception != null) {
        _sipRecordingCall.hangup();
      }
    }
  }

  @Override
  public void stop() {
    LOG.debug("stopping SIPRecording, hangup siprecording call:" + _sipRecordingCall.toString());
    _sipRecordingCall.hangup();
  }

  @Override
  public boolean cancel(boolean arg0) {
    return false;
  }

  @Override
  public RecordCompleteEvent<T> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public RecordCompleteEvent<T> get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException,
      TimeoutException {
    return _future.get(arg0, arg1);
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return _future.isDone();
  }

  @Override
  public void pause() {
    if (pauseTimestamp != null) {
      throw new IllegalStateException("Recording already paused.");
    }
    LOG.debug("Pausing SIPRecording: " + _sipRecordingCall.toString());
    _sipRecordingCall.pauseRecording();
    pauseTimestamp = System.currentTimeMillis();
  }

  @Override
  public void resume() {
    if (pauseTimestamp == null) {
      throw new IllegalStateException("Recording is not paused.");
    }
    LOG.debug("Resuming SIPRecording: " + _sipRecordingCall.toString());
    _sipRecordingCall.resumeRecording();

    long pauseDuration = System.currentTimeMillis() - pauseTimestamp;
    totalPauseDuration += pauseDuration;
    pauseTimestamp = null;
  }

  @State
  public void onEvent(CallCompleteEvent event) {
    if (startTimestamp == null) {
      recordComplete(RecordCompleteEvent.Cause.ERROR, event.getException());
    }
    else {
      recordComplete(RecordCompleteEvent.Cause.DISCONNECT, null);
    }
  }

  private void recordComplete(RecordCompleteEvent.Cause cause, Exception ex) {
    if (_future.isDone()) {
      return;
    }
    LOG.debug(_sipRecordingCall + " SIPRecording complete, cause:" + cause);
    _mixer.setSipRecording(null);

    MohoRecordCompleteEvent recordCompleteEvent = null;
    if (startTimestamp != null) {
      if (pauseTimestamp != null) {
        long pauseDuration = System.currentTimeMillis() - pauseTimestamp;
        totalPauseDuration += pauseDuration;
        pauseTimestamp = null;
      }

      long duration = System.currentTimeMillis() - startTimestamp;
      if (totalPauseDuration != null) {
        duration -= totalPauseDuration;
      }
      recordCompleteEvent = new MohoRecordCompleteEvent(_mixer, cause, duration, this);
    }
    else {
      recordCompleteEvent = new MohoRecordCompleteEvent(_mixer, cause, 0, ex != null ? ex : exception, this);
    }

    _mixer.dispatch(recordCompleteEvent);
    _future.setResult(recordCompleteEvent);
  }
}
