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
package com.qlangtech.tis.runtime.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.qlangtech.tis.manage.biz.dal.pojo.Server;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.pubhook.common.RunEnvironment;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-22
 */
public class ServerGroupAdapter {

    private final ServerGroup group;

    private final Snapshot snapshot;

    public Integer getPublishSnapshotId() {
        return group.getPublishSnapshotId();
    }

    private int maxSnapshotId;

    public RunEnvironment getEnvironment() {
        return RunEnvironment.getEnum(group.getRuntEnvironment());
    }

    public short getRuntEnvironment() {
        return group.getRuntEnvironment();
    }

    private final List<Server> serverList = new ArrayList<Server>();

    public ServerGroupAdapter(ServerGroup group, Snapshot snapshot) {
        super();
        this.group = group;
        this.snapshot = snapshot;
    }

    public Integer getGid() {
        return getGroup().getGid();
    }

    public ServerGroup getGroup() {
        return group;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public void addServer(Server server) {
        serverList.add(server);
    }

    public void addServer(List<Server> serverList) {
        if (serverList == null) {
            return;
        }
        this.serverList.addAll(serverList);
    }

    public final int getServerCount() {
        return this.serverList.size();
    }

    public List<Server> getServerList() {
        return Collections.unmodifiableList(serverList);
    }

    public int getMaxSnapshotId() {
        return maxSnapshotId;
    }

    public void setMaxSnapshotId(int maxSnapshotId) {
        this.maxSnapshotId = maxSnapshotId;
    }

    /**
     * 当前snapshot是否和当前最新的snapshotid相等
     *
     * @return
     */
    public boolean isCurrentSnapshotEqual2Neweast() {
        return this.maxSnapshotId == this.getPublishSnapshotId();
    }
}
