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

package com.qlangtech.tis.sql.parser.stream.generate;

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-07 18:15
 **/
public class TestStreamComponentCodeGeneratorFlink extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
        CenterResource.setNotFetchFromCenterRepository();
    }

    /**
     * 测试单表增量脚本生成
     *
     * @throws Exception
     */
    public void testSingleTableCodeGenerator() throws Exception {

        //  CoreAction.create
      //  String topologyName = "employees4local";
        String collectionName = "mysql_elastic";


        IAppSource appSource = IAppSource.load(null, collectionName);
        assertTrue(appSource instanceof DataxProcessor);
        DataxProcessor dataXProcessor = (DataxProcessor) appSource;

//        Optional<ERRules> erRule = ERRules.getErRule(topologyName);
//        // 测试针对单表的的topology增量脚本生成
        long timestamp = 20191111115959l;
//        SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(topologyName);
//        assertNotNull(topology);
//        if (!erRule.isPresent()) {
//            ERRules.createDefaultErRule(topology);
//        }

        List<FacadeContext> facadeList = Lists.newArrayList();
        StreamComponentCodeGeneratorFlink streamCodeGenerator
                = new StreamComponentCodeGeneratorFlink(collectionName, timestamp, facadeList, (IBasicAppSource) appSource);
        //EasyMock.replay(streamIncrGenerateStrategy);
        streamCodeGenerator.build();

        TestStreamComponentCodeGenerator
                .assertGenerateContentEqual(timestamp, collectionName, "MysqlElasticListener.scala");
        // EasyMock.verify(streamIncrGenerateStrategy);
    }


}
