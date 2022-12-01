/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GremlinClauseList {
    private final List<GremlinQueryComposer.GremlinClauseValue> list;
    private final Map<Integer, List<GremlinClauseList>>         subClauses;

    GremlinClauseList() {
        this.list       = new LinkedList<>();
        this.subClauses = new LinkedHashMap<>();
    }

    public void add(GremlinQueryComposer.GremlinClauseValue clauseValue) {
        list.add(clauseValue);
    }

    public void add(int idx, GremlinQueryComposer.GremlinClauseValue g) {
        list.add(idx, g);
    }

    public void add(GremlinClause clause, String... args) {
        list.add(new GremlinQueryComposer.GremlinClauseValue(clause, args));
    }

    public void add(int i, GremlinClause clause, String... args) {
        list.add(i, new GremlinQueryComposer.GremlinClauseValue(clause, args));
    }

    public GremlinQueryComposer.GremlinClauseValue getAt(int i) {
        return list.get(i);
    }

    public String getValue(int i) {
        return list.get(i).getClauseWithValue();
    }

    public GremlinQueryComposer.GremlinClauseValue get(int i) {
        return list.get(i);
    }

    public int size() {
        return list.size();
    }

    public int contains(GremlinClause clause) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getClause() == clause)
                return i;
        }

        return -1;
    }

    public boolean isEmpty() {
        return list.size() == 0 || containsGVLimit();
    }

    private boolean containsGVLimit() {
        return list.size() == 3 &&
                list.get(0).getClause() == GremlinClause.G &&
                list.get(1).getClause() == GremlinClause.V &&
                list.get(2).getClause() == GremlinClause.LIMIT;
    }

    public void clear() {
        list.clear();
    }

    public GremlinQueryComposer.GremlinClauseValue remove(int index) {
        GremlinQueryComposer.GremlinClauseValue gcv = get(index);
        list.remove(index);
        return gcv;
    }

    public List<GremlinQueryComposer.GremlinClauseValue> getList() {
        return list;
    }

    public void addSubClauses(int index, GremlinClauseList queryClauses) {
        if (!this.subClauses.containsKey(index)) {
            this.subClauses.put(index, new ArrayList<>());
        }

        this.subClauses.get(index).add(queryClauses);
    }

    public boolean hasSubClause(int i) {
        return subClauses.containsKey(i);
    }

    public List<GremlinClauseList> getSubClauses(int i) {
        return subClauses.get(i);
    }

    @Override
    public String toString() {
        return String.format("list.size: %d, subClauses.size: %d", this.size(), this.subClauses.size());
    }
}
