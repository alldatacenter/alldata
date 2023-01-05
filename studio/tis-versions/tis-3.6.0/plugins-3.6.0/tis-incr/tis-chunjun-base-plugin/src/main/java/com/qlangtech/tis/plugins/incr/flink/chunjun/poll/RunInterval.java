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

package com.qlangtech.tis.plugins.incr.flink.chunjun.poll;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugins.incr.flink.chunjun.offset.StartLocation;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-28 15:42
 **/
public class RunInterval extends Polling {

    public static final String DISPLAY_NAME = "RunInterval";

//        params.put("polling", true);
//        params.put("pollingInterval", 3000);
//        params.put("increColumn", "id");
//        params.put("startLocation", 0);

    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
    public String incrColumn;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer pollingInterval;

    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean useMaxFunc;

    @FormField(ordinal = 4, validate = {Validator.require})
    public StartLocation startLocation;

    @Override
    public void setParams(Map<String, Object> params) {
        // polling
        params.put("polling", true);
        params.put("pollingInterval", this.pollingInterval);
        if (StringUtils.isEmpty(this.incrColumn)) {
            throw new IllegalStateException("param incrColumn can not be null ");
        }
        params.put("increColumn", this.incrColumn);

        params.put("useMaxFunc", Objects.requireNonNull(this.useMaxFunc, "useMaxFunc can not be null"));
        startLocation.setParams(params);
    }

    /**
     * 分区键候选字段
     *
     * @return
     */
    public static List<Option> getCols() {
        return SelectedTab.getContextTableCols((cols) -> cols.stream()
                .filter((col) -> {
//                    switch (col.getType().getCollapse()) {
//                        case DataXReaderColType.INT:
//                        case DataXReaderColType.Long:
//                        case DataXReaderColType.Date:
//                            return true;
//                    }
                    DataXReaderColType type = col.getType().getCollapse();
                    return (type != DataXReaderColType.STRING && type != DataXReaderColType.Bytes);
                }));
    }

    @TISExtension
    public static class DftDesc extends Descriptor<Polling> {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        public boolean validatePollingInterval(IFieldErrorHandler msgHandler
                , Context context, String fieldName, String value) {
            if (Integer.parseInt(value) < 1000) {
                msgHandler.addFieldError(context, fieldName, "不能小于1000，1秒");
                return false;
            }
            return true;
        }

    }
}
