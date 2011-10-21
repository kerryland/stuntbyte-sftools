package com.fidelma.salesforce.misc;

import com.sforce.soap.apex.SoapConnection;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.QueryOptions_element;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;


/**
 * Interface to Salesforce apis that has some reconnection support
 * to cope with rubbish Salesforce session handling.
 *
 * Thanks again Salesforce!
 */
public class Reconnector {

    private LoginHelper lh;

    public Reconnector(LoginHelper lh) {
        this.lh = lh;
    }


    public com.sforce.soap.metadata.FileProperties[] listMetadata(com.sforce.soap.metadata.ListMetadataQuery[] queries, double asOfVersion) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().listMetadata(queries, asOfVersion);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().listMetadata(queries, asOfVersion);
        }
    }

    public com.sforce.soap.metadata.AsyncResult deploy(byte[] ZipFile, com.sforce.soap.metadata.DeployOptions DeployOptions) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().deploy(ZipFile, DeployOptions);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().deploy(ZipFile, DeployOptions);
        }
    }

    public com.sforce.soap.metadata.AsyncResult[] checkStatus(java.lang.String[] asyncProcessId) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().checkStatus(asyncProcessId);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().checkStatus(asyncProcessId);
        }
    }

    public com.sforce.soap.metadata.DeployResult checkDeployStatus(java.lang.String asyncProcessId) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().checkDeployStatus(asyncProcessId);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().checkDeployStatus(asyncProcessId);
        }
    }


    public com.sforce.soap.metadata.AsyncResult retrieve(com.sforce.soap.metadata.RetrieveRequest retrieveRequest) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().retrieve(retrieveRequest);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().retrieve(retrieveRequest);
        }
    }

    public com.sforce.soap.metadata.RetrieveResult checkRetrieveStatus(java.lang.String asyncProcessId) throws com.sforce.ws.ConnectionException {
        try {
            return lh.getMetadataConnection().checkRetrieveStatus(asyncProcessId);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getMetadataConnection().checkRetrieveStatus(asyncProcessId);
        }
    }

    public SaveResult[] create(SObject[] sObjects) throws ConnectionException {
        try {
            return lh.getPartnerConnection().create(sObjects);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getPartnerConnection().create(sObjects);
        }
    }

    public QueryResult query(String soql) throws ConnectionException {
        try {
            return lh.getPartnerConnection().query(soql);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getPartnerConnection().query(soql);
        }
    }

    public SaveResult[] update(SObject[] update) throws ConnectionException {
        try {
            return lh.getPartnerConnection().update(update);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getPartnerConnection().update(update);
        }


    }

    public QueryResult queryMore(String queryLocator) throws ConnectionException {
        try {
            return lh.getPartnerConnection().queryMore(queryLocator);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getPartnerConnection().queryMore(queryLocator);
        }

    }

    public QueryOptions_element getQueryOptions() throws ConnectionException {
        QueryOptions_element result;
        try {
            result = lh.getPartnerConnection().getQueryOptions();

        } catch (ConnectionException e) {
            lh.reconnect();
            result = lh.getPartnerConnection().getQueryOptions();
        }

        return result;
    }

    public void setQueryOptions(int fetchSize) throws ConnectionException {
        try {
            lh.getPartnerConnection().setQueryOptions(fetchSize);
        } catch (ConnectionException e) {
            lh.reconnect();
            lh.getPartnerConnection().setQueryOptions(fetchSize);
        }
    }

    public DeleteResult[] delete(String[] delete) throws ConnectionException {
        try {
            return lh.getPartnerConnection().delete(delete);
        } catch (ConnectionException e) {
            lh.reconnect();
            return lh.getPartnerConnection().delete(delete);
        }
    }

    public SoapConnection getApexConnection() throws ConnectionException {
        return lh.getApexConnection();
    }
}
