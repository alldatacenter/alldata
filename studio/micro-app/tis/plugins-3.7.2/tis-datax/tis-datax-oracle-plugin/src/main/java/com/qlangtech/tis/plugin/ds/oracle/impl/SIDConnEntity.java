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

package com.qlangtech.tis.plugin.ds.oracle.impl;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.oracle.ConnEntity;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-03 15:39
 **/
public class SIDConnEntity extends ConnEntity implements Serializable {

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String sid;

    @Override
    protected String getEntityName() {
        if (StringUtils.isEmpty(this.sid)) {
            throw new IllegalStateException("prop sid can not be empty");
        }
        return ":" + this.sid;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "sid='" + sid + '\'' +
                '}';
    }

    @TISExtension
    public static class DftDesc extends Descriptor<ConnEntity> {
        @Override
        public String getDisplayName() {
            return "SID";
        }
    }
}
