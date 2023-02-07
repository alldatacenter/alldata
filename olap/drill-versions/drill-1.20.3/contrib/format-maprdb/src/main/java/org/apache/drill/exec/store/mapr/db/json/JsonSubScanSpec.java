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
package org.apache.drill.exec.store.mapr.db.json;

import java.nio.ByteBuffer;

import org.apache.drill.exec.store.mapr.db.MapRDBSubScanSpec;
import org.ojai.store.QueryCondition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mapr.db.impl.ConditionImpl;
import com.mapr.db.impl.MapRDBImpl;
import com.mapr.db.index.IndexDesc;

/**
 * This class is a helper extension of {@link MapRDBSubScanSpec} class and does not
 * get serialized or deserialized.
 */
public class JsonSubScanSpec extends MapRDBSubScanSpec {

  protected QueryCondition condition;

  public JsonSubScanSpec(String tableName, IndexDesc indexDesc, String regionServer,
                         QueryCondition scanRangeCondition, QueryCondition userCondition,
                         byte[] startRow, byte[] stopRow, String userName) {
    super(tableName, indexDesc, regionServer, startRow, stopRow, null, null, userName);

    condition = MapRDBImpl.newCondition().and();

    if (userCondition != null && !userCondition.isEmpty()) {
      condition.condition(userCondition);
    }
    if (scanRangeCondition != null && !scanRangeCondition.isEmpty()) {
      condition.condition(scanRangeCondition);
    }

    condition.close().build();
  }

  public void setCondition(QueryCondition cond) {
    condition = cond;
  }

  @JsonIgnore
  public QueryCondition getCondition() {
    return this.condition;
  }

  @Override
  public byte[] getSerializedFilter() {
    if (this.condition != null) {
      ByteBuffer bbuf = ((ConditionImpl)this.condition).getDescriptor().getSerialized();
      byte[] serFilter = new byte[bbuf.limit() - bbuf.position()];
      bbuf.get(serFilter);
      return serFilter;
    }

    return null;
  }
}
