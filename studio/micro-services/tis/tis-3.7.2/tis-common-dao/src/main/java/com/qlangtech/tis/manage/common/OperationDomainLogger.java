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
package com.qlangtech.tis.manage.common;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-2-4
 */
public interface OperationDomainLogger {

    public String getOperationLogAppName();

    public Short getOperationLogRuntime();

    /**
     * 当前执行日志的 备注信息
     *
     * @return
     */
    public abstract String getOperationLogMemo();

    /**
     * 操作描述，默认是将插入的参数序列化成json格式的形式保存，如果该方法返回为空，则说明不需要覆写默认值
     *
     * @return
     */
    public abstract String getOpDesc();

    public boolean isLogHasBeenSet();
}
