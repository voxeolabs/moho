package com.voxeo.moho.remote.network;

import java.rmi.Naming;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.RemoteEndpoint;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.remote.RemoteParticipant;
import com.voxeo.moho.sip.LocalRemoteJoinDelegate;
import com.voxeo.moho.sip.RemoteLocalJoinDelegate;
import com.voxeo.moho.sip.RemoteParticipantImpl;
import com.voxeo.moho.sip.SIPCallImpl;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.RemoteJoinDriver;

public class RemoteCommunicationImpl implements RemoteCommunication {
  private static final Logger LOG = Logger.getLogger(RemoteCommunicationImpl.class);

  private static Pattern patter = Pattern.compile("remotejoin:(\\S+):(\\S+)///(\\S+)");

  // TODO need timer to deal with network problem?
  Map<String, RemoteParticipantImpl> remoteCommunications = new ConcurrentHashMap<String, RemoteParticipantImpl>();

  // TODO when clear data?
  Map<String, RemoteCommunication> remoteCommunic = new ConcurrentHashMap<String, RemoteCommunication>();

  protected ExecutionContext _context;

  public RemoteCommunicationImpl(ExecutionContext _context) {
    super();
    this._context = _context;
  }

  public void join(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    Matcher matcher = patter.matcher(joineeRemoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + joineeRemoteAddress);
    }

    String rmiAddress = matcher.group(3);
    RemoteCommunication ske = null;

    synchronized (rmiAddress.intern()) {
      ske = remoteCommunic.get(rmiAddress);
      if (ske == null) {
        ske = (RemoteCommunication) Naming.lookup(rmiAddress);
        remoteCommunic.put(rmiAddress, ske);
      }
    }

