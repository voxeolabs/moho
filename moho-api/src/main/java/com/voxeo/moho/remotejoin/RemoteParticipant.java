package com.voxeo.moho.remotejoin;

import com.voxeo.moho.Participant;

public interface RemoteParticipant extends Participant {

  public final String RemoteParticipant_TYPE_CALL = "call";

  public final String RemoteParticipant_TYPE_CONFERENCE = "conference";

  public final String RemoteParticipant_TYPE_DIALOG = "dialog";

  public String getRemoteParticipantID();

}
