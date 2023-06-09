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

package com.qlangtech.tis.offline.module.action;

import com.opensymphony.xwork2.ActionProxy;
import com.qlangtech.tis.BasicActionTestCase;
import com.qlangtech.tis.extension.IPropertyType.SubFormFilter;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.util.UploadPluginMeta;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-17 16:04
 **/
public class TestOfflineDatasourceAction extends BasicActionTestCase {



  public void testDoGetDsTabsVals() throws Exception {

    //request.addHeader(DataxReader.HEAD_KEY_REFERER, "/x/" + dataXName + "/config");
    request.setParameter("emethod", "get_ds_tabs_vals");
    request.setParameter("action", "offline_datasource_action");
    //

    String postContent = "{\n" +
      "    \"tabs\":[\n" +
      "        \"base\"\n" +
      "    ],\n" +
      "    \"name\":\"dataxReader\",\n" +
      "    \"require\":true,\n" +
      "    \"extraParam\":\"" + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_NAME + "_MySQL,"
      + SubFormFilter.PLUGIN_META_SUB_FORM_FIELD + "_selectedTabs," + DataxUtils.DATAX_DB_NAME + "_order,maxReaderTableCount_9999\"\n" +
      "}";
    request.setContent(postContent.getBytes(TisUTF8.get()));

    ActionProxy proxy = getActionProxy();

    String result = proxy.execute();
    assertEquals("PluginAction_ajax", result);
    AjaxValve.ActionExecResult aResult = showBizResult();
    assertNotNull(aResult);
    assertTrue(aResult.isSuccess());
    Object bizResult = aResult.getBizResult();
  }

  private ActionProxy getActionProxy() {
    ActionProxy proxy = getActionProxy("/offline/datasource.ajax");
    assertNotNull(proxy);
    OfflineDatasourceAction action = (OfflineDatasourceAction) proxy.getAction();
    assertNotNull(action);
    return proxy;
  }
}
