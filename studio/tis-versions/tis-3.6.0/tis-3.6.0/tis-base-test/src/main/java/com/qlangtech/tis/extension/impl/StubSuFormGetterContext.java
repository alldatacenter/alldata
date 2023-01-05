package com.qlangtech.tis.extension.impl;

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

import com.google.common.collect.Lists;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.TableNotFoundException;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.test.TISEasyMock;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.easymock.EasyMock;

import java.sql.Types;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-19 12:31
 **/
public class StubSuFormGetterContext {
    private final TISEasyMock easyMock;
    private MockPlugin metaPlugin;
    private UploadPluginMeta param;
    final String id1 = "id1";

    public StubSuFormGetterContext(TISEasyMock easyMock) {
        this.easyMock = easyMock;
        this.initSuForm();
    }

    private void initSuForm() {
        this.metaPlugin = easyMock.mock("metaPlugin", MockPlugin.class);
        this.param = easyMock.mock("param", UploadPluginMeta.class);

        param.putExtraParams(IPropertyType.SubFormFilter.PLUGIN_META_SUBFORM_DETAIL_ID_VALUE, id1);
        EasyMock.expectLastCall().times(1);
        EasyMock.expect(param.getExtraParam(
                IPropertyType.SubFormFilter.PLUGIN_META_SUBFORM_DETAIL_ID_VALUE)).andReturn(id1);

        List<ColumnMetaData> cols = Lists.newArrayList();
        // (int index, String key, DataType type, boolean pk)
        cols.add(new ColumnMetaData(0, "user_id", new com.qlangtech.tis.plugin.ds.DataType(Types.BIGINT), true));
        cols.add(new ColumnMetaData(1, "user_name", new com.qlangtech.tis.plugin.ds.DataType(Types.VARBINARY), false));
        try {
            EasyMock.expect(metaPlugin.getTableMetadata(EntityName.parse(id1))).andReturn(cols);
        } catch (TableNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSuFormGetterContext() {
        SuFormProperties.setSuFormGetterContext(metaPlugin, param, id1);
    }

    private interface MockPlugin extends Describable, DataSourceMeta {

    }
}
