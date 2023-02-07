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

import java.util.Iterator;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import com.carrotsearch.hppc.ObjectLongHashMap;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;

public class EndpointByteMapImpl implements EndpointByteMap{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EndpointByteMapImpl.class);

  private final ObjectLongHashMap<DrillbitEndpoint> map = new ObjectLongHashMap<>();

  private long maxBytes;

  public boolean isSet(DrillbitEndpoint endpoint){
    return map.containsKey(endpoint);
  }

  public long get(DrillbitEndpoint endpoint){
    return map.get(endpoint);
  }

  public boolean isEmpty(){
    return map.isEmpty();
  }

  public void add(DrillbitEndpoint endpoint, long bytes){
    assert endpoint != null;
    maxBytes = Math.max(maxBytes, map.putOrAdd(endpoint, bytes, bytes)+1);
  }

  public long getMaxBytes() {
    return maxBytes;
  }

  @Override
  public Iterator<ObjectLongCursor<DrillbitEndpoint>> iterator() {
    return map.iterator();
  }


}
