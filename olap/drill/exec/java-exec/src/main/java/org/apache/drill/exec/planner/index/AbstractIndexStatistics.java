/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.index;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractIndexStatistics implements IndexStatistics {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractIndexStatistics.class);
    protected final RelNode input;
    protected final RexNode condition;
    protected final DrillTable table;

    public AbstractIndexStatistics(RelNode input, RexNode condition, DrillTable table) {
            this.input = input;
            this.condition = condition;
            this.table = table;
    }
    public abstract double getRowCount();

    public List<RelCollation> getCollations() {
        throw new UnsupportedOperationException();
    }

    public RelDistribution getDistribution() {
        throw new UnsupportedOperationException();
    }
}
