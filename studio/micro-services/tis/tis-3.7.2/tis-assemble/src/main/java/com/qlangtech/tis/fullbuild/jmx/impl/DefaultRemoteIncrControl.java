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
package com.qlangtech.tis.fullbuild.jmx.impl;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import com.qlangtech.tis.fullbuild.jmx.IRemoteIncrControl;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月10日 下午2:30:34
 */
public class DefaultRemoteIncrControl implements IRemoteIncrControl {

    public static void main(String[] args) throws Exception {
        final String hostName = "10.1.7.43";
        final int portNum = 9998;
        DefaultRemoteIncrControl control = new DefaultRemoteIncrControl();
        try (JMXConnector connector = createConnector(hostName, portNum)) {
            control.pauseIncrFlow(connector, "");
        }
    }

    public static JMXConnector createConnector(String hostName, int port) throws Exception {
        // JMX客户端远程连接服务器端MBeanServer
        JMXServiceURL u = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostName + ":" + port + "/jmxrmi");
        JMXConnector c = JMXConnectorFactory.connect(u);
        return c;
    }

    @Override
    public void pauseIncrFlow(JMXConnector conn, String collectionName) {
        // http://blog.csdn.net/DryKillLogic/article/details/38412913
        executeJMXMethod(conn, collectionName, "pauseConsume");
    }

    /**
     * @param conn
     */
    protected void executeJMXMethod(JMXConnector conn, String indexName, String method) {
        try {
            MBeanServerConnection mbsc = conn.getMBeanServerConnection();
            // String oname = "tis:type=increase,name=" + index;
            ObjectName mbeanName = new ObjectName("tis:type=increase,name=" + indexName);
            mbsc.invoke(mbeanName, method, null, null);
        // mbsc.invoke(mbeanName, "resumeConsume", null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resumeIncrFlow(JMXConnector conn, String collectionName) {
        executeJMXMethod(conn, collectionName, "resumeConsume");
    }
}
