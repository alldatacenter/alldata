package com.qlangtech.tis.sql.parser;

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

import com.facebook.presto.sql.tree.Expression;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-17 10:53
 **/
public class SelectColsMetaGetter extends SqlRewriter {
    public SelectColsMetaGetter(SqlStringBuilder builder
            , Supplier<IPrimaryTabFinder> erRules, TabPartitions tabPartitions
            , Optional<List<Expression>> parameters, IJoinTaskContext joinContext) {
        super(builder,
                tabPartitions,
                erRules, parameters, false, joinContext);
    }

    @Override
    protected boolean shallCreatePtPmodCols() {
        return false;
    }

    @Override
    protected String createPtPmodCols(AliasTable a) {
        throw new UnsupportedOperationException(a.toString());
    }
}
