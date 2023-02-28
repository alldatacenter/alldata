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
package com.qlangtech.tis.fullbuild.phasestatus.impl;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.fullbuild.phasestatus.IChildProcessStatus;
import com.qlangtech.tis.fullbuild.phasestatus.IPhaseStatus;
import com.qlangtech.tis.manage.common.Config;
import java.io.File;
import java.util.Collection;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public abstract class BasicPhaseStatus<T extends IChildProcessStatus> implements IPhaseStatus<T> {

    private final int taskid;

    // 是否有将状态写入到本地磁盘
    private boolean hasFlush2Local = false;

    public static transient IFlush2Local statusWriter = null;

    public static File getFullBuildPhaseLocalFile(int taskid, FullbuildPhase phase) {
        return new File(Config.getMetaCfgDir(), "df-logs/" + taskid + "/" + phase.getName());
    }

    protected abstract FullbuildPhase getPhase();

    protected static boolean shallOpenView(Collection<? extends AbstractChildProcessStatus> childs) {
        if (childs.size() < 1) {
            return false;
        }
        // 如果全部都complete了不能open
        // 找正在运行的
        Optional<? extends AbstractChildProcessStatus> find = childs.stream().filter((r) -> !r.isComplete() && !r.isWaiting()).findFirst();
        return (childs.size() > 0 && find.isPresent());
    }

    public boolean isHasFlush2Local() {
        return hasFlush2Local;
    }

    public void setHasFlush2Local(boolean hasFlush2Local) {
        this.hasFlush2Local = hasFlush2Local;
    }

    public BasicPhaseStatus(int taskid) {
        this.taskid = taskid;
    }

    // 在客户端是否详细展示
    public abstract boolean isShallOpen();

    protected abstract Collection<T> getChildStatusNode();

    @Override
    public final int getTaskId() {
        return this.taskid;
    }

    /**
     * 是否正在执行
     */
    @Override
    public final boolean isProcessing() {
        // 判断是否都执行完成了
        if (isComplete()) {
            return false;
        }
        for (T ts : getChildStatusNode()) {
            if (!ts.isWaiting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否失败了
     */
    @Override
    public final boolean isFaild() {
        for (T ts : getChildStatusNode()) {
            if (ts.isFaild()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean isComplete() {
        Collection<T> children = getChildStatusNode();
        if (children.isEmpty()) {
            return false;
        }
        for (T ts : children) {
            if (ts.isFaild()) {
                return writeStatus2Local();
            }
            if (!ts.isComplete()) {
                return false;
            }
        }
        return writeStatus2Local();
    }

    /**
     * 将本阶段的状态写入本地文件系统
     *
     * @return
     */
    public boolean writeStatus2Local() {
        if (!this.hasFlush2Local) {
            if (statusWriter != null) {
                synchronized (this) {
                    File localFile = getFullBuildPhaseLocalFile(this.taskid, this.getPhase());
                    this.hasFlush2Local = true;
                    try {
                        if (!localFile.exists()) {
                            // 写入本地文件系统中，后续查看状态可以直接从本地文件中拿到
                            statusWriter.write(localFile, this);
                        }
                    } catch (Exception e) {
                        this.hasFlush2Local = false;
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }

    public interface IFlush2Local {

        public void write(File localFile, BasicPhaseStatus status) throws Exception;

        public BasicPhaseStatus loadPhase(File localFile) throws Exception;
    }

    @Override
    public final boolean isSuccess() {
        Collection<T> children = getChildStatusNode();
        if (children.isEmpty()) {
            return false;
        }
        for (T ts : children) {
            if (!ts.isSuccess()) {
                return false;
            }
        }
        return true;
    }
}
