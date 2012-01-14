package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.WscService;
//import com.sforce.async.AsyncApiException;
//import com.sforce.async.RestConnection;
import com.sforce.soap.apex.SoapConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
//import com.sforce.ws.wsdl.Part;

import java.util.Properties;

/**
 * Login to Salesforce, and get useful connections
 */
public class LoginHelper {
    private String server;
    private String username;
    private String password;
    private String key;

    private boolean trace = false;

    public static final double SFDC_VERSION = 22D;
    public static final double WSDL_VERSION = 22D;

    private MetadataConnection metadataConnection;
    private PartnerConnection partnerConnection;

    public LoginHelper(String server, String username, String password) {
        this(server, username, password, null);
    }

    public LoginHelper(String server, String username, String password, String key) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.key = key;
    }

    public void reconnect() {
        metadataConnection = null;
        partnerConnection = null;
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
            throw new RuntimeException("Server URL should point to API Version: " + WSDL_VERSION);
        cc.setAuthEndpoint(serverEndpoint);
        cc.setServiceEndpoint(serverEndpoint);

        cc.setManualLogin(true);
        PartnerConnection pConn = Connector.newConnection(cc);
        LoginResult lr;
        try {
            lr = pConn.login(username, password);

//            System.out.println("Login to " + lr.getUserInfo().getUserFullName());

            if (key != null) {
                LicenceService ls = new LicenceService();
                if (!ls.checkLicence(lr.getUserInfo().getUserFullName(),
                        lr.getUserInfo().getOrganizationName(),
                        key)) {
                    throw new ConnectionException("JDBC Driver Licence problem for " +
                            lr.getUserInfo().getUserFullName() + " at " +
                            lr.getUserInfo().getOrganizationName());
                }
            }

        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConnectionException("Unable to connect " + serverEndpoint + " with " + username, e);
        }
        return lr;
    }


    SoapConnection getApexConnection() throws ConnectionException {
        LoginResult lResult = doLogin();

        ConnectorConfig cc = setupConnectorConfig(lResult);
        String endpoint = lResult.getServerUrl();
        int baseUrl = endpoint.indexOf("/services/Soap/u");
        String serviceUrl = endpoint.substring(0, baseUrl);
        cc.setServiceEndpoint((new StringBuilder()).append(serviceUrl).append("/services/Soap/s/").append(WSDL_VERSION).toString());
        SoapConnection soapConnection = com.sforce.soap.apex.Connector.newConnection(cc);
        return soapConnection;
    }


    MetadataConnection getMetadataConnection() throws ConnectionException {
        if (metadataConnection == null) {
            LoginResult lResult = doLogin();

            ConnectorConfig cc = setupConnectorConfig(lResult);
            cc.setServiceEndpoint(lResult.getMetadataServerUrl());

            metadataConnection = com.sforce.soap.metadata.Connector.newConnection(cc);
        }
        return metadataConnection;
    }

    public PartnerConnection getPartnerConnectionForTestingOnly() throws ConnectionException {
        return getPartnerConnection();
    }


    PartnerConnection getPartnerConnection() throws ConnectionException {
        if (partnerConnection == null) {
            LoginResult lResult = doLogin();

            ConnectorConfig cc = setupConnectorConfig(lResult);
            cc.setServiceEndpoint(lResult.getServerUrl());
            partnerConnection = new PartnerConnection(cc);
        }
        return partnerConnection;
    }

    public ResultSetFactory createResultSetFactory(Properties info) throws ConnectionException {
        WscService svc = new WscService(getPartnerConnection(), info);
        return svc.createResultSetFactory();
    }


    private ConnectorConfig setupConnectorConfig(LoginResult lResult) {
        ConnectorConfig cc;
        cc = new ConnectorConfig();
        configureTracing(cc);
        configureProxy(cc);
        cc.setSessionId(lResult.getSessionId());
        return cc;
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
