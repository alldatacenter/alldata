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
package org.apache.drill.exec.util.record;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.RecordBatchSizer.ColumnSize;

/**
 * Utility class to capture key record batch statistics.
 */
public final class RecordBatchStats {
  // Logger
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordBatchStats.class);

  /** A prefix for all batch stats to simplify search */
  public static final String BATCH_STATS_PREFIX = "BATCH_STATS";

  /** Helper class which loads contextual record batch logging options */
  public static final class RecordBatchStatsContext {
    /** batch size logging for all readers */
    private final boolean enableBatchSzLogging;
    /** Fine grained batch size logging */
    private final boolean enableFgBatchSzLogging;
    /** Unique Operator Identifier */
    private final String contextOperatorId;

    /**
     * @param context fragment context
     * @param oContext operator context
     */
    public RecordBatchStatsContext(FragmentContext context, OperatorContext oContext) {
      final boolean operatorEnabledForStatsLogging = isBatchStatsEnabledForOperator(context, oContext);

      if (operatorEnabledForStatsLogging) {
        enableBatchSzLogging = context.getOptions().getBoolean(ExecConstants.STATS_LOGGING_BATCH_SIZE_OPTION);
        enableFgBatchSzLogging = context.getOptions().getBoolean(ExecConstants.STATS_LOGGING_FG_BATCH_SIZE_OPTION);

      } else {
        enableBatchSzLogging = false;
        enableFgBatchSzLogging = false;
      }

      contextOperatorId = new StringBuilder()
        .append(getQueryId(context))
        .append(":")
        .append(oContext.getStats().getId())
        .toString();
    }

    /**
     * @return the enableBatchSzLogging
     */
    public boolean isEnableBatchSzLogging() {
      return enableBatchSzLogging || enableFgBatchSzLogging || logger.isDebugEnabled();
    }

    /**
     * @return the enableFgBatchSzLogging
     */
    public boolean isEnableFgBatchSzLogging() {
      return enableFgBatchSzLogging || logger.isDebugEnabled();
    }

    /**
     * @return indicates whether stats messages should be logged in info or debug level
     */
    public boolean useInfoLevelLogging() {
      return isEnableBatchSzLogging() && !logger.isDebugEnabled();
    }

    /**
     * @return the contextOperatorId
     */
    public String getContextOperatorId() {
      return contextOperatorId;
    }

    private String getQueryId(FragmentContext _context) {
      if (_context instanceof FragmentContextImpl) {
        final FragmentContextImpl context = (FragmentContextImpl) _context;
        final FragmentHandle handle       = context.getHandle();

        if (handle != null) {
          return QueryIdHelper.getQueryIdentifier(handle);
        }
      }
      return "NA";
    }

    private boolean isBatchStatsEnabledForOperator(FragmentContext context, OperatorContext oContext) {
      // The configuration can select what operators should log batch statistics
      final String statsLoggingOperator = context.getOptions().getString(ExecConstants.STATS_LOGGING_BATCH_OPERATOR_OPTION).toUpperCase();
      final String allOperatorsStr = "ALL";

      // All operators are allowed to log batch statistics
      if (allOperatorsStr.equals(statsLoggingOperator)) {
        return true;
      }

      // No, only a select few are allowed; syntax: operator-id-1,operator-id-2,..
      final String[] operators = statsLoggingOperator.split(",");
      final String operatorId = oContext.getStats().getId().toUpperCase();

      for (int idx = 0; idx < operators.length; idx++) {
        // We use "contains" because the operator identifier is a composite string; e.g., 3:[PARQUET_ROW_GROUP_SCAN]
        if (operatorId.contains(operators[idx].trim())) {
          return true;
        }
      }

      return false;
    }
  }

  /** Indicates whether a record batch is Input or Output */
  public enum RecordBatchIOType {
    INPUT ("incoming"),
    INPUT_RIGHT ("incoming right"),
    INPUT_LEFT ("incoming left"),
    OUTPUT ("outgoing"),
    PASSTHROUGH ("passthrough");

    private final String ioTypeString;

    private RecordBatchIOType(String ioTypeString) {
      this.ioTypeString = ioTypeString;
    }

    /**
     * @return IO Type string
     */
    public String getIOTypeString() {
      return ioTypeString;
    }
  }

  /**
   * @see {@link RecordBatchStats#logRecordBatchStats(RecordBatchIOType, String, RecordBatch, RecordBatchStatsContext)}
   */
  public static void logRecordBatchStats(RecordBatchIOType ioType,
    String sourceId,
    RecordBatch recordBatch,
    RecordBatchStatsContext batchStatsContext) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }

    logRecordBatchStats(ioType, sourceId, new RecordBatchSizer(recordBatch), batchStatsContext);
  }

  /**
   * @see {@link RecordBatchStats#logRecordBatchStats(RecordBatchIOType, RecordBatch, RecordBatchStatsContext)}
   */
  public static void logRecordBatchStats(RecordBatchIOType ioType,
    RecordBatch recordBatch,
    RecordBatchStatsContext batchStatsContext) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }

    logRecordBatchStats(ioType, null, new RecordBatchSizer(recordBatch), batchStatsContext);
  }

  /**
   * @see {@link RecordBatchStats#logRecordBatchStats(RecordBatchIOType, String, RecordBatchSizer, RecordBatchStatsContext)}
   */
  public static void logRecordBatchStats(RecordBatchIOType ioType,
    RecordBatchSizer recordBatchSizer,
    RecordBatchStatsContext batchStatsContext) {

    logRecordBatchStats(ioType, null, recordBatchSizer, batchStatsContext);
  }

  /**
   * Logs record batch statistics for the input record batch (logging happens only
   * when record statistics logging is enabled).
   *
   * @param ioType whether a record batch is an input or/and output
   * @param sourceId optional source identifier for scanners
   * @param batchSizer contains batch sizing information
   * @param batchStatsContext batch stats context object
   */
  public static void logRecordBatchStats(RecordBatchIOType ioType,
    String sourceId,
    RecordBatchSizer batchSizer,
    RecordBatchStatsContext batchStatsContext) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }

    final String statsId = batchStatsContext.getContextOperatorId();
    final boolean verbose = batchStatsContext.isEnableFgBatchSzLogging();
    final String msg = printRecordBatchStats(statsId, ioType, sourceId, batchSizer, verbose);

    logBatchStatsMsg(batchStatsContext, msg, false);
  }

  /**
   * Logs a generic batch statistics message
   *
   * @param message log message
   * @param batchStatsContext batch stats context object
   */
  public static void logRecordBatchStats(String message,
    RecordBatchStatsContext batchStatsContext) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }

    logBatchStatsMsg(batchStatsContext, message, true);
  }

  /**
   * Logs a generic batch statistics message
   *
   * @param batchStatsContext batch stats context object
   * @param format a string format as in {@link String#format} method
   * @param args format's arguments
   */
  public static void logRecordBatchStats(RecordBatchStatsContext batchStatsContext,
    String format,
    Object...args) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }

    final String message = String.format(format, args);
    logBatchStatsMsg(batchStatsContext, message, true);
  }

  /**
   * @param allocator dumps allocator statistics
   * @return string with allocator statistics
   */
  public static String printAllocatorStats(BufferAllocator allocator) {
    StringBuilder msg = new StringBuilder();
    msg.append(BATCH_STATS_PREFIX);
    msg.append(": dumping allocator statistics:\n");
    msg.append(BATCH_STATS_PREFIX);
    msg.append(": ");
    msg.append(allocator.toString());

    return msg.toString();
  }

  /**
   * Prints the configured batch size
   *
   * @param batchStatsContext batch stats context object
   * @param batchSize contains the configured batch size
   */
  public static void printConfiguredBatchSize(RecordBatchStatsContext batchStatsContext,
    int batchSize) {

    if (!batchStatsContext.isEnableBatchSzLogging()) {
      return; // NOOP
    }
    final String message = String.format("The batch memory has been set to [%d] byte(s)", batchSize);
    logRecordBatchStats(message, batchStatsContext);
  }

