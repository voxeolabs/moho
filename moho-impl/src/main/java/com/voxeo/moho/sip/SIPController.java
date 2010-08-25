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
import com.voxeo.moho.event.ApplicationEventSource;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.text.sip.SIPTextEventImpl;
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
      Class clz = null;
      try {
        clz = this.getClass().getClassLoader().loadClass(_applicationClass);
      }
      catch (final Throwable t) {
        clz = Thread.currentThread().getContextClassLoader().loadClass(_applicationClass);
      }
      final Application app = (Application) clz.newInstance();

      _sipFacory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
      _sdpFactory = (SdpFactory) getServletContext().getAttribute("javax.servlet.sdp.SdpFactory");

      if (_sdpFactory == null) {
        log.warn("Unable to get SdpFactory, some function, such as call hold unhold mute unmute, is unavailable:");
      }

      final Properties p = new Properties();
      final Driver driver = DriverManager.getDrivers().next();
      if (driver.getFactoryPropertyInfo() != null) {
        for (final PropertyInfo info : driver.getFactoryPropertyInfo()) {
          String value = getInitParameter(info.name);
          if (value == null) {
            value = info.defaultValue;
          }
          if (value != null) {
            p.setProperty(info.name, value);
          }
        }
      }

      _mscFactory = driver.getFactory(p);

      final ApplicationContextImpl ctx = new ApplicationContextImpl(app, _mscFactory, _sipFacory, _sdpFactory,
          getServletConfig().getServletName(), this.getServletContext());

      final Enumeration e = getInitParameterNames();
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
      final SIPInviteEvent ev = new SIPInviteEventImpl(_app.getApplicationContext(), req);
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
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPIncomingCall) {
      final SIPIncomingCall call = (SIPIncomingCall) source;
      try {
        call.doCancel();
      }
      catch (final Exception e) {
        log.warn("", e);
      }
    }
  }

  @Override
  protected void doRefer(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null) {
      source.dispatch(new SIPReferEventImpl(source, req));
    }
    else {
      final SIPReferEventImpl ev = new SIPReferEventImpl(_app, req);
      _app.dispatch(ev);
    }
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
    doOthers(req);
  }

  @Override
  protected void doNotify(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null) {
      source.dispatch(new SIPNotifyEventImpl(source, req));
    }
  }

  @Override
  protected void doRegister(final SipServletRequest req) throws ServletException, IOException {
    _app.dispatch(new SIPRegisterEventImpl(_app, req));
  }

  @Override
  protected void doSubscribe(final SipServletRequest req) throws ServletException, IOException {
    _app.dispatch(new SIPSubscribeEventImpl(_app, req));
  }

  @Override
  protected void doPublish(final SipServletRequest req) throws ServletException, IOException {
    doOthers(req);
  }

  @Override
  protected void doMessage(final SipServletRequest req) throws ServletException, IOException {
    req.createResponse(200).send();

    if (req.getContentType() != null
        && (req.getContentType().equalsIgnoreCase("text/plain") || req.getContentType().equalsIgnoreCase("text/html"))) {
      _app.dispatch(new SIPTextEventImpl(_app, req));
    }
  }

  protected void doOthers(final SipServletRequest req) {
    EventSource source = null;
    if (req.isInitial()) {
      source = _app;
    }
    else {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      source.dispatch(new SIPUndefinedSignalEventImpl(source, req));
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
      if (status != SipServletResponse.SC_TRYING) {
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

}
