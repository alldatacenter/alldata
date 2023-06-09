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
package org.apache.drill.exec.server.rest;

import org.apache.drill.exec.util.ValueVectorElementFormatter;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.vector.ValueVector.Accessor;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

/**
 * {@code WebUserConnectionWrapper} which represents the {@code UserClientConnection} between
 * WebServer and Foreman, for the WebUser submitting the query. It provides
 * access to the {@code UserSession} executing the query. There is no actual physical
 * channel corresponding to this connection wrapper.
 *
 * It returns a close future which do have an EventExecutor out of BitServer EventLoopGroup.
 * Close future is set only when the {@link WebSessionResources} are closed.
 */
public class WebUserConnection extends BaseWebUserConnection {

  public final List<Map<String, String>> results = Lists.newArrayList();
  public final Set<String> columns = Sets.newLinkedHashSet();
  public final List<String> metadata = new ArrayList<>();
  private int autoLimitRowCount;
  private int rowCount;

  WebUserConnection(WebSessionResources webSessionResources) {
    super(webSessionResources);
  }

  @Override
  public void sendData(RpcOutcomeListener<Ack> listener, QueryDataPackage data) {
    processBatch(data.batch());
    listener.success(Acks.OK, null);
  }

  private void processBatch(VectorContainer batch) {
    if (batch == null) {
      // Empty query: no data, no schema.
      return;
    }

    // Build metadata only on first batch, or if the schema changes
    if (metadata.isEmpty() || batch.isSchemaChanged()) {
      columns.clear();
      metadata.clear();
      buildMetadata(batch.getSchema());
    }
    addResults(batch.getRecordCount(), batch);
    batch.zeroVectors();
  }

  private void buildMetadata(BatchSchema schema) {
    for (int i = 0; i < schema.getFieldCount(); ++i) {
      // DRILL-6847:  This section adds query metadata to the REST results
      MaterializedField col = schema.getColumn(i);
      columns.add(col.getName());
      metadata.add(webDataType(col.getType()));
    }
  }

  private void addResults(int rows, VectorAccessible batch) {
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(webSessionResources.getSession().getOptions());
    if (autoLimitRowCount > 0) {
      rows = Math.max(0, Math.min(rows, autoLimitRowCount - rowCount));
    }
    for (int i = 0; i < rows; ++i) {
      rowCount++;
      final Map<String, String> record = Maps.newHashMap();
      for (VectorWrapper<?> vw : batch) {
        final String field = vw.getValueVector().getMetadata().getNamePart().getName();
        final TypeProtos.MinorType fieldMinorType = vw.getValueVector().getMetadata().getMajorType().getMinorType();
        final Accessor accessor = vw.getValueVector().getAccessor();
        final Object value = i < accessor.getValueCount() ? accessor.getObject(i) : null;
        final String display = value == null ? null : formatter.format(value, fieldMinorType);
        record.put(field, display);
      }
      results.add(record);
    }
  }

  /**
   * For authenticated WebUser no cleanup of {@link WebSessionResources} is done since it's re-used
   * for all the queries until lifetime of the web session.
   */
  public void cleanupSession() { }

  public static class AnonWebUserConnection extends WebUserConnection {

    AnonWebUserConnection(WebSessionResources webSessionResources) {
      super(webSessionResources);
    }

    /**
     * For anonymous WebUser after each query request is completed the {@link WebSessionResources} is cleaned up.
     */
    @Override
    public void cleanupSession() {
      webSessionResources.close();
    }
  }

  /**
   * Sets an autolimit on the size of records to be sent back on the connection
   * @param autoLimitRowCount Max number of records to be sent back to WebServer
   */
  void setAutoLimitRowCount(int autoLimitRowCount) {
    this.autoLimitRowCount = autoLimitRowCount;
  }

  /**
   * Gets the max size of records to be sent back by the query
   * @return Max number of records to be sent back to WebServer
   */
  public int getAutoLimitRowCount() {
    return this.autoLimitRowCount;
  }
}
