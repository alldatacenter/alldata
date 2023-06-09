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
package org.apache.drill.exec.physical.impl.mergereceiver;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.VectorAccessible;

public abstract class MergingReceiverTemplate implements MergingReceiverGeneratorBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MergingReceiverTemplate.class);

  @Override
  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") VectorAccessible incoming,
                               @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException;

  @Override
  public abstract int doEval(@Named("leftIndex") int leftIndex,
                             @Named("rightIndex") int rightIndex) throws SchemaChangeException;

  @Override
  public abstract void doCopy(@Named("inIndex") int inIndex,
                              @Named("outIndex") int outIndex) throws SchemaChangeException;
}
