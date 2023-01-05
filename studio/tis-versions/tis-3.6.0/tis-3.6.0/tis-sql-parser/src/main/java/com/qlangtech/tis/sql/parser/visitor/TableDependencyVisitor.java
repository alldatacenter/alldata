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
package com.qlangtech.tis.sql.parser.visitor;

import java.util.Set;
import com.facebook.presto.sql.tree.DefaultTraversalVisitor;
import com.facebook.presto.sql.tree.Table;
import com.google.common.collect.Sets;

/**
 * 统计一个Sql的依赖表
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月13日
 */
public class TableDependencyVisitor extends DefaultTraversalVisitor<Void, Void> {

    private Set<String> tabDependencies = Sets.newHashSet();

    public Set<String> getTabDependencies() {
        return tabDependencies;
    }

    public static TableDependencyVisitor create() {
        return new TableDependencyVisitor();
    }

    @Override
    protected Void visitTable(Table node, Void context) {
        tabDependencies.add(String.valueOf(node.getName()));
        return null;
    }
}
