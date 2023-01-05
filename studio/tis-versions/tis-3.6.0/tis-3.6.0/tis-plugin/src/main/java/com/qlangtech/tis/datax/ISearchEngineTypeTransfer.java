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

package com.qlangtech.tis.datax;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.solrdao.ISchema;
import com.qlangtech.tis.solrdao.SchemaMetaContent;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.lang.StringUtils;

import java.util.function.Consumer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-13 18:00
 **/
public interface ISearchEngineTypeTransfer {

    public static ISearchEngineTypeTransfer load(IPluginContext context, String dataXName) {
        DataxWriter dataxWriter = DataxWriter.load(context, dataXName);
        if (!(dataxWriter instanceof ISearchEngineTypeTransfer)) {
            throw new IllegalStateException("dataxWriter must be type of " + ISearchEngineTypeTransfer.class.getSimpleName()
                    + " but now is " + dataxWriter.getClass().getName());
        }
        return (ISearchEngineTypeTransfer) dataxWriter;
    }

    static JSONObject getOriginExpertSchema(String schemaXmlContent) {
        return JSON.parseObject(StringUtils.defaultIfEmpty(schemaXmlContent, "{\"column\":[]}"));
    }

    /**
     * 在elasticsearch中的索引名称
     *
     * @return
     */
    public String getIndexName();


    /**
     * 初始化Schema内容
     *
     * @param tab
     * @return
     */
    public SchemaMetaContent initSchemaMetaContent(ISelectedTab tab);

    public ISchema projectionFromExpertModel(TableAlias tableAlias, Consumer<byte[]> schemaContentConsumer);

    public ISchema projectionFromExpertModel(JSONObject body);

    public JSONObject mergeFromStupidModel(ISchema schema, JSONObject expertSchema);
}
