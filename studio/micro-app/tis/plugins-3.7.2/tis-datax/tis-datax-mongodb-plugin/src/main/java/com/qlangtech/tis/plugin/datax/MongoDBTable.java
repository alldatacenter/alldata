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

import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-19 14:48
 **/
public class MongoDBTable implements ISelectedTab {
    public List<CMeta> cols;
    private final String collectionName;

    public MongoDBTable(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public List<CMeta> getCols() {
        return this.cols;
    }

    @Override
    public String getName() {
        return this.collectionName;
    }

    @Override
    public String getWhere() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAllCols() {
        throw new UnsupportedOperationException();
    }


}
