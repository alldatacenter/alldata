/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.atlas.AtlasClient;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import static org.testng.Assert.*;

@Test
public class ServletsTest {

    public void testEmptyMessage() throws Exception {
        //This shouldn't throw exception
        Response response =
                Servlets.getErrorResponse(new NullPointerException(), Response.Status.INTERNAL_SERVER_ERROR);
        assertNotNull(response);
        ObjectNode responseEntity = (ObjectNode) response.getEntity();
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.get(AtlasClient.ERROR));
    }
}
