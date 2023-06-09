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

package com.qlangtech.tis.plugin.datax.doris;

import com.alibaba.datax.plugin.writer.doriswriter.Keys;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.datax.common.RdbmsWriterContext;
import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-07 09:58
 **/
public class DorisWriterContext extends RdbmsWriterContext<DataXDorisWriter, DorisSourceFactory> {
    private final DorisSelectedTab dorisTab;

    public DorisWriterContext(DataXDorisWriter writer, IDataxProcessor.TableMap tabMapper) {
        super(writer, tabMapper);
        this.dorisTab = (DorisSelectedTab) tabMapper.getSourceTab();
    }

    public String getDataXName() {
        return plugin.dataXName;
    }

    public String getDatabase() {
        return this.dsFactory.dbName;
    }

    public String getLoadUrl() {
        return this.dsFactory.loadUrl;
    }

    public boolean isContainLoadProps() {
        return StringUtils.isNotBlank(this.plugin.loadProps);
    }

    public String getLoadProps() {

        if (StringUtils.isNotEmpty(dorisTab.seqKey)) {
            JSONObject props = JSON.parseObject(this.plugin.loadProps);
            props.put(Keys.COL_SEQUENCE_NAME, dorisTab.seqKey);
            return JsonUtil.toString(props);
        }

        return this.plugin.loadProps;
    }

    public boolean isContainMaxBatchRows() {
        return this.plugin.maxBatchRows != null;
    }

    public Integer getMaxBatchRows() {
        return this.plugin.maxBatchRows;
    }

    public boolean isContainMaxBatchSize() {
        return this.plugin.batchSize != null;
    }

    public Integer getMaxBatchSize() {
        return this.plugin.batchSize;
    }

    protected String colEscapeChar() {
        return StringUtils.EMPTY;
    }


}
