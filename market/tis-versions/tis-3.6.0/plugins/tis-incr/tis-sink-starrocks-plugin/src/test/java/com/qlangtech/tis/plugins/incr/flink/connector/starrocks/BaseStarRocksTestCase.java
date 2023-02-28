/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;

import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.starrocks.connector.flink.manager.StarRocksStreamLoadVisitor;
import com.starrocks.shade.org.apache.http.ProtocolException;
import com.starrocks.shade.org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.test.util.AbstractTestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-01 09:36
 **/
public class BaseStarRocksTestCase extends AbstractTestBase {
    // docker run -d -p 1521:1521 -e ORACLE_PASSWORD=test -e ORACLE_DATABASE=tis gvenzl/oracle-xe:18.4.0-slim

    static StarRocksContainer starRocksContainer;

    static String tisDatabase = null;


    private static int addBeNode() throws Exception {
        //  String command = "docker inspect -f \"{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}\" " + containerId;

        String command = "mysql -h127.0.0.1 -P" + starRocksContainer.getFePort()
                + " -uroot -e\"ALTER SYSTEM ADD BACKEND '" + StringUtils.substring(starRocksContainer.getContainerId(), 0, 12) + ":9050'\" ";

        Process exec = null;
        String containerIp;
        try {
            exec = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});

            String errMsg = null;
            System.out.println(errMsg = IOUtils.toString(exec.getErrorStream(), TisUTF8.get()));

            System.out.println(containerIp = IOUtils.toString(exec.getInputStream(), TisUTF8.get()));
            return exec.exitValue();
        } finally {
            exec.destroy();
        }

    }


    @BeforeClass
    public static void initialize() throws Exception {
        starRocksContainer = new StarRocksContainer();
       // starRocksContainer.withNetworkMode("host");
        starRocksContainer.start();

        System.out.println(starRocksContainer.getHost());
        starRocksContainer.getContainerInfo();

        //27be8d551534
        addBeNode();
        Thread.sleep(10000);
        DorisSourceFactory ds = createSourceFactory();

        try (Connection conn = ds.getConnection(
                ds.buidJdbcUrl(null, "localhost", null))) {
            try (Statement statement = conn.createStatement()) {

                // statement.execute("ALTER SYSTEM ADD BACKEND \"" + StringUtils.substring(starRocksContainer.getContainerId(), 0, 12) + ":9050\"");
                String colName = null;
                try (ResultSet result = statement.executeQuery("SHOW PROC '/backends'")) {
                    ResultSetMetaData metaData = result.getMetaData();
                    if (result.next()) {
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            colName = metaData.getColumnName(i);
                            System.out.println(colName + ":" + result.getString(colName) + " ");
                        }
                        Assert.assertTrue("be node must be alive", result.getBoolean("Alive"));
                    } else {
                        Assert.fail("must has backend node");
                    }
                }

                String databse = "tis";
                statement.execute("create database " + databse);
                tisDatabase = databse;
            }
        }


//        StarRocksStreamLoadVisitor.redirectStategySetter = () -> {
//            DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy() {
//                @Override
//                protected boolean isRedirectable(String method) {
//                    return false;
//                }
//
//                @Override
//                protected URI createLocationURI(String location) throws ProtocolException {
//                    throw new UnsupportedOperationException();
//                }
//            };
//
//            return redirectStrategy;
//        };

    }

    @AfterClass
    public static void stop() {
        starRocksContainer.stop();
    }

    @Test
    public void testStarRocks() {

//        Integer mappedPort = starRocksContainer.getMappedPort(9030);
//        Integer mappedPort1 = starRocksContainer.getMappedPort(8030);
//        System.out.println("mappedPort:" + mappedPort + ",mappedPort1:" + mappedPort1);

    }

    protected static StarRocksSourceFactory createSourceFactory() {
        StarRocksSourceFactory sourceFactory = new StarRocksSourceFactory();
        sourceFactory.loadUrl = "[\"localhost:" + starRocksContainer.getBeStreamLoadPort() + "\"]";
        sourceFactory.userName = "root";
        sourceFactory.dbName = tisDatabase;
        // sourceFactory.password = "";
        sourceFactory.port = starRocksContainer.getFePort();
        sourceFactory.nodeDesc = "localhost";
        return sourceFactory;
    }
}
