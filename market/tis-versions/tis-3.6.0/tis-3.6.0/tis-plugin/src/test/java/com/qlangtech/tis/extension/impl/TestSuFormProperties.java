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

import com.google.common.collect.Lists;
import com.qlangtech.tis.IPluginEnum;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.impl.TestIncrSourceSelectedTabExtend;
import com.qlangtech.tis.async.message.client.consumer.impl.TestMQListenerFactory;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.*;
import junit.framework.TestCase;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-01 10:40
 **/
public class TestSuFormProperties extends TestCase {
    String dataXName = "testDataXName";
    String pluginName = "test_plugin";
    File writerDescFile;

    final String meta = pluginName + ":require," + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_NAME + "_" + SubFieldContainPlugin.PLUGIN_NAME
            + "," + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_IMPLEMENTION
            + "_" + SubFieldContainPlugin.class.getName()
            + "," + UploadPluginMeta.KEY_TARGET_PLUGIN_DESC + "_incr_process_extend,subFormFieldName_"
            + SubFieldContainPlugin.SUB_PROP_FIELD_NAME + "," + DataxUtils.DATAX_NAME + "_" + dataXName + ",maxReaderTableCount_9999";

    final UploadPluginMeta pluginMeta = UploadPluginMeta.parse(meta);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.writerDescFile = IDataxProcessor.getWriterDescFile(null, dataXName);
        FileUtils.write(writerDescFile, RewriteSuFormPropertiesPlugin.class.getName(), TisUTF8.get(), false);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteQuietly(writerDescFile);
    }

    public void testSubformDetailedClick() throws Exception {
        buildSourceExtendSelectedTestEnvironment((pluginContext,pluginMeta, subReader, extendTabProps, tabId) -> {
            HeteroList<?> heteroList = pluginMeta.getHeteroList(pluginContext);
            Assert.assertNotNull(heteroList);
            //test subform_detailed_click=============================
            SuFormProperties.setSuFormGetterContext(extendTabProps, pluginMeta, tabId);
            heteroList = pluginMeta.getHeteroList(pluginContext);
            System.out.println(JsonUtil.toString(heteroList.toJSON()));
        });
    }

    public void testGetPluginFormPropertyTypesWithIncrSourceExtendSelected() throws Exception {


        buildSourceExtendSelectedTestEnvironment((pluginContext,pluginMeta, subReader, extendTabProps, tabId) -> {

            PluginFormProperties formPropertyTypes = subReader.getDescriptor().getPluginFormPropertyTypes(pluginMeta.getSubFormFilter());

            Assert.assertTrue("formPropertyTypes:" + formPropertyTypes.getClass(), formPropertyTypes instanceof IncrSourceExtendSelected);
            formPropertyTypes.accept(new PluginFormProperties.IVisitor() {
                @Override
                public <T> T visit(RootFormProperties props) {
                    Assert.fail("shall not execute");
                    return null;
                }

                @Override
                public <T> T visit(BaseSubFormProperties props) {

                    Field subFormField = props.subFormField;
                    Assert.assertEquals(SubFieldContainPlugin.SUB_PROP_FIELD_NAME, subFormField.getName());
                    Class instClazz = props.instClazz;
                    Assert.assertEquals(TestIncrSourceSelectedTabExtend.class, instClazz);
                    Descriptor subFormFieldsDescriptor = props.subFormFieldsDescriptor;
                    Assert.assertEquals(TIS.get().getDescriptor(TestIncrSourceSelectedTabExtend.class), subFormFieldsDescriptor);
                    return null;
                }
            });


            //=============================

            HeteroList<?> heteroList = pluginMeta.getHeteroList(pluginContext);
            Assert.assertNotNull(heteroList);

            Assert.assertEquals(1, heteroList.getDescriptors().size());
            System.out.println(JsonUtil.toString(heteroList.toJSON()));
        });

//        IPluginContext pluginContext = EasyMock.mock("pluginContext", IPluginContext.class);
//        EasyMock.expect(pluginContext.isCollectionAware()).andReturn(true).anyTimes();
//        EasyMock.expect(pluginContext.getCollectionName()).andReturn(dataXName).anyTimes();
//        EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("").anyTimes();
//        EasyMock.replay(pluginContext);
//
//        IPluginStore pluginStore = HeteroEnum.MQ.getPluginStore(pluginContext, null);
//        TestMQListenerFactory mqFactory = new TestMQListenerFactory();
//        List<Descriptor.ParseDescribable<?>> dlist = Lists.newArrayList(new Descriptor.ParseDescribable(mqFactory));
//        pluginStore.setPlugins(pluginContext, Optional.empty(), dlist);
//
//        final String tabId = "record1";
//
//        SubFieldContainPlugin subReader = new SubFieldContainPlugin();
//        subReader.prop2 = "testProp2";
//        subReader.prop3 = "testProp3";
//        List<SelectedTab> tabs = Lists.newArrayList();
//        SelectedTab tab = new SelectedTab();
//        tab.cols = Lists.newArrayList("col1", "clo2", "col3");
//        tab.name = tabId;
//        tab.setWhere("1=1");
//        tabs.add(tab);
//        subReader.selectedTabs = tabs;
//
//        pluginStore = HeteroEnum.DATAX_READER.getPluginStore(pluginContext, pluginMeta);
//        pluginStore.setPlugins(pluginContext, Optional.empty()
//                , Lists.newArrayList(new Descriptor.ParseDescribable(tabs)));
//
//        dlist = Lists.newArrayList(new Descriptor.ParseDescribable(subReader));
//        String metaWithOutSubfield = pluginName + ":require," + DataxUtils.DATAX_NAME + "_" + dataXName + ",maxReaderTableCount_9999";
//        pluginStore = HeteroEnum.DATAX_READER.getPluginStore(pluginContext, UploadPluginMeta.parse(metaWithOutSubfield));
//        pluginStore.setPlugins(pluginContext, Optional.empty(), dlist);
//
//
//        IPluginEnum<IncrSourceSelectedTabExtend> incrTabExtendPluginEnum = HeteroEnum.of(IncrSourceSelectedTabExtend.HETERO_ENUM_IDENTITY);
//        Assert.assertNotNull(incrTabExtendPluginEnum);
//        IPluginStore incrTabExtendPluginStore = incrTabExtendPluginEnum.getPluginStore(pluginContext, pluginMeta);
//        Assert.assertNotNull(incrTabExtendPluginStore);
//       final  TestIncrSourceSelectedTabExtend extendTabProps = new TestIncrSourceSelectedTabExtend();
//        extendTabProps.testPorop = "IncrSourceSelectedTabExtendProp";
//        extendTabProps.setName(tabId);
//        incrTabExtendPluginStore.setPlugins(pluginContext, Optional.empty(), Lists.newArrayList(new Descriptor.ParseDescribable(extendTabProps)));
//
//        IPropertyType.SubFormFilter subFormFilter = pluginMeta.getSubFormFilter().get();
////                = new IPropertyType.SubFormFilter(pluginMeta
////                , IPropertyType.SubFormFilter.KEY_INCR_PROCESS_EXTEND, SubFieldContainPlugin.class.getName(), SubFieldContainPlugin.SUB_PROP_FIELD_NAME);
//
//        PluginFormProperties formPropertyTypes = subReader.getDescriptor().getPluginFormPropertyTypes(Optional.of(subFormFilter));
//
//        Assert.assertTrue("formPropertyTypes:" + formPropertyTypes.getClass(), formPropertyTypes instanceof IncrSourceExtendSelected);
//        formPropertyTypes.accept(new PluginFormProperties.IVisitor() {
//            @Override
//            public <T> T visit(RootFormProperties props) {
//                Assert.fail("shall not execute");
//                return null;
//            }
//
//            @Override
//            public <T> T visit(BaseSubFormProperties props) {
//
//                Field subFormField = props.subFormField;
//                Assert.assertEquals(SubFieldContainPlugin.SUB_PROP_FIELD_NAME, subFormField.getName());
//                Class instClazz = props.instClazz;
//                Assert.assertEquals(TestIncrSourceSelectedTabExtend.class, instClazz);
//                Descriptor subFormFieldsDescriptor = props.subFormFieldsDescriptor;
//                Assert.assertEquals(TIS.get().getDescriptor(TestIncrSourceSelectedTabExtend.class), subFormFieldsDescriptor);
//                return null;
//            }
//        });
//
//
//        //=============================
//
//        HeteroList<?> heteroList = pluginMeta.getHeteroList(pluginContext);
//        Assert.assertNotNull(heteroList);
//
//        Assert.assertEquals(1, heteroList.getDescriptors().size());
//        System.out.println(JsonUtil.toString(heteroList.toJSON()));
//
//
//        //test subform_detailed_click=============================
//        SuFormProperties.setSuFormGetterContext(extendTabProps, pluginMeta, tabId);
//        heteroList = pluginMeta.getHeteroList(pluginContext);
//        System.out.println(JsonUtil.toString(heteroList.toJSON()));
//
//        EasyMock.verify(pluginContext);
    }

    interface IStartSourceExtendSelectedTestEnvironment {

        void apply(IPluginContext pluginContext, UploadPluginMeta pluginMeta
                , SubFieldContainPlugin subReader, TestIncrSourceSelectedTabExtend extendTabProps, String tabId) throws Exception;
    }

    private void buildSourceExtendSelectedTestEnvironment(IStartSourceExtendSelectedTestEnvironment tester) throws Exception {
        IPluginContext pluginContext = EasyMock.mock("pluginContext", IPluginContext.class);
        EasyMock.expect(pluginContext.isCollectionAware()).andReturn(true).anyTimes();
        EasyMock.expect(pluginContext.getCollectionName()).andReturn(dataXName).anyTimes();
        EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("").anyTimes();
        EasyMock.replay(pluginContext);

        IPluginStore pluginStore = HeteroEnum.MQ.getPluginStore(pluginContext, null);
        TestMQListenerFactory mqFactory = new TestMQListenerFactory();
        List<Descriptor.ParseDescribable<?>> dlist = Lists.newArrayList(new Descriptor.ParseDescribable(mqFactory));
        pluginStore.setPlugins(pluginContext, Optional.empty(), dlist);

        final String tabId = "record1";

        SubFieldContainPlugin subReader = new SubFieldContainPlugin();
        subReader.prop2 = "testProp2";
        subReader.prop3 = "testProp3";
        List<SelectedTab> tabs = Lists.newArrayList();
        SelectedTab tab = new SelectedTab();
        tab.cols = Lists.newArrayList("col1", "clo2", "col3");
        tab.name = tabId;
        tab.setWhere("1=1");
        tabs.add(tab);
        subReader.selectedTabs = tabs;

        pluginStore = HeteroEnum.DATAX_READER.getPluginStore(pluginContext, pluginMeta);
        pluginStore.setPlugins(pluginContext, Optional.empty()
                , Lists.newArrayList(new Descriptor.ParseDescribable(tabs)));

        dlist = Lists.newArrayList(new Descriptor.ParseDescribable(subReader));
        String metaWithOutSubfield = pluginName + ":require," + DataxUtils.DATAX_NAME + "_" + dataXName + ",maxReaderTableCount_9999";
        pluginStore = HeteroEnum.DATAX_READER.getPluginStore(pluginContext, UploadPluginMeta.parse(metaWithOutSubfield));
        pluginStore.setPlugins(pluginContext, Optional.empty(), dlist);


        IPluginEnum<IncrSelectedTabExtend> incrTabExtendPluginEnum = HeteroEnum.of(IncrSelectedTabExtend.HETERO_ENUM_IDENTITY);
        Assert.assertNotNull(incrTabExtendPluginEnum);
        IPluginStore incrTabExtendPluginStore = incrTabExtendPluginEnum.getPluginStore(pluginContext, pluginMeta);
        Assert.assertNotNull(incrTabExtendPluginStore);
        final TestIncrSourceSelectedTabExtend extendTabProps = new TestIncrSourceSelectedTabExtend();
        extendTabProps.testPorop = "IncrSourceSelectedTabExtendProp";
        extendTabProps.setName(tabId);
        incrTabExtendPluginStore.setPlugins(pluginContext, Optional.empty(), Lists.newArrayList(new Descriptor.ParseDescribable(extendTabProps)));

        //  IPropertyType.SubFormFilter subFormFilter = pluginMeta.getSubFormFilter().get();
//                = new IPropertyType.SubFormFilter(pluginMeta
//                , IPropertyType.SubFormFilter.KEY_INCR_PROCESS_EXTEND, SubFieldContainPlugin.class.getName(), SubFieldContainPlugin.SUB_PROP_FIELD_NAME);
        tester.apply(pluginContext, pluginMeta, subReader, extendTabProps, tabId);
//        PluginFormProperties formPropertyTypes = subReader.getDescriptor().getPluginFormPropertyTypes(Optional.of(subFormFilter));
//
//        Assert.assertTrue("formPropertyTypes:" + formPropertyTypes.getClass(), formPropertyTypes instanceof IncrSourceExtendSelected);
//        formPropertyTypes.accept(new PluginFormProperties.IVisitor() {
//            @Override
//            public <T> T visit(RootFormProperties props) {
//                Assert.fail("shall not execute");
//                return null;
//            }
//
//            @Override
//            public <T> T visit(BaseSubFormProperties props) {
//
//                Field subFormField = props.subFormField;
//                Assert.assertEquals(SubFieldContainPlugin.SUB_PROP_FIELD_NAME, subFormField.getName());
//                Class instClazz = props.instClazz;
//                Assert.assertEquals(TestIncrSourceSelectedTabExtend.class, instClazz);
//                Descriptor subFormFieldsDescriptor = props.subFormFieldsDescriptor;
//                Assert.assertEquals(TIS.get().getDescriptor(TestIncrSourceSelectedTabExtend.class), subFormFieldsDescriptor);
//                return null;
//            }
//        });
//
//
//        //=============================
//
//        HeteroList<?> heteroList = pluginMeta.getHeteroList(pluginContext);
//        Assert.assertNotNull(heteroList);
//
//        Assert.assertEquals(1, heteroList.getDescriptors().size());
//        System.out.println(JsonUtil.toString(heteroList.toJSON()));




        EasyMock.verify(pluginContext);
    }




    public void testVisitSubForm() {
        SubFieldContainPlugin plugin = new SubFieldContainPlugin();

        String meta = pluginName + ":require," + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_IMPLEMENTION + "_" + SubFieldContainPlugin.class.getName()
                + "," + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_NAME + "_incr_process_extend,subFormFieldName_"
                + SubFieldContainPlugin.SUB_PROP_FIELD_NAME + "," + DataxUtils.DATAX_NAME + "_" + dataXName + ",maxReaderTableCount_9999";
        // dataxName_" + dataXName
        UploadPluginMeta pluginMeta = UploadPluginMeta.parse(meta);

        IPropertyType.SubFormFilter subFormFilter = pluginMeta.getSubFormFilter().get();
//                = new IPropertyType.SubFormFilter(pluginMeta
//                , new UploadPluginMeta.TargetDesc(SubFieldContainPlugin.PLUGIN_NAME), SubFieldContainPlugin.class.getName(), SubFieldContainPlugin.SUB_PROP_FIELD_NAME);

        Descriptor<DataxReader> descriptor = plugin.getDescriptor();
        assertNotNull("descriptor can not be null", descriptor);
        PluginFormProperties pluginFormPropertyTypes = descriptor.getPluginFormPropertyTypes(Optional.of(subFormFilter));
        assertNotNull("pluginFormPropertyTypes can not be null", pluginFormPropertyTypes);
        // AtomicBoolean hasExecVisitSubForm = new AtomicBoolean(false);
        boolean hasExecVisitSubForm
                = pluginFormPropertyTypes.accept(new DescriptorsJSON.SubFormFieldVisitor(Optional.of(subFormFilter)) {
            @Override
            public Boolean visit(//SuFormProperties.SuFormPropertiesBehaviorMeta behaviorMeta,
                                 BaseSubFormProperties props) {
//                assertNotNull("behaviorMeta can not be null", behaviorMeta);
                assertNotNull("prop can not be null", props);

//                assertEquals("设置", behaviorMeta.getClickBtnLabel());
//                Map<String, SuFormProperties.SuFormPropertyGetterMeta>
//                        onClickFillData = behaviorMeta.getOnClickFillData();
//                assertEquals("onClickFillData.size() > 0", 2, onClickFillData.size());


//                SuFormProperties.SuFormPropertyGetterMeta getterMeta = onClickFillData.get("cols");
//                assertNotNull(getterMeta);
//                assertEquals("getTableMetadata", getterMeta.getMethod());
//                assertTrue("getParams equal"
//                        , CollectionUtils.isEqualCollection(Collections.singleton("id"), getterMeta.getParams()));
//
//
//                getterMeta = onClickFillData.get("recordField");
//                assertNotNull(getterMeta);
//                assertEquals("getPrimaryKeys", getterMeta.getMethod());
//                assertTrue("getParams equal"
//                        , CollectionUtils.isEqualCollection(Collections.singleton("id"), getterMeta.getParams()));

                //===============================================
                Set<Map.Entry<String, PropertyType>> kvTuples = props.getKVTuples();
                assertEquals(3, kvTuples.size());


                assertTrue(CollectionUtils.isEqualCollection(Lists.newArrayList("name", "subProp1", "subProp2")
                        , kvTuples.stream().map((kv) -> kv.getKey()).collect(Collectors.toList())));

                Object subField = props.newSubDetailed();

                assertTrue("subField must be type of " + SelectedTab.class.getSimpleName()
                        , subField instanceof SelectedTab);

                return true;
            }
        });

        assertTrue("hasExecVisitSubForm must has execute", hasExecVisitSubForm);
    }

//    public void testVisitSubForm() {
//
//    }
}
