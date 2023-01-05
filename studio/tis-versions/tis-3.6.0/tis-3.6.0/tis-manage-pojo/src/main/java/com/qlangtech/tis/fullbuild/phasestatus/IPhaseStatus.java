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
package com.qlangtech.tis.fullbuild.phasestatus;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public interface IPhaseStatus<T extends IChildProcessStatus> {

    public int getTaskId();

    /**
     * 是否完成 （成功或者失敗都是完成狀態）
     *
     * @return
     */
    public boolean isComplete();

    /**
     * 是否成功
     *
     * @return
     */
    public boolean isSuccess();

    /**
     * 是否失败了
     * @return
     */
    public boolean isFaild();

    /**
     * 是否正在执行
     * @return
     */
    public boolean isProcessing();

    public IProcessDetailStatus<T> getProcessStatus();
}
