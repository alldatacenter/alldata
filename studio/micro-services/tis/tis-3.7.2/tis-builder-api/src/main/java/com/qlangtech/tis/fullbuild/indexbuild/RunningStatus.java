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
package com.qlangtech.tis.fullbuild.indexbuild;

import java.io.IOException;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class RunningStatus {

    public static final RunningStatus SUCCESS = new RunningStatus(1f, true, true);

    public static final RunningStatus FAILD = new RunningStatus(0f, true, false);

    private final float progress;

    private final boolean complete;

    private final boolean success;

    /**
     * @param progress
     * @param complete
     */
    public RunningStatus(float progress, boolean complete, boolean success) {
        super();
        this.progress = progress;
        this.complete = complete;
        this.success = success;
    }

    public float progress() throws IOException {
        return this.progress;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public boolean isSuccess() {
        return success;
    }
}
