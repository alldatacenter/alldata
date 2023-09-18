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
package com.qlangtech.tis.runtime.module.action;

import com.qlangtech.tis.BasicActionTestCase;
import com.qlangtech.tis.manage.common.HttpUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-01 12:40
 */
public class TestAddAppAction extends BasicActionTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    HttpUtils.mockConnMaker.clearStubs();
//    HttpUtils.addMockApply(CoreAction.CREATE_COLLECTION_PATH, (url) -> {
//      return TestCollectionAction.class.getResourceAsStream("s4employees_create_success.json");
//    });
  }

//  /**
//   * 测试创建
//   *
//   * @throws Exception
//   */
//  public void testDoCreateCollection() throws Exception {
//    request.setParameter("emethod", "create_collection");
//    request.setParameter("action", "add_app_action");
//
//    try (InputStream content = this.getClass().getResourceAsStream("create_confirm_index_http_body.json")) {
//      assertNotNull(content);
//      request.setContent(IOUtils.toByteArray(content));
//    }
//    ITISCoordinator zkCoordinator = MockZKUtils.createZkMock();
//    MockZooKeeperGetter.mockCoordinator = zkCoordinator;
//
//    setCollection(TestSchemaAction.collection);
//
//    ActionProxy proxy = getActionProxy();
//    replay();
//    String result = proxy.execute();
//    assertEquals("AddAppAction_ajax", result);
//    AjaxValve.ActionExecResult aResult = showBizResult();
//    assertNotNull(aResult);
//    assertTrue(aResult.isSuccess());
//    verifyAll();
//
//    IAppSource appSource = IAppSource.load(null, TestSchemaAction.collection);
//    // assertTrue(appSource instanceof SingleTableAppSource);
//  }

//  private ActionProxy getActionProxy() {
//    ActionProxy proxy = getActionProxy("/runtime/addapp.ajax");
//    assertNotNull(proxy);
//    AddAppAction schemaAction = (AddAppAction) proxy.getAction();
//    assertNotNull(schemaAction);
//    return proxy;
//  }
}
