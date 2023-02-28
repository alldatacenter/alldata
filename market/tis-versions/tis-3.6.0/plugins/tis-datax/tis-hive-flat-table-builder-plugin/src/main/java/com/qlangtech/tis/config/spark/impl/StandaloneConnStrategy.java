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

package com.qlangtech.tis.config.spark.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.config.spark.SparkConnStrategy;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-14 17:33
 **/
public class StandaloneConnStrategy extends SparkConnStrategy {

    @FormField(ordinal = 1, validate = {Validator.require})
    public String master;

    @Override
    public String getSparkMaster(File cfgDir) {
        return this.master;
    }

    @TISExtension
    public static class DefaultDesc extends Descriptor<SparkConnStrategy> {
        public DefaultDesc() {
            super();
        }

        public boolean validateMaster(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            final String masterPrefix = "spark://";
            if (!StringUtils.startsWith(value, masterPrefix)) {
                msgHandler.addFieldError(context, fieldName, "必须以" + masterPrefix + "作为前缀");
                return false;
            }
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Standalone";
        }
    }
}
