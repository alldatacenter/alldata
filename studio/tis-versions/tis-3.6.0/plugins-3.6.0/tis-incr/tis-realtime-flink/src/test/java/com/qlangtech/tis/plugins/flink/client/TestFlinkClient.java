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

package com.qlangtech.tis.plugins.flink.client;

import com.google.common.collect.Lists;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.RestOptions;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-11 09:23
 **/
public class TestFlinkClient {

    @Test
    public void testSubmitJar() throws Exception {
        Configuration configuration = new Configuration();
        int port = 8081;
        configuration.setString(JobManagerOptions.ADDRESS, "192.168.28.201");
        configuration.setInteger(JobManagerOptions.PORT, port);
        configuration.setInteger(RestOptions.PORT, port);
        RestClusterClient restClient = new RestClusterClient<>(configuration, "my-first-flink-cluster");
//        restClient.setPrintStatusDuringExecution(true);
//        restClient.setDetached(true);

        FlinkClient flinkClient = new FlinkClient();
        // JarLoader jarLoader = EasyMock.createMock("jarLoader", JarLoader.class);

//        File streamJar = new File("/tmp/TopSpeedWindowing.jar");
        File streamJar = new File("/Users/mozhenghua/j2ee_solution/project/plugins/tis-incr/tis-flink-cdc-plugin/target/tis-flink-cdc-plugin.jar");

        Assert.assertTrue("streamJar must be exist", streamJar.exists());
        // EasyMock.expect(jarLoader.downLoad(EasyMock.anyString(), EasyMock.eq(true))).andReturn(streamJar);
        // flinkClient.setJarLoader(jarLoader);

        JarSubmitFlinkRequest request = new JarSubmitFlinkRequest();
        //request.setCache(true);
        request.setDependency(streamJar.getAbsolutePath());
        request.setParallelism(1);
        request.setEntryClass("com.qlangtech.plugins.incr.flink.cdc.test.TISFlinkCDCMysqlSourceFunction");
        List<URL> classPaths = Lists.newArrayList();
        classPaths.add((new File("/Users/mozhenghua/j2ee_solution/project/plugins/tis-incr/tis-elasticsearch7-sink-plugin/target/tis-elasticsearch7-sink-plugin.jar")).toURL());
        classPaths.add((new File("/Users/mozhenghua/j2ee_solution/project/plugins/tis-incr/tis-realtime-flink/target/tis-realtime-flink.jar")).toURL());
        request.setUserClassPaths(classPaths);

        // EasyMock.replay(jarLoader);
        //  AtomicBoolean launchResult = new AtomicBoolean();
        flinkClient.submitJar(restClient, request);

        //assertTrue("launchResult must success", launchResult.get());

        // EasyMock.verify(jarLoader);
    }
}
