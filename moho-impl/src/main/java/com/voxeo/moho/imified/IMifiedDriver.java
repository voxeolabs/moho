package com.voxeo.moho.imified;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.spi.HTTPDriver;
import com.voxeo.moho.spi.SpiFramework;

public class IMifiedDriver implements HTTPDriver {

  private static final long serialVersionUID = -6982167135072972700L;

  private static final Logger LOG = Logger.getLogger(IMifiedDriver.class);

  protected SpiFramework _appEventSource = null;

  protected DefaultHttpClient _httpClient = null;

  protected String _imifiedApiURL = "https://www.imified.com/api/bot/";
  
  protected final static String[] SCHEMAS = new String[]{"im"};

  @Override
  public void init(SpiFramework framework, HttpServlet servlet) {
    _appEventSource = framework;
    
    if (servlet.getServletConfig().getInitParameter("imifiedApiURL") != null) {
      _imifiedApiURL = servlet.getServletConfig().getInitParameter("imifiedApiURL");
    }

    HttpParams params = new BasicHttpParams();
    ConnManagerParams.setMaxTotalConnections(params, 20);
    ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
    ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    _httpClient = new DefaultHttpClient(cm, params);

    _httpClient.setReuseStrategy(new DefaultConnectionReuseStrategy());
    _httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
      public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (it.hasNext()) {
          HeaderElement he = it.nextElement();
          String param = he.getName();
          String value = he.getValue();
          if (value != null && param.equalsIgnoreCase("timeout")) {
            try {
              return Long.parseLong(value) * 1000;
            }
            catch (NumberFormatException ex) {
              // ignore
            }
          }
        }
        // otherwise
        return -1;
      }
    });
  }
  
  protected String getIMifiedURL() {
    return _imifiedApiURL;
  }
    
  protected HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
    return _httpClient.execute(request);
  }

  @Override
  public void destroy() {
    if (_httpClient != null) {
      _httpClient.getConnectionManager().shutdown();
    }
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received do GET request.");
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received POST request");

    BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
    String data = reader.readLine();

    if (LOG.isDebugEnabled()) {
      LOG.debug("The msg:" + data);
    }

    if (data != null && data.trim().length() > 0) {
      String[] array = data.split("&");

      try {
        Map<String, String> keyValues = new HashMap<String, String>();
        Map<String, String> history = null;

        for (String part : array) {

          String key = part.substring(0, part.indexOf('='));
          String value = part.substring(part.indexOf('=') + 1);

          if (key.startsWith("value")) {
            if (history == null) {
              history = new HashMap<String, String>();
            }
            history.put(key, URLDecoder.decode(value, "UTF-8"));
          }
          else {
            keyValues.put(key, URLDecoder.decode(value, "UTF-8"));
          }
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("The parsed keyValues:" + keyValues);
          LOG.debug("The parsed historyValue:" + history);
        }

        IMifiedEventImpl event = new IMifiedEventImpl(_appEventSource, this, keyValues.get("channel"),
            keyValues.get("botkey"), keyValues.get("userkey"), keyValues.get("user"), keyValues.get("network"),
            keyValues.get("msg"), keyValues.get("step"), keyValues.get("to"), history);
        _appEventSource.dispatch(event);
      }
      catch (Exception ex) {
        LOG.error("Exception when processing message from imified.", ex);
        throw new ServletException(ex);
      }
    }
  }

  @Override
  public String getProtocolFamily() {
    return PROTOCOL_HTTP;
  }

  @Override
  public String[] getEndpointSchemas() {
    return SCHEMAS;
  }

  @Override
  public SpiFramework getFramework() {
    return _appEventSource;
  }

  @Override
  public Endpoint createEndpoint(String addr) {
    return new ImifiedEndpointImpl(this, addr.substring(addr.indexOf(":") + 1));
  }

  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received do PUT request.");
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  public void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received do TRACE request.");
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received do HEAD request.");
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Received do OPTIONS request.");
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  public long getLastModified(HttpServletRequest req) {
    return 0;
  }
}
