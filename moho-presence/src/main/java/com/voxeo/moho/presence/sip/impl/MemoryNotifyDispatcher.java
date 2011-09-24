package com.voxeo.moho.presence.sip.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.voxeo.moho.presence.NotifyDispatcher;

/**
 * 
 * MemoryNotifyDispatcher from SIPoint
 * 
 */
public class MemoryNotifyDispatcher implements NotifyDispatcher {
  private static final Logger LOG = Logger.getLogger(MemoryNotifyDispatcher.class);

  int _remainingThreshold = 0;

  int _cap = 10000;

  Executor _executor = null;

  boolean _stop = false;

  BlockingQueue<NotifyRequest> _queue = null;


  // The tolal length of the queue
  public int getCapability() {
    return _cap;
  }

  // currently used length
  public int remainingCapacity() {
    return _queue.remainingCapacity();
  }

  public boolean isBusy() {
    return remainingCapacity() < _remainingThreshold;
  }

  public boolean isFull() {
    boolean result = false;
    result = (remainingCapacity() == 0);
    return result;
  }

  /**
   * Adds the specified element to the tail of this queue, waiting if necessary
   * for space to become available.
   * 
   * @param nr
   * @return
   * @throws InterruptedException
   */
  public void put(NotifyRequest nr) throws InterruptedException {
    _queue.put(nr);
  }

  /**
   * Retrieves and removes the head of this queue, waiting if no elements are
   * present on this queue *
   * 
   * @return
   * @throws InterruptedException
   */
  public NotifyRequest take() throws InterruptedException {
    return _queue.take();
  }

  public MemoryNotifyDispatcher(Executor executor, int cap) {
    /**
     * by limiting the max thread number of this independent thread pool, we can
     * avoid too many blocked threads caused by the SipMessage.send(), this
     * happens when the remote TCP socket is down, and the OS has to wait for a
     * time period befroe it throws an IOException
     */
    _executor = executor;
    _cap = cap;
    _remainingThreshold = (int) (_cap * 0.25);
    _queue = new LinkedBlockingQueue<NotifyRequest>(_cap);
  }

  public void shutdown() {
    _stop = true;
    // return immediately if full
    _queue.offer(ShutdownSignal.SHUTDOWN_SIGNAL);
  }

  public void run() {
    String name = Thread.currentThread().getName(); 
    Thread.currentThread().setName(toString());
    LOG.info("Started " + toString());
    while (!_stop) {
      try {
        NotifyRequest nr = take();
        if (nr == ShutdownSignal.SHUTDOWN_SIGNAL) {
          break;
        }
        sendIt(nr);
      }
      catch (InterruptedException e) {
        continue;
      }
      catch (Throwable t) {
        LOG.error("MemoryNotifyDispatcher Error", t);
        continue;
      }
    }
    LOG.info("Stopped " + toString());
    Thread.currentThread().setName(name);
  }

  /**
   * Dispath a NOTIFY message to a new thread in the thread pool to send it.
   * <br>
   * If the thread pool is full, this method will block until it can get a spare
   * thread to send the notify.
   * 
   * @param session
   * @param id
   * @return
   */
  protected void sendIt(NotifyRequest nr) {
    try {
      // wait until the thread pool is able to send it
      while (true) {
        try {
          _executor.execute(nr);
          break;
        }
        catch (RejectedExecutionException e) {
          if (_executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor exec = (ThreadPoolExecutor) _executor;
            LOG.warn("MemoryNotifySender thread pool is full. Thread num = " + exec.getPoolSize());
          }
          else {
            LOG.warn("MemoryNotifySender thread pool is full");
          }
          try {
            Thread.sleep(500);
          }
          catch (InterruptedException e1) {
            //ignore
          }
        }
      }
    }
    catch (Throwable t) {
      LOG.error("MemoryNotifyDispatcher Error", t);
    }
    finally {
      ;
    }
  }

  // indicate the shutdown of the queue worker thread
  static class ShutdownSignal extends NotifyRequest {
    static final ShutdownSignal SHUTDOWN_SIGNAL = new ShutdownSignal();
    ShutdownSignal() {
    }
  }
  
   @Override
   public String toString() {
    return "MemNotifySender[cap=" + _cap + "]";
  }
}
