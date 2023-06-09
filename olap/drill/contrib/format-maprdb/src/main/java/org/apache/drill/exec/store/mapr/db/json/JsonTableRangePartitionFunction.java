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

import java.util.List;
import java.util.Objects;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.exec.planner.physical.AbstractRangePartitionFunction;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.vector.ValueVector;
import org.ojai.store.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.mapr.db.Table;
import com.mapr.db.impl.ConditionImpl;
import com.mapr.db.impl.IdCodec;
import com.mapr.db.impl.ConditionNode.RowkeyRange;
import com.mapr.db.scan.ScanRange;
import com.mapr.fs.jni.MapRConstants;
import com.mapr.org.apache.hadoop.hbase.util.Bytes;

@JsonTypeName("jsontable-range-partition-function")
public class JsonTableRangePartitionFunction extends AbstractRangePartitionFunction {
  private static final Logger logger = LoggerFactory.getLogger(JsonTableRangePartitionFunction.class);

  @JsonProperty("refList")
  protected List<FieldReference> refList;

  @JsonProperty("tableName")
  protected String tableName;

  @JsonIgnore
  protected String userName;

  @JsonIgnore
  protected ValueVector partitionKeyVector = null;

  // List of start keys of the scan ranges for the table.
  @JsonProperty
  protected List<byte[]> startKeys = null;

  // List of stop keys of the scan ranges for the table.
  @JsonProperty
  protected List<byte[]> stopKeys = null;

  @JsonCreator
  public JsonTableRangePartitionFunction(
      @JsonProperty("refList") List<FieldReference> refList,
      @JsonProperty("tableName") String tableName,
      @JsonProperty("startKeys") List<byte[]> startKeys,
      @JsonProperty("stopKeys") List<byte[]> stopKeys) {
    this.refList = refList;
    this.tableName = tableName;
    this.startKeys = startKeys;
    this.stopKeys = stopKeys;
  }

  public JsonTableRangePartitionFunction(List<FieldReference> refList,
      String tableName, String userName, MapRDBFormatPlugin formatPlugin) {
    this.refList = refList;
    this.tableName = tableName;
    this.userName = userName;
    initialize(formatPlugin);
  }

  @JsonProperty("refList")
  @Override
  public List<FieldReference> getPartitionRefList() {
    return refList;
  }

  @Override
  public void setup(List<VectorWrapper<?>> partitionKeys) {
    if (partitionKeys.size() != 1) {
      throw new UnsupportedOperationException(
          "Range partitioning function supports exactly one partition column; encountered " + partitionKeys.size());
    }

    VectorWrapper<?> v = partitionKeys.get(0);

    partitionKeyVector = v.getValueVector();

    Preconditions.checkArgument(partitionKeyVector != null, "Found null partitionKeVector.");
  }

