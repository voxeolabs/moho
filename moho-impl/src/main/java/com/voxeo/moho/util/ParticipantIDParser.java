package com.voxeo.moho.util;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.voxeo.moho.remotejoin.RemoteParticipant;

public class ParticipantIDParser {

	protected static long TYPE_CALL = 1L;
	protected static long TYPE_CONFERENCE = 2L;
	protected static long TYPE_DIALOG = 3L;
	
	// format moho://ip:port/<type>/<callid>
	public static Pattern pattern = Pattern.compile("moho://(\\S+):(\\S+)/(\\S+)/(\\S+)");
	
	// We use only lowercased letters and numbers as other protocols may need normalized ids so 
	// using this alphabet makes things much simpler
	static String alphabet = "abcdefghijklmnopqrstuvwxyz1234567890";
	
	
	public static String encode(String raw) {

		String[] parts = parseId(raw);
		return shorten(toBigInteger(parts[0])) + "-" + 
			   shorten(toBigInteger(parts[1], parts[2])) + "-" + 
			   shorten(new BigInteger(parts[3]));
	}

	private static BigInteger toBigInteger(String ip) {

		StringBuilder builder = new StringBuilder();
		builder.append(ipToNormalizedLongString(ip));
		
		return new BigInteger(builder.toString());
	}

	private static BigInteger toBigInteger(String port, String type) {

		StringBuilder builder = new StringBuilder();
		builder.append(portToNormalizedLongString(port));
		builder.append(getNumericType(type));
		
		return new BigInteger(builder.toString());
	}	
	
	protected static Object portToNormalizedLongString(String port) {

		return String.format("%05d", Long.parseLong(port));
	}

	protected static String ipToNormalizedLongString(String ip) {
		
		StringBuilder builder = new StringBuilder();
		int i = 0,j = 0;
		while ((j=ip.indexOf('.', i)) != -1) {
			builder.append(String.format("%03d",Integer.parseInt(ip.substring(i,j))));
			i = j+1;
		}
		builder.append(String.format("%03d",Integer.parseInt(ip.substring(i, ip.length()))));
		return builder.toString();
	}

	public static String decode(String encoded) {

		String[] parts = StringUtils.split(encoded, "-");
		BigInteger decodedIp = unshort(parts[0]);
		String ipAddress = toIpAddress(decodedIp.longValue());
		
		String portAndType = String.valueOf(unshort(parts[1]));	
		String type = toRemoteType(portAndType.charAt(portAndType.length()-1));
		String port = String.valueOf(Long.valueOf(portAndType.substring(0, portAndType.length()-1)));
		String timestamp = unshort(parts[2]).toString();
		
		return String.format("moho://%s:%s/%s/%s", ipAddress, port, type, timestamp);
	}

	protected static String toIpAddress(long encodedIp) {

		StringBuilder builder = new StringBuilder();
		String str = String.format("%012d", encodedIp);

		int i=0;
		while (i<str.length()) {
			builder.append(Long.valueOf(str.substring(i,i+3)) + ".");
			i+=3;
		}
		builder.deleteCharAt(builder.length()-1);
		return builder.toString();
	}

	public static String[] parseId(String raw) {
		
		// ip, port, type, id
		Matcher matcher = pattern.matcher(raw);
		if (matcher.matches()) {
			return new String[] { matcher.group(1), matcher.group(2),
					matcher.group(3), matcher.group(4) };
		}
		throw new IllegalArgumentException("Illegal ID format:" + raw);
	}

	public static String getIpAddress(String encoded) {

		String decoded = decode(encoded);
		if (decoded != null) {
			String[] parts = parseId(decoded);
			if (parts != null && parts.length > 0) {
				return parts[0];
			}
		}
		return null;
	}

	public static String[] parseEncodedId(String encodedId) {
		String raw = ParticipantIDParser.decode(encodedId);
		return ParticipantIDParser.parseId(raw);
	}
	
	protected static long getNumericType(String raw) {
		
		if (raw.contains(RemoteParticipant.RemoteParticipant_TYPE_CALL)) {
			return TYPE_CALL;
		} else if (raw.contains(RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE)) {
			return TYPE_CONFERENCE;
		} else if (raw.contains(RemoteParticipant.RemoteParticipant_TYPE_DIALOG)) {
			return TYPE_DIALOG;
		}
		return 0;
	}
	
	protected static String toRemoteType(char type) {
		
		long value = Long.parseLong("" + type);
		if (value == TYPE_CALL) {
			return RemoteParticipant.RemoteParticipant_TYPE_CALL;
		} else if (value == TYPE_CONFERENCE) {
			return RemoteParticipant.RemoteParticipant_TYPE_CONFERENCE;
		} else if (value == TYPE_DIALOG) {
			return RemoteParticipant.RemoteParticipant_TYPE_DIALOG;
		}
		return null;
	}

	protected static String shorten(BigInteger number) {
		
		StringBuilder builder = new StringBuilder();
		int url;
		do {
			url = number.mod(new BigInteger("36")).intValue(); 
			number = number.divide(new BigInteger("36"));
			builder.append(alphabet.charAt(url));
		} while (number.compareTo(BigInteger.ZERO) > 0); 

		return builder.toString();
	}
	
	protected static BigInteger unshort(String shorted) {
		
		BigInteger total = new BigInteger("0");
		//shorted = StringUtils.reverse(shorted);
		for (int i=0;i<shorted.length();i++) {
			long j = (long)(Math.pow(36, i) * alphabet.indexOf(shorted.charAt(i)));
			total = total.add(new BigInteger(String.valueOf(j)));
		}
		return total;
	}
	
	public static void main(String[] args) {
		
		System.out.println(ipToNormalizedLongString("127.0.0.1"));
		System.out.println(ipToNormalizedLongString("12.0.20.1"));
		System.out.println(ipToNormalizedLongString("1.0.0.1"));
		System.out.println(ipToNormalizedLongString("127.120.202.221"));
		System.out.println(ipToNormalizedLongString("12.120.22.221"));
	}
}