// ----------------------------------------------------------------------------
// Local Implementation
// ----------------------------------------------------------------------------

  /**
   * Disabling class object instantiation.
   */
  private RecordBatchStats() {
  }

  /**
   * Constructs record batch statistics for the input record batch
   *
   * @param statsId instance identifier
   * @param ioType whether a record batch is an input or/and output
   * @param sourceId optional source identifier for scanners
   * @param batchSizer contains batch sizing information
   * @param verbose whether to include fine-grained stats
   *
   * @return a string containing the record batch statistics
   */
  private static String printRecordBatchStats(String statsId,
    RecordBatchIOType ioType,
    String sourceId,
    RecordBatchSizer batchSizer,
    boolean verbose) {

    final StringBuilder msg = new StringBuilder();

    msg.append(BATCH_STATS_PREFIX);
    msg.append(" Operator: {");
    msg.append(statsId);
    if (sourceId != null) {
      msg.append(':');
      msg.append(sourceId);
    }
    msg.append("}, IO Type: {");
    msg.append(toString(ioType));
    msg.append("}, Batch size: {");
    msg.append( "  Records: " );
    msg.append(batchSizer.rowCount());
    msg.append(", Total size: ");
    msg.append(batchSizer.getActualSize());
    msg.append(", Data size: ");
    msg.append(batchSizer.getNetBatchSize());
    msg.append(", Gross row width: ");
    msg.append(batchSizer.getGrossRowWidth());
    msg.append(", Net row width: ");
    msg.append(batchSizer.getNetRowWidth());
    msg.append(", Density: ");
    msg.append(batchSizer.getAvgDensity());
    msg.append("% }\n");

    if (verbose) {
      msg.append("Batch schema & sizes: {\n");
      for (ColumnSize colSize : batchSizer.columns().values()) {
        msg.append(BATCH_STATS_PREFIX);
        msg.append("\t");
        msg.append(statsId);
        msg.append('\t');
        msg.append(colSize.toString());
        msg.append(" }\n");
      }
      msg.append(" }\n");
    }

    return msg.toString();
  }

  private static void logBatchStatsMsg(RecordBatchStatsContext batchStatsContext,
    String msg,
    boolean includePrefix) {

    if (includePrefix) {
      final String statsId = batchStatsContext.getContextOperatorId();
      msg = BATCH_STATS_PREFIX + " Operator: {" + statsId + "} " + msg;
    }

    if (batchStatsContext.useInfoLevelLogging()) {
      logger.info(msg);
    } else {
      logger.debug(msg);
    }
  }

  private static String toString(RecordBatchIOType ioType) {
    Preconditions.checkNotNull(ioType, "The record batch IO type cannot be null");

    switch (ioType) {
      case INPUT: return "incoming";
      case INPUT_RIGHT: return "incoming right";
      case INPUT_LEFT: return "incoming left";
      case OUTPUT: return "outgoing";
      case PASSTHROUGH: return "passthrough";

      default: throw new RuntimeException("Unexpected record batch IO type..");
    }

  }

}
