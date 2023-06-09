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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.util.DescribableJSON;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.qlangtech.tis.util.UploadPluginMeta;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-30 18:59
 * @see com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory
 **/
public class IncrSourceExtendSelected extends BaseSubFormProperties {
    private final UploadPluginMeta uploadPluginMeta;
    // private final Descriptor<IncrSelectedTabExtend> incrExtendDesc;

    public static final Class<SelectedTab> selectedTabClass = SelectedTab.class;

    public IncrSourceExtendSelected(UploadPluginMeta uploadPluginMeta, Field subFormField) {
        super(subFormField, selectedTabClass, TIS.get().getDescriptor(selectedTabClass));
        this.uploadPluginMeta = uploadPluginMeta;
    }

    @Override
    public PropertyType getPropertyType(String fieldName) {
        return (PropertyType) subFormFieldsDescriptor.getPropertyType(fieldName);
    }

    @Override
    public boolean atLeastOne() {
        return true;
    }

    @Override
    public DescriptorsJSON.IPropGetter getSubFormIdListGetter(IPropertyType.SubFormFilter filter) {
//        return (filter) -> {
//            IPluginStore<?> readerSubFieldStore
//                    = HeteroEnum.getDataXReaderAndWriterStore(filter.uploadPluginMeta.getPluginContext(), true, filter.uploadPluginMeta, Optional.of(filter));
//            List<?> plugins = readerSubFieldStore.getPlugins();
//            return plugins.stream().map((p) -> ((IdentityName) p).identityValue()).collect(Collectors.toList());
//        };
        throw new UnsupportedOperationException();
    }

//    @Override
//    public <T> T visitAllSubDetailed(Map<String, JSONObject> formData, SuFormProperties.ISubDetailedProcess<T> subDetailedProcess) {
//        //  throw new UnsupportedOperationException();
//        Descriptor.PluginValidateResult validateResult = null;
//        return null;
//    }

//    @Override
//    public JSONObject createSubFormVals(Collection<IdentityName> subFormFieldInstance) {
//        //  throw new UnsupportedOperationException(subFormFieldInstance.stream().map((i) -> i.identityValue()).collect(Collectors.joining(",")));
//        // IncrSourceSelectedTabExtend.INCR_SELECTED_TAB_EXTEND.
//        // IncrSourceSelectedTabExtend.INCR_SELECTED_TAB_EXTEND.
//        Map<String, SelectedTab> tabExtends = getSelectedTabs(subFormFieldInstance);
//        JSONObject vals = null;
//        try {
//
//            SelectedTab ext = null;
//            vals = new JSONObject();
//            JSONArray pair = null;
//            // DescribableJSON itemJson = null;
//            // RootFormProperties props = (new RootFormProperties(getPropertyType()));
//            if (subFormFieldInstance != null) {
//                for (IdentityName subItem : subFormFieldInstance) {
//                    ext = tabExtends.get(subItem.identityValue());
////                    Objects.requireNonNull(tabExtends.get(subItem.identityValue())
////                            , "table:" + subItem.identityValue() + " relevant tab ext can not be null");
////                    ext = Objects.requireNonNull(tabExtends.get(subItem.identityValue())
////                            , "table:" + subItem.identityValue() + " relevant tab ext can not be null");
//                    if (ext == null) {
//                        continue;
//                    }
//                    pair = new JSONArray();
//                    addSubItems(ext, pair);
//
////                    if (ext.getIncrSinkProps() != null) {
////                        itemJson = new DescribableJSON(ext.getIncrSinkProps());
////                        pair.add(itemJson.getItemJson());
////                    }
//                    vals.put(subItem.identityValue(), pair);
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return vals;
//    }

    @Override
    protected Map<String, SelectedTab> getSelectedTabs(Collection<IdentityName> subFormFieldInstance) {
        return IncrSelectedTabExtend.getTabExtend(this.uploadPluginMeta);
    }

    @Override
    protected void addSubItems(SelectedTab ext, JSONArray pair) throws Exception {
        addExt(ext.getIncrSourceProps(), pair);
        addExt(ext.getIncrSinkProps(), pair);
    }

    public void addExt(IncrSelectedTabExtend ext, JSONArray pair) throws Exception {
        DescribableJSON itemJson;
        if (ext != null) {
            itemJson = new DescribableJSON(ext);
            pair.add(itemJson.getItemJson());
        }
    }

    @Override
    public Set<Map.Entry<String, PropertyType>> getKVTuples() {
        return Descriptor.filterFieldProp(subFormFieldsDescriptor).entrySet();
    }

    // private Map<String, PropertyType> props = null;


//    public Map<String, PropertyType> getPropertyType() {
////        if (props == null) {
////            props = Descriptor.filterFieldProp(subFormFieldsDescriptor);
////        }
////        return props;
//    }
}
