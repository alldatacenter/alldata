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
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.util.GroovyShellEvaluate;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.SubForm;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.util.DescribableJSON;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.qlangtech.tis.util.HeteroEnum;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-11 13:22
 */
public class SuFormProperties extends BaseSubFormProperties {

    public static final ThreadLocal<SuFormGetterContext> subFormGetterProcessThreadLocal = ThreadLocal.withInitial(() -> {
        return new SuFormGetterContext();
    });

    public static SuFormGetterContext setSuFormGetterContext(Describable plugin, UploadPluginMeta pluginMeta, String subFormDetailId) {
        if (StringUtils.isEmpty(subFormDetailId)) {
            throw new IllegalArgumentException("param subFormDetailId can not be empty");
        }
        SuFormProperties.SuFormGetterContext subFormContext = subFormGetterProcessThreadLocal.get();
        subFormContext.plugin = plugin;
        pluginMeta.putExtraParams(IPropertyType.SubFormFilter.PLUGIN_META_SUBFORM_DETAIL_ID_VALUE, subFormDetailId);
        subFormContext.param = pluginMeta;
        return subFormContext;
    }

    public final Map<String, /*** fieldname */PropertyType> fieldsType;


    public final SubForm subFormFieldsAnnotation;
    public final Class<?> parentClazz;
    public final PropertyType pkPropertyType;

    private DescriptorsJSON.IPropGetter subFormFieldsEnumGetter;

    @Override
    public PropertyType getPropertyType(String fieldName) {
        PropertyType propertyType = this.fieldsType.get(fieldName);
        return propertyType;
    }
    //

    /**
     * 至少选一个
     *
     * @return
     */
    @Override
    public boolean atLeastOne() {
        return this.subFormFieldsAnnotation.atLeastOne();
    }

    public static SuFormProperties copy(Map<String, /*** fieldname */PropertyType> fieldsType
            , Class<?> instClazz, Descriptor subFormFieldsDescriptor, SuFormProperties old) {
        SuFormProperties result = new SuFormProperties(old.parentClazz, old.subFormField
                , old.subFormFieldsAnnotation, subFormFieldsDescriptor, fieldsType);
        if (!old.subFormFieldsAnnotation.desClazz().isAssignableFrom(instClazz)) {
            throw new IllegalStateException("class:" + old.subFormFieldsAnnotation.desClazz() + " must be parent of " + instClazz);
        }
        return result.overWriteInstClazz(instClazz);
    }

    /**
     * @param parentClazz
     * @param subFormField
     * @param subFormFieldsAnnotation
     * @param subFormFieldsDescriptor Example: SelectedTable.DefaultDescriptor
     * @param fieldsType              Example: SelectedTable.DefaultDescriptor 对应plugin的 props
     */
    public SuFormProperties(Class<?> parentClazz, Field subFormField
            , SubForm subFormFieldsAnnotation, Descriptor subFormFieldsDescriptor, Map<String, PropertyType> fieldsType) {
        super(subFormField, subFormFieldsAnnotation != null ? subFormFieldsAnnotation.desClazz() : null, subFormFieldsDescriptor);
        Objects.requireNonNull(fieldsType, "fieldsType can not be null");
        this.parentClazz = parentClazz;
        this.fieldsType = fieldsType;
        // this.subFormField = subFormField;
        this.subFormFieldsAnnotation = subFormFieldsAnnotation;
//        if () {
//            this.instClazz = ;
//        }
        if (MapUtils.isNotEmpty(fieldsType)) {
            Optional<Map.Entry<String, PropertyType>> idType
                    = fieldsType.entrySet().stream().filter((ft) -> ft.getValue().isIdentity()).findFirst();
            if (!idType.isPresent()) {
                throw new IllegalArgumentException(subFormFieldsAnnotation.desClazz()
                        + " has not define a identity prop,exist keys:"
                        + fieldsType.keySet().stream().collect(Collectors.joining(",")));
            }
            this.pkPropertyType = idType.get().getValue();
        } else {
            this.pkPropertyType = null;
        }

        //this.subFormFieldsDescriptor = subFormFieldsDescriptor;
    }


    private String getIdListGetScript() {
        return this.subFormFieldsAnnotation.idListGetScript();
    }


