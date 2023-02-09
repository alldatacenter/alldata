/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.internal.OSSConstants;

public class ServiceSignatureTest {
    /**
     * Test method for {@link com.aliyun.oss.common.auth.ServiceSignature#computeSignature(java.lang.String, java.lang.String)}.
     * @throws Exception 
     */
    @Test
    public void testComputeSignature() throws Exception {
        ServiceSignature sign = ServiceSignature.create();
        String key = "csdev";
        String data = "/ListTable\n"
                + "Date=Mon%2C%2028%20Nov%202011%2014%3A02%3A46%20GMT"
                + "&OTSAccessKeyId=csdev&APIVersion=1"
                + "&SignatureMethod=HmacSHA1&SignatureVersion=1";
        String expected = "3mAoxRc8hd7WdaFB5Ii7dGNcwx0=";

        String signature = sign.computeSignature(key, data);
        assertEquals(expected, signature);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        Date dt;
        try {
            dt = DateUtil.parseRfc822Date("Mon, 28 Nov 2011 14:02:46 GMT");
            parameters.put("Date", DateUtil.formatRfc822Date(dt));
        } catch (ParseException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        parameters.put("OTSAccessKeyId", "csdev");
        parameters.put("APIVersion", "1");
        parameters.put("SignatureMethod", "HmacSHA1");
        parameters.put("SignatureVersion", "1");
        data = "/ListTable\n" + HttpUtil.paramToQueryString(parameters, OSSConstants.DEFAULT_CHARSET_NAME);
        signature = sign.computeSignature("csdev", data);
        assertEquals(expected, signature);
    }
}
