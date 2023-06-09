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
package com.qlangtech.tis.extension;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-11 12:11
 */
public interface IPropertyType {

    /**
     * plugin form 某一个field为集合类型，且字段内为一个javabean类型，需要填写多个字段
     */
    public class SubFormFilter {

        public static final String KEY_INCR_PROCESS_EXTEND = "incr_process_extend";

        /**
         * 表明点击进入子表单显示
         */
        public static String PLUGIN_META_SUBFORM_DETAIL_ID_VALUE = "subformDetailIdValue";


        public static String PLUGIN_META_SUB_FORM_FIELD = "subFormFieldName";
        public final UploadPluginMeta.TargetDesc targetDesc;
        //public final String targetDescImpl;
        public final String subFieldName;
        public final UploadPluginMeta uploadPluginMeta;
        // 是否显示子表单内容
        public final boolean subformDetailView;
        public final String subformDetailId;

        public Descriptor getTargetDescriptor() {
            Descriptor parentDesc = Objects.requireNonNull(TIS.get().getDescriptor(this.targetDesc.impl)
                    , this.targetDesc + "->" + this.targetDesc.impl + " relevant desc can not be null");
            return parentDesc;
        }

        /**
         * 增量流程中需要对已经选的表进程属性设置？
         *
         * @return
         */
        public boolean isIncrProcessExtend() {
            return this.targetDesc.isNameMatch(KEY_INCR_PROCESS_EXTEND);// .equals(this.targetDescriptorName);
        }

        public boolean match(Descriptor<?> desc) {
            //return targetDesc.descDisplayName(desc.getDisplayName());
            return StringUtils.equals(desc.getDisplayName(), this.targetDesc.descDisplayName);
            // return StringUtils.equals(desc.getDisplayName(), this.targetDescriptorName.getName());
        }


        public final String param(String key) {
            return uploadPluginMeta.getExtraParam(key);
        }

        /**
         * 取得子表单的宿主plugin
         *
         * @param pluginContext
         * @param <T>
         * @return
         */
        public <T> T getOwnerPlugin(IPluginContext pluginContext) {
            Optional<Object> first = this.uploadPluginMeta.getHeteroEnum().getPlugins(pluginContext, this.uploadPluginMeta).stream().findFirst();
            if (!first.isPresent()) {
                throw new IllegalStateException("can not find owner plugin:" + uploadPluginMeta.toString());
            }
            return (T) first.get();
        }

        public SubFormFilter(UploadPluginMeta uploadPluginMeta, UploadPluginMeta.TargetDesc targetDescriptorName //, String targetDescImpl
                , String subFieldName) {
            if ((targetDescriptorName) == null) {
                throw new IllegalArgumentException("param fieldName can not be empty");
            }
            if (StringUtils.isEmpty(subFieldName)) {
                throw new IllegalArgumentException("param subFieldName can not be empty");
            }
            this.targetDesc = targetDescriptorName;
            //this.targetDescImpl = targetDescImpl;
            this.subFieldName = subFieldName;
            this.uploadPluginMeta = uploadPluginMeta;
            this.subformDetailView = StringUtils.isNotEmpty(
                    subformDetailId = uploadPluginMeta.getExtraParam(PLUGIN_META_SUBFORM_DETAIL_ID_VALUE));
        }

        public boolean useCache(){
            return this.uploadPluginMeta.isUseCache();
        }

        @Override
        public String toString() {
            return "{" +
                    "  descName='" + targetDesc + '\'' +
                    ", subFieldName='" + subFieldName + '\'' +
                    ", subformDetailId='" + subformDetailId + '\'' +
                    '}';
        }
    }
}
