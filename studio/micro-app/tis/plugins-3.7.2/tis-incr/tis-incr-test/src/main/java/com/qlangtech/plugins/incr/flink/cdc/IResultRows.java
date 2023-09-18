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

package com.qlangtech.plugins.incr.flink.cdc;

import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-28 17:28
 **/
public interface IResultRows {

    public static void printRow(ResultSet resultSet) throws SQLException {
        StringBuffer rowDesc = new StringBuffer();
        ResultSetMetaData metaData = resultSet.getMetaData();
        String col = null;
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            col = metaData.getColumnName(i);
            Object obj = resultSet.getObject(col);
            rowDesc.append(col).append("=").append(obj).append("[").append((obj != null) ? obj.getClass().getSimpleName() : "").append("]").append(" , ");
        }
        System.out.println("test_output==>" + rowDesc.toString());
    }

    IConsumerHandle getConsumerHandle();


    public default CloseableIterator<Row> getRowSnapshot(String tabName) {
        TableResult tableResult = getSourceTableQueryResult().get(tabName);
        Objects.requireNonNull(tableResult, "tabName:" + tabName + " relevant TableResult can not be null");
        CloseableIterator<Row> collect = tableResult.collect();
        return collect;
    }

    Map<String, TableResult> getSourceTableQueryResult();

    public Object deColFormat(String tableName, String colName, Object val);

    /**
     * 终止flink
     */

    public default void cancel() {
        try {
            for (TableResult tabResult : getSourceTableQueryResult().values()) {
                tabResult.getJobClient().get().cancel().get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
