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
package com.qlangtech.tis.offline.module.manager.impl;

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import junit.framework.TestCase;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年9月12日
 */
public class OfflineManagerTest extends TestCase {

    // @Override
    // protected void runTest() throws Throwable {
    //
    // super.runTest();
    // }
    private OfflineManager offline;

    @Override
    protected void setUp() throws Exception {
        this.offline = new OfflineManager();
    // super.setUp();
    }

  //  public void testGetTables() throws Exception {
//        List<String> tabs = this.offline.getTables("order");
//        assertNotNull(tabs.size());
//        for (String tab : tabs) {
//            System.out.println(tab);
//        }
   // }

//    public void testGetColumn() throws Exception {
//        List<ColumnMetaData> list = offline.getTableMetadata("order", "totalpayinfo");
//        assertTrue(list.size() > 0);
//        for (ColumnMetaData c : list) {
//            System.out.println(c.getKey());
//        }
//    // List<String> tabs = this.offline.getTables("order");
//    // Assert.assertNotNull(tabs.size());
//    // for (String tab : tabs) {
//    // System.out.println(tab);
//    // }
//    }
}
