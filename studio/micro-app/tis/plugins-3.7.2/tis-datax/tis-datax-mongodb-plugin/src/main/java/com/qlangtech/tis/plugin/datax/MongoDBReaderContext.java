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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxReaderContext;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-06 14:53
 **/
public class MongoDBReaderContext extends BasicMongoDBContext implements IDataxReaderContext {
    private final DataXMongodbReader mongodbReader;
    private final String taskName;

    public MongoDBReaderContext(String taskName, DataXMongodbReader mongodbReader) {
        super(mongodbReader.getDsFactory());
        this.mongodbReader = mongodbReader;
        this.taskName = taskName;
    }

    @Override
    public String getReaderContextId() {
        return this.dsFactory.identityValue();
    }

    public String getCollectionName() {
        return mongodbReader.collectionName;
    }

    public String getColumn() {
        return this.mongodbReader.column;
    }

    public boolean isContainQuery() {
        return StringUtils.isNotEmpty(this.mongodbReader.query);
    }

    public String getQuery() {
        return this.mongodbReader.query;
    }

    @Override
    public String getTaskName() {
        return this.taskName;
    }

    @Override
    public String getSourceTableName() {
        return DataXMongodbReader.DATAX_NAME;
    }

    @Override
    public String getSourceEntityName() {
        return DataXMongodbReader.DATAX_NAME;
    }
}
