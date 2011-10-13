package com.voxeo.moho.util;

import java.util.UUID;

import com.voxeo.moho.remote.RemoteParticipant;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.RemoteJoinDriver;

public class IDGenerator {

	public static String generateId(ExecutionContext context) {

		if (context != null) {
			String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
			String rawid = ((RemoteJoinDriver) context
					.getFramework()
					.getDriverByProtocolFamily(RemoteJoinDriver.PROTOCOL_REMOTEJOIN))
					.getRemoteAddress(
							RemoteParticipant.RemoteParticipant_TYPE_CALL, uid);
			
			return ParticipantIDParser.encode(rawid);
		} else {
			return UUID.randomUUID().toString();
		}
	}
}
