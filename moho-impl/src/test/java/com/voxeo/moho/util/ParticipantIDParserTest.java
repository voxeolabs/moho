package com.voxeo.moho.util;

import java.math.BigInteger;

import org.junit.Test;
import static org.junit.Assert.*;

import com.eaio.uuid.UUID;
import com.voxeo.moho.remotejoin.RemoteParticipant;

public class ParticipantIDParserTest {

	@Test
	public void testEncodeIPs() {
		
		assertEquals(ParticipantIDParser.ipToNormalizedLongString("127.0.0.1"), "127000000001");
		assertEquals(ParticipantIDParser.ipToNormalizedLongString("12.0.20.1"), "012000020001");
		assertEquals(ParticipantIDParser.ipToNormalizedLongString("1.0.0.1"), "001000000001");
		assertEquals(ParticipantIDParser.ipToNormalizedLongString("127.120.202.221"), "127120202221");
		assertEquals(ParticipantIDParser.ipToNormalizedLongString("12.120.22.221"), "012120022221");
	}
	
	@Test
	public void testEncodePorts() {
		
		assertEquals(ParticipantIDParser.portToNormalizedLongString("20"), "00020");
		assertEquals(ParticipantIDParser.portToNormalizedLongString("200"), "00200");
		assertEquals(ParticipantIDParser.portToNormalizedLongString("2000"), "02000");
		assertEquals(ParticipantIDParser.portToNormalizedLongString("20000"), "20000");
	}

	@Test
	public void testEncodeType() {
		
		assertEquals(ParticipantIDParser.getNumericType(
				RemoteParticipant.RemoteParticipant_TYPE_CALL), ParticipantIDParser.TYPE_CALL);
		assertEquals(ParticipantIDParser.getNumericType(
				RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE), ParticipantIDParser.TYPE_CONFERENCE);
		assertEquals(ParticipantIDParser.getNumericType(
				RemoteParticipant.RemoteParticipant_TYPE_DIALOG), ParticipantIDParser.TYPE_DIALOG);
	}
	
	@Test
	public void testShorten() {
		
		assertEquals(new BigInteger("1"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("1"))));
		assertEquals(new BigInteger("23"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("23"))));
		assertEquals(new BigInteger("234"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("234"))));
		assertEquals(new BigInteger("45234"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("45234"))));
		assertEquals(new BigInteger("98045234"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("98045234"))));
		assertEquals(new BigInteger("25698045234"), ParticipantIDParser.unshort(ParticipantIDParser.shorten(new BigInteger("25698045234"))));		
	}
	
	@Test
	public void testEncode() {
		
		String raw = "moho://127.0.0.1:8080/call/" + Math.abs(new UUID().getTime());
		assertEquals(raw, ParticipantIDParser.decode(ParticipantIDParser.encode(raw)));
		
		raw = "moho://34.67.128.98:80/call/" + Math.abs(new UUID().getTime());
		assertEquals(raw, ParticipantIDParser.decode(ParticipantIDParser.encode(raw)));

		raw = "moho://10.0.0.2:23490/call/" + Math.abs(new UUID().getTime());
		assertEquals(raw, ParticipantIDParser.decode(ParticipantIDParser.encode(raw)));
	}
}
