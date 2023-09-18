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

package com.qlangtech.tis.plugin.ds.split;

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.DataXJobInfo;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.SplitTableStrategy;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.test.TISEasyMock;
import org.apache.commons.collections.CollectionUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-24 15:31
 **/
public class TestDefaultSplitTableStrategy implements TISEasyMock {

    private final String sourceTableName = "base";

    private static final List<String> splitTabsBase = Lists.newArrayList("base_01", "base", "base_02");
    private static final List<String> splitTabs;

    static {
        splitTabs = Lists.newArrayList("order");
        splitTabs.addAll(splitTabsBase);
    }


    @Test
    public void testTabAggre() {
        // final String jdbcUrl = "jdbc_url_1";
        DBIdentity dbId = DBIdentity.parseId("order2");
        final String dataXCfgFileName = "base_0.json";
        DefaultSplitTableStrategy splitTableStrategy = new DefaultSplitTableStrategy();
        TableInDB tableInDB = splitTableStrategy.createTableInDB(dbId);

        for (String tab : splitTabs) {
            tableInDB.add(DataXJobSubmit.TableDataXEntity.TEST_JDBC_URL, tab);
        }

        DataXJobInfo baseJobInfo
                = tableInDB.createDataXJobInfo(DataXJobSubmit.TableDataXEntity.createTableEntity4Test(dataXCfgFileName, sourceTableName));
        Optional<String[]> targetTableNames = baseJobInfo.getTargetTableNames();

        Assert.assertTrue(targetTableNames.isPresent());
        String[] baseTabs = targetTableNames.get();
        Assert.assertEquals(String.join(",", baseTabs), 3, baseTabs.length);
    }

    @Test
    public void testGetAllPhysicsTabs() {

        String jdbcUrl = "jdbc:mysql://192.168.28.200:3306/order1?useUnicode=yes&useCursorFetch=true&useSSL=false&serverTimezone=Asia%2FShanghai&useCompression=true&characterEncoding=utf8";


        DefaultSplitTableStrategy splitTableStrategy = new DefaultSplitTableStrategy();

        DataSourceFactory dsFactory = mock("dsFactory", DataSourceFactory.class);

        DefaultSplitTableStrategy.SplitableTableInDB tabsInDB
                = new DefaultSplitTableStrategy.SplitableTableInDB(dsFactory, SplitTableStrategy.PATTERN_PHYSICS_TABLE);

        for (String base : splitTabsBase) {
            tabsInDB.add(jdbcUrl, base);
        }
      //  dsFactory.refresh();
        EasyMock.expect(dsFactory.getTablesInDB()).andReturn(tabsInDB);


        replay();

        List<String> allPhysicsTabs = splitTableStrategy.getAllPhysicsTabs(dsFactory, jdbcUrl, sourceTableName);
        verifyAll();
        Assert.assertTrue(CollectionUtils.isEqualCollection(splitTabsBase, allPhysicsTabs));


    }
}
