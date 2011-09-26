package com.voxeo.rayo.mohoremote.impl.utils;

import java.util.concurrent.Future;

/**
 * This interface is typically used in conjunction with
 * {@link SettableResultFuture} and can be passed along to processing
 * componenets allowing them to set hte result of some {@link Future} operation.
 * 
 * @author jdecastro
 * @param The
 *          type of result that can be set
 */
public interface FutureResult<C> {

  public void setResult(C result);

  public void setException(Throwable t);

}
