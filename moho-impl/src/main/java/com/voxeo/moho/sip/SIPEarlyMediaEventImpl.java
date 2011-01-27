package com.voxeo.moho.sip;

import java.util.Map;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventSource;

public class SIPEarlyMediaEventImpl extends SIPEarlyMediaEvent {

  protected SIPEarlyMediaEventImpl(EventSource source, SipServletResponse res) {
    super(source, res);
  }

  @Override
  public void reject(Reason reason) throws SignalException {
    reject(reason, null);
  }

  @Override
  public void reject(Reason reason, Map<String, String> headers) throws SignalException {
    this.checkState();
    this.setState(RejectableEventState.REJECTED);

    if (source instanceof SIPCallImpl) {
      SIPCallImpl call = (SIPCallImpl) source;

      try {
        call.doResponse(_res, headers);
      }
      catch (final Exception e) {
        throw new SignalException(e);
      }
    }
    // do the following in delegate
    // if join to media server, process as normal.

    // if bridge, didn't join the two network at this point

    // if direct, send the SDP this to the peer.
  }

  @Override
  public void accept(Map<String, String> headers) throws SignalException, IllegalStateException {
    this.checkState();
    this.setState(AcceptableEventState.ACCEPTED);
    // if join to media server, process as normal.

    // if bridge, join networks of two call.
    if (source instanceof SIPCallImpl) {
      SIPCallImpl call = (SIPCallImpl) source;

      try {
        call.doResponse(_res, headers);
      }
      catch (final Exception e) {
        throw new SignalException(e);
      }

      JoinDelegate delegate = call.getJoinDelegate();
      if (delegate instanceof Media2NOJoinDelegate) {
        try {
          if (call.getBridgeJoiningPeer() != null && call.getBridgeJoiningPeer().getMediaObject() == null) {
            call.getBridgeJoiningPeer().joinWithoutCheckOperation(Direction.DUPLEX);
          }
          if (call.getMediaObject() instanceof Joinable
              && call.getBridgeJoiningPeer().getMediaObject() instanceof Joinable) {
            ((Joinable) call.getMediaObject()).join(Direction.DUPLEX, (Joinable) call.getBridgeJoiningPeer()
                .getMediaObject());
          }

        }
        catch (final Exception e) {
          throw new SignalException(e);
        }
      }
    }

    // if direct, send the SDP this to the peer.
  }

}
