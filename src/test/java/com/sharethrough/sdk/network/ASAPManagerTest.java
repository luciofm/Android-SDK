package com.sharethrough.sdk.network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.sharethrough.sdk.TestBase;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
/**
 Created by engineers on 5/2/16.
 */
public class ASAPManagerTest extends TestBase  {
    private ASAPManager subject;
    private String pkey;
    @Mock private RequestQueue requestQueue;
    @Mock private ASAPManager.ASAPManagerListener asapManagerListener;
    @Before
    public void setUp() throws Exception {
        pkey = "fakePkey";
        subject = new ASAPManager(pkey, requestQueue);
    }
    @Test
    public void callASAP2_triggersAdRequest() {
        subject.callASAP2(asapManagerListener);
        verify(requestQueue).add((Request)anyObject());
    }
    @Test
    public void handleResponse_callsOnErrorIfResponseStatusIsNotOK() {
        String responseStatusNotOK = "{ 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'undefined', 'keyValue': 'undefined', 'status': 'Error'}";
        subject.handleResponse(responseStatusNotOK, asapManagerListener);
        verify(asapManagerListener).onError("Error");
    }
    @Test
    public void handleResponse_callsSuccessWithCorrectParamsForDirectSell() {
        String responseForDirectSell = "{ 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'creative_key', 'keyValue': 'fakeckey', 'status': 'OK'}";
        subject.handleResponse(responseForDirectSell, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        queryStringParams.add(new BasicNameValuePair("creative_key", "fakeckey"));
        verify(asapManagerListener).onSuccess(queryStringParams);
    }
    @Test
    public void handleResponse_callsSuccessWithCorrectParamsForProgrammatic() {
        String responseForProgrammatic = "{ 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'stx_monetize', 'keyValue': 'undefined', 'status': 'OK'}";
        subject.handleResponse(responseForProgrammatic, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        verify(asapManagerListener).onSuccess(queryStringParams);
    }

    @Test
    public void handleResponse_callsSuccessWithCorrectParamsNoAdServerSetup() {
        String responseForNoAdServer = "{ 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'undefined', 'keyValue': 'undefined', 'status': 'OK'}";
        subject.handleResponse(responseForNoAdServer, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        verify(asapManagerListener).onSuccess(queryStringParams);
    }
    @Test
    public void handleResponse_callsOnErrorWithInCorrectParams() {
        String responseForIncorrectResponse = "{'pkey': 'c1a0a591'}";
        subject.handleResponse(responseForIncorrectResponse, asapManagerListener);
        verify(asapManagerListener).onError(anyString());
    }

    @Test
    public void handleResponse_callsOnErrorWithInCorrectJson() {
        String responseIncorrectJson = "{'pkey'l;}";
        subject.handleResponse(responseIncorrectJson, asapManagerListener);
        verify(asapManagerListener).onError(anyString());
    }
}