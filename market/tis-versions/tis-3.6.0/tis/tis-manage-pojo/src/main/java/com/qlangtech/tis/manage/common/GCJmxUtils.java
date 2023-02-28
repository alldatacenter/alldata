/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年2月23日
 */
public class GCJmxUtils {

    private static final MBeanServer DEFAULT_MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    public static long getYongGC() throws Exception {
        return getYoungGC(DEFAULT_MBEAN_SERVER);
    }

    public static long getFullGC() throws Exception {
        return getFullGC(DEFAULT_MBEAN_SERVER);
    }

    private static ObjectName youngGCObjectName = null;

    private static ObjectName fullGCObjectName = null;

    private static long getYoungGC(MBeanServerConnection mbeanServer) throws Exception {
        // ObjectName objectName;
        if (youngGCObjectName == null) {
            if (mbeanServer.isRegistered(new ObjectName("java.lang:type=GarbageCollector,name=ParNew"))) {
                youngGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=ParNew");
            } else if (mbeanServer.isRegistered(new ObjectName("java.lang:type=GarbageCollector,name=Copy"))) {
                youngGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=Copy");
            } else {
                youngGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=PS Scavenge");
            }
        }
        return (Long) mbeanServer.getAttribute(youngGCObjectName, "CollectionCount");
    }

    private static long getFullGC(MBeanServerConnection mbeanServer) throws Exception {
        if (fullGCObjectName == null) {
            if (mbeanServer.isRegistered(new ObjectName("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep"))) {
                fullGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep");
            } else if (mbeanServer.isRegistered(new ObjectName("java.lang:type=GarbageCollector,name=MarkSweepCompact"))) {
                fullGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=MarkSweepCompact");
            } else {
                fullGCObjectName = new ObjectName("java.lang:type=GarbageCollector,name=PS MarkSweep");
            }
        }
        return (Long) mbeanServer.getAttribute(fullGCObjectName, "CollectionCount");
    }

    public static void main(String[] args) throws Exception {
        long curr = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            GCJmxUtils.getFullGC();
            System.out.println( getYongGC() );
             Thread.sleep(1000);
        }
        System.out.println(System.currentTimeMillis() - curr);
    }
}
