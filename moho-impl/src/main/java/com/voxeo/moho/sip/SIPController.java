/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.media.mscontrol.spi.PropertyInfo;
import javax.sdp.SdpFactory;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.conference.ConferenceMangerImpl;
import com.voxeo.moho.event.ApplicationEventSource;
import com.voxeo.moho.event.DtmfRelayEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.media.GenericMediaServiceFactory;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.util.SessionUtils;

public class SIPController extends SipServlet {

  private static final long serialVersionUID = -6039446683694940149L;

  private static final Logger log = Logger.getLogger(SIPController.class);

  protected ApplicationEventSource _app = null;

  protected SipFactory _sipFacory;

  protected SdpFactory _sdpFactory;

  protected MsControlFactory _mscFactory;

  protected String _applicationClass = null;

  @SuppressWarnings("unchecked")
  @Override
  public void init() {
    try {
      _applicationClass = getInitParameter("ApplicationClass");
      if (_applicationClass == null) {
        throw new IllegalArgumentException("Cannot found moho application class.");
      }
      log.info("Moho using applicationClass:" + _applicationClass);
      final Application app = createApplicationInstance();

      _sipFacory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
      _sdpFactory = (SdpFactory) getServletContext().getAttribute("javax.servlet.sdp.SdpFactory");

      if (_sdpFactory == null) {
        log.warn("Unable to get SdpFactory, some function, such as call hold unhold mute unmute, is unavailable:");
      }

      // msctrl.min.threadpool , msctrl.max.threadpool used to set thread pool
      // size of 309
      final Properties p = new Properties();
      final Driver driver = DriverManager.getDrivers().next();
      if (driver.getFactoryPropertyInfo() != null) {
        for (final PropertyInfo info : driver.getFactoryPropertyInfo()) {
          String value = getInitParameter(info.name);
          if (value == null) {
            value = info.defaultValue;
          }
          if (value != null) {
            log.info("Moho using property for msFactory:" + info.name + ":" + value);
            p.setProperty(info.name, value);
          }
        }
      }

      _mscFactory = driver.getFactory(p);

      int eventDispatcherThreadPoolSize = 50;
      final String eventDipatcherThreadPoolSizePara = getInitParameter("eventDispatcherThreadPoolSize");
      if (eventDipatcherThreadPoolSizePara != null) {
        eventDispatcherThreadPoolSize = Integer.valueOf(eventDipatcherThreadPoolSizePara);
      }
      log.info("Moho using eventDipatcherThreadPoolSize:" + eventDispatcherThreadPoolSize);

      Class<? extends MediaDialect> mediaDialectClass = com.voxeo.moho.media.dialect.GenericDialect.class;
      final String mediaDialectClassName = getInitParameter("mediaDialectClass");
      if (mediaDialectClassName != null) {
        mediaDialectClass = (Class<? extends MediaDialect>) Class.forName(mediaDialectClassName);
      }
      final MediaDialect mediaDialect = mediaDialectClass.newInstance();

      final ApplicationContextImpl ctx = new ApplicationContextImpl(app, _mscFactory, _sipFacory, _sdpFactory,
          getServletConfig().getServletName(), this.getServletContext(), eventDispatcherThreadPoolSize);

      ctx.setMediaServiceFactory(new GenericMediaServiceFactory(mediaDialect));
      ctx.setConferenceManager(new ConferenceMangerImpl(ctx));

      final Enumeration<?> e = getInitParameterNames();
      while (e.hasMoreElements()) {
        final String name = (String) e.nextElement();
        final String value = getInitParameter(name);
        ctx.setParameter(name, value);
      }

      _app = new ApplicationEventSource(ctx, app);
      app.init(ctx);
      getServletContext().setAttribute(ApplicationContext.APPLICATION, app);
      getServletContext().setAttribute(ApplicationContext.APPLICATION_CONTEXT, ctx);
    }
    catch (final Throwable t) {
      log.error("Unable to initialize Moho:", t);
      throw new RuntimeException(t);
    }
  }

