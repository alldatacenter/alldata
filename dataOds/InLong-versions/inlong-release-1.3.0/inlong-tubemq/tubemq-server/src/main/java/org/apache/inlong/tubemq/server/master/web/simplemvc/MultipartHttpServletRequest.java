/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.simplemvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;

public class MultipartHttpServletRequest extends HttpServletRequestWrapper {
    private boolean parsed;
    private HashMap<String, String> cachedParams = new HashMap<>();

    public MultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
        parsed = false;
        // One could set-up a shared cache buffer here for the use of cachedParams for
        // multipart/form-data realization, however, it depends on the size.
        // If the data size is trivial like tens of bytes, there's simply no need to do this.
    }

    /**
     * It's not the recommended way to handle "multipart/form-data", as form-data can carry non-char data
     * like bytes array. Provided currently there's ONLY string used in WebAPI, so for CONVENIENT, this
     * function translate all form-data to traditional "parameters" (like how it does in "x-www-form-urlencoded")
     * for reusing current dispatcher-execute framework.
     * If one day there's need to received serialized POJO or some kinds of binary stream, please compose a
     * full powered function to replace this one, as this is ONLY for backward compatibility.
     * @param req
     */
    private void extractParametersFromMultipartFormData(HttpServletRequest req) throws IOException, ServletException {
        for (Part dataPart : req.getParts()) {
            if (dataPart.getSize() > WebApiServlet.MAX_MULTIPART_POST_DATA_SIZE) {
                continue;  // too big, so we simply skip
            }

            String dataKey = dataPart.getName();
            int dataSize = (int) dataPart.getSize();
            byte[] buffer = new byte[dataSize];
            dataPart.getInputStream().read(buffer, 0, dataSize);
            String dataValue = new String(buffer, StandardCharsets.UTF_8);
            cachedParams.put(dataKey, dataValue);
        }
    }

    @Override
    public String getParameter(String name) {
        if (!parsed) {
            parsed = true;  // invert here to ensure only once, or there would be infinite parse loops.
            try {
                extractParametersFromMultipartFormData((HttpServletRequest) getRequest());
            } catch (Exception e) {
                cachedParams.clear();
            }
        }
        if (cachedParams.containsKey(name)) {
            return cachedParams.get(name);
        } else {
            return null;
        }
    }
}
