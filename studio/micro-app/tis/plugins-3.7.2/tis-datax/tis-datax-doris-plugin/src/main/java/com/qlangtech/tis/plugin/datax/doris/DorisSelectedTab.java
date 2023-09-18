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

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-05-21 15:00
 **/
public class DorisSelectedTab extends SelectedTab {

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {})
    public String seqKey;


    /**
     * seq cols候选列
     *
     * @return
     */
    public static List<Option> getSeqKeys() {
        List<Option> result = SelectedTab.getContextTableCols((cols) -> {
            return cols.stream().filter((c) -> {
                DataXReaderColType t = c.getType().getCollapse();
                return t == DataXReaderColType.INT || t == DataXReaderColType.Long || t == DataXReaderColType.Date;
            });
        });
        return result;
    }

    @TISExtension
    public static class DefaultDescriptor extends SelectedTab.DefaultDescriptor {

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, SelectedTab postFormVals) {
            return true;
        }
    }
}
