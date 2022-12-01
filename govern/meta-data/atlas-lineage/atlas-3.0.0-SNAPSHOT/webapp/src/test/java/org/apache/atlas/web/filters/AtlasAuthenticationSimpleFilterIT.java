/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.filters;

import org.apache.atlas.web.security.BaseSecurityTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.apache.commons.codec.binary.Base64;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 *
 */
public class AtlasAuthenticationSimpleFilterIT extends BaseSecurityTest {
    private Base64 enc = new Base64();

    @Test(enabled = false)
    public void testSimpleLoginForValidUser() throws Exception {
        URL url = new URL("http://localhost:31000/api/atlas/admin/session");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        String userpassword = "admin:admin"; // right password
        String encodedAuthorization = enc.encodeToString(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic " +
                encodedAuthorization);
        connection.connect();

        assertEquals(connection.getResponseCode(), Response.Status.OK.getStatusCode());
    }


    @Test(enabled = true)
    public void testAccessforUnauthenticatedResource() throws Exception {

        URL url = new URL("http://localhost:31000/api/atlas/admin/status");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        assertEquals(connection.getResponseCode(), Response.Status.OK.getStatusCode());

    }




    @Test(enabled = false)
    public void testSimpleLoginWithInvalidCrendentials() throws Exception {

        URL url = new URL("http://localhost:31000/api/atlas/admin/session");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        String userpassword = "admin:admin1"; //wrong password
        String encodedAuthorization = enc.encodeToString(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic " +
                encodedAuthorization);
        connection.connect();
        assertEquals(connection.getResponseCode(), 401);
    }

}
