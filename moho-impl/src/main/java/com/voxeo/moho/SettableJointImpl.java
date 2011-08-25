package com.voxeo.moho;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.util.SettableResultFuture;

public class SettableJointImpl implements Joint {

  protected SettableResultFuture<JoinCompleteEvent> _future = new SettableResultFuture<JoinCompleteEvent>();

  public SettableJointImpl() {
    super();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public JoinCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public JoinCompleteEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
      TimeoutException {
    return _future.get(timeout, unit);
  }

  @Override
  public boolean isCancelled() {
    return _future.isDone();
  }

  @Override
  public boolean isDone() {
    return _future.isDone();
  }

  public void done(final JoinCompleteEvent event) {
    _future.setResult(event);
  }

  public void done(final Exception ex) {
    _future.setException(ex);
  }
}
