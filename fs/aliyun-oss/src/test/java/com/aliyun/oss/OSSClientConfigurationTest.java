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

package com.aliyun.oss;

import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.RetryStrategy;
import junit.framework.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class OSSClientConfigurationTest {

    @Test
    public void testSetSignerHandlers() {

        ClientConfiguration configuration = new ClientConfiguration();
        try {
            configuration.setSignerHandlers(null);
        } catch (Exception e) {
            // expected exception.
        }

        try {
            List<RequestSigner> singers = new LinkedList<RequestSigner>();
            RequestSigner requestSigner = new RequestSigner() {
                @Override
                public void sign(RequestMessage request) throws ClientException {
                }
            };
            singers.add(requestSigner);
            configuration.setSignerHandlers(singers);
            Assert.assertEquals(1, configuration.getSignerHandlers().size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testTimeoutClientEnable() {
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        Assert.assertFalse(configuration.isRequestTimeoutEnabled());

        configuration.setRequestTimeoutEnabled(true);
        OSS ossClient = new OSSClientBuilder()
                .build("test-endpoint","test-accessId", "test-accessKey", configuration);
        Assert.assertTrue(configuration.isRequestTimeoutEnabled());
    }

    @Test
    public void testUserRetryStrategy() {
        ClientConfiguration configuration = new ClientConfiguration();
        Assert.assertNull(configuration.getRetryStrategy());

        configuration.setRetryStrategy(new TestRetryStrategy());
        Assert.assertNotNull(configuration.getRetryStrategy());
        Assert.assertTrue(configuration.getRetryStrategy().shouldRetry(null, null, null, 1));
        Assert.assertFalse(configuration.getRetryStrategy().shouldRetry(null, null, null, 2));
    }

}


class TestRetryStrategy extends RetryStrategy {

    @Override
    public boolean shouldRetry(Exception ex, RequestMessage request, ResponseMessage response, int retries) {
        return retries < 2 ? true : false;
    }
}