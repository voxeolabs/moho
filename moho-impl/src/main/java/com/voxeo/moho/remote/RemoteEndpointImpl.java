package com.voxeo.moho.remote;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.voxeo.moho.Participant;
import com.voxeo.moho.RemoteEndpoint;
import com.voxeo.moho.sip.RemoteParticipantImpl;
import com.voxeo.moho.spi.ExecutionContext;

public class RemoteEndpointImpl implements RemoteEndpoint {

  private static Pattern patter = Pattern.compile("remotejoin:(\\S+):(\\S+)///(\\S+)");

  protected ExecutionContext _ctx;

  protected String _address;

  protected URI _uri;

  // call id, or conference id, or dialog id.
  protected String _id;

  // call, conference, dialog
  protected String _type;

  protected RemoteJoinDriverImpl _joinDriver;

  public RemoteEndpointImpl(ExecutionContext ctx, String address, RemoteJoinDriverImpl joinDriver) {
    Matcher matcher = patter.matcher(address);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Illegal remote address:" + address);
    }
    _type = matcher.group(1);
    _id = matcher.group(2);

    _ctx = ctx;
    _address = address;
    _uri = URI.create(address);

    _joinDriver = joinDriver;
  }

  @Override
  public String getName() {
    return _address;
  }

  @Override
  public URI getURI() {
    return _uri;
  }

  @Override
  public Participant getParticipant() {
    return new RemoteParticipantImpl(this);
  }

  public ExecutionContext getApplicationContext() {
    return _ctx;
  }

  public String getAddress() {
    return _address;
  }

  public String getId() {
    return _id;
  }

  public String getType() {
    return _type;
  }

  public RemoteJoinDriverImpl getJoinDriver() {
    return _joinDriver;
  }
}
