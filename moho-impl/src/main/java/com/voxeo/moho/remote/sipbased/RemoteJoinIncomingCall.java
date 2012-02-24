package com.voxeo.moho.remote.sipbased;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.UnjointImpl;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.GenericMediaService;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.sip.RemoteParticipantImpl;
import com.voxeo.moho.sip.SIPIncomingCall;
import com.voxeo.moho.spi.ExecutionContext;

public class RemoteJoinIncomingCall extends SIPIncomingCall implements RemoteParticipant {

  private static final Logger LOG = Logger.getLogger(RemoteJoinIncomingCall.class);

  private Direction _x_Join_Direction;

  private JoinType _x_Join_Type;

  private boolean _x_Join_Force;

  private SipURI joiner;

  private SipURI joinee;

  public RemoteJoinIncomingCall(ExecutionContext context, SipServletRequest req) {
    super(context, req);
    _x_Join_Direction = Direction.valueOf(req.getHeader(Constants.x_Join_Direction));
    _x_Join_Type = JoinType.valueOf(req.getHeader(Constants.x_Join_Type));
    _x_Join_Force = Boolean.valueOf(req.getHeader(Constants.x_Join_Force));
    joiner = (SipURI) req.getFrom().getURI();
    joinee = (SipURI) req.getTo().getURI();
  }

  public void setCallID(String id) {
    _id = id;
  }

  public Direction getX_Join_Direction() {
    return _x_Join_Direction;
  }

  public void setX_Join_Direction(Direction x_Join_Direction) {
    _x_Join_Direction = x_Join_Direction;
  }

  public JoinType getX_Join_Type() {
    return _x_Join_Type;
  }

  public void setX_Join_Type(JoinType x_Join_Type) {
    _x_Join_Type = x_Join_Type;
  }

  public boolean getX_Join_Force() {
    return _x_Join_Force;
  }

  public void setX_Join_Force(boolean x_Join_Force) {
    _x_Join_Force = x_Join_Force;
  }

  public SipURI getJoiner() {
    return joiner;
  }

  public SipURI getJoinee() {
    return joinee;
  }

  @Override
  public Unjoint unjoin(final Participant other, final boolean isInitiator) {
    Unjoint task = new UnjointImpl(_context.getExecutor(), new Callable<UnjoinCompleteEvent>() {
      @Override
      public UnjoinCompleteEvent call() throws Exception {
        UnjoinCompleteEvent event = doUnjoin(other, isInitiator);
        RemoteJoinIncomingCall.this.disconnect();
        return event;
      }
    });

    return task;
  }

  protected synchronized void terminate(final CallCompleteEvent.Cause cause, final Exception exception,
      final Map<String, String> headers) {
    _context.removeCall(getId());

    if (_service != null) {
      ((GenericMediaService) _service)
          .release((cause == CallCompleteEvent.Cause.DISCONNECT || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) ? true
              : false);
      _service = null;
    }

    destroyNetworkConnection();

    Participant[] _joineesArray = _joinees.getJoinees();
    for (Participant participant : _joineesArray) {
      UnjoinCompleteEvent.Cause unjoinCause = UnjoinCompleteEvent.Cause.ERROR;
      if (cause == CallCompleteEvent.Cause.DISCONNECT || cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) {
        unjoinCause = UnjoinCompleteEvent.Cause.DISCONNECT;
      }

      dispatch(new MohoUnjoinCompleteEvent(this, participant, unjoinCause, exception, true));

      if (participant instanceof ParticipantContainer) {
        try {
          ((ParticipantContainer) participant).doUnjoin(this, false);
        }
        catch (Exception e) {
          LOG.error("", e);
        }
      }
    }
    _joinees.clear();

    synchronized (_peers) {
      // for (final Call peer : _peers) {
      // try {
      // peer.disconnect();
      // }
      // catch (final Throwable t) {
      // LOG.warn("", t);
      // }
      // }
      _peers.clear();
    }

    // TODO
    if (_joinDelegate != null) {
      if (cause == CallCompleteEvent.Cause.NEAR_END_DISCONNECT) {
        _joinDelegate.done(JoinCompleteEvent.Cause.DISCONNECTED, exception);
      }
      else {
        _joinDelegate.done(JoinCompleteEvent.Cause.ERROR, exception);
      }
    }

    this.dispatch(new MohoCallCompleteEvent(this, cause, exception, headers));

    _callDelegate = null;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(this.getClass().getSimpleName()).append("[").append(_signal).append(",")
        .append(_id).append(",").append(_cstate).append(", ").append(_x_Join_Type).append(",").append(_x_Join_Force)
        .append(",").append(_x_Join_Direction).append("]").toString();
  }

  @Override
  public String getRemoteParticipantID() {
    return joiner.getUser();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((joiner.getUser() == null) ? 0 : joiner.getUser().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass() && !(obj instanceof RemoteParticipant))
      return false;
    if (obj instanceof RemoteParticipantImpl) {
      RemoteParticipantImpl other = (RemoteParticipantImpl) obj;
      if (this.getJoiner().getUser().equalsIgnoreCase(other.getId())) {
        return true;
      }
      else {
        return false;
      }
    }
    else {
      RemoteJoinIncomingCall other = (RemoteJoinIncomingCall) obj;
      if (_id == null) {
        if (other.getId() != null)
          return false;
      }
      else if (!_id.equals(other.getId()))
        return false;
      return true;
    }
  }
}
