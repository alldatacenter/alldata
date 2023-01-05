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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxContext;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-20 14:01
 **/
public class MongoDBWriterContext extends BasicMongoDBContext implements IDataxContext {
    private final DataXMongodbWriter writer;

    public MongoDBWriterContext(DataXMongodbWriter writer) {
        super(writer.getDsFactory());
        this.writer = writer;
    }

    public String getDataXName() {
        return this.writer.dataXName;
    }

    public String getCollectionName() {
        return this.writer.collectionName;
    }

    public String getColumn() {
        return this.writer.column;
    }


    public boolean isContainUpsertInfo() {
        return StringUtils.isNotBlank(this.writer.upsertInfo);
    }

    public String getUpsertInfo() {
        return this.writer.upsertInfo;
    }
}
