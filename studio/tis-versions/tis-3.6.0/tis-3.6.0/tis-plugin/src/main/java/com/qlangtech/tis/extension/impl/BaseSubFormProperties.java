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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.util.AttrValMap;
import com.qlangtech.tis.util.DescriptorsJSON;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-30 18:44
 **/
public abstract class BaseSubFormProperties extends PluginFormProperties implements IPropertyType {

    public final Field subFormField;
    public Class instClazz;
    public final Descriptor subFormFieldsDescriptor;


    /**
     * @param subFormField
     * @param subFormFieldsDescriptor Example: SelectedTable.DefaultDescriptor
     */
    public BaseSubFormProperties(Field subFormField, Class instClazz, Descriptor subFormFieldsDescriptor) {
        this.subFormField = subFormField;
        this.instClazz = instClazz;
        this.subFormFieldsDescriptor = subFormFieldsDescriptor;
    }

    public abstract PropertyType getPropertyType(String fieldName);
    //PropertyType propertyType = props.fieldsType.get(descField.field)

    public String getSubFormFieldName() {
        return this.subFormField.getName();
    }

    /**
     * 至少选一个
     *
     * @return
     */
    public abstract boolean atLeastOne();

    @Override
    public JSON getInstancePropsJson(Object instance) {
//        Class<?> fieldType = subFormField.getType();
//        if (!Collection.class.isAssignableFrom(fieldType)) {
//            // 现在表单只支持1对n 关系的子表单，因为1对1就没有必要有子表单了
//            throw new UnsupportedOperationException("sub form field:" + subFormField.getName()
//                    + " just support one2multi relationship,declarFieldClass:" + fieldType.getName());
//        }
//        getSubFormPropVal(instance);
//        try {
//            Object o = subFormField.get(instance);

        return createSubFormVals(getSubFormPropVal(instance));

//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
    }

    public abstract DescriptorsJSON.IPropGetter getSubFormIdListGetter(IPropertyType.SubFormFilter filter);

    public Collection<IdentityName> getSubFormPropVal(Object instance) {
        Class<?> fieldType = subFormField.getType();
        if (!Collection.class.isAssignableFrom(fieldType)) {
            // 现在表单只支持1对n 关系的子表单，因为1对1就没有必要有子表单了
            throw new UnsupportedOperationException("sub form field:" + subFormField.getName()
                    + " just support one2multi relationship,declarFieldClass:" + fieldType.getName());
        }

        try {
            Object o = subFormField.get(instance);
            return (o == null) ? Collections.emptyList() : (Collection<IdentityName>) o;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T newSubDetailed() {
        try {
            return (T) this.instClazz.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final <T> T accept(IVisitor visitor) {
        return visitor.visit(this);
    }

    protected abstract Map<String, SelectedTab> getSelectedTabs(Collection<IdentityName> subFormFieldInstance);
    protected abstract void addSubItems(SelectedTab ext, JSONArray pair) throws Exception ;

    public final  JSONObject createSubFormVals(Collection<IdentityName> subFormFieldInstance) {
        Map<String, SelectedTab> tabExtends = getSelectedTabs(subFormFieldInstance);
        JSONObject vals = null;
        try {
            SelectedTab ext = null;
            vals = new JSONObject();
            JSONArray pair = null;
            if (subFormFieldInstance != null) {
                for (IdentityName subItem : subFormFieldInstance) {
                    ext = tabExtends.get(subItem.identityValue());
                    if (ext == null) {
                        continue;
                    }
                    pair = new JSONArray();
                    addSubItems(ext, pair);
                    vals.put(subItem.identityValue(), pair);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return vals;
    }


    public final <T> T visitAllSubDetailed(
            // Map<String, /*** attr key */JSONArray> formData
            AttrValMap.IAttrVals formData
            , ISubDetailedProcess<T> subDetailedProcess) {
        String subFormId = null;
        //JSONObject subformData = null;
        AttrValMap attrVals = null;

        for (Map.Entry<String, JSONArray> entry : formData.asSubFormDetails().entrySet()) {
            subFormId = entry.getKey();

            //  subform = Maps.newHashMap();

            for (Object o : entry.getValue()) {

                attrVals = AttrValMap.parseDescribableMap(Optional.empty(), (JSONObject) o);

                T result = subDetailedProcess.process(subFormId, attrVals);
                if (result != null) {
                    return result;
                }
            }

        }
        return null;
    }

    public interface ISubDetailedProcess<T> {
        T process(String subFormId, AttrValMap attrVals);
    }
}
