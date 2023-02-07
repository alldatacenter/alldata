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
package org.apache.drill.exec.physical.impl.window;

import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.WindowPOP;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;

import javax.inject.Named;
import java.util.List;

public interface WindowFramer {
  TemplateClassDefinition<WindowFramer> NOFRAME_TEMPLATE_DEFINITION =
      new TemplateClassDefinition<>(WindowFramer.class, NoFrameSupportTemplate.class);
  TemplateClassDefinition<WindowFramer> FRAME_TEMPLATE_DEFINITION =
      new TemplateClassDefinition<>(WindowFramer.class, FrameSupportTemplate.class);

  void setup(final List<WindowDataBatch> batches, final VectorContainer container,
      final OperatorContext operatorContext, final boolean requireFullPartition,
      final WindowPOP popConfig) throws SchemaChangeException;

  /**
   * process the inner batch and write the aggregated values in the container
   * @throws SchemaChangeException
   * @throws DrillException
   */
  void doWork() throws SchemaChangeException;

  /**
   * @return number rows processed in last batch
   */
  int getOutputCount();

  void cleanup();

  /**
   * compares two rows from different batches (can be the same), if they have the same value for the partition by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  boolean isSamePartition(@Named("b1Index") int b1Index,
                          @Named("b1") VectorAccessible b1,
                          @Named("b2Index") int b2Index,
                          @Named("b2") VectorAccessible b2)
          throws SchemaChangeException;

  /**
   * compares two rows from different batches (can be the same), if they have the same value for the order by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  boolean isPeer(@Named("b1Index") int b1Index,
                 @Named("b1") VectorAccessible b1,
                 @Named("b2Index") int b2Index,
                 @Named("b2") VectorAccessible b2)
          throws SchemaChangeException;
}
