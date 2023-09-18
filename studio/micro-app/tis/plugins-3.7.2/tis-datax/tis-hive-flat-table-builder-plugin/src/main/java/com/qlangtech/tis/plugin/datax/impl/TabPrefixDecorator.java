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

package com.qlangtech.tis.plugin.datax.impl;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.TabNameDecorator;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-27 12:47
 **/
public class TabPrefixDecorator extends TabNameDecorator {

    public static final String TAB_PREFIX = "tabPrefix";
    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String prefix;

    @Override
    public String decorate(String tableName) {
        return StringUtils.trimToEmpty(prefix) + tableName;
    }


    @TISExtension
    public static class PrefixDesc extends Descriptor<TabNameDecorator> {
        @Override
        public String getDisplayName() {
            return TAB_PREFIX;
        }
    }
}
