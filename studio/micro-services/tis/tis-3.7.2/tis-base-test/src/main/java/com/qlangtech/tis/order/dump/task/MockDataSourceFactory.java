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
package com.qlangtech.tis.order.dump.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-03 14:24
 */
public class MockDataSourceFactory extends DataSourceFactory implements ITestDumpCommon {

    protected Connection getConnection(String jdbcUrl, String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, StringUtils.trimToNull(username), StringUtils.trimToNull(password));
    }


    @Override
    public DBConfig getDbConfig() {
        throw new IllegalStateException();
    }

    @Override
    public void visitFirstConnection(IConnProcessor connProcessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {

    }
//    @Override
//    public void refectTableInDB(TableInDB tabs, Connection conn) throws SQLException {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public String identityValue() {
        return "mockDs";
    }

    /**
     * 模拟Employee表的导入
     *
     * @return
     * @throws Exception
     */
    public static MockDataSourceFactory getMockEmployeesDataSource() throws Exception {
        TestEmployeeDataSourceDumper dumper = new TestEmployeeDataSourceDumper(DB_EMPLOYEES, TABLE_EMPLOYEES);

        MockDataSourceFactory sourceFactory = new MockDataSourceFactory(dumper);

        return sourceFactory;
    }

    private final TestEmployeeDataSourceDumper dumper;

    private MockDataSourceFactory(TestEmployeeDataSourceDumper dumper) {
        this.dumper = dumper;
    }


    @Override
    public TableInDB getTablesInDB() {
        throw new UnsupportedOperationException();
    }



    @Override
    public List<ColumnMetaData> getTableMetadata(boolean inSink,EntityName table) {
        if (!StringUtils.equals(dumper.testTableName, table.getTableName())) {
            Assert.fail("dumper.testTableName:" + dumper.testTableName + " must equal with:" + table);
        }
        return dumper.getMetaData();
    }

    @Override
    public DataDumpers getDataDumpers(TISTable table) {

        if (!StringUtils.equals(dumper.testDBName, table.getDbName())) {
            Assert.fail("dumper.testDBName:" + dumper.testDBName + " must equal with:" + table.getDbName());
        }

        List<IDataSourceDumper> dumpList = Lists.newArrayList(dumper);
        DataDumpers dumpers = new DataDumpers(1, dumpList.iterator());
        return dumpers;
    }

    private static class TestEmployeeDataSourceDumper implements IDataSourceDumper {
        private List<ColumnMetaData> colsMeta;
        private List<Map<String, Object>> dumpData;
        public final String testDBName;
        public final String testTableName;


        TestEmployeeDataSourceDumper(String testDBName, String tableName) throws IOException {
            this.testDBName = testDBName;
            this.testTableName = tableName;
            this.colsMeta = Lists.newArrayList();
            List<String> lines = null;
            try (InputStream input = TestEmployeeDataSourceDumper.class.getResourceAsStream("employee_data_source.txt")) {
                Assert.assertNotNull(input);
                lines = IOUtils.readLines(input, TisUTF8.get());
                String[] titles = StringUtils.split(lines.get(1), "|");
                ColumnMetaData cm = null;
                for (int i = 0; i < titles.length; i++) {
                    //int index, String key, int type, boolean pk
                    cm = new ColumnMetaData(i, StringUtils.trimToEmpty(titles[i]), new DataType(Types.VARCHAR), false);
                    this.colsMeta.add(cm);
                }
            }
            String line = null;
            dumpData = Lists.newArrayList();
            Map<String, Object> row = null;
            String[] vals = null;
            for (int i = 3; i < lines.size(); i++) {
                row = Maps.newHashMap();
                line = lines.get(i);
                vals = StringUtils.split(line, "|");
                for (int titleIndex = 0; titleIndex < colsMeta.size(); titleIndex++) {
                    row.put(colsMeta.get(titleIndex).getKey(), StringUtils.trimToNull(vals[titleIndex]));
                }
                dumpData.add(row);
            }

            Assert.assertTrue(dumpData.size() > 0);
        }

        public static void main(String[] args) throws Exception {
            //  TestEmployeeDataSourceDumper d = new TestEmployeeDataSourceDumper();
        }

        @Override
        public void closeResource() {

        }

        @Override
        public int getRowSize() {
            return dumpData.size();
        }

        @Override
        public List<ColumnMetaData> getMetaData() {
            return this.colsMeta;
        }

        @Override
        public Iterator<Map<String, Object>> startDump() {
            return dumpData.iterator();
        }

        @Override
        public String getDbHost() {
            return "getDbHost";
        }
    }
}
