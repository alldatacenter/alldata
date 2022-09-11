/*
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

package org.apache.inlong.dataproxy.sink;

import com.google.common.base.Charsets;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.event.EventBuilder;
import org.junit.Before;
import org.junit.Test;

public class TestPulsarSink {

    private MemoryChannel channel;

    @Before
    public void setUp() throws Exception {

        PulsarSink sink = new PulsarSink();
        channel = new MemoryChannel();
        Context context = new Context();
        context.put("type", "org.apache.inlong.dataproxy.sink.PulsarSink");
        sink.setChannel(channel);

        this.channel.configure(context);
    }

    @Test
    public void testProcess() throws Exception {

        Event event = EventBuilder.withBody("test event 1", Charsets.UTF_8);

        Transaction transaction = channel.getTransaction();
        transaction.begin();
        for (int i = 0; i < 10; i++) {
            channel.put(event);
        }
        transaction.commit();
        transaction.close();
    }
}
