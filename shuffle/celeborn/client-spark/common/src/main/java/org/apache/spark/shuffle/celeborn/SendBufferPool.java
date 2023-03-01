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

package org.apache.spark.shuffle.celeborn;

import java.util.LinkedList;

public class SendBufferPool {
  private static volatile SendBufferPool _instance;

  public static SendBufferPool get(int capacity) {
    if (_instance == null) {
      synchronized (SendBufferPool.class) {
        if (_instance == null) {
          _instance = new SendBufferPool(capacity);
        }
      }
    }
    return _instance;
  }

  private final int capacity;

  // numPartitions -> buffers
  private LinkedList<byte[][]> buffers;

  public SendBufferPool(int capacity) {
    this.capacity = capacity;
    buffers = new LinkedList<>();
  }

  public synchronized byte[][] acquireBuffer(int numPartitions) {
    for (int i = 0; i < buffers.size(); i++) {
      if (buffers.get(i).length == numPartitions) {
        return buffers.remove(i);
      }
    }
    if (buffers.size() == capacity) {
      buffers.removeFirst();
    }
    return null;
  }

  public synchronized void returnBuffer(byte[][] buffer) {
    if (buffers.size() == capacity) {
      buffers.removeFirst();
    }
    buffers.addLast(buffer);
  }
}
