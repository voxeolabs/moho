package com.voxeo.moho.xmpp;

import java.io.IOException;

import org.w3c.dom.Element;

import com.voxeo.moho.Framework;
import com.voxeo.moho.SignalException;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;

public class XMPPIQEventImpl extends XMPPEventImpl implements XMPPIQEvent {
  
  protected boolean _rejected = false;

  protected boolean _accepted = false;
  
  public XMPPIQEventImpl(Framework framework, IQRequest request) {
    super(framework, request);
  }

  @Override
  public synchronized boolean isAccepted() {
    return _accepted;
  }

  @Override
  public synchronized boolean isRejected() {
    return _rejected;
  }

  @Override
  public void accept() throws SignalException {
    accept(null);
  }

  @Override
  public synchronized void reject(Reason reason, String text) {
    _rejected = true;
    IQRequest request = (IQRequest) _request;
    try {
      IQResponse reponse = request.createError(Reason.BAD_REQUEST.getErrorType(), Reason.BAD_REQUEST.getCondition(), text);
      reponse.send();
    }
    catch (IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public void reject(Reason reason) {
    reject(reason, null);
  }

  @Override
  public synchronized void accept(Element... elem) {
    _accepted = true;
    IQRequest request = (IQRequest) _request;
    try {
      IQResponse reponse = request.createResult(elem);
      reponse.send();
    }
    catch (IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public boolean isProcessed() {
    return isAccepted() || isRejected();
  }
}
