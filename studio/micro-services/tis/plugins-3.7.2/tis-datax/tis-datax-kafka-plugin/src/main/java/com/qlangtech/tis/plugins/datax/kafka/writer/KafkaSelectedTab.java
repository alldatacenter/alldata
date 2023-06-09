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

package com.qlangtech.tis.plugins.datax.kafka.writer;

import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.datax.SelectedTab;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-20 09:20
 **/
public class KafkaSelectedTab extends SelectedTab {

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {})
    public List<String> partitionFields;

    public static List<Option> getPtCandidateFields() {
        return SelectedTab.getContextTableCols((cols) -> cols.stream()
                .filter((col) -> {
                    return true;
//                    switch (col.getType().getCollapse()) {
//                        // case STRING:
//                        case INT:
//                        case Long:
//                        case Date:
//                            return true;
//                    }
//                    return false;
                }));

    }


    @TISExtension
    public static class DftDescriptor extends SelectedTab.DefaultDescriptor {
        public DftDescriptor() {
            super();
        }
    }
}
