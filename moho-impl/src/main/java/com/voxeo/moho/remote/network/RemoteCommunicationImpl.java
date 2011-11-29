package com.voxeo.moho.remote.network;

import java.rmi.Naming;

import javax.media.mscontrol.join.Joinable.Direction;

import org.apache.log4j.Logger;

import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.ParticipantContainer;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.sip.DirectRemoteLocalJoinDelegate;
import com.voxeo.moho.sip.JoinDelegate;
import com.voxeo.moho.sip.RemoteLocalJoinDelegate;
import com.voxeo.moho.sip.RemoteParticipantImpl;
import com.voxeo.moho.sip.SIPCallImpl;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.ParticipantIDParser;

public class RemoteCommunicationImpl implements RemoteCommunication {
  private static final Logger LOG = Logger.getLogger(RemoteCommunicationImpl.class);

  protected ExecutionContext _context;

  public RemoteCommunicationImpl(ExecutionContext context) {
    super();
    _context = context;
  }

  public void join(String joinerRemoteAddress, String joineeRemoteAddress, JoinType joinType, byte[] sdp)
      throws Exception {
    LOG.debug("Starting remote join, joiner:" + joinerRemoteAddress + ", joinee:" + joineeRemoteAddress);
    String rmiAddress = getRmiAddress(joineeRemoteAddress);
    RemoteCommunication ske = (RemoteCommunication) Naming.lookup(rmiAddress);
    ske.remoteJoin(joinerRemoteAddress, joineeRemoteAddress, joinType, sdp);
  }

  public void joinAnswer(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    LOG.debug("Starting remote joinAnswer, joiner:" + joinerRemoteAddress + ", joinee:" + joineeRemoteAddress);
    String rmiAddress = getRmiAddress(joinerRemoteAddress);
    RemoteCommunication ske = (RemoteCommunication) Naming.lookup(rmiAddress);
    ske.remoteJoinAnswer(joinerRemoteAddress, joineeRemoteAddress, sdp);
  }

  public void joinDone(String invokerRemoteAddress, String remoteAddress, Cause cause, Exception exception) {
    LOG.debug("Starting remote joinDone, invoker:" + invokerRemoteAddress + ", remote:" + remoteAddress);

    String rmiAddress = getRmiAddress(remoteAddress);
    try {
      RemoteCommunication ske = null;
      ske = (RemoteCommunication) Naming.lookup(rmiAddress);
      ske.remoteJoinDone(invokerRemoteAddress, remoteAddress, cause, exception);
    }
    catch (Exception ex) {
      LOG.warn("", ex);
    }
  }

  public void unjoin(String invokerRemoteAddress, String remoteAddress) {
    LOG.debug("Starting remote unjoin, invoker:" + invokerRemoteAddress + ", remote:" + remoteAddress);

    String rmiAddress = getRmiAddress(remoteAddress);
    try {
      RemoteCommunication ske = null;
      ske = (RemoteCommunication) Naming.lookup(rmiAddress);
      ske.remoteUnjoin(invokerRemoteAddress, remoteAddress);
    }
    catch (Exception ex) {
      LOG.warn("", ex);
    }
  }

  @Override
  public void remoteJoin(String joinerRemoteAddress, String joineeRemoteAddress, JoinType joinType, byte[] sdp)
      throws Exception {
    LOG.debug("received remote join, joiner:" + joinerRemoteAddress + ", joinee:" + joineeRemoteAddress);

    Participant localParticipant = _context.getParticipant(joineeRemoteAddress);
    RemoteParticipantImpl remoteParticipant = (RemoteParticipantImpl) _context.getParticipant(joinerRemoteAddress);

    JoinDelegate joinDelegate = null;
    if (joinType != JoinType.DIRECT) {
      joinDelegate = new RemoteLocalJoinDelegate(remoteParticipant, localParticipant, Direction.DUPLEX, sdp);
    }
    else {
      joinDelegate = new DirectRemoteLocalJoinDelegate(remoteParticipant, (SIPCallImpl) localParticipant,
          Direction.DUPLEX, sdp);
    }

    joinDelegate.doJoin();
  }

  @Override
  public void remoteJoinAnswer(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception {
    LOG.debug("received remote joinAnswer, joiner:" + joinerRemoteAddress + ", joinee:" + joineeRemoteAddress);

    Participant localParticipant = _context.getParticipant(joinerRemoteAddress);
    if (localParticipant instanceof ParticipantContainer) {
      JoinDelegate joinDelegate = ((ParticipantContainer) localParticipant).getJoinDelegate(joineeRemoteAddress);

      joinDelegate.remoteJoinAnswer(sdp);
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + localParticipant);
    }
  }

  @Override
  public void remoteJoinDone(String invokerRemoteAddress, String remoteAddress, Cause cause, Exception exception) {
    LOG.debug("received remote joinDone, invoker:" + invokerRemoteAddress + ", remote:" + remoteAddress);

    Participant localParticipant = _context.getParticipant(remoteAddress);
    if (localParticipant instanceof ParticipantContainer) {
      JoinDelegate joinDelegate = ((ParticipantContainer) localParticipant).getJoinDelegate(invokerRemoteAddress);

      joinDelegate.done(cause, exception);
    }
    else {
      throw new IllegalArgumentException("Unsupported type:" + localParticipant);
    }
  }

  @Override
  public void remoteUnjoin(String initiatorRemoteAddress, String remoteAddress) throws Exception {
    LOG.debug("received remote unjoin, invoker:" + initiatorRemoteAddress + ", remote:" + remoteAddress);

    Participant localParticipant = _context.getParticipant(remoteAddress);

    if (localParticipant instanceof ParticipantContainer) {
      Participant[] participants = localParticipant.getParticipants();

      for (Participant participant : participants) {
        if (participant.getRemoteAddress().equalsIgnoreCase(initiatorRemoteAddress)) {
          RemoteParticipantImpl remote = (RemoteParticipantImpl) participant;
          remote.setRemoteInitiateUnjoin(true);
          ((ParticipantContainer) localParticipant).doUnjoin(remote, true);
        }
      }
    }

    else {
      throw new IllegalArgumentException("Unsupported type:" + localParticipant);
    }
  }

  protected String getRmiAddress(String encodedID) {
    String rawID = ParticipantIDParser.decode(encodedID);
    String[] parseResult = ParticipantIDParser.parseId(rawID);
    String rmiAddress = "rmi://" + parseResult[0] + ":" + parseResult[1] + "/RemoteCommunication";

    return rmiAddress;
  }
}
