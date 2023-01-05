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

package com.qlangtech.tis.fullbuild.indexbuild.impl;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;

import java.util.List;

/**
 * @author: baisui 百岁
 * @create: 2020-04-23 19:59
 **/
public class DefaultCallbackHandler implements AMRMClientAsync.CallbackHandler {
    private boolean isShutdown;

    public void onShutdownRequest() {
        System.out.println("onShutdownRequest");
        this.isShutdown = true;
    }

    public void onNodesUpdated(List<NodeReport> updatedNodes) {
        System.out.println("onNodesUpdated");
    }

    @Override
    public float getProgress() {
        return 0;
    }

    public void onError(Throwable e) {
        e.printStackTrace();
    }

    /* 由于这里没有slaver所以这里两个方法不会触发 */
    @Override
    public void onContainersCompleted(List<ContainerStatus> statuses) {
    }

    @Override
    public void onContainersAllocated(List<Container> containers) {
    }
}
