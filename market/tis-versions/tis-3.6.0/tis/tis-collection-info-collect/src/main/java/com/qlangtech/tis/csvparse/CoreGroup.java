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
package com.qlangtech.tis.csvparse;

import java.util.HashSet;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class CoreGroup {

    private final int index;

    private final String name;

    public CoreGroup(int index, String name) {
        super();
        this.index = index;
        this.name = name;
    }

    private long indexSize;

    private long indexNum;

    private long queryCount;

    private float queryConsumeTime;

    public long getIndexSize() {
        return indexSize;
    }

    public void setIndexSize(long indexSize) {
        this.indexSize = indexSize;
    }

    public long getIndexNum() {
        return indexNum;
    }

    public void setIndexNum(long indexNum) {
        this.indexNum = indexNum;
    }

    public long getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(long queryCount) {
        this.queryCount = queryCount;
    }

    public float getQueryConsumeTime() {
        return queryConsumeTime;
    }

    public void setQueryConsumeTime(float queryConsumeTime) {
        this.queryConsumeTime = queryConsumeTime;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + index;
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    public static void main(String[] arg) {
        CoreGroup group = new CoreGroup(0, "abc");
        Set<CoreGroup> groupSet = new HashSet<CoreGroup>();
        groupSet.add(group);
        group = new CoreGroup(0, "abc");
        groupSet.add(group);
        group = new CoreGroup(0, "abcc");
        groupSet.add(group);
        System.out.println(groupSet.size());
    }
}
