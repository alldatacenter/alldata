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

package org.apache.inlong.dataproxy.source;

import java.util.HashMap;
import java.util.Map;
import org.apache.flume.Context;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.junit.Assert;
import org.junit.Test;

public class UdpSourceTest {

    @Test
    public void configTest() {
        Map<String, String> map = new HashMap<>();
        map.put(ConfigConstants.MAX_THREADS, "32");
        map.put(ConfigConstants.CONFIG_PORT, "8080");
        map.put(ConfigConstants.CONFIG_HOST, "127.0.0.1");
        map.put(ConfigConstants.TOPIC, "topic");
        map.put(ConfigConstants.ATTR, "{}");
        Context context = new Context(map);
        SimpleUdpSource udpSource = new SimpleUdpSource();
        udpSource.configure(context);
        int threadNum = udpSource.getContext().getInteger(ConfigConstants.MAX_THREADS);
        Assert.assertEquals(threadNum, 32);
    }
}
