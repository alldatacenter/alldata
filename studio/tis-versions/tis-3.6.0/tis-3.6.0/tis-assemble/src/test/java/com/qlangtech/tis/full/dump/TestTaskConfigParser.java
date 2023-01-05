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
package com.qlangtech.tis.full.dump;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.qlangtech.tis.fullbuild.taskflow.ITask;
import com.qlangtech.tis.fullbuild.taskflow.WorkflowTaskConfigParser.ProcessTask;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年1月4日 上午10:54:56
 */
public class TestTaskConfigParser extends TestCase {
    // public void test() throws Exception {
    // // HiveTaskFactory hiveTaskFactory = new HiveTaskFactory();
    // TaskConfigParser parser = TaskConfigParser.getInstance();// (hiveTaskFactory);
    // final AtomicInteger taskCount = new AtomicInteger();
    // parser.traverseTask("search4totalpay", new ProcessTask() {
    // public void process(ITask task) {
    // System.out.println(task.getName());
    // taskCount.incrementAndGet();
    // }
    // });
    //
    // Assert.assertEquals(20, taskCount.get());
    // }
    // public void testParseTask() throws Exception {
    // // HiveTaskFactory hiveTaskFactory = new HiveTaskFactory();
    // TaskConfigParser parser = TaskConfigParser.getInstance();// (hiveTaskFactory);
    // String task = "<execute partitionSaveCount=\"1\">\n" +
    // "    <unionTask name=\"task1\" table_name=\"union_tab_name\" partition=\"pt,pmod\">\n" +
    // "        <subTab>\n" +
    // "            select id, goods_id, entity_id, is_valid, last_ver,\n" +
    // "            supplier_id, create_time, 'supplier_goods' AS table_name,\n" +
    // "            pt, '0' AS pmod\n" +
    // "            FROM union_supplier_goods WHERE is_valid=1\n" +
    // "        </subTab>\n" +
    // "\n" +
    // "        <subTab>\n" +
    // "            select id, goods_id, entity_id, self_entity_id, is_valid, last_ver,\n" +
    // "            warehouse_id, create_time, 'warehouse_goods' AS table_name,\n" +
    // "            pt, '0' AS pmod\n" +
    // "            FROM union_warehouse_goods WHERE is_valid=1\n" +
    // "        </subTab>\n" +
    // "\n" +
    // "        <subTab>\n" +
    // "            select id, goods_id, entity_id, self_entity_id, is_valid, last_ver,\n" +
    // "            warehouse_id, op_time, 'stock_change_log' AS table_name,\n" +
    // "            pt, '0' AS pmod\n" +
    // "            FROM union_stock_change_log WHERE is_valid=1\n" +
    // "        </subTab>\n" +
    // "\n" +
    // "        <subTab>\n" +
    // "            select id, goods_id, entity_id, is_valid, last_ver,\n" +
    // "            create_time, 'goods_sale_allow' AS table_name,\n" +
    // "            pt, '0' AS pmod\n" +
    // "            FROM union_goods_sale_allow WHERE is_valid=1\n" +
    // "        </subTab>\n" +
    // "\n" +
    // "        <subTab>\n" +
    // "            select id, goods_id, entity_id, is_valid, last_ver,\n" +
    // "            create_time, 'provide_goods' AS table_name,\n" +
    // "            pt, '0' AS pmod\n" +
    // "            FROM union_provide_goods WHERE is_valid=1\n" +
    // "        </subTab>\n" +
    // "    </unionTask>\n" +
    // "</execute>";
    // List<ITask> list = parser.parseTask(new ByteArrayInputStream(task.getBytes()));
    // int i = 1;
    // }
}
