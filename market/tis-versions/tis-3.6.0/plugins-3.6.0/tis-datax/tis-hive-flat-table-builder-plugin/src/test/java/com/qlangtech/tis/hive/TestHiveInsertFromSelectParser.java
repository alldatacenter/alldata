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

package com.qlangtech.tis.hive;

import com.qlangtech.tis.manage.common.TisUTF8;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.List;

/**
 * @author: baisui 百岁
 * @create: 2020-06-03 13:57
 **/
public class TestHiveInsertFromSelectParser extends TestCase {

    public void testSqlParse() throws Exception {
        HiveInsertFromSelectParser parse = new HiveInsertFromSelectParser();

        try (InputStream input = TestHiveInsertFromSelectParser.class.getResourceAsStream("tmp_pay.sql")) {
            parse.start(IOUtils.toString(input, TisUTF8.get()));
            System.out.println("getTargetTableName:" + parse.getTargetTableName());
            List<HiveColumn> columns = parse.getColsExcludePartitionCols();
            assertEquals(5, columns.size());
            assertEquals("totalpay_id totalpay_id", columns.get(0).toString());
            assertEquals("kindpay kindpay", columns.get(1).toString());
            assertEquals("fee fee", columns.get(2).toString());
            assertEquals("is_enterprise_card_pay is_enterprise_card_pay", columns.get(3).toString());
            assertEquals("pay_customer_ids pay_customer_ids", columns.get(4).toString());

            for (HiveColumn c : columns) {
                System.out.println(c.getName());
            }
        }


    }
}
