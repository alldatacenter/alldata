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

package com.qlangtech.tis.plugin.datax.server;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-14 17:07
 **/
public class FTPServer implements Describable<FTPServer> {

    public static final String SERVER = "Server";

    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = {Validator.require})
    public String protocol;
    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String host;
    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer port;
    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer timeout;
    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {})
    public String connectPattern;
    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String username;
    @FormField(ordinal = 6, type = FormFieldType.PASSWORD, validate = {Validator.require})
    public String password;

    @TISExtension
    public static class DefaultDesc extends Descriptor<FTPServer> {
        @Override
        public String getDisplayName() {
            return SERVER;
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            //  return super.verify(msgHandler, context, postFormVals);
            return true;
        }
    }
}
