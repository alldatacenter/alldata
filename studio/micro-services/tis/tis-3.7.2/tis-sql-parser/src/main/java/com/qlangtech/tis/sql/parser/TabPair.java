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

/**
 * 描述dataflow中的表之间有两两依赖关系表关系，两个表之间 完全为对等关系
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TabPair {

    private final DependencyNode one;

    private final DependencyNode another;

    public TabPair(DependencyNode one, DependencyNode another) {
        this.one = one;
        this.another = another;
    }

    public DependencyNode getOne() {
        return this.one;
    }

    public DependencyNode getAnother() {
        return this.another;
    }
}
