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


import com.qlangtech.tis.plugin.IdentityName;

public interface IContainerPodSpec extends IdentityName {
//    // 最大yarn堆内存大小,单位为M
//    private final int maxYarnHeapMemory;
//    // 单个任务CPU内核大小
//     private final int maxYarnCPUCores;

    /**
     * 最大内存开销，单位：M兆
     *
     * @return
     */
    public int getMaxHeapMemory();

    /**
     * 最大内存core数
     *
     * @return
     */
    public int getMaxCPUCores();

    /**
     * 远程JAVA调试端口
     *
     * @return
     */
    public int getRunjdwpPort();

    /**
     * 最大容忍错误数量
     *
     * @return
     */
    default int getMaxMakeFaild() {
        return Integer.MAX_VALUE;
    }

}
