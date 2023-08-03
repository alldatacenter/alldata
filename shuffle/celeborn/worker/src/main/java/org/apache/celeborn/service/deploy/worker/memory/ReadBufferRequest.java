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

package org.apache.celeborn.service.deploy.worker.memory;

import static com.google.common.base.Preconditions.checkArgument;

public class ReadBufferRequest {
  private final int number;
  private final int bufferSize;
  private final ReadBufferListener readBufferListener;

  public ReadBufferRequest(int number, int bufferSize, ReadBufferListener readBufferListener) {
    checkArgument(number > 0);
    this.number = number;
    this.bufferSize = bufferSize;
    this.readBufferListener = readBufferListener;
  }

  public int getNumber() {
    return number;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public ReadBufferListener getBufferListener() {
    return readBufferListener;
  }
}
