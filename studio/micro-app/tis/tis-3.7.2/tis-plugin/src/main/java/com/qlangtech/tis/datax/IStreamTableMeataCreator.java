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

import com.qlangtech.tis.plugin.ds.IColMetaGetter;

import java.util.List;

/**
 * 针对类似Hudi 这样的数据类型，增量写入需要是Flink SQL写入方式，
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-19 13:02
 **/
public interface IStreamTableMeataCreator //extends IStreamIncrGenerateStrategy
{

    /**
     * 比表写入相关的元数据信息
     *
     * @param tableName
     * @return
     */
    public IStreamTableMeta getStreamTableMeta(String tableName);

    interface IStreamTableMeta {
        /**
         * 表相关的列信息
         *
         * @return
         */
        List<IColMetaGetter> getColsMeta();
    }


    interface ISourceStreamMetaCreator extends IStreamTableMeataCreator {
    }

    interface ISinkStreamMetaCreator extends IStreamTableMeataCreator {
    }
}