  @Override
  public int hashCode() {
    return Objects.hash(refList, tableName, userName, partitionKeyVector, startKeys, stopKeys);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JsonTableRangePartitionFunction) {
      JsonTableRangePartitionFunction rpf = (JsonTableRangePartitionFunction) obj;
      List<FieldReference> thisPartRefList = this.getPartitionRefList();
      List<FieldReference> otherPartRefList = rpf.getPartitionRefList();
      if (thisPartRefList.size() != otherPartRefList.size()) {
        return false;
      }
      for (int refIdx = 0; refIdx < thisPartRefList.size(); refIdx++) {
        if (!thisPartRefList.get(refIdx).equals(otherPartRefList.get(refIdx))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int eval(int index, int numPartitions) {

    String key = partitionKeyVector.getAccessor().getObject(index).toString();
    byte[] encodedKey = IdCodec.encodeAsBytes(key);

    int tabletId = -1;

    // Do a 'range' binary search through the list of start-stop keys to find nearest range.  Assumption is
    // that the list of start keys is ordered (this is ensured by MetaTable.getScanRanges())
    // TODO: This search should ideally be delegated to MapR-DB once an appropriate API is available
    // to optimize this
    int low = 0, high = startKeys.size() - 1;
    while (low <= high) {
      int mid = low + (high-low)/2;

      byte[] start = startKeys.get(mid);
      byte[] stop  = stopKeys.get(mid);

      // Check if key is present in the mid interval of [start, stop].
      // Account for empty byte array start/stop
      if ((Bytes.compareTo(encodedKey, start) >= 0 ||
             Bytes.equals(start, MapRConstants.EMPTY_BYTE_ARRAY)
           ) &&
           (Bytes.compareTo(encodedKey, stop) < 0 ||
             Bytes.equals(stop, MapRConstants.EMPTY_BYTE_ARRAY)
           )
         ) {
        tabletId = mid;
        break;
      }

      if (Bytes.compareTo(encodedKey, start) >= 0) {
        // key is greater, ignore left side
        low = mid + 1;
      } else {
        // key is smaller, ignore right side
        high = mid - 1;
      }
    }

    if (tabletId < 0) {
      tabletId = 0;
      logger.warn("Key {} was not found in any of the start-stop ranges. Using default tabletId {}", key, tabletId);
    }

    int partitionId = tabletId % numPartitions;

    logger.trace("Key = {}, tablet id = {}, partition id = {}", key, tabletId, partitionId);

    return partitionId;
  }


  public void initialize(MapRDBFormatPlugin plugin) {

    // get the table handle from the table cache
    Table table = plugin.getJsonTableCache().getTable(tableName, userName);

    // Get all scan ranges for the primary table.
    // The reason is the row keys could typically belong to any one of the tablets of the table, so
    // there is no use trying to get only limited set of scan ranges.
    // NOTE: here we use the restrictedScanRangeSizeMB because the range partitioning should be parallelized
    // based on the number of scan ranges on the RestrictedJsonTableGroupScan.
    List<ScanRange> ranges = table.getMetaTable().getScanRanges(plugin.getRestrictedScanRangeSizeMB());

    this.startKeys = Lists.newArrayList();
    this.stopKeys = Lists.newArrayList();

    logger.debug("Num scan ranges for table {} = {}", table.getName(), ranges.size());

    int count = 0;
    for (ScanRange r : ranges) {
      QueryCondition condition = r.getCondition();
      List<RowkeyRange> rowkeyRanges =  ((ConditionImpl)condition).getRowkeyRanges();
      byte[] start = rowkeyRanges.get(0).getStartRow();
      byte[] stop  = rowkeyRanges.get(rowkeyRanges.size() - 1).getStopRow();

      Preconditions.checkNotNull(start, String.format("Encountered a null start key at position %d for scan range condition %s.", count, condition.toString()));
      Preconditions.checkNotNull(stop, String.format("Encountered a null stop key at position %d for scan range condition %s.", count, condition.toString()));

      if (count > 0) {
        // after the first start key, rest should be non-empty
        Preconditions.checkState( !(Bytes.equals(start, MapRConstants.EMPTY_BYTE_ARRAY)), String.format("Encountered an empty start key at position %d", count));
      }

      if (count < ranges.size() - 1) {
        // except for the last stop key, rest should be non-empty
        Preconditions.checkState( !(Bytes.equals(stop, MapRConstants.EMPTY_BYTE_ARRAY)), String.format("Encountered an empty stop key at position %d", count));
      }

      startKeys.add(start);
      stopKeys.add(stop);
      count++;
    }

    // check validity; only need to check one of the lists since they are populated together
    Preconditions.checkArgument(startKeys.size() > 0, "Found empty list of start/stopKeys.");

    Preconditions.checkState(startKeys.size() == ranges.size(),
        String.format("Mismatch between the lengths: num start keys = %d, num scan ranges = %d", startKeys.size(), ranges.size()));

    Preconditions.checkState(stopKeys.size() == ranges.size(),
        String.format("Mismatch between the lengths: num stop keys = %d, num scan ranges = %d", stopKeys.size(), ranges.size()));

  }

}
