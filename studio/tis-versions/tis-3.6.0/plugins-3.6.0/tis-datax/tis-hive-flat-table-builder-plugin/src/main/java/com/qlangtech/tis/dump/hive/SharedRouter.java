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
package com.qlangtech.tis.dump.hive;

// import com.taobao.terminator.hdfs.client.router.SolrCloudPainRouter;
import org.apache.hadoop.hive.ql.exec.UDF;

/* *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年3月30日
 */
public class SharedRouter extends UDF {

    // private static SolrCloudPainRouter cloudPainRouter;
    public String evaluate(final String shardValue, final String collection, final String runtime) {
        // return getRouter(collection, runtime).getShardIndex(shardValue);
        return null;
    }
    // private SolrCloudPainRouter getRouter(String collection, String runtime) {
    // if (cloudPainRouter == null) {
    // synchronized (SharedRouter.class) {
    // if (cloudPainRouter == null) {
    // try {
    // RunEnvironment.setSysRuntime(RunEnvironment.getEnum(runtime));
    //
    // RealtimeTerminatorBeanFactory beanFactory = new RealtimeTerminatorBeanFactory();
    // beanFactory.setServiceName(collection);
    // beanFactory.setJustDump(false);
    //
    // beanFactory.setIncrDumpProvider(new MockHDFSProvider());
    // beanFactory.setFullDumpProvider(new MockHDFSProvider());
    // beanFactory.setGrouprouter(null);
    //
    // beanFactory.afterPropertiesSet();
    //
    // cloudPainRouter = (SolrCloudPainRouter) beanFactory.getGrouprouter();
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // }
    // }
    // }
    // return cloudPainRouter;
    //
    // }
}
