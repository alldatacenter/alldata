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
package org.apache.drill.exec.ops;

import java.util.Iterator;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.MetricValue;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile.Builder;
import org.apache.drill.exec.proto.UserBitShared.StreamProfile;

import com.carrotsearch.hppc.IntDoubleHashMap;
import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import com.carrotsearch.hppc.procedures.IntDoubleProcedure;
import com.carrotsearch.hppc.procedures.IntLongProcedure;
import org.apache.drill.exec.server.rest.profile.CoreOperatorType;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

public class OperatorStats {
  protected final int operatorId;
  protected final String operatorType;
  private final BufferAllocator allocator;

  private final IntLongHashMap longMetrics = new IntLongHashMap();
  private final IntDoubleHashMap doubleMetrics = new IntDoubleHashMap();

  public long[] recordsReceivedByInput;
  public long[] batchesReceivedByInput;
  private final long[] schemaCountByInput;


  private boolean inProcessing = false;
  private boolean inSetup = false;
  private boolean inWait = false;

  protected long processingNanos;
  protected long setupNanos;
  protected long waitNanos;

  private long processingMark;
  private long setupMark;
  private long waitMark;

  private final int inputCount;

  public OperatorStats(OpProfileDef def, BufferAllocator allocator){
    this(def.getOperatorId(), def.getOperatorType(), def.getIncomingCount(), allocator);
  }

  /**
   * Copy constructor to be able to create a copy of existing stats object shell and use it independently
   * this is useful if stats have to be updated in different threads, since it is not really
   * possible to update such stats as waitNanos, setupNanos and processingNanos across threads
   * @param original - OperatorStats object to create a copy from
   * @param isClean - flag to indicate whether to start with clean state indicators or inherit those from original object
   */

  public OperatorStats(OperatorStats original, boolean isClean) {
    this(original.operatorId, original.operatorType, original.inputCount, original.allocator);

    if ( !isClean ) {
      inProcessing = original.inProcessing;
      inSetup = original.inSetup;
      inWait = original.inWait;

      processingMark = original.processingMark;
      setupMark = original.setupMark;
      waitMark = original.waitMark;
    }
  }

  @VisibleForTesting
  public OperatorStats(int operatorId, String operatorType, int inputCount, BufferAllocator allocator) {
    super();
    this.allocator = allocator;
    this.operatorId = operatorId;
    this.operatorType = operatorType;
    this.inputCount = inputCount;
    this.recordsReceivedByInput = new long[inputCount];
    this.batchesReceivedByInput = new long[inputCount];
    this.schemaCountByInput = new long[inputCount];
  }

  private String assertionError(String msg){
    return String.format("Failure while %s for operator id %d. Currently have states of processing:%s, setup:%s, waiting:%s.", msg, operatorId, inProcessing, inSetup, inWait);
  }

  /**
   * OperatorStats merger - to merge stats from other OperatorStats
   * this is needed in case some processing is multithreaded that needs to have
   * separate OperatorStats to deal with
   * WARN - this will only work for metrics that can be added
   * @param from - OperatorStats from where to merge to "this"
   * @return OperatorStats - for convenience so one can merge multiple stats in one go
   */

  public OperatorStats mergeMetrics(OperatorStats from) {
    final IntLongHashMap fromMetrics = from.longMetrics;

    final Iterator<IntLongCursor> iter = fromMetrics.iterator();
    while (iter.hasNext()) {
      final IntLongCursor next = iter.next();
      longMetrics.putOrAdd(next.key, next.value, next.value);
    }

    final IntDoubleHashMap fromDMetrics = from.doubleMetrics;
    final Iterator<IntDoubleCursor> iterD = fromDMetrics.iterator();

    while (iterD.hasNext()) {
      final IntDoubleCursor next = iterD.next();
      doubleMetrics.putOrAdd(next.key, next.value, next.value);
    }
    return this;
  }

  /**
   * Clear stats
   */
  public synchronized void clear() {
    processingNanos = 0l;
    setupNanos = 0l;
    waitNanos = 0l;
    longMetrics.clear();
    doubleMetrics.clear();
  }

  public synchronized void startSetup() {
    assert !inSetup  : assertionError("starting setup");
    stopProcessing();
    inSetup = true;
    setupMark = System.nanoTime();
  }

  public synchronized void stopSetup() {
    assert inSetup :  assertionError("stopping setup");
    startProcessing();
    setupNanos += System.nanoTime() - setupMark;
    inSetup = false;
  }

  public synchronized void startProcessing() {
    assert !inProcessing : assertionError("starting processing");
    processingMark = System.nanoTime();
    inProcessing = true;
  }

  public synchronized void stopProcessing() {
    assert inProcessing : assertionError("stopping processing");
    processingNanos += System.nanoTime() - processingMark;
    inProcessing = false;
  }

  public synchronized void startWait() {
    assert !inWait : assertionError("starting waiting");
    stopProcessing();
    inWait = true;
    waitMark = System.nanoTime();
  }

