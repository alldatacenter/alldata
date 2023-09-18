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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;

/**
 * @author: baisui 百岁
 * @create: 2021-04-21 09:29
 **/
@Public
public class DataXGlobalConfig extends ParamsConfig implements IDataxGlobalCfg {

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public int channel;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public int errorLimitCount;

    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public float errorLimitPercentage;

    @FormField(ordinal = 4, type = FormFieldType.TEXTAREA, advance = false ,validate = {Validator.require})
    public String template;

    @Override
    public String identityValue() {
        return name;
    }

    @Override
    public String getTemplate() {
        return this.template;
    }

    public static void main(String[] args) {
        // System.out.println(DataXGlobalConfig.class);
    }

    @Override
    public IDataxGlobalCfg createConfigInstance() {
        return this;
    }

    @Override
    public int getChannel() {
        return this.channel;
    }

    @Override
    public int getErrorLimitCount() {
        return this.errorLimitCount;
    }

    @Override
    public float getErrorLimitPercentage() {
        return this.errorLimitPercentage;
    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {
        @Override
        public String getDisplayName() {
            return "DataX-global";
        }

        public DefaultDescriptor() {
            super();
        }

        public boolean validateChannel(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            if (Integer.parseInt(value) < 1) {
                msgHandler.addFieldError(context, fieldName, "不能小于1");
                return false;
            }
            return true;
        }

        public boolean validateErrorLimitCount(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            if (Integer.parseInt(value) < 0) {
                msgHandler.addFieldError(context, fieldName, "不能为负数");
                return false;
            }
            return true;
        }

        public boolean validateErrorLimitPercentage(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            float fVal = 0;
            try {
                fVal = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                msgHandler.addFieldError(context, fieldName, "请输入一个浮点数");
                return false;
            }

            if (fVal <= 0 || fVal >= 1) {
                msgHandler.addFieldError(context, fieldName, "浮点数值必须大于0且小于1之间");
                return false;
            }

            return true;
        }
    }


}
