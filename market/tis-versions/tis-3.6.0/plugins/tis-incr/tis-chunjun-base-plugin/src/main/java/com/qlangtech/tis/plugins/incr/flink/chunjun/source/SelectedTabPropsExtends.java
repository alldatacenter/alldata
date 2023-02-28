/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.chunjun.source;

import com.qlangtech.tis.plugins.incr.flink.chunjun.poll.Polling;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-30 12:02
 **/
public class SelectedTabPropsExtends extends IncrSelectedTabExtend {
    @FormField(ordinal = 0, validate = {Validator.require})
    public Polling polling;

    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {})
    public String splitPk;

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {})
    public String where;
    @Override
    public boolean isSource() {
        return true;
    }
    /**
     * 分区键候选字段
     *
     * @return
     */
    public static List<Option> getSplitPkCols() {
        return SelectedTab.getContextTableCols((cols) -> cols.stream()
                .filter((col) -> {
//                    switch (col.getType().getCollapse()) {
//                        case DataXReaderColType.INT:
//                        case DataXReaderColType.Long:
//                            return true;
//                    }
                    DataXReaderColType type = col.getType().getCollapse();
                    if (type == DataXReaderColType.INT || type == DataXReaderColType.Long) {
                        return true;
                    }
                    return false;
                }));
    }

    public void setParams(Map<String, Object> params) {
        if (StringUtils.isNotEmpty(this.splitPk)) {
            params.put("splitPk", this.splitPk);
        }
        if (StringUtils.isNotEmpty(this.where)) {
            params.put("where", this.where);
        }
        polling.setParams(params);
    }

    @TISExtension
    public static class DftDesc extends BaseDescriptor {
        public DftDesc() {
            super();
        }

    }
}
