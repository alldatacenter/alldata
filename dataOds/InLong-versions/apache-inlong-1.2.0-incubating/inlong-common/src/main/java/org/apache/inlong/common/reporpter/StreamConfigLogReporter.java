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

package org.apache.inlong.common.reporpter;

import java.util.concurrent.RejectedExecutionHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.inlong.common.reporpter.dto.StreamConfigLogInfo;

public class StreamConfigLogReporter extends AbstractReporter<StreamConfigLogInfo> {

    public StreamConfigLogReporter(String serverUrl) {
        super(serverUrl);
    }

    public StreamConfigLogReporter(CloseableHttpClient httpClient, String serverUrl) {
        super(httpClient, serverUrl);
    }

    public StreamConfigLogReporter(String serverUrl, int corePoolSize, int maximumPoolsize,
            int syncSendQueueSize,
            RejectedExecutionHandler rejectedExecutionHandler) {
        super(serverUrl, corePoolSize, maximumPoolsize, syncSendQueueSize,
                rejectedExecutionHandler);
    }
}
