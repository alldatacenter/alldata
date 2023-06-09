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

package com.qlangtech.tis.plugins.incr.flink.connector.streamscript;

import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;

import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-24 10:48
 **/
public abstract class BasicFlinkStreamScriptCreator implements IStreamIncrGenerateStrategy //, IStreamTableMeataCreator
{


    protected final IStreamTableMeataCreator.ISinkStreamMetaCreator sinkStreamMetaGetter;

    public BasicFlinkStreamScriptCreator(IStreamTableMeataCreator.ISinkStreamMetaCreator sinkStreamMetaGetter) {
        this.sinkStreamMetaGetter = Objects.requireNonNull(sinkStreamMetaGetter, "sinkStreamMetaGetter can not be null");
    }

    //    @Override
//    public final IStreamTableMeta getStreamTableMeta(final String tableName) {
//        final Pair<HudiSelectedTab, HudiTableMeta> tabMeta = hudiSinkFactory.getTableMeta(tableName);
//        return new IStreamTableMeta() {
//            @Override
//            public List<HdfsColMeta> getColsMeta() {
//                return tabMeta.getRight().colMetas;
//            }
//        };
//    }


}
