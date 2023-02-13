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

package com.qlangtech.tis.coredefine.module.action;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.opensymphony.xwork2.ActionProxy;
import com.qlangtech.tis.BasicActionTestCase;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.trigger.util.JsonUtil;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-15 12:13
 **/
public class TestPluginAction extends BasicActionTestCase {

  /**
   * 在DataX实例创建时，使用Mysql类型的Reader时 需要选择导入表的的步骤中需要使用到
   *
   * @throws Exception
   */
  public void testGetPluginConfigInfoWithTargetDescriptorName() throws Exception {
    // this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,);
//    event_submit_do_get_plugin_config_info: y
//    action: plugin_action
//    plugin: dataxReader:require,targetDescriptorName_MySQL,subFormFieldName_selectedTabs,dataxName_baisuitest

    //doGetPluginConfigInfo
    String dataXName = "baisuitestTestcase";
    request.addHeader(DataxReader.HEAD_KEY_REFERER, "/x/" + dataXName + "/config");
    request.setParameter("event_submit_do_get_plugin_config_info", "y");
    request.setParameter("action", "plugin_action");
    request.setParameter("plugin", "dataxReader:require,targetDescriptorName_MySQL,subFormFieldName_selectedTabs,dataxName_" + dataXName);
    //JSONObject content = new JSONObject();

    //content.put(CollectionAction.KEY_INDEX_NAME, TEST_TABLE_EMPLOYEES_NAME);
    //request.setContent(content.toJSONString().getBytes(TisUTF8.get()));

    ActionProxy proxy = getActionProxy();
    this.replay();
    String result = proxy.execute();
    assertEquals("PluginAction_ajax", result);
    AjaxValve.ActionExecResult aResult = showBizResult();
    assertNotNull(aResult);
    assertTrue(aResult.isSuccess());
    Object bizResult = aResult.getBizResult();
    assertNotNull(bizResult);
    JSONObject bizJSON = (JSONObject) bizResult;
    JSONArray plugins = bizJSON.getJSONArray("plugins");
    assertEquals(1, plugins.size());
    JSONObject plugin = plugins.getJSONObject(0);
    JSONObject descriptors = plugin.getJSONObject("descriptors");
    JSONObject descriptor = null;
    JSONObject subFormMeta = null;
    int descriptorCount = 0;
    for (String readerImpl : descriptors.keySet()) {
      assertEquals("com.qlangtech.tis.plugin.datax.DataxMySQLReader", readerImpl);
      descriptor = descriptors.getJSONObject(readerImpl);
      assertNotNull(descriptor);
      subFormMeta = descriptor.getJSONObject("subFormMeta");
      assertNotNull(subFormMeta);

      JsonUtil.assertJSONEqual(TestPluginAction.class, "pluginAction-subformmeta.json", subFormMeta, (m, e, a) -> {
        assertEquals(m, e, a);
      });

      descriptorCount++;
    }
    assertEquals(1, descriptorCount);
    this.verifyAll();

  }

  private ActionProxy getActionProxy() {
    ActionProxy proxy = getActionProxy("/coredefine/corenodemanage.ajax");
    assertNotNull(proxy);
    PluginAction pluginAction = (PluginAction) proxy.getAction();
    assertNotNull(pluginAction);
    return proxy;
  }
}
