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
package org.apache.drill.exec.physical.impl.validate;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.config.IteratorValidator;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.record.RecordBatch;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class IteratorValidatorCreator implements BatchCreator<IteratorValidator>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IteratorValidatorCreator.class);

  @Override
  public IteratorValidatorBatchIterator getBatch(ExecutorFragmentContext context, IteratorValidator config,
                                                 List<RecordBatch> children)
      throws ExecutionSetupException {
    Preconditions.checkArgument(children.size() == 1);
    RecordBatch child = children.iterator().next();
    IteratorValidatorBatchIterator iter = new IteratorValidatorBatchIterator(child, config.isRepeatable);
    boolean validateBatches = context.getOptions().getOption(ExecConstants.ENABLE_VECTOR_VALIDATOR) ||
                              context.getConfig().getBoolean(ExecConstants.ENABLE_VECTOR_VALIDATION);
    iter.enableBatchValidation(validateBatches);
    logger.trace("Iterator validation enabled for " + child.getClass().getSimpleName() +
                 (validateBatches ? " with vector validation" : ""));
    return iter;
  }
}
