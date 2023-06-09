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
package com.qlangtech.tis.config.module.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.opensymphony.xwork2.ActionProxy;
import com.qlangtech.tis.BasicActionTestCase;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.CreateNewTaskResult;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-06 16:19
 */
public class TestFullbuildWorkflowAction extends BasicActionTestCase {

  /**
   * 执行异步任务 创建->任务提提交->等待反馈-> 完成
   *
   * @throws Exception
   */
  public void testAsynJobExecute() throws Exception {
    FullbuildWorkflowAction.MAX_CAS_RETRY_COUNT = 100;
    /**=====================================================
     * 创建任务
     =======================================================*/
    int taskId = -1;
    final int subTaskSize = 20;
    try {
      String dataxName = "ttt";
      request.setParameter("emethod", "createNewTask");
      request.setParameter("action", "fullbuild_workflow_action");
      request.setParameter(IFullBuildContext.KEY_TRIGGER_TYPE, String.valueOf(TriggerType.MANUAL.getValue()));
      request.setParameter(IFullBuildContext.KEY_APP_NAME, dataxName);

      ActionProxy proxy = getActionProxy();
      this.replay();
      String result = proxy.execute();
      assertEquals("FullbuildWorkflowAction_ajax", result);
      AjaxValve.ActionExecResult aResult = showBizResult();
      assertNotNull(aResult);
      assertTrue(aResult.isSuccess());
      CreateNewTaskResult bizResult = (CreateNewTaskResult) aResult.getBizResult();
      assertNotNull(bizResult);
      taskId = bizResult.getTaskid();
      assertTrue(taskId > 0);
      this.verifyAll();

      this.setUp();


      /**======================================================
       * 提交异步任务
       ========================================================*/
      request.setParameter("emethod", "taskComplete");
      request.setParameter("action", "fullbuild_workflow_action");
      request.setParameter(JobCommon.KEY_TASK_ID, String.valueOf(taskId));
      request.setParameter(IParamContext.KEY_EXEC_RESULT, String.valueOf(ExecResult.SUCCESS.getValue()));

      Set<String> jobs = Sets.newHashSet();
      String[] jobsName = new String[subTaskSize];
      for (int i = 0; i < subTaskSize; i++) {
        jobsName[i] = "customer_order_relation_" + i + ".json";
        jobs.add(jobsName[i]);
      }
      request.setParameter(IParamContext.KEY_ASYN_JOB_NAME, jobsName);
      proxy = getActionProxy();
      result = proxy.execute();
      assertEquals("FullbuildWorkflowAction_ajax", result);
      aResult = showBizResult();
      assertNotNull(aResult);
      assertTrue(aResult.isSuccess());

      WorkFlowBuildHistory history = this.runContext.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByPrimaryKey(taskId);
      assertNotNull("taskid:" + taskId + " relevant history", history);
      assertTrue(ExecResult.parse(history.getState()) == ExecResult.ASYN_DOING);


      JSONObject subTaskStatus = JSON.parseObject(history.getAsynSubTaskStatus());
      assertEquals(subTaskSize, subTaskStatus.keySet().size());
      subTaskStatus.forEach((key, val) -> {
        assertTrue(jobs.contains(key));
        JSONObject s = (JSONObject) val;
        assertFalse(key, s.getBoolean(IParamContext.KEY_ASYN_JOB_SUCCESS));
        assertFalse(key, s.getBoolean(IParamContext.KEY_ASYN_JOB_COMPLETE));
      });

      /**======================================================
       * 等待接收反馈信息
       ========================================================*/
      ExecutorService executorService = Executors.newFixedThreadPool(20);
      Throwable[] excep = new Throwable[1];
      CountDownLatch countDown = new CountDownLatch(subTaskSize);
      final int tskid = taskId;
      for (String jobName : jobsName) {
        executorService.submit(() -> {
          try {
            TestFullbuildWorkflowAction subTest = new TestFullbuildWorkflowAction();
            subTest.setUp();
            subTest.feedbackAsynTaskStatus(tskid, jobName);
            subTest.tearDown();
          } catch (Throwable e) {
            excep[0] = e;
          } finally {
            countDown.countDown();
          }
        });
//        this.setUp();
//        feedbackAsynTaskStatus(tskid, jobName);
      }
      countDown.await();
      if (excep[0] != null) {
        throw new RuntimeException(excep[0]);
      }

      history = this.runContext.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByPrimaryKey(taskId);

      subTaskStatus = JSON.parseObject(history.getAsynSubTaskStatus());
      assertEquals(subTaskSize, subTaskStatus.keySet().size());
      subTaskStatus.forEach((key, val) -> {
        assertTrue(jobs.contains(key));
        JSONObject s = (JSONObject) val;
        assertTrue(key, s.getBoolean(IParamContext.KEY_ASYN_JOB_SUCCESS));
        assertTrue(key, s.getBoolean(IParamContext.KEY_ASYN_JOB_COMPLETE));
      });

      assertEquals(ExecResult.SUCCESS, ExecResult.parse(history.getState()));
      assertEquals((int) subTaskSize + 1, (int) history.getLastVer());

    } finally {
      if (taskId > 0) {
        this.runContext.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().deleteByPrimaryKey(taskId);
      }
    }

  }

