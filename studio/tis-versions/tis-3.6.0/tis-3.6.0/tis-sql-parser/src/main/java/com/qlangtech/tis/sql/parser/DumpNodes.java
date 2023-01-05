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
package com.qlangtech.tis.sql.parser;

import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DumpNodes {

    private List<DependencyNode> dumps;

    public DumpNodes(List<DependencyNode> dumps) {
        this.dumps = dumps;
    }

    public DumpNodes() {
        this(Lists.newArrayList());
    }

    public void addAll(List<DependencyNode> nodes) {
        this.dumps.addAll(nodes);
    }

    public void add(DependencyNode node) {
        this.dumps.add(node);
    }

    public List<DependencyNode> getDumps() {
        return dumps;
    }

    public void setDumps(List<DependencyNode> dumps) {
        this.dumps = dumps;
    }
}
