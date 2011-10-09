package com.voxeo.moho.xmpp;

import org.w3c.dom.Element;

import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;

public interface XMPPAcceptableEvent {
  public enum Reason {
    BAD_REQUEST {
      @Override
      public Condition getCondition() {
        return Condition.BAD_REQUEST;
      }

      @Override
      public Type getErrorType() {
        return Type.MODIFY;
      }
    },
    CONFLICT {

      @Override
      public Condition getCondition() {
        return Condition.CONFLICT;
      }

      @Override
      public Type getErrorType() {
        return Type.CANCEL;
      }
      
    },
    ITEM_NOT_FOUND {

      @Override
      public Condition getCondition() {
        return Condition.ITEM_NOT_FOUND;
      }

      @Override
      public Type getErrorType() {
        return Type.CANCEL;
      }
      
    },
    FEATURE_NOT_IMPLEMENTED {
      @Override
      public Condition getCondition() {
        return Condition.FEATURE_NOT_IMPLEMENTED;
      }

      @Override
      public Type getErrorType() {
        return Type.CANCEL;
      }
    },
    INTERNAL_SERVER_ERROR {
      @Override
      public Condition getCondition() {
        return Condition.INTERNAL_SERVER_ERROR;
      }

      @Override
      public Type getErrorType() {
        return Type.CANCEL;
      }
    },
    SERVICE_UNAVAILABLE {

      @Override
      public Condition getCondition() {
        return Condition.SERVICE_UNAVAILABLE;
      }

      @Override
      public Type getErrorType() {
        return Type.CANCEL;
      }
      
    };

    public abstract Condition getCondition();

    public abstract Type getErrorType();
  }

  boolean isAccepted();

  boolean isRejected();

  void reject(Reason reason, String text);

  void reject(Reason reason);

  void accept(Element... elem);

  void accept();
}
