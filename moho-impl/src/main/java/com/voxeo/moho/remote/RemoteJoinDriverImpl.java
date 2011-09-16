package com.voxeo.moho.remote;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.remote.network.RemoteCommunication;
import com.voxeo.moho.remote.network.RemoteCommunicationImpl;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.RemoteJoinDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.util.NetworkUtils;

public class RemoteJoinDriverImpl implements RemoteJoinDriver {
  // remote endpoint address format.
  // remotejoin:type:callid///rmi://ipaddress:port/RemoteCommunication"
  // the default port is 4231
  // type is call, conference, vxml

  private static final Logger LOG = Logger.getLogger(RemoteJoinDriverImpl.class);

  protected ExecutionContext _applicationContext;

  protected SpiFramework _spiFramework;

  protected Registry registry = null;

  protected RemoteCommunicationImpl _remoteCommunication;

  protected String _remoteCommunicationRMIAddress;

  @Override
  public void init(SpiFramework framework) {
    _spiFramework = framework;
    _applicationContext = (ExecutionContext) framework.getApplicationContext();

    _remoteCommunication = new RemoteCommunicationImpl(_applicationContext);

    try {
      //registry = LocateRegistry.getRegistry();
      //if (registry == null) {
        registry = LocateRegistry.createRegistry(4231);
      //}

      RemoteCommunication stub = (RemoteCommunication) UnicastRemoteObject.exportObject(_remoteCommunication, 0);

      registry.rebind("RemoteCommunication", stub);

      // TODO configure address
      _remoteCommunicationRMIAddress = "rmi://" + NetworkUtils.getLocalAddress() + ":4231" + "/RemoteCommunication";
    }
    catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws Exception {
    Registry registry = LocateRegistry.createRegistry(4231);
    RemoteCommunication stub = (RemoteCommunication) UnicastRemoteObject.exportObject(
        new RemoteCommunicationImpl(null), 0);

    registry.rebind("RemoteCommunication", stub);

  }

  @Override
  public String getProtocolFamily() {
    return PROTOCOL_REMOTEJOIN;
  }

  @Override
  public String[] getEndpointSchemas() {
    return RemoteJoinDriver.schemas;
  }

  @Override
  public SpiFramework getFramework() {
    return _spiFramework;
  }

  @Override
  public Endpoint createEndpoint(String addr) {
    return new RemoteEndpointImpl(_applicationContext, addr, this);
  }

  @Override
  public void destroy() {
    try {
      UnicastRemoteObject.unexportObject(_remoteCommunication, true);

      UnicastRemoteObject.unexportObject(registry, true);
    }
    catch (NoSuchObjectException e) {
      LOG.warn("", e);
    }
  }

  @Override
  public String getRemoteAddress(String type, String id) {
    // TODO ADDRESS
    String address = RemoteJoinDriver.schemas[0] + ":" + type + ":" + id + "///" + _remoteCommunicationRMIAddress;
    return address;
  }

  public RemoteCommunicationImpl getRemoteCommunication() {
    return _remoteCommunication;
  }
}