    ske.remoteJoin(joinerRemoteAddress, joineeRemoteAddress, sdp);
  }

  public void joinAnswer(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    Matcher matcher = patter.matcher(joinerRemoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + joinerRemoteAddress);
    }

    String rmiAddress = matcher.group(3);
    RemoteCommunication ske = null;

    synchronized (rmiAddress.intern()) {
      ske = remoteCommunic.get(rmiAddress);
      if (ske == null) {
        ske = (RemoteCommunication) Naming.lookup(rmiAddress);
        remoteCommunic.put(rmiAddress, ske);
      }
    }

    ske.remoteJoinAnswer(joinerRemoteAddress, joineeRemoteAddress, sdp);
  }

  public void joinDone(String invokerRemoteAddress, String remoteAddress, Cause cause, Exception exception) {
    try {
      Matcher matcher = patter.matcher(remoteAddress);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Illegal remote address:" + remoteAddress);
      }

      String rmiAddress = matcher.group(3);
      RemoteCommunication ske = null;

      synchronized (rmiAddress.intern()) {
        ske = remoteCommunic.get(rmiAddress);
        if (ske == null) {
          ske = (RemoteCommunication) Naming.lookup(rmiAddress);
          remoteCommunic.put(rmiAddress, ske);
        }
      }

      ske.remoteJoinDone(invokerRemoteAddress, remoteAddress, cause, exception);
    }
    catch (Exception ex) {
      LOG.warn("", ex);
    }
  }

  public void unjoin(String invokerRemoteAddress, String remoteAddress) {
    try {
      Matcher matcher = patter.matcher(remoteAddress);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Illegal remote address:" + remoteAddress);
      }

      String rmiAddress = matcher.group(3);
      RemoteCommunication ske = null;

      synchronized (rmiAddress.intern()) {
        ske = remoteCommunic.get(rmiAddress);
        if (ske == null) {
          ske = (RemoteCommunication) Naming.lookup(rmiAddress);
          remoteCommunic.put(rmiAddress, ske);
        }
      }

      ske.remoteUnjoin(invokerRemoteAddress, remoteAddress);
    }
    catch (Exception ex) {
      LOG.warn("", ex);
    }
  }

  @Override
  public void remoteJoin(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    Matcher matcher = patter.matcher(joineeRemoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + joineeRemoteAddress);
    }

    String type = matcher.group(1);
    String id = matcher.group(2);

    if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CALL)) {
      SIPCallImpl call = (SIPCallImpl) _context.getCall(id);
      RemoteEndpoint remoteEnd = (RemoteEndpoint) _context.getFramework()
          .getDriverByProtocolFamily(RemoteJoinDriver.PROTOCOL_REMOTEJOIN).createEndpoint(joinerRemoteAddress);
      RemoteParticipantImpl remoteParticipant = (RemoteParticipantImpl) remoteEnd.getParticipant();

      RemoteLocalJoinDelegate joinDelegate = new RemoteLocalJoinDelegate(remoteParticipant, call, Direction.DUPLEX, sdp);

      joinDelegate.doJoin();
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE)) {
      Conference conference = _context.getConferenceManager().getConference(id);
      RemoteEndpoint remoteEnd = (RemoteEndpoint) _context.getFramework()
          .getDriverByProtocolFamily(RemoteJoinDriver.PROTOCOL_REMOTEJOIN).createEndpoint(joinerRemoteAddress);
      RemoteParticipantImpl remoteParticipant = (RemoteParticipantImpl) remoteEnd.getParticipant();

      RemoteLocalJoinDelegate joinDelegate = new RemoteLocalJoinDelegate(remoteParticipant, conference,
          Direction.DUPLEX, sdp);

      joinDelegate.doJoin();
    }

    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_DIALOG)) {
      // TODO
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + type);
    }

  }

  @Override
  public void remoteJoinAnswer(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    Matcher matcher = patter.matcher(joinerRemoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + joineeRemoteAddress);
    }

    String type = matcher.group(1);
    String id = matcher.group(2);

    if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CALL)) {
      SIPCallImpl call = (SIPCallImpl) _context.getCall(id);

      ((LocalRemoteJoinDelegate) call.getJoinDelegate()).remoteJoinAnswer(sdp);
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE)) {
      Conference conference = _context.getConferenceManager().getConference(id);

      LocalRemoteJoinDelegate joinDelegate = (LocalRemoteJoinDelegate) ((ParticipantContainer) conference)
          .getJoinDelegate(joineeRemoteAddress);

      joinDelegate.remoteJoinAnswer(sdp);
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_DIALOG)) {
      // TODO
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + type);
    }
  }

  @Override
  public void remoteJoinDone(String invokerRemoteAddress, String remoteAddress, Cause cause, Exception exception) {
    Matcher matcher = patter.matcher(remoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + remoteAddress);
    }

    String type = matcher.group(1);
    String id = matcher.group(2);

    if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CALL)) {
      SIPCallImpl call = (SIPCallImpl) _context.getCall(id);

      ((RemoteLocalJoinDelegate) call.getJoinDelegate()).done(cause, exception);
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE)) {
      Conference conference = _context.getConferenceManager().getConference(id);

      LocalRemoteJoinDelegate joinDelegate = (LocalRemoteJoinDelegate) ((ParticipantContainer) conference)
          .getJoinDelegate(invokerRemoteAddress);

      joinDelegate.done(cause, exception);
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_DIALOG)) {
      // TODO
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + type);
    }
  }

  @Override
  public void remoteUnjoin(String initiatorRemoteAddress, String remoteAddress) throws Exception {
    Matcher matcher = patter.matcher(remoteAddress);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + remoteAddress);
    }

    String type = matcher.group(1);
    String id = matcher.group(2);

    if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CALL)) {
      SIPCallImpl call = (SIPCallImpl) _context.getCall(id);
      Participant[] participants = call.getParticipants();

      for (Participant participant : participants) {
        if (participant.getRemoteAddress().equalsIgnoreCase(initiatorRemoteAddress)) {
          RemoteParticipantImpl remote = (RemoteParticipantImpl) participant;
          remote.setRemoteInitiateUnjoin(true);
          ((ParticipantContainer) participant).doUnjoin(call, true);
        }
      }
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE)) {
      Conference conference = _context.getConferenceManager().getConference(id);

      Participant[] participants = conference.getParticipants();

      for (Participant participant : participants) {
        if (participant.getRemoteAddress().equalsIgnoreCase(initiatorRemoteAddress)) {
          RemoteParticipantImpl remote = (RemoteParticipantImpl) participant;
          remote.setRemoteInitiateUnjoin(true);
          ((ParticipantContainer) participant).doUnjoin(conference, true);
        }
      }
    }
    else if (type.equalsIgnoreCase(RemoteParticipant.RemoteParticipant_TYPE_DIALOG)) {
      // TODO
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + type);
    }
  }
}
