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

package org.apache.uniffle.storage.handler.api;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;

public interface ClientReadHandler {

  ShuffleDataResult readShuffleData();

  void close();

  // The handler only returns the segment,
  // but does not know the actually consumed blocks,
  // so the consumer should let the handler update statistics.
  // Each type of handler can design their rules.
  void updateConsumedBlockInfo(BufferSegment bs, boolean isSkippedMetrics);

  // Display the statistics of consumed blocks
  void logConsumedBlockInfo();
}
