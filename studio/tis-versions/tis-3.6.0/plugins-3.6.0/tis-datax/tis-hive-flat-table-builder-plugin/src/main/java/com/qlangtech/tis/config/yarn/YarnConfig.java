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
package com.qlangtech.tis.config.yarn;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fullbuild.indexbuild.impl.Hadoop020RemoteJobTriggerFactory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

/*
 * @create: 2020-02-08 12:15
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Public
public class YarnConfig extends ParamsConfig implements IYarnConfig {

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, validate = {Validator.host, Validator.require})
    public String rmAddress;
    // SCHEDULER
    @FormField(ordinal = 2, validate = {Validator.host, Validator.require})
    public String schedulerAddress;

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public YarnConfiguration createConfigInstance() {
        Thread.currentThread().setContextClassLoader(Hadoop020RemoteJobTriggerFactory.class.getClassLoader());
        return getYarnConfig();
    }

    private YarnConfiguration getYarnConfig() {
        YarnConfiguration conf = new YarnConfiguration();
        conf.set(YarnConfiguration.RM_ADDRESS, rmAddress);
        conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, schedulerAddress);
        return conf;
    }


    @TISExtension
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {

        // private List<YarnConfig> installations;
        @Override
        public String getDisplayName() {
            return KEY_DISPLAY_NAME;
        }

        public DefaultDescriptor() {
            super();
            // this.load();
        }
    }
}
