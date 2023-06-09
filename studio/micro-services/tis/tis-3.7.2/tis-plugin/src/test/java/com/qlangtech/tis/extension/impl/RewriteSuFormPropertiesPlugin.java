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

package com.qlangtech.tis.extension.impl;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.datax.SelectedTab;

import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-02 11:34
 **/
public class RewriteSuFormPropertiesPlugin implements Describable<RewriteSuFormPropertiesPlugin> {

    public static final String PLUGIN_NAME = "RewriteSuFormPropertiesPlugin";

    @Override
    public Descriptor<RewriteSuFormPropertiesPlugin> getDescriptor() {
        Descriptor<RewriteSuFormPropertiesPlugin> descriptor = TIS.get().getDescriptor(this.getClass());
        return descriptor;
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<RewriteSuFormPropertiesPlugin> implements DataxWriter.IRewriteSuFormProperties {
        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }

        @Override
        public Descriptor<SelectedTab> getRewriterSelectTabDescriptor() {
//            String overwriteSubField = IOUtils.loadResourceFromClasspath(RewriteSuFormPropertiesPlugin.class
//                    , RewriteSuFormPropertiesPlugin.class.getSimpleName()
//                            + "." + subformProps.getSubFormFieldName() + ".json", true);
//            JSONObject subField = JSON.parseObject(overwriteSubField);
//            Class<? extends Describable> clazz =
//                    (Class<? extends Describable>) RewriteSuFormPropertiesPlugin.class.getClassLoader().loadClass(subField.getString(SubForm.FIELD_DES_CLASS));
            Descriptor<SelectedTab> subFormDescriptor = Objects.requireNonNull(TIS.get().getDescriptor(com.qlangtech.tis.extension.impl.SubFieldExtend.class)
                    , "class:" + clazz + " relevant subFormDescriptor can not be null");

            return subFormDescriptor;
        }

        @Override
        public SuFormProperties overwriteSubPluginFormPropertyTypes(SuFormProperties subformProps) throws Exception {
//            String overwriteSubField = IOUtils.loadResourceFromClasspath(RewriteSuFormPropertiesPlugin.class
//                    , RewriteSuFormPropertiesPlugin.class.getSimpleName()
//                            + "." + subformProps.getSubFormFieldName() + ".json", true);
//            JSONObject subField = JSON.parseObject(overwriteSubField);
//            Class<? extends Describable> clazz =
//                    (Class<? extends Describable>) RewriteSuFormPropertiesPlugin.class.getClassLoader().loadClass(subField.getString(SubForm.FIELD_DES_CLASS));
            Descriptor subFormDescriptor = getRewriterSelectTabDescriptor();
            return SuFormProperties.copy(filterFieldProp(Descriptor.buildPropertyTypes(Optional.of(subFormDescriptor), clazz)), clazz, subFormDescriptor, subformProps);
        }

//        @Override
//        public SuFormProperties.SuFormPropertiesBehaviorMeta overwriteBehaviorMeta(SuFormProperties.SuFormPropertiesBehaviorMeta behaviorMeta) throws Exception {
//
//
//            SuFormProperties.SuFormPropertyGetterMeta propProcess = new SuFormProperties.SuFormPropertyGetterMeta();
//            propProcess.setMethod("getPrimaryKeys");
//            propProcess.setParams(Collections.singletonList("id"));
//
//            behaviorMeta.onClickFillData.put("recordField", propProcess);
//
//            return behaviorMeta;
//        }
    }
}