    public SuFormProperties overWriteInstClazz(Class instClazz) {
        this.instClazz = instClazz;
        return this;
    }

//    private static final CustomerGroovyClassLoader loader = new CustomerGroovyClassLoader();
//
//    private  static final class CustomerGroovyClassLoader extends GroovyClassLoader {
//        public CustomerGroovyClassLoader() {
//            super(new ClassLoader(SuFormProperties.class.getClassLoader()) {
//                      @Override
//                      protected Class<?> findClass(String name) throws ClassNotFoundException {
//                          // return super.findClass(name);
//                          return TIS.get().getPluginManager().uberClassLoader.findClass(name);
//                      }
//                  }
//            );
//        }
//
//        @SuppressWarnings("all")
//        public void loadMyClass(String name, String script) throws Exception {
//            CompilationUnit unit = new CompilationUnit();
//            SourceUnit su = unit.addSource(name, script);
//            ClassCollector collector = createCollector(unit, su);
//            unit.setClassgenCallback(collector);
//            unit.compile(Phases.CLASS_GENERATION);
//            int classEntryCount = 0;
//            for (Object o : collector.getLoadedClasses()) {
//                setClassCacheEntry((Class<?>) o);
//                // System.out.println(o);
//                classEntryCount++;
//            }
//        }
//    }


    @Override
    public DescriptorsJSON.IPropGetter getSubFormIdListGetter(IPropertyType.SubFormFilter filter) {

        if (filter.isIncrProcessExtend()) {
            return (f) -> {
                IPluginStore<?> readerSubFieldStore
                        = HeteroEnum.getDataXReaderAndWriterStore(f.uploadPluginMeta.getPluginContext(), true, f.uploadPluginMeta, Optional.of(f));
                List<?> plugins = readerSubFieldStore.getPlugins();
                return plugins.stream().map((p) -> ((IdentityName) p).identityValue()).collect(Collectors.toList());
            };
        }

        try {
            if (subFormFieldsEnumGetter == null) {
                synchronized (this) {
                    if (subFormFieldsEnumGetter == null) {
                        String className = this.parentClazz.getSimpleName() + "_SubFormIdListGetter_" + subFormField.getName();
                        String pkg = this.parentClazz.getPackage().getName();
                        String script = "	package " + pkg + " ;"
                                + "import java.util.Map;"
                                + "import com.qlangtech.tis.coredefine.module.action.DataxAction; "
                                + "import com.qlangtech.tis.util.DescriptorsJSON.IPropGetter; "
                                + "import com.qlangtech.tis.extension.IPropertyType; "
                                + "class " + className + " implements IPropGetter {"
                                + "	@Override"
                                + "	public Object build(IPropertyType.SubFormFilter filter) {" + this.getIdListGetScript() + "	}" + "}";
//                        //this.getIdListGetScript()
//                        loader.loadMyClass(className, script);
//                        Class<?> groovyClass = loader.loadClass(pkg + "." + className);
                        subFormFieldsEnumGetter = GroovyShellEvaluate.createParamizerScript(this.parentClazz, className, script);
                        //subFormFieldsEnumGetter = (DescriptorsJSON.IPropGetter) groovyClass.newInstance();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return subFormFieldsEnumGetter;
    }


    @Override
    public Set<Map.Entry<String, PropertyType>> getKVTuples() {
        return fieldsType.entrySet();
    }


//    public JSONObject createSubFormVals(Collection<IdentityName> subFormFieldInstance) {
//        JSONObject vals = new JSONObject();
//
//        if (CollectionUtils.isNotEmpty( subFormFieldInstance)) {
//            for (IdentityName subItem : subFormFieldInstance) {
//                vals.put(String.valueOf(pkPropertyType.getVal(subItem))
//                        , (new RootFormProperties(fieldsType)).getInstancePropsJson(subItem));
//            }
//        }
//
//        return vals;
//    }

    @Override
    protected Map<String, SelectedTab> getSelectedTabs(Collection<IdentityName> subFormFieldInstance) {
        return subFormFieldInstance.stream().collect(
                Collectors.toMap((id) -> id.identityValue(), (tab) -> (SelectedTab) tab));
    }

    @Override
    protected void addSubItems(SelectedTab ext, JSONArray pair) throws Exception {
        DescribableJSON itemJson = null;
        if (ext != null) {
            itemJson = new DescribableJSON(ext);
            pair.add(itemJson.getItemJson());
        }
    }

    /**
     * 子表单执行过程中与线程绑定的上下文对象
     */
    public static class SuFormGetterContext {
        public static final String FIELD_SUBFORM_ID = "id";
        public Describable plugin;
        public UploadPluginMeta param;

        private ConcurrentHashMap<String, Object> props = new ConcurrentHashMap<>();

        public <T> T getContextAttr(String key, Function<String, T> func) {
            return (T) props.computeIfAbsent(key, func);
        }

        public String getSubFormIdentityField() {
            String id = param.getExtraParam(IPropertyType.SubFormFilter.PLUGIN_META_SUBFORM_DETAIL_ID_VALUE);
            if (StringUtils.isEmpty(id)) {
                throw new IllegalStateException(IPropertyType.SubFormFilter.PLUGIN_META_SUBFORM_DETAIL_ID_VALUE + " can not be empty");
            }
            return id;
        }
    }
}
