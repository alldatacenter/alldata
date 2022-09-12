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

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.flume.Transaction;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * 
 * TestEsChannelWorker
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({EsSinkFactory.class, MetricRegister.class})
public class TestEsChannelWorker {

    private EsSinkContext context;

    /**
     * before
     * 
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        LinkedBlockingQueue<EsIndexRequest> dispatchQueue = new LinkedBlockingQueue<>();
        this.context = TestEsSinkContext.mock(dispatchQueue);
    }

    /**
     * test
     */
    @Test
    public void test() {
        // prepare
        ProfileEvent event = TestEsSinkContext.mockProfileEvent();
        Transaction tx = this.context.getChannel().getTransaction();
        tx.begin();
        this.context.getChannel().put(event);
        tx.commit();
        tx.close();
        // test
        EsChannelWorker worker = new EsChannelWorker(context, 0);
        worker.doRun();
        worker.start();
        worker.close();
    }
}
