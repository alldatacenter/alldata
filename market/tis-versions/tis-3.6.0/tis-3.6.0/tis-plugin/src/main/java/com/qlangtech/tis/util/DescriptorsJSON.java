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
package com.qlangtech.tis.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.extension.impl.RootFormProperties;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DescriptorsJSON<T extends Describable<T>> {

    //public static final int FORM_START_LEVEL = 1;

    public static final String KEY_DISPLAY_NAME = "displayName";
    public static final String KEY_EXTEND_POINT = "extendPoint";
    public static final String KEY_IMPL = "impl";
    public static final String KEY_IMPL_URL = "implUrl";

    private final List<Descriptor<T>> descriptors;

    public static JSONObject desc(String requestDescId) {
        return new DescriptorsJSON(TIS.get().getDescriptor(requestDescId)).getDescriptorsJSON();
    }

    public static JSONObject desc(Descriptor desc) {
        return new DescriptorsJSON(desc).getDescriptorsJSON();
    }

    public DescriptorsJSON(List<Descriptor<T>> descriptors) {
        this.descriptors = descriptors;
    }

    public DescriptorsJSON(Descriptor<T> descriptor) {
        this(Collections.singletonList(descriptor));
    }


    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static abstract class SubFormFieldVisitor implements PluginFormProperties.IVisitor {

        final Optional<IPropertyType.SubFormFilter> subFormFilter;

        public SubFormFieldVisitor(Optional<IPropertyType.SubFormFilter> subFormFilter) {
            this.subFormFilter = subFormFilter;
        }

    }

    public JSONObject getDescriptorsJSON() {
        return getDescriptorsJSON(Optional.empty());
    }


    public JSONObject getDescriptorsJSON(Optional<IPropertyType.SubFormFilter> subFormFilter) {
        JSONArray attrs;
        String key;
        PropertyType val;
        JSONObject extraProps = null;
        // FormField fieldAnnot;
        JSONObject attrVal;
        JSONObject descriptors = new JSONObject();
        Map<String, Object> extractProps;
        // IPropertyType.SubFormFilter subFilter = null;
        PluginFormProperties pluginFormPropertyTypes;
        for (Descriptor<T> dd : this.descriptors) {
            pluginFormPropertyTypes = dd.getPluginFormPropertyTypes(subFormFilter);

            JSONObject des = new JSONObject();
            // des.put("formLevel", formLevel);
            Descriptor desc = pluginFormPropertyTypes.accept(new SubFormFieldVisitor(subFormFilter) {
                @Override
                public Descriptor visit(RootFormProperties props) {
                    return dd;
                }

                @Override
                public Descriptor visit(BaseSubFormProperties props) {
                    JSONObject subForm = new JSONObject();
                    subForm.put("fieldName", props.getSubFormFieldName());
                    if (subFormFilter.isPresent()) {

                        IPropertyType.SubFormFilter filter = subFormFilter.get();
                        if (!filter.subformDetailView) {
                            des.put("subForm", true);
                            subForm.put("idList", props.getSubFormIdListGetter(filter).build(filter));
                        }
                    }
                    des.put("subFormMeta", subForm);
                    return props.subFormFieldsDescriptor;
                }
            });

            des.put(KEY_EXTEND_POINT, desc.getT().getName());

            this.setDescInfo(desc, des);

            des.put("veriflable", desc.overWriteValidateMethod);
            if (IdentityName.class.isAssignableFrom(desc.clazz)) {
                des.put("pkField", desc.getIdentityField().displayName);
            }
            extractProps = desc.getExtractProps();
            if (!extractProps.isEmpty()) {
                des.put("extractProps", extractProps);
            }

            attrs = new JSONArray();
            ArrayList<Map.Entry<String, PropertyType>> entries
                    = Lists.newArrayList(pluginFormPropertyTypes.getKVTuples());

            entries.sort(((o1, o2) -> o1.getValue().ordinal() - o2.getValue().ordinal()));
            boolean containAdvanceField = false;
            for (Map.Entry<String, PropertyType> pp : entries) {
                key = pp.getKey();
                val = pp.getValue();
                // fieldAnnot = val.getFormField();
                attrVal = new JSONObject();
                attrVal.put("key", key);
                // 是否是主键
                attrVal.put("pk", val.isIdentity());
                attrVal.put("describable", val.isDescribable());
                attrVal.put("type", val.typeIdentity());
                attrVal.put("required", val.isInputRequired());
                attrVal.put("ord", val.ordinal());
                // 是否是高级组
                if (val.advance()) {
                    containAdvanceField = true;
                    attrVal.put("advance", true);
                }
                extraProps = val.getExtraProps();
                if (extraProps != null) {
                    // 额外属性
                    attrVal.put("eprops", extraProps);
                }

                if (val.typeIdentity() == FormFieldType.SELECTABLE.getIdentity()) {
                    attrVal.put("options", getSelectOptions(desc, val, key));
                }
                if (val.isDescribable()) {
                    DescriptorsJSON des2Json = new DescriptorsJSON(val.getApplicableDescriptors());
                    attrVal.put("descriptors", des2Json.getDescriptorsJSON());
                }
                // attrs.put(attrVal);
                attrs.add(attrVal);
            }
            // 对象拥有的属性
            des.put("attrs", attrs);
            // 包含高级字段
            des.put("containAdvance", containAdvanceField);
            // processor.process(attrs.keySet(), d);
            descriptors.put(desc.getId(), des);
        }
        return descriptors;
    }

    public static void setDescInfo(Descriptor d, JSONObject des) {
        des.put(KEY_DISPLAY_NAME, d.getDisplayName());
        des.put(KEY_IMPL, d.getId());
        des.put(KEY_IMPL_URL, Config.TIS_PUB_PLUGINS_DOC_URL + StringUtils.remove(StringUtils.lowerCase(d.clazz.getName()), "."));
    }

    public static List<Descriptor.SelectOption> getSelectOptions(Descriptor descriptor, PropertyType propType, String fieldKey) {
        ISelectOptionsGetter optionsCreator = null;
        if (propType.typeIdentity() != FormFieldType.SELECTABLE.getIdentity()) {
            throw new IllegalStateException("propType must be:" + FormFieldType.SELECTABLE + " but now is:" + propType.typeIdentity());
        }
        if (!(descriptor instanceof ISelectOptionsGetter)) {
            throw new IllegalStateException("descriptor:" + descriptor.getClass()
                    + " has a selectable field:" + fieldKey + " descriptor must be an instance of 'ISelectOptionsGetter'");
        }
        optionsCreator = descriptor;
        List<Descriptor.SelectOption> selectOptions = optionsCreator.getSelectOptions(fieldKey);

        return selectOptions;
    }

    public interface IPropGetter {
        public Object build(IPropertyType.SubFormFilter filter);
    }
}
