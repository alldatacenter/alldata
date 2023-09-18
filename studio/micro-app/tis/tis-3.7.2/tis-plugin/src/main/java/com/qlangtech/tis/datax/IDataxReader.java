/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.datax;

import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-07 14:36
 */
public interface IDataxReader extends DataSourceMeta, IDataXPluginMeta, IStreamTableMeataCreator.ISourceStreamMetaCreator {

    /**
     * 是否支持导入多个子表，当reader如果只支持单个表，那writer如果是MysqlWriter就可以指定表名称和列名
     *
     * @return
     */
    default boolean hasMulitTable() {
        return getSelectedTabs().size() > 0;
    }

    <T extends ISelectedTab> List<T> getSelectedTabs();

    /**
     * 取得子任务
     *
     * @return
     */
    default IGroupChildTaskIterator getSubTasks() {
        return getSubTasks((tab) -> true);
    }

    IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter);

    /**
     * 取得配置模版
     *
     * @return
     */
    String getTemplate();
}
