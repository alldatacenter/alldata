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
package com.qlangtech.tis.datax;

import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;

import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-07 14:36
 */
public interface IDataxWriter extends IDataXPluginMeta {
    public String getTemplate();

    public DataxWriter.BaseDataxWriterDescriptor getWriterDescriptor();

    /**
     * 取得子任务
     *
     * @return
     */
    default IDataxContext getSubTask() {
        return getSubTask(Optional.empty());
    }

    /**
     * 取得子任务
     *
     * @return
     */
    IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap);

    /**
     * 用户已经把自动生成ddl 脚本的开关给关闭了
     *
     * @return
     */
    default boolean isGenerateCreateDDLSwitchOff() {
        return true;
    }

    /**
     * 生成创建table的脚本
     */
    default CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {
        throw new UnsupportedOperationException();
    }
}
