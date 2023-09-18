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

package com.qlangtech.tis.plugin.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.TableNotFoundException;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.UploadPluginMeta;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-24 15:06
 **/
public class TestSelectedTab extends TestCase {

    static final String tabName = "order";

    public void testGetContextTableCols() {
        DataSourceMetaPlugin sourceMetaPlugin = new DataSourceMetaPlugin();

        UploadPluginMeta pluginMeta = UploadPluginMeta.parse("test");

        SuFormProperties.setSuFormGetterContext(sourceMetaPlugin, pluginMeta, tabName);

        List<Option> result = SelectedTab.getContextTableCols((cols) -> {
            return cols.stream();
        });
        Assert.assertEquals(2, result.size());
    }


    public static class DataSourceMetaPlugin
            implements Describable<DataSourceMetaPlugin>, DataSourceMeta {
        public List<ColumnMetaData> getTableMetadata(EntityName table) throws TableNotFoundException {
            Assert.assertEquals(tabName, table.getTableName());
            List<ColumnMetaData> cols = Lists.newArrayList();
            //  int index, String key, DataType type, boolean pk
            ColumnMetaData col = new ColumnMetaData(0, "order_id", DataType.createVarChar(10), true);
            cols.add(col);

            col = new ColumnMetaData(1, "name", DataType.createVarChar(10), false);
            cols.add(col);
            return cols;
        }

        @Override
        public void refresh() {

        }
    }
}