  @SuppressWarnings("rawtypes")
  private Application createApplicationInstance() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      Class clz = null;
      try {
        clz = this.getClass().getClassLoader().loadClass(_applicationClass);
      }
      catch (final Throwable t) {
        clz = Thread.currentThread().getContextClassLoader().loadClass(_applicationClass);
      }
      final Application app = (Application) clz.newInstance();
      return app;
  }

  @Override
  public void destroy() {
    try {
      ((ApplicationContextImpl) _app.getApplicationContext()).destroy();
    }
    catch (final Throwable t) {
      log.error("Unable to dispose Moho:", t);
      throw new RuntimeException(t);
    }
  }

  @Override
  protected void doRequest(final SipServletRequest req) throws ServletException, IOException {
    final String s = req.getMethod();
    if ("INVITE".equals(s)) {
      doInvite(req);
    }
    else if ("ACK".equals(s)) {
      doAck(req);
    }
    else if ("OPTIONS".equals(s)) {
      doOptions(req);
    }
    else if ("BYE".equals(s)) {
      doBye(req);
    }
    else if ("CANCEL".equals(s)) {
      doCancel(req);
    }
    else if ("REGISTER".equals(s)) {
      doRegister(req);
    }
    else if ("SUBSCRIBE".equals(s)) {
      doSubscribe(req);
    }
    else if ("NOTIFY".equals(s)) {
      doNotify(req);
    }
    else if ("MESSAGE".equals(s)) {
      doMessage(req);
    }
    else if ("INFO".equals(s)) {
      doInfo(req);
    }
    else if ("UPDATE".equals(s)) {
      doUpdate(req);
    }
    else if ("REFER".equals(s)) {
      doRefer(req);
    }
    else if ("PUBLISH".equals(s)) {
      doPublish(req);
    }
    else if ("PRACK".equals(s)) {
      doPrack(req);
    }
    else {
      doOthers(req);
    }
  }

  @Override
  protected void doInvite(final SipServletRequest req) throws ServletException, IOException {
    if (req.isInitial()) {
      final SIPCall ev = new SIPIncomingCall((ExecutionContext) _app.getApplicationContext(), req);
      _app.dispatch(ev);
    }
    else {
      final EventSource source = SessionUtils.getEventSource(req);
      if (source != null) {
        source.dispatch(new SIPReInviteEventImpl(source, req));
      }
    }
  }

  @Override
  protected void doBye(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null) {
      source.dispatch(new SIPDisconnectEventImpl(source, req));
    }
  }

  @Override
  protected void doAck(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;
      try {
        call.doAck(req);
      }
      catch (final Exception e) {
        log.warn("", e);
      }
    }
  }

  @Override
  protected void doPrack(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPIncomingCall) {
      final SIPIncomingCall call = (SIPIncomingCall) source;
      try {
        call.doPrack(req);
      }
      catch (final Exception e) {
        log.warn("", e);
      }
    }
  }

  @Override
  protected void doCancel(final SipServletRequest req) throws ServletException, IOException {
    // final EventSource source = SessionUtils.getEventSource(req);
    // if (source instanceof SIPIncomingCall) {
    // final SIPIncomingCall call = (SIPIncomingCall) source;
    // try {
    // call.doCancel();
    // }
    // catch (final Exception e) {
    // log.warn("", e);
    // }
    // }

    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null) {
      source.dispatch(new SIPDisconnectEventImpl(source, req));
    }
  }

  @Override
  protected void doRefer(final SipServletRequest req) throws ServletException, IOException {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      final SignalEvent event = new SIPReferEventImpl(source, req);
      source.dispatch(event);
    }
    else {
      final SignalEvent event = new SIPReferEventImpl(_app, req);
      _app.dispatch(event, new NoHandleHandler(event, req));
    }
  }

  @Override
  protected void doNotify(final SipServletRequest req) throws ServletException, IOException {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      final SignalEvent event = new SIPNotifyEventImpl(source, req);
      source.dispatch(event);
    }
    else {
      final SignalEvent event = new SIPNotifyEventImpl(_app, req);
      _app.dispatch(event, new NoHandleHandler(event, req));
    }
  }

  @Override
  protected void doMessage(final SipServletRequest req) throws ServletException, IOException {
    req.createResponse(200).send();
    final String type = req.getContentType();
    if (type != null && type.startsWith("text/")) {
      EventSource source = null;
      if (!req.isInitial()) {
        source = SessionUtils.getEventSource(req);
      }
      if (source != null) {
        final TextEvent event = new SIPTextEventImpl(source, req);
        source.dispatch(event);
      }
      else {
        final TextEvent event = new SIPTextEventImpl(_app, req);
        _app.dispatch(event, new SessionDisposer(req));
      }
    }
    else {
      try {
        req.getApplicationSession().invalidate();
      }
      catch (final Throwable t) {
        ;
      }
    }
  }

  @Override
  protected void doRegister(final SipServletRequest req) throws ServletException, IOException {
    final SignalEvent event = new SIPRegisterEventImpl(_app, req);
    _app.dispatch(event, new NoHandleHandler(event, req));
  }

  @Override
  protected void doSubscribe(final SipServletRequest req) throws ServletException, IOException {
    final SignalEvent event = new SIPSubscribeEventImpl(_app, req);
    _app.dispatch(event, new NoHandleHandler(event, req));
  }

  @Override
  protected void doUpdate(final SipServletRequest req) throws ServletException, IOException {
    doOthers(req);
  }

  @Override
  protected void doOptions(final SipServletRequest req) throws ServletException, IOException {
    doOthers(req);
  }

  @Override
  protected void doInfo(final SipServletRequest req) throws ServletException, IOException {
    String contentType = req.getContentType();
    if (contentType != null && "application/dtmf-relay".equals(contentType)) {
        String contents = new String((byte[]) req.getContent());
        EventSource source = SessionUtils.getEventSource(req);
        DtmfRelayEvent event = new DtmfRelayEvent(source, contents);
        source.dispatch(event);
    }
    else {
        doOthers(req);
    }
  }

  @Override
  protected void doPublish(final SipServletRequest req) throws ServletException, IOException {
    doOthers(req);
  }

  protected void doOthers(final SipServletRequest req) {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      final SignalEvent event = new SIPUndefinedSignalEventImpl(source, req);
      source.dispatch(event);
    }
    else {
      final SignalEvent event = new SIPUndefinedSignalEventImpl(_app, req);
      _app.dispatch(event, new NoHandleHandler(event, req));
    }
  }

  @Override
  protected void doResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      super.doResponse(res);
    }
    else {
      final SipServletRequest req = (SipServletRequest) SIPHelper.getLinkSIPMessage(res.getRequest());
      if (req != null) {
        final SipServletResponse newRes = req.createResponse(res.getStatus(), res.getReasonPhrase());
        SIPHelper.copyContent(res, newRes);
        newRes.send();
      }
    }
  }

  @Override
  protected void doProvisionalResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      final int status = res.getStatus();
      if (status == SipServletResponse.SC_SESSION_PROGRESS) {
        source.dispatch(new SIPEarlyMediaEventImpl(source, res));
      }
      else if (status != SipServletResponse.SC_TRYING) {
        source.dispatch(new SIPRingEventImpl(source, res));
      }
    }
  }

  @Override
  protected void doSuccessResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      source.dispatch(new SIPSuccessEventImpl(source, res));
    }
  }

  @Override
  protected void doRedirectResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      source.dispatch(new SIPRedirectEventImpl(source, res));
    }
  }

  @Override
  protected void doErrorResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;
      try {
        call.doResponse(res, null);
      }
      catch (final Exception e) {
        log.warn("", e);
      }
    }
    else if (source instanceof SIPRegistrationImpl) {
      source.dispatch(new SIPErrorEventImpl(source, res));
    }
    else if (source instanceof SIPSubscriptionImpl) {
      source.dispatch(new SIPErrorEventImpl(source, res));
    }
    else {
      log.info("Cannot found EventSource to handle error response: " + source);
    }
  }

  
  private class NoHandleHandler implements Runnable {

    private SignalEvent _event;

    private SipServletRequest _req;

    private boolean _invalidate = false;

    public NoHandleHandler(final SignalEvent event, final SipServletRequest req) {
      this(event, req, false);
    }

    public NoHandleHandler(final SignalEvent event, final SipServletRequest req, final boolean invalidate) {
      _event = event;
      _req = req;
    }

    public void run() {
      if (!_event.isProcessed() && _req.isInitial() && !_req.isCommitted()) {
        try {
          _req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, "Request not handled by app").send();
        }
        catch (final Throwable t) {
          log.warn("", t);
        }
      }
      if (_invalidate) {
        try {
          _req.getApplicationSession().invalidate();
        }
        catch (final Throwable t) {
          log.warn("", t);
        }
      }
    }
  }

  private class SessionDisposer implements Runnable {

    private SipServletRequest _req;

    public SessionDisposer(final SipServletRequest req) {
      _req = req;
    }

    @Override
    public void run() {
      if (_req.isInitial()) {
        try {
          _req.getApplicationSession().invalidate();
        }
        catch (final Throwable t) {
          log.warn("", t);
        }
      }
    }

  }

}
