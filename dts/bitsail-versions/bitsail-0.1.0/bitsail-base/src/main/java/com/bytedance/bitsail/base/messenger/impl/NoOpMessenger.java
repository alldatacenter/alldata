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

package com.bytedance.bitsail.base.messenger.impl;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.Messenger;

import java.io.IOException;

public class NoOpMessenger<T> extends Messenger<T> {

  public NoOpMessenger() {
    super(null);
  }

  @Override
  public void open() {
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void addSuccessRecord(Object message) {
  }

  @Override
  public void addFailedRecord(Object message, Throwable throwable) {
  }

  @Override
  public long getSuccessRecords() {
    return 0;
  }

  @Override
  public long getSuccessRecordBytes() {
    return 0;
  }

  @Override
  public long getFailedRecords() {
    return 0;
  }

  @Override
  public void commit() {

  }

  @Override
  public void restoreMessengerCounter(ProcessResult<?> processResult) {

  }
}
