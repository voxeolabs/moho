package com.voxeo.moho;

public interface InternalParticipant extends Participant {
  
  Unjoint unjoin(Participant other, boolean callPeerUnjoin);

}
