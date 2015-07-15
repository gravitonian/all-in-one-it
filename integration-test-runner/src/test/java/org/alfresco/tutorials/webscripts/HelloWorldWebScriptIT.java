/*
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.alfresco.tutorials.webscripts;

import org.alfresco.tutorials.cmis.CmisHelper;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration Test (IT) for the sample Hello World web script.
 *
 * @author martin.bergljung@alfresco.com
 * @version 2.1.x
 */
public class HelloWorldWebScriptIT {
    private static final String OPENCMIS_CON_NAME = "it-01";
    private static final String ALFRESCO_USERNAME = "admin";
    private static final String ALFRESCO_PWD = "admin";
    private static final String TEST_FOLDER_NAME = "TestFolder";
    private static final String TEST_DOC_NAME = "TestDoc.txt";
;
    private Folder testFolder;
    private Session session;
    private Document testDocument;
    private CmisHelper cmisHelper;

    public HelloWorldWebScriptIT() {
        cmisHelper = new CmisHelper();
        session = cmisHelper.getSession(OPENCMIS_CON_NAME, ALFRESCO_USERNAME, ALFRESCO_PWD);
    }

    @Before
    public void loadTestData() {
        testFolder = cmisHelper.createFolder(session, TEST_FOLDER_NAME);
        try {
            testDocument = cmisHelper.createDocument(session, testFolder, TEST_DOC_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void removeTestData() {
        if (testDocument != null) {
            boolean deleteAllVersions = true;
            testDocument.delete(deleteAllVersions);
        }
        if (testFolder != null) {
            testFolder.delete();
        }
    }

    @Test
    public void testWebScriptCall() throws Exception {
        String webscriptURL = "http://localhost:8080/alfresco/service/sample/helloworld";
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope("localhost", 8080),
                new UsernamePasswordCredentials(ALFRESCO_USERNAME, ALFRESCO_PWD));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        try {
            HttpGet httpget = new HttpGet(webscriptURL);
            HttpResponse httpResponse = httpclient.execute(httpget);
            assertEquals("HTTP Response Status is not OK(200)",
                    HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
            HttpEntity entity = httpResponse.getEntity();
            assertNotNull("Response from Web Script is null", entity);
            String response = EntityUtils.toString(entity);
            JSONParser parser=new JSONParser();
            JSONObject jsonResponseObj = (JSONObject)parser.parse(response);
            assertTrue("Folder not found", (boolean)jsonResponseObj.get("foundFolder"));
            assertTrue("Doc not found", (boolean) jsonResponseObj.get("foundDoc"));
        } finally {
            httpclient.close();
        }
    }
}
