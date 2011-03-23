package com.fidelma.salesforce.misc;

import com.sforce.soap.apex.SoapConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 8/03/2011
 * Time: 6:35:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoginHelper {

    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";

    private String server;
    private String username;
    private String password;

    private String sessionId;
    private boolean trace;
    
    public static final double SFDC_VERSION = 20D;
    private static final double WSDL_VERSION = 20D;


    public LoginHelper(String server, String username, String password) {

        this.server = server;
        this.username = username;
        this.password = password;
    }

    private LoginResult doLogin() throws ConnectionException {
          ConnectorConfig cc;
          cc = new ConnectorConfig();
          configureTracing(cc);
          configureProxy(cc);
          String serverEndpoint = server;
          if (!serverEndpoint.startsWith("http://") && !serverEndpoint.startsWith("https://"))
              serverEndpoint = (new StringBuilder()).append("https://").append(serverEndpoint).toString();
          if (!serverEndpoint.contains("/services/Soap/u/")) {
              if (!serverEndpoint.endsWith("/"))
                  serverEndpoint = (new StringBuilder()).append(serverEndpoint).append("/").toString();
              serverEndpoint = (new StringBuilder()).append(serverEndpoint).append("services/Soap/u/").append(WSDL_VERSION).toString();
          } else if (!serverEndpoint.endsWith(String.valueOf(WSDL_VERSION)))
              throw new RuntimeException("Server URL should point to API Version: 20.0");
          cc.setAuthEndpoint(serverEndpoint);
          cc.setServiceEndpoint(serverEndpoint);
          if (sessionId != null) {
              LoginResult lResult = new LoginResult();
              lResult.setSessionId(sessionId);
              lResult.setServerUrl(serverEndpoint);
              lResult.setMetadataServerUrl(serverEndpoint.replaceFirst("/u/", "/m/"));
              return lResult;
          }

          cc.setManualLogin(true);
          PartnerConnection pConn = Connector.newConnection(cc);
          return pConn.login(username, password);
      }


      public SoapConnection getApexConnection() throws ConnectionException {
          ConnectorConfig cc;
          LoginResult lResult = doLogin();
          cc = new ConnectorConfig();
          configureTracing(cc);
          configureProxy(cc);
          cc.setSessionId(lResult.getSessionId());
          String endpoint = lResult.getServerUrl();
          int baseUrl = endpoint.indexOf("/services/Soap/u");
          String serviceUrl = endpoint.substring(0, baseUrl);
          cc.setServiceEndpoint((new StringBuilder()).append(serviceUrl).append("/services/Soap/s/").append(WSDL_VERSION).toString());
          return com.sforce.soap.apex.Connector.newConnection(cc);
      }

      public MetadataConnection getMetadataConnection() throws ConnectionException {
          ConnectorConfig cc;
          LoginResult lResult = doLogin();
          cc = new ConnectorConfig();
          configureTracing(cc);
          configureProxy(cc);
          cc.setSessionId(lResult.getSessionId());
          cc.setServiceEndpoint(lResult.getMetadataServerUrl());


          return com.sforce.soap.metadata.Connector.newConnection(cc);
      }

      public PartnerConnection getPartnerConnection() throws ConnectionException {
          ConnectorConfig cc;
          LoginResult lResult = doLogin();
          cc = new ConnectorConfig();
          configureTracing(cc);
          configureProxy(cc);
          cc.setSessionId(lResult.getSessionId());
          cc.setServiceEndpoint(lResult.getServerUrl());
          return new PartnerConnection(cc);
      }

    private void configureTracing(ConnectorConfig cc) {
           cc.setTraceMessage(trace);
           cc.setPrettyPrintXml(trace);
       }


       private void configureProxy(ConnectorConfig cc) {
           String proxyHost = System.getProperty("http.proxyHost");
           String proxyPort = System.getProperty("http.proxyPort");
           if (proxyHost != null) {
               if (proxyHost.startsWith("http://"))
                   throw new RuntimeException("proxyHost should not start with: http://");
               int port = proxyPort != null ? Integer.parseInt(proxyPort) : 80;
               cc.setProxy(proxyHost, port);
               String proxyUser = System.getProperty("http.proxyUser");
               cc.setProxyUsername(proxyUser);
               String proxyPassword = System.getProperty("http.proxyPassword");
               cc.setProxyPassword(proxyPassword);
               log((new StringBuilder()).append("Using proxy: ").append(proxyHost).append(":").append(port).append(proxyUser == null ? "" : (new StringBuilder()).append(" user ").append(proxyUser).toString()).toString());
           }
       }


    private void log(String msg) {
          System.out.println(msg);
      }
}