  // public void testFeedbackAsynTaskStatus() throws Exception {
  // feedbackAsynTaskStatus(887, "customer_order_relation_7.json");

//    while (true) {
//      WorkFlowBuildHistory buildHistory = this.runContext.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByPrimaryKey(887);
//      System.out.println("last_ver:" + buildHistory.getLastVer());
//      Thread.sleep(1000);
//    }
  //}

  private void feedbackAsynTaskStatus(int taskId, String jobName) throws Exception {

    WorkFlowBuildHistory buildHistory = this.runContext.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByPrimaryKey(taskId);
    assertNotNull("buildHistory can not be null", buildHistory);

    request.setParameter(IParamContext.KEY_REQUEST_DISABLE_TRANSACTION, String.valueOf(true));
    request.setParameter("emethod", "feedbackAsynTaskStatus");
    request.setParameter("action", "fullbuild_workflow_action");
    request.setParameter(JobCommon.KEY_TASK_ID, String.valueOf(taskId));
    request.setParameter(IParamContext.KEY_ASYN_JOB_NAME, jobName);
    request.setParameter(IParamContext.KEY_ASYN_JOB_SUCCESS, String.valueOf(true));
    ActionProxy proxy = getActionProxy();
    String result = proxy.execute();
    assertEquals("FullbuildWorkflowAction_ajax", result);
    AjaxValve.ActionExecResult aResult = showBizResult();
    assertNotNull(aResult);
    assertTrue(aResult.isSuccess());
  }


  public void testDoCreateNewTaskWithSingleTableCollectionFullBuild() throws Exception {
    // createMockCollection(COLLECTION_NAME);
    String collectionName = "search4employee4local";
    request.setParameter("emethod", "createNewTask");
    request.setParameter("action", "fullbuild_workflow_action");
    request.setParameter(IFullBuildContext.KEY_TRIGGER_TYPE, String.valueOf(TriggerType.MANUAL.getValue()));
    request.setParameter(IFullBuildContext.KEY_APP_NAME, collectionName);
    // JSONObject content = new JSONObject();

//    content.put(CollectionAction.KEY_INDEX_NAME, TEST_TABLE_EMPLOYEES_NAME);
//    request.setContent(content.toJSONString().getBytes(TisUTF8.get()));

    ActionProxy proxy = getActionProxy();
    this.replay();
    String result = proxy.execute();
    assertEquals("FullbuildWorkflowAction_ajax", result);
    AjaxValve.ActionExecResult aResult = showBizResult();
    assertNotNull(aResult);
    assertTrue(aResult.isSuccess());
    this.verifyAll();
  }

  private ActionProxy getActionProxy() {
    ActionProxy proxy = getActionProxy("/config/config.ajax");
    assertNotNull(proxy);
    FullbuildWorkflowAction fullbuildWorkflowAction = (FullbuildWorkflowAction) proxy.getAction();
    assertNotNull(fullbuildWorkflowAction);
    return proxy;
  }

}
