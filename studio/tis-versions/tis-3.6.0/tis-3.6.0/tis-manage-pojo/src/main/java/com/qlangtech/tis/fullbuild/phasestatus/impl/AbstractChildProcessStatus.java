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

import com.qlangtech.tis.fullbuild.phasestatus.IChildProcessStatus;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年7月6日
 */
public abstract class AbstractChildProcessStatus implements IChildProcessStatus {

    private boolean faild;

    private boolean complete;

    // 等待导入中
    private boolean waiting = true;

    public final boolean isWaiting() {
        return this.waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean isComplete() {
        if (this.faild) {
            return true;
        }
        return complete;
    }

    public final void setComplete(boolean complete) {
        if (complete) {
            this.waiting = false;
        }
        this.complete = complete;
    }

    /**
     * 是否成功，完成了且没有错误
     */
    public final boolean isSuccess() {
        return this.isComplete() && !this.isFaild();
    }

    @Override
    public boolean isFaild() {
        return this.faild;
    }

    public void setFaild(boolean faild) {
        this.faild = faild;
    }
}
