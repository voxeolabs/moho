package com.voxeo.moho.remote.network;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;

public interface RemoteCommunication extends Remote {

  void remoteJoin(String joinerRemoteAddress, String joineeRemoteAddress, JoinType joinType, byte[] sdp) throws Exception, RemoteException;;

  void remoteJoinAnswer(String joinerRemoteAddress, String joineeRemoteAddress, byte[] sdp) throws Exception,
      RemoteException;;

  void remoteJoinDone(String invokerRemoteAddress, String remoteAddress, Cause cause, Exception exception)
      throws RemoteException;

  void remoteUnjoin(String initiatorRemoteAddress, String remoteAddress) throws Exception, RemoteException;

}
