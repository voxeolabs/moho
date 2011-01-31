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

package com.voxeo.moho.event;

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.SignalException;
import com.voxeo.utils.Event;

public abstract class SignalEvent extends Event<EventSource> implements AcceptableEvent {

  public enum Reason {
    BUSY {
      @Override
      public int getCode() {
        return SipServletResponse.SC_BUSY_HERE;
      }
    },

    DECLINE {
      @Override
      public int getCode() {
        return SipServletResponse.SC_DECLINE;
      }
    },

    FORBIDEN {
      @Override
      public int getCode() {
        return SipServletResponse.SC_FORBIDDEN;
      }
    },

    ERROR {
      @Override
      public int getCode() {
        return SipServletResponse.SC_SERVER_INTERNAL_ERROR;
      }
    };

    public abstract int getCode();

  }

  private static final long serialVersionUID = -4047823356801745059L;

  protected boolean _accepted = false;

  protected SignalEvent(final EventSource source) {
    super(source);
  }

  @Override
  public boolean isAccepted() {
    return _accepted;
  }

  public void accept() throws SignalException, IllegalStateException {
    this.accept(null);
  }

  public boolean isProcessed() {
    return isAccepted();
  }

  protected synchronized void checkState() {
    if (isProcessed()) {
      throw new IllegalStateException("Event already be processed.");
    }
  }

}
