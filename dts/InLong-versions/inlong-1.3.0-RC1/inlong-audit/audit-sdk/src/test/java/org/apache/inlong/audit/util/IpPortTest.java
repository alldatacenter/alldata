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

package org.apache.inlong.audit.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IpPortTest {
    private IpPort test = new IpPort("127.0.0.1", 80);

    @Test
    public void getIpPortKey() {
        String ipPortKey = test.getIpPortKey("127.0.0.1", 80);
        assertTrue(ipPortKey.equals("127.0.0.1:80"));
    }

    @Test
    public void testHashCode() {
        int hashCode = test.hashCode();
        assertTrue(hashCode != 0);
    }

    @Test
    public void testEquals() {
        IpPort test1 = new IpPort("127.0.0.1", 81);
        boolean ret = test.equals(test1);
        assertFalse(ret);

        IpPort test2 = new IpPort("127.0.0.1", 80);
        ret = test.toString().equals(test2.toString());
        assertTrue(ret);

        IpPort test3 = test;
        ret = test.equals(test3);
        assertTrue(ret);
    }
}