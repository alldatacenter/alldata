/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.common.metric.reporter;

import static org.mockito.Mockito.mock;

import java.util.concurrent.Future;
import org.apache.inlong.common.reporpter.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.inlong.common.reporpter.StreamConfigLogReporter;
import org.apache.inlong.common.reporpter.dto.StreamConfigLogInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ReporterTest {

    @Test
    public void streamConfigLogReporterTest() throws Exception {
        String serverUrl = "http://127.0.0.1:8083/api/inlong/manager/openapi/stream/log"
                + "/reportConfigLogStatus";
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        StreamConfigLogReporter streamConfigLogReporter = new StreamConfigLogReporter(httpClient,
                serverUrl);
        StreamConfigLogReporter spy = Mockito.spy(streamConfigLogReporter);
        StreamConfigLogInfo info = new StreamConfigLogInfo();
        Future<Response> future = spy.asyncReportData(info, serverUrl);
        Assert.assertEquals(future.get(),null);
    }
}
