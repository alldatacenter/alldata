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
package org.apache.drill.exec.physical.impl.metadata;

import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.MetadataHashAggPOP;
import org.apache.drill.exec.physical.impl.aggregate.HashAggBatch;
import org.apache.drill.exec.physical.impl.aggregate.HashAggregator;
import org.apache.drill.exec.record.RecordBatch;

import java.util.List;

public class MetadataHashAggBatch extends HashAggBatch {
  private List<NamedExpression> valueExpressions;

  public MetadataHashAggBatch(MetadataHashAggPOP popConfig, RecordBatch incoming, FragmentContext context) {
    super(popConfig, incoming, context);
  }

  @Override
  protected HashAggregator createAggregatorInternal() {
    MetadataHashAggPOP popConfig = (MetadataHashAggPOP) this.popConfig;

    valueExpressions = new MetadataAggregateHelper(popConfig.getContext(),
            new ColumnNamesOptions(context.getOptions()), incoming.getSchema(), popConfig.getPhase())
        .getValueExpressions();

    return super.createAggregatorInternal();
  }

  @Override
  protected List<NamedExpression> getValueExpressions() {
    return valueExpressions;
  }
}
