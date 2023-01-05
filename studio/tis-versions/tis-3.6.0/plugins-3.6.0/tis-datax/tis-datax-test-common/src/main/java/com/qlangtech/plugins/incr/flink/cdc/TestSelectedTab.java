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

import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-28 16:23
 **/
public class TestSelectedTab extends SelectedTab {
    final List<CMeta> colsMeta;

    public static SelectedTab createSelectedTab(EntityName tabName, BasicDataSourceFactory dataSourceFactory) {
        return createSelectedTab(tabName, dataSourceFactory, (t) -> {
        });
    }

    public static SelectedTab createSelectedTab(EntityName tabName //
            , BasicDataSourceFactory dataSourceFactory //
            , Consumer<SelectedTab> baseTabSetter) {
        List<ColumnMetaData> tableMetadata = dataSourceFactory.getTableMetadata(tabName);
        if (CollectionUtils.isEmpty(tableMetadata)) {
            throw new IllegalStateException("tabName:" + tabName + " relevant can not be empty");
        }
        List<CMeta> colsMeta = tableMetadata.stream().map((col) -> {
            CMeta c = new CMeta();
            c.setPk(col.isPk());
            c.setName(col.getName());
            c.setNullable(col.isNullable());
            c.setType(col.getType());
            c.setComment(col.getComment());
            return c;
        }).collect(Collectors.toList());
        SelectedTab baseTab = new TestSelectedTab(tabName.getTableName(), colsMeta);
        baseTab.setCols(tableMetadata.stream().map((m) -> m.getName()).collect(Collectors.toList()));
        baseTabSetter.accept(baseTab);

        return baseTab;
    }

    public TestSelectedTab(String name, List<CMeta> colsMeta) {
        super(name);
        this.colsMeta = colsMeta;
    }

    @Override
    public List<CMeta> getCols() {
        return colsMeta;
    }
}
