package com.voxeo.moho.remote.impl;

import java.util.Map;

import org.apache.log4j.Logger;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.Observer;

public class IncomingCallImpl extends CallImpl implements IncomingCall {
  private static final Logger LOG = Logger.getLogger(IncomingCallImpl.class);

  protected boolean isRejected;

  protected boolean isAccepted;

  protected boolean isRedirected;

  public IncomingCallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller, CallableEndpoint callee,
      Map<String, String> headers) {
    super(mohoRemote, callID, caller, callee, headers);
    _state = Call.State.INITIALIZED;
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
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isRejected = true;
        _state = Call.State.DISCONNECTED;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException(e);
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
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isRedirected = true;
        _state = Call.State.DISCONNECTED;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException(e);
    }
  }

  @Override
  public boolean isAcceptedWithEarlyMedia() {
    // TODO rayo doesn't support this yes.
    return false;
  }

  @Override
  public void acceptWithEarlyMedia() throws SignalException, MediaException {
    // TODO rayo doesn't support this yes.

  }

  @Override
  public void acceptWithEarlyMedia(Observer... observer) throws SignalException, MediaException {
    // TODO rayo doesn't support this yes.

  }

  @Override
  public void acceptWithEarlyMedia(Map<String, String> headers) throws SignalException, MediaException {
    // TODO rayo doesn't support this yes.
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
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isAccepted = true;
        _state = Call.State.ACCEPTED;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException(e);
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
    try {
      AnswerCommand command = new AnswerCommand();
      command.setHeaders(headers);
      command.setCallId(this.getId());
      IQ iq = _mohoRemote.getRayoClient().command(command, this.getId());

      if (iq.isError()) {
        com.rayo.client.xmpp.stanza.Error error = iq.getError();
        throw new SignalException(error.getCondition() + error.getText());
      }
      else {
        isAccepted = true;
        _state = Call.State.CONNECTED;
      }
    }
    catch (XmppException e) {
      LOG.error("", e);
      throw new SignalException(e);
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
}
