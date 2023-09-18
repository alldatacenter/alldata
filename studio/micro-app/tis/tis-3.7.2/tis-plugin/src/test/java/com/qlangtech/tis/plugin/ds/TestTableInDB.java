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

package com.qlangtech.tis.plugin.ds;

import com.qlangtech.tis.BasicTestCase;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.DataXJobInfo;
import com.qlangtech.tis.datax.DataXJobSubmit;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-30 15:12
 **/
public class TestTableInDB extends BasicTestCase {

    /**
     * 测试默认无分表下的执行逻辑
     */
    public void testDftTableInDB() {
        String jdbcUrl = "jdbcUrl";
        String testTabOrder = "orderdetail";
        DBIdentity order2 = DBIdentity.parseId("order2");
        TableInDB tableInDB = TableInDB.create(order2);
        tableInDB.add(jdbcUrl, testTabOrder);

        DataXJobSubmit.TableDataXEntity tableEntity = DataXJobSubmit.TableDataXEntity.createTableEntity(null, jdbcUrl, testTabOrder);

        DataXJobInfo infoJob = tableInDB.createDataXJobInfo(tableEntity);

        Assert.assertNotNull("infoJob can not be null", infoJob);

        Optional<String[]> targetTableNames = infoJob.getTargetTableNames();
        Assert.assertTrue(targetTableNames.isPresent());

        String[] tabs = targetTableNames.get();


        Assert.assertEquals(1, tabs.length);
        Assert.assertEquals(testTabOrder, tabs[0]);
    }
}
