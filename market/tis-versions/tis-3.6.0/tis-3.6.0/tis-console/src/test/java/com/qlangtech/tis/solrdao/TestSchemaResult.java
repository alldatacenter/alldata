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
package com.qlangtech.tis.solrdao;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.plugin.solr.schema.FieldTypeFactory;
import com.qlangtech.tis.runtime.module.action.SchemaAction;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.solrdao.impl.ParseResult;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.HeteroEnum;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-27 14:01
 */
public class TestSchemaResult extends TestCase {
  static final String collection = "search4totalpay";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CenterResource.setNotFetchFromCenterRepository();
  }

  /**
   * 在schema配置文件中已经添加了 <br/>
   * <fieldType name="test" class="plugin:test" precisionStep="0" positionIncrementGap="0"/>
   * 该类型也要能自动加入到傻瓜模式下，string类型的联动下拉框 <br/>
   *
   * @throws Exception
   */
//  public void testParseSchemaResultAlreadyContainFieldPlugin() throws Exception {
//    IMessageHandler msgHandler = EasyMock.createMock("msgHandler", IMessageHandler.class);
//    Context context = EasyMock.createMock("context", Context.class);
//
//    assertEquals(FieldTypeFactory.class, HeteroEnum.SOLR_FIELD_TYPE.extensionPoint);
//
//    IPluginStore<FieldTypeFactory> fieldTypePluginStore = TIS.getPluginStore(collection, FieldTypeFactory.class);
//    assertNotNull(fieldTypePluginStore);
//    final String testFieldTypeName = "test";
//
//    final List<FieldTypeFactory> plugins = fieldTypePluginStore.getPlugins();
//    assertTrue(plugins.size() > 0);
//
//    FieldTypeFactory fieldType = fieldTypePluginStore.find(testFieldTypeName);
//    assertNotNull("fieldType can not be null", fieldType);
//
//    try (InputStream schema = this.getClass().getResourceAsStream("s4totalpay-schema-already-contain-fieldtype-plugin.xml")) {
//      EasyMock.replay(msgHandler, context);
//
//      SchemaResult schemaResult = SchemaAction.parseSchemaResultWithPluginCfg(collection, msgHandler, context, IOUtils.toByteArray(schema));
//
//      Collection<SolrFieldsParser.SolrType> fieldTypes = ((ParseResult) schemaResult.getParseResult()).getFieldTypes();
//      assertEquals("fieldTypes size", 2, fieldTypes.size());
//
////      String content = (com.alibaba.fastjson.JSON.toJSONString(schemaResult.toJSON()
////        , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat));
////      try (InputStream assertSchemaResultInput = this.getClass().getResourceAsStream("s4totalpay-schema-already-contain-fieldtype-plugin-schema-result.json")) {
////        assertNotNull(assertSchemaResultInput);
////        FileUtils.write(new File("test.json"), content, TisUTF8.get());
////        assertEquals(StringUtils.trim(IOUtils.toString(assertSchemaResultInput, TisUTF8.get())), content);
////      }
//
//      JsonUtil.assertJSONEqual(this.getClass(), "s4totalpay-schema-already-contain-fieldtype-plugin-schema-result.json"
//        , schemaResult.toJSON(), (m, e, a) -> {
//          assertEquals(m, e, a);
//        });
//    }
//
//    EasyMock.verify(msgHandler, context);
//  }

//  /**
//   * 模拟用户刚添加fieldtype plugin，则用户第一次家在schema中要自动加入 fieldtype
//   *
//   * @throws Exception
//   */
//  public void testParseSchemaResult() throws Exception {
//    IMessageHandler msgHandler = EasyMock.createMock("msgHandler", IMessageHandler.class);
//    Context context = EasyMock.createMock("context", Context.class);
//
//    assertEquals(FieldTypeFactory.class, HeteroEnum.SOLR_FIELD_TYPE.extensionPoint);
//
//    final String collection = "search4totalpay";
//
//    IPluginStore<FieldTypeFactory> fieldTypePluginStore = TIS.getPluginStore(collection, FieldTypeFactory.class);
//    assertNotNull(fieldTypePluginStore);
//    final String testFieldTypeName = "test";
//
//    final List<FieldTypeFactory> plugins = fieldTypePluginStore.getPlugins();
//    assertTrue(plugins.size() > 0);
//
//    FieldTypeFactory fieldType = fieldTypePluginStore.find(testFieldTypeName);
//    assertNotNull("fieldType can not be null", fieldType);
//
//    try (InputStream schema = this.getClass().getResourceAsStream("s4totalpay-schema.xml")) {
//      EasyMock.replay(msgHandler, context);
//
//      SchemaResult schemaResult = SchemaAction.parseSchemaResultWithPluginCfg(collection, msgHandler, context, IOUtils.toByteArray(schema));
//
//      Collection<SolrFieldsParser.SolrType> fieldTypes = ((ParseResult) schemaResult.getParseResult()).getFieldTypes();
//      assertEquals("fieldTypes size", 16, fieldTypes.size());
//
////      String content = (com.alibaba.fastjson.JSON.toJSONString(schemaResult.toJSON()
////        , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat));
////
////      try (InputStream assertSchemaResultInput = this.getClass().getResourceAsStream("assertSchemaResult.json")) {
////        assertNotNull(assertSchemaResultInput);
////        assertEquals(IOUtils.toString(assertSchemaResultInput, TisUTF8.get()), content);
////      }
//
//      JsonUtil.assertJSONEqual(this.getClass(), "assertSchemaResult.json", schemaResult.toJSON(), (m, e, a) -> {
//        assertEquals(m, e, a);
//      });
//    }
//
//    EasyMock.verify(msgHandler, context);
//
//  }
}
