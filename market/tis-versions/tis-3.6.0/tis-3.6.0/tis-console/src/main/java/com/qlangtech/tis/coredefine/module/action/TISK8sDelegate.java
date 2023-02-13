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

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.impl.AdapterRCController;
import com.qlangtech.tis.coredefine.module.action.impl.FlinkJobDeploymentDetails;
import com.qlangtech.tis.coredefine.module.action.impl.RcDeployment;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.incr.IncrStreamFactory;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.util.HeteroEnum;
import com.qlangtech.tis.util.PluginItems;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TISK8sDelegate {

  private static final Logger logger = LoggerFactory.getLogger(TISK8sDelegate.class);

  private static final ConcurrentHashMap<String, TISK8sDelegate> colIncrLogMap = new ConcurrentHashMap<>();

  static {
    PluginItems.addPluginItemsSaveObserver(new PluginItems.PluginItemsSaveObserver() {

      @Override
      public void afterSaved(PluginItems.PluginItemsSaveEvent event) {
        if (!event.cfgChanged) {
          return;
        }
        if (event.heteroEnum == HeteroEnum.PARAMS_CONFIG
          || event.heteroEnum == HeteroEnum.INCR_STREAM_CONFIG
        ) {
          colIncrLogMap.values().forEach((r) -> {
            r.close();
          });
          colIncrLogMap.clear();
        }
      }
    });
  }

  public static TISK8sDelegate getK8SDelegate(String collection) {
    TISK8sDelegate delegate = null;
    if ((delegate = colIncrLogMap.get(collection)) == null) {
      delegate = colIncrLogMap.computeIfAbsent(collection, (c) -> {
        return new TISK8sDelegate(c);
      });
    }
    return delegate;
  }

  private final TargetResName indexName;


  private final IRCController incrSync;
  //private RcDeployment incrDeployment;
  private IDeploymentDetail incrDeployment;


  private long latestIncrDeploymentFetchtime;

  private Map<String, WatchPodLog> watchPodLogMap = Maps.newHashMap();

  private TISK8sDelegate(String indexName) {
    if (StringUtils.isEmpty(indexName)) {
      throw new IllegalArgumentException("param indexName can not be null");
    }
    if (DataXJobWorker.K8S_DATAX_INSTANCE_NAME.getName().equals(indexName)
      || DataXJobWorker.K8S_FLINK_CLUSTER_NAME.getName().equals(indexName)
    ) {
      DataXJobWorker dataxWorker = DataXJobWorker.getJobWorker(new TargetResName(indexName));
      this.incrSync = new AdapterRCController() {
        @Override
        public WatchPodLog listPodAndWatchLog(TargetResName collection, String podName, ILogListener listener) {
          return dataxWorker.listPodAndWatchLog(podName, listener);
        }
      };
    } else {
      IPluginStore<IncrStreamFactory> store = TIS.getPluginStore(indexName, IncrStreamFactory.class);
      IncrStreamFactory k8sConfig = store.getPlugin();
      if (k8sConfig == null) {
        throw new IllegalStateException("key" + indexName + " have not set k8s plugin");
      }
      this.incrSync = k8sConfig.getIncrSync();
    }

    // 因为K8S中名称不能有下划线所以在这里要替换一下
    this.indexName = new TargetResName(indexName);// StringUtils.replace(indexName, "_", "-");
  }

  public void checkUseable() {
    incrSync.checkUseable();
  }

  public static void main(String[] args) throws Exception {
    ReplicasSpec incrSpec = new ReplicasSpec();
    incrSpec.setReplicaCount(1);
    incrSpec.setCpuLimit(Specification.parse("2"));
    incrSpec.setCpuRequest(Specification.parse("500m"));
    incrSpec.setMemoryLimit(Specification.parse("2G"));
    incrSpec.setMemoryRequest(Specification.parse("500M"));
    TISK8sDelegate incrK8s = new TISK8sDelegate("search4totalpay");
    incrK8s.isRCDeployment(true);
  }

  public void deploy(ReplicasSpec incrSpec, final long timestamp) throws Exception {
    this.incrSync.deploy(this.indexName, incrSpec, timestamp);
  }

  /**
   * 是否存在RC，有即证明已经完成发布流程
   *
   * @return
   */
  public boolean isRCDeployment(boolean canGetCache) {
    return getRcConfig(canGetCache) != null;
  }

  public IDeploymentDetail getRcConfig(boolean canGetCache) {
    long current = System.currentTimeMillis();
    // 40秒缓存
    if (!canGetCache || this.incrDeployment == null || (current > (latestIncrDeploymentFetchtime + 40000))) {
      latestIncrDeploymentFetchtime = current;
      this.incrDeployment = null;
      this.incrDeployment = this.incrSync.getRCDeployment(this.indexName);
      if (this.incrDeployment == null) {
        return null;
      }
      this.incrDeployment.accept(new IDeploymentDetail.IDeploymentDetailVisitor() {
        @Override
        public void visit(RcDeployment rcDeployment) {
          watchPodLogMap.entrySet().removeIf((entry) -> {
            return !rcDeployment.getPods().stream().filter((p) -> StringUtils.equals(p.getName(), entry.getKey())).findFirst().isPresent();
          });
        }

        @Override
        public void visit(FlinkJobDeploymentDetails details) {
          // throw new UnsupportedOperationException();
        }
      });


    }
    return incrDeployment;
  }

  /**
   * 停止增量实例
   */
  public void stopIncrProcess(IMessageHandler handler, Context context) {

    IRCController.SupportTriggerSavePointResult vresult
      = incrSync.supportTriggerSavePoint(this.indexName);
    if (!vresult.support) {
      handler.addErrorMessage(context, vresult.getUnSupportReason());
      return;
    }

    try {
      this.incrSync.stopInstance(this.indexName);
      this.cleanResource();
    } catch (Exception e) {
      if (this.incrSync.getRCDeployment(this.indexName) == null) {
        this.cleanResource();
      }
      throw new RuntimeException(this.indexName.getName(), e);
    }

  }

  /**
   * 删除增量实例
   */
  public void removeIncrProcess() {
    try {
      this.incrSync.removeInstance(this.indexName);
      this.cleanResource();
    } catch (Throwable e) {
      // 看一下RC 是否已经没有了如果是没有了 就直接回收资源
      if (this.incrSync.getRCDeployment(this.indexName) == null) {
        this.cleanResource();
      }
      throw new RuntimeException(this.indexName.getName(), e);
    }
  }

  private void cleanResource() {
    this.incrDeployment = null;
    TISK8sDelegate tisk8sDelegate = colIncrLogMap.remove(this.indexName);
    if (tisk8sDelegate != null) {
      tisk8sDelegate.close();
    }
  }

  /**
   * 列表pod，并且显示日志
   */
  public void listPodsAndWatchLog(String podName, ILogListener listener) {
    if (StringUtils.isEmpty(podName)) {
      throw new IllegalArgumentException("illegal argument 'podName' can not be null");
    }
    WatchPodLog watchPodLog = null;
    synchronized (this) {
      if ((watchPodLog = this.watchPodLogMap.get(podName)) == null) {
        watchPodLog = this.incrSync.listPodAndWatchLog(this.indexName, podName, listener);
        this.watchPodLogMap.put(podName, watchPodLog);
      } else {
        watchPodLog.addListener(listener);
      }
    }
  }

  public void close() {
    this.watchPodLogMap.values().forEach((r) -> {
      r.close();
    });
  }


}
