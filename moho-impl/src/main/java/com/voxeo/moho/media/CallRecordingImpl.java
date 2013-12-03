package com.voxeo.moho.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.mscontrol.networkconnection.NetworkConnection;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.GenericMediaService.MaxCallRecordDurationTask;
import com.voxeo.moho.media.dialect.MediaDialect;

public class CallRecordingImpl<T extends EventSource> implements Recording<T> {

  protected NetworkConnection _nc;

  protected MediaDialect _dialect;

  protected boolean _normalDisconnected = false;
  
  protected ScheduledFuture maxDurationTimerFuture;
  
  protected MaxCallRecordDurationTask maxDurationTask;

  protected SettableResultFuture<RecordCompleteEvent<T>> _future = new SettableResultFuture<RecordCompleteEvent<T>>();
  
  protected boolean maxDurationStop;

  public CallRecordingImpl(NetworkConnection nc, MediaDialect dialect) {
    super();
    this._nc = nc;
    this._dialect = dialect;
  }

  protected void done(final RecordCompleteEvent<T> event) {
    _future.setResult(event);
  }

  protected void done(final MediaException exception) {
    _future.setException(exception);
  }

  @Override
  public void stop() {
    if (!_future.isDone()) {
      _dialect.stopCallRecord(_nc);

      try {
        _future.get();
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (ExecutionException e) {
        // ignore
      }
    }

  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public RecordCompleteEvent<T> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public RecordCompleteEvent<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
      TimeoutException {
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
  public void pause() {
    throw new UnsupportedOperationException("Call record doesn't support pause()");
  }

  @Override
  public void resume() {
    throw new UnsupportedOperationException("Call record doesn't support resume()");
  }

  public void normalDisconnect(boolean normal) {
    _normalDisconnected = true;
  }

  public boolean isNormalDisconnect() {
    return _normalDisconnected;
  }

  public ScheduledFuture getMaxDurationTimerFuture() {
    return maxDurationTimerFuture;
  }

  public void setMaxDurationTimerFuture(ScheduledFuture maxDurationTimerFuture) {
    this.maxDurationTimerFuture = maxDurationTimerFuture;
  }

  public MaxCallRecordDurationTask getMaxDurationTask() {
    return maxDurationTask;
  }

  public void setMaxDurationTask(MaxCallRecordDurationTask maxDurationTask) {
    this.maxDurationTask = maxDurationTask;
  }

  public boolean isMaxDurationStop() {
    return maxDurationStop;
  }

  public void setMaxDurationStop(boolean maxDurationStop) {
    this.maxDurationStop = maxDurationStop;
  }
}
