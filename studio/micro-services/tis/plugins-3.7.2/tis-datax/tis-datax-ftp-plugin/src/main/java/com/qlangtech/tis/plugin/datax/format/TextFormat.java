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

package com.qlangtech.tis.plugin.datax.format;

import com.qlangtech.tis.datax.Delimiter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-14 17:03
 **/
public class TextFormat extends FileFormat {

    @FormField(ordinal = 16, type = FormFieldType.ENUM, validate = {})
    public Boolean header;

    @FormField(ordinal = 9, type = FormFieldType.ENUM, validate = {Validator.require})
    public String fieldDelimiter;

    @Override
    public String getFieldDelimiter() {
        return String.valueOf(Delimiter.parse(this.fieldDelimiter).val);
    }

    @Override
    public boolean containHeader() {
        return this.header;
    }

    @TISExtension
    public static class Desc extends Descriptor<FileFormat> {
        @Override
        public String getDisplayName() {
            return "TEXT";
        }
    }
}
