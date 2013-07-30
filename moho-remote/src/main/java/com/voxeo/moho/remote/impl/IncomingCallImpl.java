package com.voxeo.moho.remote.impl;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.remote.MohoRemoteException;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.xmpp.stanza.IQ;

public class IncomingCallImpl extends CallImpl implements IncomingCall {
  private static final Logger LOG = Logger.getLogger(IncomingCallImpl.class);

  protected boolean isRejected;

  protected boolean isAccepted;

  protected boolean isRedirected;

  protected boolean isAcceptedWithEarlyMedia;

  protected JointImpl waitAnswerJoint = null;

  public IncomingCallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller, CallableEndpoint callee,
      Map<String, String> headers) {
    super(mohoRemote, callID, caller, callee, headers);
    this.setCallState(Call.State.INITIALIZED);
  }

  @Override
  public boolean isAccepted() {
    return isAccepted;
  }

  @Override
  public boolean isRejected() {
    return isRejected;
  }

  @Override
  public void reject(Reason reason) throws SignalException {
    reject(reason, null);
  }

  @Override
  public void reject(Reason reason, Map<String, String> headers) throws SignalException {
    try {
      RejectCommand command = new RejectCommand();
      command.setHeaders(headers);
      command.setCallId(this.getId());
      command.setReason(getRayoCallRejectReasonByMohoReason(reason));
      IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());
      if (iq.isError()) {
        this.setCallState(Call.State.FAILED);
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isRejected = true;
        this.setCallState(Call.State.DISCONNECTED);
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public boolean isRedirected() {
    return isRedirected;
  }

  @Override
  public void redirect(Endpoint other) throws SignalException {
    redirect(other, null);
  }

  @Override
  public void redirect(Endpoint other, Map<String, String> headers) throws SignalException {
    RedirectCommand redirect = new RedirectCommand();

    redirect.setTo(other.getURI());
    redirect.setCallId(this.getId());
    redirect.setHeaders(headers);
    try {
      IQ iq = _mohoRemote.getRayoClient().command(redirect, this.getId());

      if (iq.isError()) {
        this.setCallState(Call.State.FAILED);
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isRedirected = true;
        this.setCallState(Call.State.DISCONNECTED);
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public boolean isAcceptedWithEarlyMedia() {
    return isAcceptedWithEarlyMedia;
  }

  @Override
  public void acceptWithEarlyMedia() throws SignalException, MediaException {
    acceptWithEarlyMedia((Map<String, String>) null);
  }

  @Override
  public void acceptWithEarlyMedia(Observer... observer) throws SignalException, MediaException {
    addObserver(observer);
    acceptWithEarlyMedia();
  }

  @Override
  public void acceptWithEarlyMedia(Map<String, String> headers) throws SignalException, MediaException {
    try {
      AcceptCommand command = new AcceptCommand();
      command.setHeaders(headers);
      command.setCallId(this.getId());
      command.setEarlyMedia(true);
      IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());
      if (iq.isError()) {
        this.setCallState(Call.State.FAILED);
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isAcceptedWithEarlyMedia = true;
        this.setCallState(Call.State.INPROGRESS);
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public void accept() throws SignalException {
    accept((Map<String, String>) null);
  }

  @Override
  public void accept(Map<String, String> headers) throws SignalException {
    try {
      AcceptCommand command = new AcceptCommand();
      command.setHeaders(headers);
      command.setCallId(this.getId());
      IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());
      if (iq.isError()) {
        this.setCallState(Call.State.FAILED);
        com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isAccepted = true;
        this.setCallState(Call.State.ACCEPTED);
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new MohoRemoteException(e);
    }
  }

  @Override
  public void accept(Observer... observer) throws SignalException {
    addObserver(observer);
    accept();
  }

  @Override
  public void answer() throws SignalException, MediaException {
    this.answer((Map<String, String>) null);
  }

  @Override
  public void answer(Observer... observer) throws SignalException, MediaException {
    addObserver(observer);
    answer();
  }

  @Override
  public void answer(Map<String, String> headers) throws SignalException, MediaException {
    final Joint joint = this.internalAnswer(null, headers, false);
    while (!joint.isDone()) {
      try {
        joint.get();
      }
      catch (final InterruptedException e) {
        // ignore
      }
      catch (final ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SignalException) {
          throw (SignalException) cause;
        }
        throw new SignalException(cause);
      }
    }
  }

  @Override
  public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isProxied() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void proxyTo(boolean recordRoute, boolean parallel, Map<String, String> headers, Endpoint... destinations) {
    // does rayo support proxyTo?
    // TODO Auto-generated method stub
  }

  @Override
  public Call getSource() {
    return this;
  }

  @Override
  public String startJoin() throws MohoRemoteException {
    return _id;
  }

  // TODO this method is sync now.
  // should make _mohoRemote.getRayoClient().answer() asynchronous, and make
  // this method just send out command, the joint and event stuff should be
  // processed in the join() method, wait for the result or error IQ.
  private Joint internalAnswer(Direction direction, Map<String, String> headers, boolean dispatchJoinToMediaEvent) {
    if (waitAnswerJoint == null) {
      waitAnswerJoint = new JointImpl(this, direction);
      MohoJoinCompleteEvent joinCompleteEvent = null;
      try {
        AnswerCommand command = new AnswerCommand();
        command.setHeaders(headers);
        command.setCallId(this.getId());
        IQ iq = _mohoRemote.getRayoClient().answer(this.getId(), command);

        if (iq.isError()) {
          this.setCallState(Call.State.FAILED);
          com.voxeo.rayo.client.xmpp.stanza.Error error = iq.getError();
          SignalException exception = new SignalException(error.getCondition() + error.getText());
          joinCompleteEvent = new MohoJoinCompleteEvent(this, null, JoinCompleteEvent.Cause.ERROR, exception, true);
          if (dispatchJoinToMediaEvent) {
            dispatch(joinCompleteEvent);
          }
          waitAnswerJoint.done(exception);
        }
        else {
          isAccepted = true;
          this.setCallState(Call.State.CONNECTED);
          joinCompleteEvent = new MohoJoinCompleteEvent(this, null, JoinCompleteEvent.Cause.JOINED, true);
          if (dispatchJoinToMediaEvent) {
            dispatch(joinCompleteEvent);
          }
          waitAnswerJoint.done(joinCompleteEvent);
        }
      }
      catch (XmppException e) {
        LOG.error("", e);
        throw new MohoRemoteException(e);
      }
    }

    return waitAnswerJoint;
  }

  @Override
  public Joint join(Direction direction) {
    return internalAnswer(direction, null, true);
  }

  @Override
  public Endpoint getAddress() {
    return _caller;
  }

  @Override
  public String getRemoteAddress() {
    return _callee.getURI().toString();
  }

  @Override
  public void setAsync(boolean async) {

  }

  @Override
  public boolean isAsync() {
    return false;
  }
}
