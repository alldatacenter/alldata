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
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.spark.ISparkConnGetter;
import com.qlangtech.tis.config.spark.SparkConnStrategy;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-04 19:40
 **/
public class DefaultSparkConnGetter extends ParamsConfig implements ISparkConnGetter {

    private static final String KEY_MASTER_ADDRESS = "master";
    @FormField(ordinal = 0, identity = true, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, validate = {Validator.require})
    public SparkConnStrategy connStrategy;


    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public ISparkConnGetter createConfigInstance() {
        return this;
    }

    @Override
    public String getSparkMaster(File cfgDir) {
        if (!cfgDir.exists() || !cfgDir.isDirectory()) {
            throw new IllegalStateException("cfgDir must be exist dir:" + cfgDir.getAbsolutePath());
        }
        return this.connStrategy.getSparkMaster(cfgDir);
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {
        public DefaultDescriptor() {
            super();
            // this.registerSelectOptions(HiveFlatTableBuilder.KEY_FIELD_NAME, () -> TIS.getPluginStore(FileSystemFactory.class).getPlugins());
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            // return HiveFlatTableBuilder.validateHiveAvailable(msgHandler, context, postFormVals);
            //  String masterAddress = postFormVals.getField(KEY_MASTER_ADDRESS);

            // File sparkHome = HudiConfig.getSparkHome();

            return true;
        }

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            return this.verify(msgHandler, context, postFormVals);
        }

        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }
    }
}
