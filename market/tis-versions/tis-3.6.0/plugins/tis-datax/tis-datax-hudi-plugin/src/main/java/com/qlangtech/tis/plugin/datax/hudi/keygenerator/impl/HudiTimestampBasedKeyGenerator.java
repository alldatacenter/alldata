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

package com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl;

//import com.alibaba.citrus.turbine.Context;
//import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
//import com.qlangtech.plugins.org.apache.hudi.keygen.TimestampBasedAvroKeyGenerator;
//import com.qlangtech.tis.annotation.Public;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.manage.common.Option;
//import com.qlangtech.tis.org.apache.hudi.keygen.constant.KeyGeneratorType;
//import com.qlangtech.tis.plugin.ValidatorCommons;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
//import com.qlangtech.tis.plugin.datax.hudi.keygenerator.HudiKeyGenerator;
//import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
//import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;

/**
 * ref:KeyGeneratorType
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-25 07:31
 * see org.apache.hudi.keygen.TimestampBasedAvroKeyGenerator
 **/
//@Public
//public class HudiTimestampBasedKeyGenerator extends HudiKeyGenerator {


//    @TISExtension
//    public static class DefaultDescriptor extends Descriptor<HudiKeyGenerator> {
//        public DefaultDescriptor() {
//            super();
//            addFieldDesc(this);
//        }
//
//        @Override
//        public PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {
//            return super.getPluginFormPropertyTypes(Optional.empty());
//        }
//
//
//        @Override
//        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
//            ParseDescribable<Describable> i = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
//            HudiTimestampBasedKeyGenerator keyGenerator = i.getInstance();
//
//            return validateForm(msgHandler, context, keyGenerator);
//        }
//
//
//        @Override
//        public String getDisplayName() {
//            return KeyGeneratorType.TIMESTAMP.name();
//        }
//    }


//}
