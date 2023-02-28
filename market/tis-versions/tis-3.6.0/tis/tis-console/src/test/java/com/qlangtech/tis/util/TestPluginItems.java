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
package com.qlangtech.tis.util;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.coredefine.module.action.DataxAction;
import com.qlangtech.tis.extension.impl.*;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.offline.module.action.OfflineDatasourceAction;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.PluginStubUtils;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.trigger.util.JsonUtil;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestPluginItems extends TestCase {
  static final String dataXName = "kk";
  static final UploadPluginMeta subFieldPluginMeta = UploadPluginMeta.parse("dataxReader:require,subFormFieldName_selectedTabs,targetDescriptorName_MySQL,dataxName_" + dataXName);

  KeyedPluginStore<DataxReader> readerStore;


  @Override
  public void setUp() throws Exception {

    PluginStubUtils.setDataDir(Config.DEFAULT_DATA_DIR);
    CenterResource.setNotFetchFromCenterRepository();
    HttpUtils.addMockGlobalParametersConfig();
    List<Option> opts = Lists.newArrayList();
    opts.add(new Option("orderdb", "orderdb"));
    OfflineDatasourceAction.existDbs = opts;
    DataxAction.cleanDepsCache();
    this.readerStore = DataxReader.getPluginStore(null, dataXName);
    File targetFile = readerStore.getTargetFile().getFile();
    File parentDir = targetFile.getParentFile();
    FileUtils.deleteQuietly(parentDir);
  }

  public void testDataXItemsSave() throws Exception {
    validateRootFormSave();
    validateSubFormSave();
    validatePluginValue();
    validateSubField2Json();
  }

  private void validatePluginValue() throws Exception {

    DataxReader reader = readerStore.getPlugin();
    assertNotNull(reader);
    PluginFormProperties rootPropertyTypes = reader.getDescriptor().getPluginFormPropertyTypes();

    assertTrue("get RootFormProperties process result", rootPropertyTypes.accept(new PluginFormProperties.IVisitor() {
      @Override
      public Boolean visit(RootFormProperties props) {
        Map<String, PropertyType> propertiesType = props.propertiesType;
        validatePropertyValue(propertiesType, "dbName", "order1", reader);
        validatePropertyValue(propertiesType, "splitPk", true, reader);

//        validatePropertyValue(propertiesType, "template"
//          , IOUtils.loadResourceFromClasspath(TestPluginItems.class, "datax_reader_mysql_prop_template.json"), reader);

        assertEquals(3, propertiesType.size());

        return true;
      }
    }));
    Optional<IPropertyType.SubFormFilter> subFormFilter = subFieldPluginMeta.getSubFormFilter();
    assertTrue(subFormFilter.isPresent());
    PluginFormProperties pluginFormPropertyTypes = reader.getDescriptor().getPluginFormPropertyTypes(subFormFilter);
    assertTrue("get SuFormProperties process result", pluginFormPropertyTypes.accept(new PluginFormProperties.IVisitor() {
      @Override
      public Boolean visit(BaseSubFormProperties props) {
        assertEquals("selectedTabs", props.getSubFormFieldName());
        //Map<String, PropertyType> fieldsType = props.fieldsType;
        //assertEquals(3, fieldsType.size());

        List<ISelectedTab> selectedTabs = reader.getSelectedTabs();
        assertNotNull("selectedTabs can not be null", selectedTabs);
        assertEquals(1, selectedTabs.size());
        ISelectedTab selectedTab = selectedTabs.get(0);


        assertEquals("customer_order_relation", selectedTab.getName());
        assertEquals("1=999", selectedTab.getWhere());

        List<CMeta> cols = selectedTab.getCols();
        assertEquals(5, cols.size());
        Set<String> selectedCols = Sets.newHashSet("customerregister_id", "waitingorder_id", "kind", "create_time", "last_ver");
        cols.forEach((c) -> assertTrue(selectedCols.contains(c.getName())));

        return true;
      }
    }));


  }

  private void validateSubField2Json() throws Exception {
    IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
    // EasyMock.expect(pluginContext.isDataSourceAware()).andReturn(false);
    EasyMock.expect(pluginContext.isCollectionAware()).andReturn(false).anyTimes();
    EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("/x/" + dataXName + "/config").times(2);


    EasyMock.replay(pluginContext);
    HeteroList<?> heteroList = subFieldPluginMeta.getHeteroList(pluginContext);
    JSONObject pluginDesc = heteroList.toJSON();


    JsonUtil.assertJSONEqual(this.getClass(), "datax_reader_mysql_post_subfield_to_json.json", pluginDesc.toJSONString(), (m, e, a) -> {
      assertEquals(m, e, a);
    });

    EasyMock.verify(pluginContext);
  }

  private void validatePropertyValue(Map<String, PropertyType> propertiesType, String key, Object value, DataxReader reader) {
    PropertyType pt = null;
    pt = propertiesType.get(key);
    assertNotNull(pt);
    assertEquals(value, pt.getVal(reader));
  }

  private void validateSubFormSave() {
    IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
    EasyMock.expect(pluginContext.isDataSourceAware()).andReturn(false);
    EasyMock.expect(pluginContext.isCollectionAware()).andReturn(false).anyTimes();
    EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("/x/" + dataXName + "/config").times(2);
    Context context = EasyMock.createMock("context", Context.class);
    //targetDescriptorName_MySQL,subFormFieldName_selectedTabs

    Optional<IPropertyType.SubFormFilter> subFormFilter = subFieldPluginMeta.getSubFormFilter();
    assertTrue("subFormFilter.isPresent():true", subFormFilter.isPresent());
    PluginItems pluginItems = new PluginItems(pluginContext, subFieldPluginMeta);
    IControlMsgHandler fieldErrorHandler = EasyMock.createMock("fieldErrorHandler", IControlMsgHandler.class);

    JSONArray jsonArray = IOUtils.loadResourceFromClasspath(TestPluginItems.class
      , "datax_reader_mysql_post_subfield_form.json", true, (input) -> {
        return JSON.parseArray(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
      });

    JSONArray itemsArray = jsonArray.getJSONArray(0);
    // Optional.empty();
    List<AttrValMap> items = AttrValMap.describableAttrValMapList( itemsArray, subFormFilter);
    pluginItems.items = items;

    EasyMock.replay(pluginContext, context, fieldErrorHandler);
    pluginItems.save(context);


    EasyMock.verify(pluginContext, context, fieldErrorHandler);
  }

  private void validateRootFormSave() {

    IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
    EasyMock.expect(pluginContext.isDataSourceAware()).andReturn(false);
    EasyMock.expect(pluginContext.isCollectionAware()).andReturn(false).anyTimes();
    EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("/x/" + dataXName + "/config").anyTimes();
    Context context = EasyMock.createMock("context", Context.class);
    //targetDescriptorName_MySQL,subFormFieldName_selectedTabs
    UploadPluginMeta pluginMeta = UploadPluginMeta.parse("dataxReader:require,dataxName_" + dataXName);

    PluginItems pluginItems = new PluginItems(pluginContext, pluginMeta);
    IControlMsgHandler fieldErrorHandler = EasyMock.createMock("fieldErrorHandler", IControlMsgHandler.class);

    JSONArray jsonArray = IOUtils.loadResourceFromClasspath(TestPluginItems.class, "datax_reader_mysql_post.json", true, (input) -> {
      return JSON.parseArray(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
    });

    JSONArray itemsArray = jsonArray.getJSONArray(0);
    Optional<IPropertyType.SubFormFilter> subFormFilter = pluginMeta.getSubFormFilter();
    assertFalse("subFormFilter.isPresent():false", subFormFilter.isPresent());
    List<AttrValMap> items = AttrValMap.describableAttrValMapList( itemsArray, subFormFilter);
    pluginItems.items = items;

    EasyMock.replay(pluginContext, context, fieldErrorHandler);
    pluginItems.save(context);


    EasyMock.verify(pluginContext, context, fieldErrorHandler);
  }
}
