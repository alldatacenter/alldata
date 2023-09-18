package com.qlangtech.tis.datax;

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

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.plugin.ds.TableNotFoundException;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-03 19:49
 **/
public class AdapterDataxReader implements IDataxReader {
    private final IDataxReader reader;

    public AdapterDataxReader(IDataxReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasMulitTable() {
        return reader.hasMulitTable();
    }

    @Override
    public <T extends ISelectedTab> List<T> getSelectedTabs() {
        return reader.getSelectedTabs();
    }

    @Override
    public IGroupChildTaskIterator getSubTasks() {
        return reader.getSubTasks();
    }

    @Override
    public IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {
        return reader.getSubTasks(filter);
    }

    @Override
    public String getTemplate() {
        return reader.getTemplate();
    }

    @Override
    public Optional<String> getEscapeChar() {
        return reader.getEscapeChar();
    }

    @Override
    public TableInDB getTablesInDB() {
        return reader.getTablesInDB();
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(boolean inSink, EntityName table) throws TableNotFoundException {
        return reader.getTableMetadata(false, table);
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(JDBCConnection conn, boolean inSink, EntityName table) throws TableNotFoundException {
        return reader.getTableMetadata(conn, false, table);
    }

    @Override
    public void refresh() {
        reader.refresh();
    }

    @Override
    public DataXMeta getDataxMeta() {
        return reader.getDataxMeta();
    }

    @Override
    public Class<?> getOwnerClass() {
        return reader.getOwnerClass();
    }

    @Override
    public IStreamTableMeta getStreamTableMeta(String tableName) {
        return reader.getStreamTableMeta(tableName);
    }
}
