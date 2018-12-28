package com.stuntbyte.salesforce.jdbc.dml;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.sobject.*;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.ConnectionException;
import com.sforce.soap.partner.Error;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;

public class PartnerSamples {
    PartnerConnection partnerConnection = null;
    private static BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws ConnectionException {
        PartnerSamples samples = new PartnerSamples();
        if (samples.login()) {
            // Add calls to the methods in this class.
            // For example:
            // samples.querySample();

            samples.insertData();
        }
    }

    private String getUserInput(String prompt) {
        String result = "";
        try {
            System.out.print(prompt);
            result = reader.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return result;
    }

    private boolean login() {
        boolean success = false;
//        String username = getUserInput("Enter username: ");
//        String password = getUserInput("Enter password: ");
//        String authEndPoint = getUserInput("Enter auth end point: ");

        String username = "salesforce@fidelma.com";
        String password = "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1";
        String authEndPoint = "https://login.salesforce.com/services/Soap/u/36.0";


        try {
            ConnectorConfig config = new ConnectorConfig();
            config.setUsername(username);
            config.setPassword(password);

            config.setAuthEndpoint(authEndPoint);
            config.setTraceFile("traceLogs.txt");
            config.setTraceMessage(true);
            config.setPrettyPrintXml(true);

            partnerConnection = new PartnerConnection(config);

            success = true;
        } catch (ConnectionException ce) {
            ce.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

        return success;
    }

    //
    // Add your methods here.
    //

    public void insertData() throws ConnectionException {

//        partnerConnection.retrieve()


        DescribeSObjectResult[] sObjectResult = partnerConnection.describeSObjects(new String[]{"wibble__c"});

        for (DescribeSObjectResult describeSObjectResult : sObjectResult) {
            for (Field field : describeSObjectResult.getFields()) {

                System.out.println(describeSObjectResult.getName() + "." + field.getName() + " " + field.getUpdateable());
            }

        }

        SObject wibble = new com.sforce.soap.partner.sobject.SObject();

        wibble.setType("wibble__c");
//        wibble.setField("boring__c", "hello");
        wibble.setField("custom_field__c", "hello");



        SaveResult[] saveResults = partnerConnection.create(new SObject[]{wibble});
        for (SaveResult saveResult : saveResults) {
            if (!saveResult.isSuccess()) {
                for (Error error : saveResult.getErrors()) {
                    System.out.println("ERROR: " + error.getMessage());
                }
                ;
            }
        }
    }


}
