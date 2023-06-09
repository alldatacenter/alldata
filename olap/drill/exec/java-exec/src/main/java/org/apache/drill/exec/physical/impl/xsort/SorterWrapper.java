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
package org.apache.drill.exec.physical.impl.xsort;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;

/**
 * Single-batch sorter using a generated implementation based on the
 * schema and sort specification. The generated sorter is reused
 * across batches. The sorter must be closed at each schema change
 * so that the sorter will generate a new implementation against
 * the changed schema.
 */

public class SorterWrapper extends BaseSortWrapper {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SorterWrapper.class);

  /**
   * Generated sort operation used to sort each incoming batch according to
   * the sort criteria specified in the {@link ExternalSort} definition of
   * this operator.
   */

  private SingleBatchSorter sorter;

  public SorterWrapper(OperatorContext opContext) {
    super(opContext);
  }

  public void sortBatch(VectorContainer convertedBatch, SelectionVector2 sv2) {

    SingleBatchSorter sorter = getSorter(convertedBatch);
    try {
      sorter.setup(context.getFragmentContext(), sv2, convertedBatch);
      sorter.sort(sv2);
    } catch (SchemaChangeException e) {
      convertedBatch.clear();
      throw UserException.unsupportedError(e)
            .message("Unexpected schema change.")
            .build(logger);
    }
  }

  public void close() {
    sorter = null;
  }

  private SingleBatchSorter getSorter(VectorAccessible batch) {
    if (sorter == null) {
      sorter = newSorter(batch);
    }
    return sorter;
  }

  private SingleBatchSorter newSorter(VectorAccessible batch) {
    CodeGenerator<SingleBatchSorter> cg = CodeGenerator.get(
        SingleBatchSorter.TEMPLATE_DEFINITION, context.getFragmentContext().getOptions());
    ClassGenerator<SingleBatchSorter> g = cg.getRoot();
    cg.plainJavaCapable(true);
    // Uncomment out this line to debug the generated code.
//    cg.saveCodeForDebugging(true);

    generateComparisons(g, batch, logger);
    return getInstance(cg, logger);
  }
}
