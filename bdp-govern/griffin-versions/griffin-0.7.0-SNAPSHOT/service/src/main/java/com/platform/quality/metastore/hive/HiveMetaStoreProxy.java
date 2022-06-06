/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.platform.quality.metastore.hive;

import javax.annotation.PreDestroy;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HiveMetaStoreProxy {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(HiveMetaStoreProxy.class);

    @Value("hive.metastore.uris")
    private static String uris = "thrift://localhost:9083";

    /**
     * Set attempts and interval for HiveMetastoreClient to retry.
     *
     * @hive.hmshandler.retry.attempts: The number of times to retry a
     * HMSHandler call if there were a connection error
     * .
     * @hive.hmshandler.retry.interval: The time between HMSHandler retry
     * attempts on failure.
     */
    @Value("${hive.hmshandler.retry.attempts}")
    private int attempts;

    @Value("${hive.hmshandler.retry.interval}")
    private String interval;

    //HiveMetaStore的客户端
    private static HiveMetaStoreClient client;

    public HiveMetaStoreClient getHiveMetaStoreClient() {
        if (client != null) {
            return client;
        }
        HiveConf hiveConf = new HiveConf();
        hiveConf.addResource("hive-site.xml");
        hiveConf.set("hive.metastore.uris", uris);
        hiveConf.set("hive.metastore.client.capability.check", "false");
        hiveConf.set("hive.metastore.warehouse.dir", "/user/hive/warehouse");
        hiveConf.set("hive.server2.thrift.client.user", "hive");
        hiveConf.set("hive.server2.thrift.client.password", "r3odWYorGQjsxYTi");
        try {
            //设置hiveMetaStore服务的地址
            client = new HiveMetaStoreClient(hiveConf);
        } catch (MetaException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        }
        return client;
    }

    @PreDestroy
    public void destroy() {
        if (null != client) {
            client.close();
        }
    }
}
