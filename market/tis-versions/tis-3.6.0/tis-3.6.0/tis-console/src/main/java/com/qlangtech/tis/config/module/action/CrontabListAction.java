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
package com.qlangtech.tis.config.module.action;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.TriggerCrontab;
import com.qlangtech.tis.manage.common.apps.TerminatorAdminAppsFetcher;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * 显示所有有效的全量触发器
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年10月28日下午1:33:02
 */
public class CrontabListAction extends BasicModule {

  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(CrontabListAction.class);

  // 集群中已存活的索引名称
  private static List<String> colls;

  public void doGetList(Context context) throws Exception {
    List<TriggerCrontab> crontablist = getAllTriggerCrontab(this);
    JSONArray result = new JSONArray();
    JSONObject o = null;
    for (TriggerCrontab tab : crontablist) {
      o = new JSONObject();
      o.put("name", tab.getAppName());
      o.put("fulldump", tab.getFcrontab());
      o.put("fulljobid", tab.getFjobId());
      result.put(o);
    }
    //
    // context.put("query_result", result.toString(1));
    this.setBizResult(context, result);
  }

  /**
   * @return
   */
  public static List<TriggerCrontab> getAllTriggerCrontab(final RunContext context) {
    if (colls == null) {
      synchronized (CrontabListAction.class) {
        if (colls == null) {
          colls = new ArrayList<String>();
          setIndexCollectionName(context);
//          context.getSolrZkClient().addOnReconnect(() -> {
//            setIndexCollectionName(context);
//          });
        }
      }
    }
    List<TriggerCrontab> crontablist = TerminatorAdminAppsFetcher.getAllTriggerTabs(context.getUsrDptRelationDAO());
    TriggerCrontab next = null;
    Iterator<TriggerCrontab> cronIt = crontablist.iterator();
    while (cronIt.hasNext()) {
      next = cronIt.next();
      if (!colls.contains(next.getAppName()) || next.isFstop()) {
        cronIt.remove();
      }
    }
    return crontablist;
  }

  // public static IAppsFetcher getAppsFetcher(HttpServletRequest request,
  // boolean maxMatch, IUser user, RunContext context) {
  //
  // if (maxMatch) {
  // return AppsFetcher.create(user, context, true);
  // }
  //
  // return UserUtils.getAppsFetcher(request, context);
  // }

  /**
   * @throws
   * @throws InterruptedException
   */
  private static void setIndexCollectionName(final RunContext context) {
//    try {
//      colls.addAll(context.getSolrZkClient().getChildren(ZkStateReader.COLLECTIONS_ZKNODE, new AbstractWatcher() {
//
//        @Override
//        protected void process(Watcher watcher) throws KeeperException, InterruptedException {
//          // TisTriggerJobManage.setAppAndRuntime();
//          synchronized (CrontabListAction.class) {
//            log.info("receive a new rewatch colls event");
//            setIndexCollectionName(context);
//          }
//        }
//      }, true));
//      log.info("colls:{}", colls);
//    } catch (Exception e) {
//      log.error(e.getMessage(), e);
//      throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
//    }
  }
}
