package com.voxeo.moho.xmpp;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Framework;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.spi.XMPPDriver;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;

public class XMPPDriverImpl implements XMPPDriver {

  private static final Logger LOG = Logger.getLogger(XMPPDriverImpl.class);

  protected SpiFramework _app = null;

  protected XmppFactory _xmppFactory;

  protected XmppServlet _servlet;

  @Override
  public void init(SpiFramework framework) {
    _app = framework;
    _servlet = framework.getXMPPController();
    _xmppFactory = (XmppFactory) _servlet.getServletContext().getAttribute(XmppServlet.XMPP_FACTORY);
  }

  @Override
  public String getProtocolFamily() {
    return PROTOCOL_XMPP;
  }

  @Override
  public String[] getEndpointSchemas() {
    return new String[]{};
  }

  @Override
  public SpiFramework getFramework() {
    return _app;
  }

  @Override
  public Endpoint createEndpoint(String addr) {
    return null;
  }

  @Override
  public void destroy() {

  }

  @Override
  public void doMessage(InstantMessage req) throws ServletException, IOException {
    XMPPMessageEvent event = new XMPPMessageEventImpl(getFramework(), req);
    _app.dispatch(event);
  }

  @Override
  public void doPresence(PresenceMessage req) throws ServletException, IOException {
    XMPPPresenceEvent presence = new XMPPPresenceEventImpl(getFramework(), req);
    _app.dispatch(presence);
  }

  @Override
  public void doIQRequest(IQRequest req) throws ServletException, IOException {
    XMPPIQEvent iqEvent = null;
    if (req.getElement("query", "jabber:iq:roster") != null) {
      if (IQRequest.TYPE_GET.equalsIgnoreCase(req.getType())) {
        iqEvent = new RosterGetEventImpl(_app, req);
      }
      else if (IQRequest.TYPE_SET.equalsIgnoreCase(req.getType())) {
        iqEvent = new RosterSetEventImpl(_app, req);
      }
    }
    else {
      iqEvent = new XMPPIQEventImpl(getFramework(), req);
    }
    _app.dispatch(iqEvent, new NoHandleHandler<Framework>(iqEvent, req));
  }

  private class NoHandleHandler<T extends EventSource> implements Runnable {

    private XMPPEvent<T> _event;

    private IQRequest _req;

    private boolean _invalidate = false;

    public NoHandleHandler(final XMPPEvent<T> event, final IQRequest req) {
      this(event, req, false);
    }

    public NoHandleHandler(final XMPPEvent<T> event, final IQRequest req, final boolean invalidate) {
      _event = event;
      _req = req;
    }

    public void run() {
      if (!_event.isProcessed()) {
        try {
          _req.createError(Type.CANCEL, Condition.FEATURE_NOT_IMPLEMENTED, "Not handled by application").send();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
      if (_invalidate) {
        try {
          _req.getApplicationSession(false).invalidate();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
    }
  }

  @Override
  public void doIQResponse(IQResponse resp) throws ServletException, IOException {
//    EventSource source = XMPPSessionUtils.getEventSource(resp);
//    source.dispatch(null);
  }

}
