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

import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.trigger.jst.ILogListener;

/**
 * 增量调用远端客户端接口
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IRCController {

    /**
     * 连接是否可用进行校验，如果连接不可用直接抛出TisException
     */
    void checkUseable();

    /**
     * 发布增量实例
     *
     * @param collection
     * @param incrSpec   增量实例规格
     * @param timestamp
     * @throws Exception
     */
    void deploy(TargetResName collection, ReplicasSpec incrSpec, long timestamp) throws Exception;

    /**
     * 删除 增量实例
     *
     * @param collection
     * @throws Exception
     */
    void removeInstance(TargetResName collection) throws Exception;


    /**
     * 停止 增量实例，执行过程中会触发savepoint的记录
     *
     * @param indexName
     * @throws Exception
     */
    void stopInstance(TargetResName indexName);

    SupportTriggerSavePointResult supportTriggerSavePoint(TargetResName collection);

    public class SupportTriggerSavePointResult {
        public final boolean support;
        String unSupportReason;

        public SupportTriggerSavePointResult(boolean support) {
            this.support = support;
        }

        public String getUnSupportReason() {
            return unSupportReason;
        }

        public void setUnSupportReason(String unSupportReason) {
            this.unSupportReason = unSupportReason;
        }
    }

    /**
     * 创建一个Savepoint
     *
     * @param collection
     */
    void triggerSavePoint(TargetResName collection);

    /**
     * 重启增量节点
     *
     * @param collection
     */
    void relaunch(TargetResName collection, String... targetPod);

//    IFlinkIncrJobStatus getIncrJobStatus(TargetResName collection);

    /**
     * 取得增量实例
     *
     * @param collection
     * @return
     */
    IDeploymentDetail getRCDeployment(TargetResName collection);

    /**
     * 开始增量监听
     *
     * @param collection
     * @param listener
     */
    WatchPodLog listPodAndWatchLog(TargetResName collection, String podName, ILogListener listener);

    /**
     * 将已经存在Savepoint删除
     *
     * @param resName
     * @param savepointPath
     */
    void discardSavepoint(TargetResName resName, String savepointPath);
}
