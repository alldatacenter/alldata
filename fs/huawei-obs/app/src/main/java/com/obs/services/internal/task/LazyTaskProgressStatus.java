/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.task;

public class LazyTaskProgressStatus extends DefaultTaskProgressStatus {

    @Override
    public void execTaskIncrement() {
    }

    @Override
    public void succeedTaskIncrement() {
    }

    @Override
    public void failTaskIncrement() {
    }

    @Override
    public void setTotalTaskNum(int totalNum) {
    }

    @Override
    public int getExecPercentage() {
        return -1;
    }

    @Override
    public int getTotalTaskNum() {
        return -1;
    }

    @Override
    public int getExecTaskNum() {
        return -1;
    }

    @Override
    public int getSucceedTaskNum() {
        return -1;
    }

    @Override
    public int getFailTaskNum() {
        return -1;
    }

}
