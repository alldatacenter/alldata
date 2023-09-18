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

package com.qlangtech.tis.plugins.incr.flink.connector.hudi.streamscript;

import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;

import java.util.Collections;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-14 09:09
 **/
class TestStreamTemplateData implements IStreamIncrGenerateStrategy.IStreamTemplateData {
    private final TargetResName collection;
    private final String targetTableName;

    public TestStreamTemplateData(TargetResName collection, String targetTableName) {
        this.collection = collection;
        this.targetTableName = targetTableName;
    }

    @Override
    public String getCollection() {
        return this.collection.getName();
    }

    @Override
    public List<EntityName> getDumpTables() {
        return Collections.singletonList(EntityName.parse(this.targetTableName));
    }
}
