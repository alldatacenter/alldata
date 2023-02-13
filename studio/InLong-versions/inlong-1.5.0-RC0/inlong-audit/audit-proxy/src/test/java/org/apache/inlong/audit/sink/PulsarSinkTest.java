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

package org.apache.inlong.audit.sink;

import com.google.common.base.Charsets;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Sink;
import org.apache.flume.Transaction;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurables;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.lifecycle.LifecycleController;
import org.apache.flume.lifecycle.LifecycleState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulsarSinkTest {

    private static final Logger logger = LoggerFactory
            .getLogger(PulsarSinkTest.class);
    private static final String hostname = "127.0.0.1";
    private static final Integer port = 1234;
    private String zkStr = "127.0.0.1:2181";
    private String zkRoot = "/meta";
    private int batchSize = 1;

    private PulsarSink sink;
    private Channel channel;

    @Before
    public void setUp() {
        sink = new PulsarSink();
        channel = new MemoryChannel();

        Context context = new Context();

        context.put("type", "org.apache.inlong.dataproxy.sink.PulsarSink");
        context.put("pulsar_server_url", "pulsar://127.0.0.1:6650");

        sink.setChannel(channel);

        Configurables.configure(sink, context);
        Configurables.configure(channel, context);
    }

    @Test
    public void testProcess() throws InterruptedException, EventDeliveryException {
        setUp();
        Event event = EventBuilder.withBody("test event 1", Charsets.UTF_8);
        sink.start();
        Assert.assertTrue(LifecycleController.waitForOneOf(sink,
                LifecycleState.START_OR_ERROR, 5000));

        Transaction transaction = channel.getTransaction();

        transaction.begin();
        for (int i = 0; i < 10; i++) {
            channel.put(event);
        }
        transaction.commit();
        transaction.close();

        for (int i = 0; i < 5; i++) {
            Sink.Status status = sink.process();
            Assert.assertEquals(Sink.Status.READY, status);
        }

        sink.stop();
        Assert.assertTrue(LifecycleController.waitForOneOf(sink,
                LifecycleState.STOP_OR_ERROR, 5000));
    }

}
