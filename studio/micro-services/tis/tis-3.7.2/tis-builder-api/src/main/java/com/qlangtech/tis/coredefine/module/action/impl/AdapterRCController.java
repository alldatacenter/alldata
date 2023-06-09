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

package com.qlangtech.tis.coredefine.module.action.impl;

import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.trigger.jst.ILogListener;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-07 22:47
 **/
public class AdapterRCController implements IRCController {
    @Override
    public void deploy(TargetResName collection, ReplicasSpec incrSpec, long timestamp) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportTriggerSavePointResult supportTriggerSavePoint(TargetResName collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void triggerSavePoint(TargetResName collection) {
        throw new UnsupportedOperationException();
    }
//    @Override
//    public IFlinkIncrJobStatus getIncrJobStatus(TargetResName collection) {
//        throw new UnsupportedOperationException();
//    }


    @Override
    public void discardSavepoint(TargetResName resName, String savepointPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkUseable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeInstance(TargetResName collection) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopInstance(TargetResName indexName) {
        throw new UnsupportedOperationException();
    }
//    @Override
//    public void relaunch(String collection) {
//        throw new UnsupportedOperationException();
//    }


    @Override
    public void relaunch(TargetResName collection, String... targetPod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RcDeployment getRCDeployment(TargetResName collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchPodLog listPodAndWatchLog(TargetResName collection, String podName, ILogListener listener) {
        throw new UnsupportedOperationException();
    }
}
