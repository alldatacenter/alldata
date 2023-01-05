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

package com.qlangtech.tis.hdfs.impl;

import com.qlangtech.tis.TIS;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;

import java.net.URL;
import java.util.Enumeration;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-22 11:49
 **/
public class TestJobConfig {
    @Test
    public void testJobConfCreate() throws Exception {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("org/apache/xerces/jaxp/DocumentBuilderFactoryImpl.class");

        while (resources.hasMoreElements()) {
            System.out.println(resources.nextElement());
        }

        org.apache.hadoop.conf.Configuration cfg = new org.apache.hadoop.conf.Configuration();
        cfg.setClassLoader(TIS.get().getPluginManager().uberClassLoader);

        org.apache.hadoop.mapred.JobConf conf = new JobConf(cfg);

        System.out.println(conf);

    }
}
