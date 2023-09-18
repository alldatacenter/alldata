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
package com.qlangtech.tis.sql.parser.tuple.creator.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-22 16:51
 */
public class ColRef {
    /**
     * colName
     */
    private ListMap colRefMap = new ListMap();

    private Map<String, IDataTupleCreator> /*** ref*/
            baseRefMap = Maps.newHashMap();

    public Set<Map.Entry<String, IDataTupleCreator>> getBaseRefEntities() {
        return this.baseRefMap.entrySet();
    }

    public IDataTupleCreator createBaseRefIfNull(String baseRef) {
        IDataTupleCreator tupleCreator = null;
        if ((tupleCreator = baseRefMap.get(baseRef)) == null) {
            tupleCreator = new TableTupleCreator(baseRef, NodeType.JOINER_SQL);
            baseRefMap.put(baseRef, tupleCreator);
        }
        return tupleCreator;
    }

    public int getBaseRefSize() {
        return this.baseRefMap.size();
    }

    public IDataTupleCreator getTupleCreator(String alias) {
        return this.baseRefMap.get(alias);
    }

    public Set<String> getBaseRefKeys() {
        return this.baseRefMap.keySet();
    }

    public ListMap getColRefMap() {
        return this.colRefMap;
    }

    public static class ListMap {

        private final Map<ColName, IDataTupleCreator> /**
         * colName
         */
                colRefMap = Maps.newHashMap();

        private final List<ColName> cols = Lists.newArrayList();

        public void put(ColName col, IDataTupleCreator tuple) {
            this.colRefMap.put(col, tuple);
            this.cols.add(col);
        }

        public Collection<IDataTupleCreator> values() {
            return this.colRefMap.values();
        }

        public Set<Map.Entry<ColName, IDataTupleCreator>> entrySet() {
            return this.colRefMap.entrySet();
        }

        public int size() {
            return colRefMap.size();
        }

        public IDataTupleCreator get(ColName col) {
            return colRefMap.get(col);
        }

        public List<ColName> keySet() {
            return this.cols;
        }
    }
}
