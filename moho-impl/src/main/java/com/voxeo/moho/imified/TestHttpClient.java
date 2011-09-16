package com.voxeo.moho.imified;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class TestHttpClient {

  /**
   * @param args
   */
  public static void main(String[] args) {
    HttpParams params = new BasicHttpParams();
    ConnManagerParams.setMaxTotalConnections(params, 20);
    ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
    ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    DefaultHttpClient _httpClient = new DefaultHttpClient(cm, params);
    
    
    final HttpTrace trace = new HttpTrace("http://localhost:8080/DisableTrace/DisableTrace");

    try {
      HttpResponse resp = _httpClient.execute(trace);
     InputStream input =  resp.getEntity().getContent();
     InputStreamReader reader = new InputStreamReader(input);
     BufferedReader bf = new BufferedReader(reader);
     String line = null;
     while((line = bf.readLine())!= null){
       System.out.println(line);
     }
     
     byte[] bytes = new byte[1024];
     
    }
    catch (ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
