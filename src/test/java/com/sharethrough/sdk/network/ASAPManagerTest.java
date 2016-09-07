package com.sharethrough.sdk.network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.sharethrough.sdk.ContextInfo;
import com.sharethrough.sdk.TestBase;
import junit.framework.Assert;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
/**
 Created by engineers on 5/2/16.
 */
public class ASAPManagerTest extends TestBase  {
    private ASAPManager subject;
    private String pkey;
    private String mediationRequestId;
    @Mock private RequestQueue requestQueue;
    @Mock private ASAPManager.ASAPManagerListener asapManagerListener;
    @Before
    public void setUp() throws Exception {
        pkey = "fakePkey";
        mediationRequestId = "fakeMrid";
        subject = new ASAPManager(pkey, requestQueue);
    }
    @Test
    public void callASAP_triggersAdRequest() {
        subject.callASAP(asapManagerListener);
        verify(requestQueue).add((Request)anyObject());
    }

    @Test
    public void generateCustomKeyValues_setsCustomKeyValueQueryString() {
        Map<String, String> keyValues = new HashMap<String,String>();
        keyValues.put("key1", "value1");
        keyValues.put("key2", "value2");

        ContextInfo ci = new ContextInfo(RuntimeEnvironment.application);
        String expectedResult = "http://asap.sharethrough.com/v1?pkey=fakePkey&pubAppName=com.sharethrough.android.sdk&pubAppVersion=v4.1.0&customKeys%5Bkey1%5D=value1&customKeys%5Bkey2%5D=value2";
        String result = subject.generateEndpointWithCustomKeyValues(keyValues);
        System.out.println("result:" + result);
        System.out.println("expected:" + expectedResult);

        assertThat(subject.generateEndpointWithCustomKeyValues(keyValues)).isEqualTo(expectedResult);
    }

    @Test
    public void handleResponse_callsOnErrorIfResponseStatusIsNotOK() {
        String responseStatusNotOK = "{ 'mrid': 'fakeMrid', 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'undefined', 'keyValue': 'undefined', 'status': 'Error'}";
        subject.handleResponse(responseStatusNotOK, asapManagerListener);
        verify(asapManagerListener).onError("Error");
    }
    @Test
    public void handleResponse_callsSuccessWithCorrectParamsForDirectSell() {
        String responseForDirectSell = "{ 'mrid': 'fakeMrid', 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'creative_key', 'keyValue': 'fakeckey', 'status': 'OK'}";
        subject.handleResponse(responseForDirectSell, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        queryStringParams.add(new BasicNameValuePair("creative_key", "fakeckey"));
        verify(asapManagerListener).onSuccess(queryStringParams, mediationRequestId);
    }
    @Test
    public void handleResponse_callsSuccessWithCorrectParamsForProgrammatic() {
        String responseForProgrammatic = "{ 'mrid': 'fakeMrid', 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'stx_monetize', 'keyValue': 'undefined', 'status': 'OK'}";
        subject.handleResponse(responseForProgrammatic, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        verify(asapManagerListener).onSuccess(queryStringParams, mediationRequestId);
    }

    @Test
    public void handleResponse_callsSuccessWithCorrectParamsNoAdServerSetup() {
        String responseForNoAdServer = "{ 'mrid': 'fakeMrid', 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'undefined', 'keyValue': 'undefined', 'status': 'OK'}";
        subject.handleResponse(responseForNoAdServer, asapManagerListener);

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
        queryStringParams.add(new BasicNameValuePair("placement_key", "fakePkey"));
        verify(asapManagerListener).onSuccess(queryStringParams, mediationRequestId);
    }
    @Test
    public void handleResponse_callsOnErrorWithInCorrectParams() {
        String responseForIncorrectResponse = "{'mrid': 'fakeMrid', 'pkey': 'c1a0a591'}";
        subject.handleResponse(responseForIncorrectResponse, asapManagerListener);
        verify(asapManagerListener).onError(anyString());
    }

    @Test
    public void handleResponse_callsOnErrorWithInCorrectJson() {
        String responseIncorrectJson = "{'pkey'l;}";
        subject.handleResponse(responseIncorrectJson, asapManagerListener);
        verify(asapManagerListener).onError(anyString());
    }

    @Test
    public void handleResponse_callsOnErrorWithNoMrid() {
        String responseForNoMrid = "{ 'pkey': 'c1a0a591', 'adServer': 'DFP', 'keyType': 'undefined', 'keyValue': 'undefined', 'status': 'OK'}";
        subject.handleResponse(responseForNoMrid, asapManagerListener);
        verify(asapManagerListener).onError(anyString());
    }
}