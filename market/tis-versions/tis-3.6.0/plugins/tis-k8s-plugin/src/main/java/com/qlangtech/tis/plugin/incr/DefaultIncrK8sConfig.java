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
package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.k8s.K8sImage;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-12 11:06
 * @date 2020/04/13
 */
public class DefaultIncrK8sConfig extends IncrStreamFactory {

    public static final String KEY_FIELD_NAME = "k8sImage";

//    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
//    public String name;

    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String k8sImage;

    private IRCController incrSync;

    @Override
    public IRCController getIncrSync() {
        if (incrSync != null) {
            return this.incrSync;
        }
        this.incrSync = new K8sIncrSync(TIS.getPluginStore(K8sImage.class).find(k8sImage));
        return this.incrSync;
    }

    @Override
    public <StreamExecutionEnvironment> StreamExecutionEnvironment createStreamExecutionEnvironment() {
        throw new UnsupportedOperationException();
    }
//    @TISExtension()
//    public static class DescriptorImpl extends Descriptor<IncrStreamFactory> {
//
//        public DescriptorImpl() {
//            super();
//            this.registerSelectOptions(KEY_FIELD_NAME, () -> {
//                PluginStore<K8sImage> images = TIS.getPluginStore(K8sImage.class);
//                return images.getPlugins();
//            });
//        }
//
//        @Override
//        public String getDisplayName() {
//            return "k8s-incr";
//        }
//    }
}
