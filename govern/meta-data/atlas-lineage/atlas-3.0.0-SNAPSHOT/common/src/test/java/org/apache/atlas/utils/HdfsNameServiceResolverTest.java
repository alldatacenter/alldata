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
package org.apache.atlas.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HdfsNameServiceResolverTest {

    @Test
    public void testResolution() {
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("test"), "");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("test1"), "");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("test", 8020), "");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("test1", 8020), "");
//
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("ctr-e137-1514896590304-41888-01-000003"), "mycluster");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("ctr-e137-1514896590304-41888-01-000003", 8020), "mycluster");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("ctr-e137-1514896590304-41888-01-000004"), "mycluster");
//        assertEquals(HdfsNameServiceResolver.getNameServiceID("ctr-e137-1514896590304-41888-01-000004", 8020), "mycluster");

        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://ctr-e137-1514896590304-41888-01-000004:8020/tmp/xyz"), "hdfs://mycluster/tmp/xyz");
        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://ctr-e137-1514896590304-41888-01-000004:8020/tmp/xyz/ctr-e137-1514896590304-41888-01-000004:8020"), "hdfs://mycluster/tmp/xyz/ctr-e137-1514896590304-41888-01-000004:8020");
        assertEquals(HdfsNameServiceResolver.getNameServiceIDForPath("hdfs://ctr-e137-1514896590304-41888-01-000004:8020/tmp/xyz"), "mycluster");

        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://ctr-e137-1514896590304-41888-01-000003:8020/tmp/xyz"), "hdfs://mycluster/tmp/xyz");
        assertEquals(HdfsNameServiceResolver.getNameServiceIDForPath("hdfs://ctr-e137-1514896590304-41888-01-000003:8020/tmp/xyz"), "mycluster");

        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://ctr-e137-1514896590304-41888-01-000003/tmp/xyz"), "hdfs://mycluster/tmp/xyz");
        assertEquals(HdfsNameServiceResolver.getNameServiceIDForPath("hdfs://ctr-e137-1514896590304-41888-01-000003/tmp/xyz"), "mycluster");

        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://ctr-e137-1514896590304-41888-01-000003/tmp/xyz/ctr-e137-1514896590304-41888-01-000003"), "hdfs://mycluster/tmp/xyz/ctr-e137-1514896590304-41888-01-000003");
        assertEquals(HdfsNameServiceResolver.getNameServiceIDForPath("hdfs://ctr-e137-1514896590304-41888-01-000003/tmp/xyz/ctr-e137-1514896590304-41888-01-000003"), "mycluster");

        assertEquals(HdfsNameServiceResolver.getPathWithNameServiceID("hdfs://mycluster/tmp/xyz"), "hdfs://mycluster/tmp/xyz");
        assertEquals(HdfsNameServiceResolver.getNameServiceIDForPath("hdfs://mycluster/tmp/xyz"), "mycluster");

    }
}