  public synchronized void stopWait() {
    assert inWait : assertionError("stopping waiting");
    startProcessing();
    waitNanos += System.nanoTime() - waitMark;
    inWait = false;
  }

  public synchronized void batchReceived(int inputIndex, long records, boolean newSchema) {
    recordsReceivedByInput[inputIndex] += records;
    batchesReceivedByInput[inputIndex]++;
    if(newSchema){
      schemaCountByInput[inputIndex]++;
    }
  }

  public String getId() {
    return new StringBuilder()
        .append(this.operatorId)
        .append(":")
        .append("[")
        .append(operatorType)
        .append("]")
        .toString();
  }

  @SuppressWarnings("deprecation")
  public OperatorProfile getProfile() {
    final OperatorProfile.Builder b = OperatorProfile
        .newBuilder()
        .setOperatorTypeName(operatorType)
        .setOperatorId(operatorId)
        .setSetupNanos(setupNanos)
        .setProcessNanos(processingNanos)
        .setWaitNanos(waitNanos);

    CoreOperatorType coreOperatorType = CoreOperatorType.forName(operatorType);

    if (coreOperatorType != null) {
      b.setOperatorType(coreOperatorType.getId());
    }

    if (allocator != null) {
      b.setPeakLocalMemoryAllocated(allocator.getPeakMemoryAllocation());
    }

    addAllMetrics(b);
    return b.build();
  }

  public void addAllMetrics(OperatorProfile.Builder builder) {
    addStreamProfile(builder);
    addLongMetrics(builder);
    addDoubleMetrics(builder);
  }

  public void addStreamProfile(OperatorProfile.Builder builder) {
    for(int i = 0; i < recordsReceivedByInput.length; i++){
      builder.addInputProfile(StreamProfile.newBuilder().setBatches(batchesReceivedByInput[i]).setRecords(recordsReceivedByInput[i]).setSchemas(this.schemaCountByInput[i]));
    }
  }

  private static class LongProc implements IntLongProcedure {

    private final OperatorProfile.Builder builder;

    public LongProc(Builder builder) {
      super();
      this.builder = builder;
    }

    @Override
    public void apply(int key, long value) {
      builder.addMetric(MetricValue.newBuilder().setMetricId(key).setLongValue(value));
    }
  }

  public void addLongMetrics(OperatorProfile.Builder builder) {
    if (longMetrics.size() > 0) {
      longMetrics.forEach(new LongProc(builder));
    }
  }

  private static class DoubleProc implements IntDoubleProcedure {
    private final OperatorProfile.Builder builder;

    public DoubleProc(Builder builder) {
      super();
      this.builder = builder;
    }

    @Override
    public void apply(int key, double value) {
      builder.addMetric(MetricValue.newBuilder().setMetricId(key).setDoubleValue(value));
    }
  }

  public void addDoubleMetrics(OperatorProfile.Builder builder) {
    if (doubleMetrics.size() > 0) {
      doubleMetrics.forEach(new DoubleProc(builder));
    }
  }

  /**
   * Set a stat to the specified long value. Creates the stat
   * if the stat does not yet exist.
   *
   * @param metric the metric to update
   * @param value the value to set
   */

  public void addLongStat(MetricDef metric, long value){
    longMetrics.putOrAdd(metric.metricId(), value, value);
  }

  @VisibleForTesting
  public long getLongStat(MetricDef metric) {
    return longMetrics.get(metric.metricId());
  }

  /**
   * Add a double value to the existing value. Creates the stat
   * (with an initial value of zero) if the stat does not yet
   * exist.
   *
   * @param metric the metric to update
   * @param value the value to add to the existing value
   */

  public void addDoubleStat(MetricDef metric, double value){
    doubleMetrics.putOrAdd(metric.metricId(), value, value);
  }

  @VisibleForTesting
  public double getDoubleStat(MetricDef metric) {
    return doubleMetrics.get(metric.metricId());
  }

  /**
   * Add a long value to the existing value. Creates the stat
   * (with an initial value of zero) if the stat does not yet
   * exist.
   *
   * @param metric the metric to update
   * @param value the value to add to the existing value
   */

  public void setLongStat(MetricDef metric, long value){
    longMetrics.put(metric.metricId(), value);
  }

  /**
   * Set a stat to the specified double value. Creates the stat
   * if the stat does not yet exist.
   *
   * @param metric the metric to update
   * @param value the value to set
   */

  public void setDoubleStat(MetricDef metric, double value){
    doubleMetrics.put(metric.metricId(), value);
  }

  public long getWaitNanos() {
    return waitNanos;
  }

  /**
   * Adjust waitNanos based on client calculations
   * @param waitNanosOffset - could be negative as well as positive
   */
  public void adjustWaitNanos(long waitNanosOffset) {
    this.waitNanos += waitNanosOffset;
  }

  public long getProcessingNanos() {
    return processingNanos;
  }
}
