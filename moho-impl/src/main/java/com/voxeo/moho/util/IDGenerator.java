package com.voxeo.moho.util;

import java.util.UUID;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;

public class IDGenerator {

	public static String generateId(ExecutionContext context) {

		if (context != null) {
			String uid = String.valueOf(new com.eaio.uuid.UUID().getTime());
			String rawid = ((ApplicationContextImpl)context).generateID(
							RemoteParticipant.RemoteParticipant_TYPE_CALL, uid);
			
			return ParticipantIDParser.encode(rawid);
		} else {
			return UUID.randomUUID().toString();
		}
	}
}
