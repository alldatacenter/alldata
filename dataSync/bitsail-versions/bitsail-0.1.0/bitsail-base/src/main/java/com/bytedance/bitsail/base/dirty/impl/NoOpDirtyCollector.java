/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.base.dirty.impl;

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;

import java.io.IOException;

/**
 * Created 2020/12/23.
 **/
public class NoOpDirtyCollector extends AbstractDirtyCollector {

  public NoOpDirtyCollector() {
    this(BitSailConfiguration.newDefault(), 0);
  }

  public NoOpDirtyCollector(BitSailConfiguration jobConf, int taskId) {
    super(jobConf, taskId);
  }

  @Override
  protected void collect(Object dirtyObj, Throwable e, long processingTime) throws IOException {

  }

  @Override
  public void onProcessingTime(long processingTime) throws IOException {

  }

  @Override
  public void clear() throws IOException {

  }

  @Override
  public void close() throws IOException {
  }
}
