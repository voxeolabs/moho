package com.voxeo.moho.common.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SettableResultFuture<C> implements Future<C>, FutureResult<C> {

  final Lock lock = new ReentrantLock();

  private Condition hasResult = lock.newCondition();

  private C result;

  private Throwable exception;

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return result != null || exception != null;
  }

  @Override
  public C get() throws InterruptedException, ExecutionException {
    if (result == null && exception == null) {
      lock.lock();
      try {
        while (result == null && exception == null) {
          hasResult.await();
        }
      }
      finally {
        lock.unlock();
      }
    }
    if (exception != null) {
      throw new ExecutionException(exception);
    }
    return result;
  }

  @Override
  public C get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (result == null && exception == null) {
      lock.lock();
      try {
        while (result == null && exception == null) {
          if (!hasResult.await(timeout, unit)) {
            throw new TimeoutException();
          }
        }
      }
      finally {
        lock.unlock();
      }
    }
    if (exception != null) {
      throw new ExecutionException(exception);
    }
    return result;
  }

  public void setResult(C result) {
    this.result = result;
    lock.lock();
    try {
      hasResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  public void setException(Throwable t) {
    this.exception = t;
    lock.lock();
    try {
      hasResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }
}
