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

package com.qlangtech.tis.hive;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * @author: baisui 百岁
 * @create: 2020-06-03 13:57
 **/
public class TestHiveInsertFromSelectParser extends TestCase {
    private static ColumnMetaData createCol(int index, String name, DataType type) {
        ColumnMetaData col = new ColumnMetaData(index, name, type, false, true);
        return col;
    }

    public void testSqlParse() throws Exception {

        List<ColumnMetaData> cols = createPayInfoCols();


        ITabPartition pt = () -> TimeFormat.yyyyMMddHHmmss.format(TimeFormat.getCurrentTimeStamp());
        Map<IDumpTable, ITabPartition> ps = Maps.newHashMap();
        ps.put(EntityName.create("order", "payinfo"), pt);
        TabPartitions tabPartition = new TabPartitions(ps);

        try (InputStream input = TestHiveInsertFromSelectParser.class.getResourceAsStream("tmp_pay.sql")) {

            HiveInsertFromSelectParser parse = new HiveInsertFromSelectParser(IOUtils.toString(input, TisUTF8.get()), (sql) -> {
                return cols;
            });

//            parse.start(IOUtils.toString(input, TisUTF8.get()), tabPartition, (sql) -> {
//                return cols;
//            });
            // parse.start("select p.id from  orderrrr.payinfo p");
            System.out.println("getTargetTableName:" + parse.getTargetTableName());
            parse.reflectColsType();
            List<HiveColumn> columns = parse.getColsExcludePartitionCols();
            assertEquals(5, columns.size());

            int colIndex = 0;
            for (ColumnMetaData col : cols) {
                HiveColumn hiveColumn = columns.get(colIndex++);
                assertEquals(col.getKey(), hiveColumn.getName());
                assertEquals("type of " + col.getKey(), col.getType().accept(HiveColumn.hiveTypeVisitor), hiveColumn.getDataType());
            }


//            assertEquals("kindpay kindpay", columns.get(1).toString());
//            assertEquals("fee fee", columns.get(2).toString());
//            assertEquals("is_enterprise_card_pay is_enterprise_card_pay", columns.get(3).toString());
//            assertEquals("pay_customer_ids pay_customer_ids", columns.get(4).toString());

            for (HiveColumn c : columns) {
                System.out.println(c.getName());
            }
        }


    }

    public  static List<ColumnMetaData> createPayInfoCols() {
        int index = 0;
        return Lists.newArrayList(
                createCol(index++, "totalpay_id", DataType.createVarChar(32)),
                createCol(index++, "kindpay", new DataType(Types.INTEGER)),
                createCol(index++, "fee", new DataType(Types.DECIMAL,"decimal",20)),
                createCol(index++, "is_enterprise_card_pay", new DataType(Types.BIT)),
                createCol(index++, "pay_customer_ids", DataType.createVarChar(256)));
    }
}
