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
package org.apache.drill.exec.store.schedule;

import com.carrotsearch.hppc.ObjectFloatHashMap;
import com.carrotsearch.hppc.cursors.ObjectFloatCursor;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AffinityCreator {

  private static final Logger logger = LoggerFactory.getLogger(AffinityCreator.class);

  public static <T extends CompleteWork> List<EndpointAffinity> getAffinityMap(List<T> work){
    Stopwatch watch = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;

    long totalBytes = work.stream()
      .mapToLong(CompleteWork::getTotalBytes)
      .sum();

    ObjectFloatHashMap<DrillbitEndpoint> affinities = new ObjectFloatHashMap<>();
    for (CompleteWork entry : work) {
      for (ObjectLongCursor<DrillbitEndpoint> cursor : entry.getByteMap()) {
        long bytes = cursor.value;
        float affinity = totalBytes == 0 ? 0.0F : (float) bytes / (float) totalBytes;
        affinities.putOrAdd(cursor.key, affinity, affinity);
      }
    }

    List<EndpointAffinity> affinityList = new LinkedList<>();
    for (ObjectFloatCursor<DrillbitEndpoint> d : affinities) {
      logger.debug("Endpoint {} has affinity {}", d.key.getAddress(), d.value);
      affinityList.add(new EndpointAffinity(d.key, d.value));
    }

    if (watch != null) {
      logger.debug("Took {} ms to get operator affinity", watch.elapsed(TimeUnit.MILLISECONDS));
    }
    return affinityList;
  }
}
