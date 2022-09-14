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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * 
 * TestDefaultEvent2IndexRequestHandler
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({MetricRegister.class})
public class TestDefaultEvent2IndexRequestHandler {

    /**
     * test that ProfileEvent transform to EsIndexRequest
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        LinkedBlockingQueue<EsIndexRequest> dispatchQueue = new LinkedBlockingQueue<>();
        EsSinkContext context = TestEsSinkContext.mock(dispatchQueue);
        ProfileEvent event = TestEsSinkContext.mockProfileEvent();
        String uid = event.getUid();
        EsIdConfig idConfig = context.getIdConfig(uid);
        String indexName = idConfig.parseIndexName(event.getRawLogTime());
        DefaultEvent2IndexRequestHandler handler = new DefaultEvent2IndexRequestHandler();
        EsIndexRequest indexRequest = handler.parse(context, event);
        assertEquals(indexName, indexRequest.index());
    }
}
