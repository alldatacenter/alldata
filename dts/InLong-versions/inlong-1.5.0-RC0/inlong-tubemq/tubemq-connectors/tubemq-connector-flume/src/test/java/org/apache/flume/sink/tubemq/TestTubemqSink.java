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

package org.apache.flume.sink.tubemq;

import static org.apache.flume.sink.tubemq.ConfigOptions.MASTER_HOST_PORT_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurables;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.junit.Test;

public class TestTubemqSink {

    private Context prepareDefaultContext() {
        // Prepares a default context with Kafka Server Properties
        Context context = new Context();
        context.put(MASTER_HOST_PORT_LIST, "localhost:9092");
        return context;
    }

    @Test
    public void testTubeProperties() {

        TubemqSink tubemqSink = new TubemqSink();
        Context context = new Context();
        context.put(MASTER_HOST_PORT_LIST, "ip1:9092,ip2:9092");
        Configurables.configure(tubemqSink, context);

        TubeClientConfig config = tubemqSink.getClientConfig();

        // check that we have defaults set
        for (String host : config.getMasterInfo().getNodeHostPortList()) {
            if (host.startsWith("ip1")) {
                assertEquals("ip1:9092", host);
            } else if (host.startsWith("ip2")) {
                assertEquals("ip2:9092", host);
            } else {
                fail("config should contains host list");
            }
        }
    }

    @Test
    public void testTubeSink() throws Exception {
        /*
         * TubemqSink tubeSink = new TubemqSink(); Context context = prepareDefaultContext();
         * Configurables.configure(tubeSink, context); Channel memoryChannel = new MemoryChannel();
         * Configurables.configure(memoryChannel, context); tubeSink.setChannel(memoryChannel); tubeSink.start();
         * 
         * String msg = "default-topic-test"; Transaction tx = memoryChannel.getTransaction(); tx.begin(); Event event =
         * EventBuilder.withBody(msg.getBytes()); Map<String, String> eventHeader = new HashMap<>();
         * eventHeader.put(TOPIC, msg); event.setHeaders(eventHeader); memoryChannel.put(event); tx.commit();
         * tx.close();
         * 
         * try { Sink.Status status = tubeSink.process(); if (status == Sink.Status.BACKOFF) { fail("Error Occurred"); }
         * } catch (Exception ex) { // ignore }
         * 
         * await().atMost(20, TimeUnit.SECONDS).until(() -> tubeSink.getCounter().getTubeSendCount() == 1);
         * assertEquals(1, tubeSink.getCounter().getTubeSendCount());
         * 
         * tubeSink.stop();
         */
    }
}
