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
package org.apache.drill.exec.expr;

import com.sun.codemodel.JBlock;

/**
 * Uses this class to keep track # of Drill Logical Expressions that are
 * put to JBlock.
 *
 * JBlock is final class; we could not extend JBlock directly.
 */
public class SizedJBlock {
  private final JBlock block;
  private int count; // # of Drill Logical Expressions added to this block

  public SizedJBlock(JBlock block) {
    this.block = block;
    // Project, Filter and Aggregator receives JBlock, using ClassGenerator.addExpr() method,
    // but the Copier is doing kind of short-cut handling, by accessing the eval() and setup() directly.
    // To take into account JBlocks, that were filled in Copier, sets count to 1.
    this.count = 1;
  }

  public JBlock getBlock() {
    return this.block;
  }

  public void incCounter() {
    this.count++;
  }

  public int getCount() {
    return this.count;
  }

}
