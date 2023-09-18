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

package com.qlangtech.tis.plugin.ds.tidb;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.*;
import junit.framework.TestCase;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: baisui 百岁
 * @create: 2020-11-24 17:57
 **/
public class TestTiKVDataSourceFactory extends TestCase {
    // public static final String DB_NAME = "employees";
    public static final String DB_NAME = "order2";

    public static final String TABLE_NAME = "employees";


    private static final String COL_EMP_NO = "emp_no";
    private static final String COL_BIRTH_DATE = "birth_date";
    private static final String COL_FIRST_NAME = "first_name";
    private static final String COL_LAST_NAME = "last_name";
    private static final String COL_GENDER = "gender";
    private static final String COL_HIRE_DATE = "hire_date";

    public void testColMetadata() {

        validateColumnMeta(true);
        validateColumnMeta(false);
    }

    public void testColMetaParse() {
        GetColsMeta getColsMeta = new GetColsMeta().invoke();
        List<ColumnMetaData> table1Cols = getColsMeta.getColsMeta("table1");
        DataType type = null;
        for (ColumnMetaData col : table1Cols) {
            type = col.getType();
            System.out.println(col.getKey() + ",getDecimalDigits:" + type.getDecimalDigits() + ",columnSize:" + type.columnSize + ",type:" + type.type);
        }
    }

    public static List<SelectedTab> createTabOfEmployees() {
        List<SelectedTab> selectedTabs = Lists.newArrayList();
        SelectedTab selectedTab = new SelectedTab();
        selectedTab.setCols(Lists.newArrayList(COL_EMP_NO, COL_BIRTH_DATE, COL_FIRST_NAME, COL_LAST_NAME, COL_GENDER, COL_HIRE_DATE));
        selectedTab.name = TABLE_NAME;
        selectedTabs.add(selectedTab);
        return selectedTabs;
    }

    private void validateColumnMeta(boolean datetimeFormat) {
        GetColsMeta getColsMeta = new GetColsMeta().invoke(datetimeFormat);

        List<ColumnMetaData> employeesCols = getColsMeta.getEmployeesCols();
        assertNotNull(employeesCols);
        assertEquals(6, employeesCols.size());
        ColumnMetaData pk = null;
        Map<String, Integer> colTypes = Maps.newHashMap();
        colTypes.put(COL_EMP_NO, Types.BIGINT);
        colTypes.put(COL_BIRTH_DATE, datetimeFormat ? Types.DATE : Types.INTEGER);
        colTypes.put(COL_FIRST_NAME, Types.VARCHAR);
        colTypes.put(COL_LAST_NAME, Types.VARCHAR);
        colTypes.put(COL_GENDER, Types.VARCHAR);
        colTypes.put(COL_HIRE_DATE, datetimeFormat ? Types.DATE : Types.INTEGER);
        Integer colType = null;
        for (ColumnMetaData cmeta : employeesCols) {
            if (cmeta.isPk()) {
                pk = cmeta;
            }
            colType = colTypes.get(cmeta.getKey());
            assertNotNull(colType);
            assertEquals(cmeta.getKey(), (int) colType, cmeta.getType());

            System.out.println(cmeta.getIndex() + ":" + cmeta.getKey() + ":" + cmeta.getType());
        }
        assertNotNull(pk);
        assertEquals("emp_no", pk.getKey());
    }

    public void testGetPlugin() {

        GetColsMeta getColsMeta = new GetColsMeta().invoke();
        TiKVDataSourceFactory dataSourceFactory = getColsMeta.getDataSourceFactory();
        List<ColumnMetaData> employeesCols = getColsMeta.getEmployeesCols();


        TISTable dumpTable = new TISTable();
        dumpTable.setDbName(DB_NAME);
        dumpTable.setTableName(TABLE_NAME);
        dumpTable.setReflectCols(employeesCols);

        DataDumpers dataDumpers = dataSourceFactory.getDataDumpers(dumpTable);

        assertEquals(1, dataDumpers.splitCount);

        Iterator<IDataSourceDumper> dumpers = dataDumpers.dumpers;
        Map<String, Object> row = null;
        StringBuffer rowContent = null;
        int rowCount = 0;
        while (dumpers.hasNext()) {
            IDataSourceDumper dumper = dumpers.next();
//            assertEquals(300024, );
            assertTrue(dumper.getRowSize() > 0);
            try {
                Iterator<Map<String, Object>> rowIterator = dumper.startDump();

                while (rowIterator.hasNext()) {
                    rowContent = new StringBuffer();
                    row = rowIterator.next();
//                    if ("251149".equals(row.get("emp_no"))) {
//                        System.out.println("==========="+row.get("emp_no"));
//                    }
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        rowContent.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
                    }
                    rowCount++;
                    System.out.println(rowContent);
                }
            } finally {
                dumper.closeResource();
            }
        }
        assertEquals(300024, rowCount);
    }

}
