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
package org.apache.drill.exec.physical.impl.statistics;

import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.metastore.statistics.Statistic;

public abstract class AbstractMergedStatistic implements MergedStatistic, Statistic {
  protected String name;
  protected String inputName;
  protected double samplePercent;
  protected State state;

  public void initialize(String name, String inputName, double samplePercent) {
    this.name = name;
    this.inputName = inputName;
    this.samplePercent = samplePercent;
  }

  @Override
  public abstract void initialize(String inputName, double samplePercent);

  @Override
  public abstract String getName();

  @Override
  public abstract String getInput();

  @Override
  public abstract void merge(MapVector input);

  @Override
  public abstract void setOutput(MapVector output);
}
