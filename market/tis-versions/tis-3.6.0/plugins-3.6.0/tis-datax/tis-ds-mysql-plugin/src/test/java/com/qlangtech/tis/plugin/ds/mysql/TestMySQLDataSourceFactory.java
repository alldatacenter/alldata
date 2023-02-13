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

package com.qlangtech.tis.plugin.ds.mysql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.*;

import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2020-11-24 17:42
 **/
public class TestMySQLDataSourceFactory  //extends TestCase
{

    private static final String DB_ORDER = "order1";

    static {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
    }

    private static final String empNo = "emp_no";

    @Test
    public void testGetPlugin() throws Exception {

        DataSourceFactoryPluginStore dbPluginStore = TIS.getDataBasePluginStore(new PostedDSProp(DB_ORDER));

        DataSourceFactory dataSourceFactory = dbPluginStore.getPlugin();
        assertNotNull(dataSourceFactory);

        List<ColumnMetaData> cols = dataSourceFactory.getTableMetadata(EntityName.parse("totalpayinfo"));
        for (ColumnMetaData col : cols) {
            System.out.println(col.getKey() + " " + col.getType());
        }

        FacadeDataSource datasource = dbPluginStore.createFacadeDataSource();
        assertNotNull(datasource);


        cols = dataSourceFactory.getTableMetadata(EntityName.parse("base"));
        for (ColumnMetaData col : cols) {
            System.out.println(col.getKey() + " " + col.getType());
        }

//        List<Descriptor<DataSourceFactory>> descList
//                = TIS.get().getDescriptorList(DataSourceFactory.class);
//        assertNotNull(descList);
//        assertEquals(1, descList.size());


//        Descriptor<DataSourceFactory> mysqlDS = descList.get(0);
//
//        mysqlDS.validate()
    }


    @Test
    public void testDataDumpers() throws Exception {
        MySQLDataSourceFactory dataSourceFactory = new MySQLDataSourceFactory() {
//            @Override
//            protected Connection getConnection(String jdbcUrl, String username, String password) throws SQLException {
//                throw new UnsupportedOperationException();
//            }
        };
        dataSourceFactory.dbName = "employees";
        dataSourceFactory.password = null;
        dataSourceFactory.userName = "root";
        dataSourceFactory.nodeDesc = "192.168.28.202";
        dataSourceFactory.port = 4000;
        dataSourceFactory.encode = "utf8";
        TISTable dumpTable = new TISTable();
        dumpTable.setSelectSql("SELECT emp_no,birth_date,first_name,last_name,gender,hire_date FROM employees");
        dumpTable.setTableName("employees");
        DataDumpers dataDumpers = dataSourceFactory.getDataDumpers(dumpTable);
        assertNotNull("dataDumpers can not be null", dataDumpers);

        assertEquals(1, dataDumpers.splitCount);
        Iterator<IDataSourceDumper> dumpers = dataDumpers.dumpers;
        Map<String, Object> row = null;
        int testRows = 0;

        List<Map<String, String>> exampleRows = getExampleRows();

        while (dumpers.hasNext()) {
            IDataSourceDumper dumper = null;
            try {
                dumper = dumpers.next();
                assertEquals(300024, dumper.getRowSize());
                Iterator<Map<String, Object>> mapIterator = dumper.startDump();
                assertNotNull(mapIterator);
                while (mapIterator.hasNext()) {
                    row = mapIterator.next();
                    assertTrue(empNo + " can not empty", row.get(empNo) != null);
                    Map<String, String> exampleRow = exampleRows.get(testRows);
                    for (Map.Entry<String, String> entry : exampleRow.entrySet()) {
                        assertEquals(entry.getKey() + " shall be equal", entry.getValue(), row.get(entry.getKey()));
                    }
                    if (++testRows >= exampleRows.size()) {
                        break;
                    }
                }
            } finally {
                dumper.closeResource();
            }
        }
    }

    private List<Map<String, String>> getExampleRows() throws Exception {
        List<Map<String, String>> result = Lists.newArrayList();
        Map<String, String> r = null;
        List<String> titles = null;
        String[] row = null;
        int colIndex;
        try (InputStream input = TestMySQLDataSourceFactory.class.getResourceAsStream("employees_pre_20_rows.txt")) {
            LineIterator li = IOUtils.lineIterator(input, TisUTF8.get());
            li.hasNext();
            li.next();
            li.hasNext();
            titles = Arrays.stream(StringUtils.split(li.nextLine(), "|")).map((t) -> StringUtils.trimToEmpty(t)).collect(Collectors.toList());
            li.hasNext();
            li.next();
            while (li.hasNext()) {
                row = StringUtils.split(li.next(), "|");
                r = Maps.newHashMap();
                colIndex = 0;
                for (String title : titles) {
                    r.put(title, StringUtils.trimToNull(row[colIndex++]));
                }
                result.add(r);
            }
        }
        return result;
    }

}
