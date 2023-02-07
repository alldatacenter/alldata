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
package org.apache.drill.exec.planner;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.hadoop.fs.Path;

import java.util.List;

/**
 * Abstract class for simple partition. It contains the
 * location of the entire partition and also stores the
 * value of the individual partition keys for this partition.
 */
public abstract class SimplePartitionLocation implements PartitionLocation{
  @Override
  public boolean isCompositePartition() {
    return false;
  }

  @Override
  public Path getCompositePartitionPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SimplePartitionLocation> getPartitionLocationRecursive() {
    return ImmutableList.of(this);
  }

}
