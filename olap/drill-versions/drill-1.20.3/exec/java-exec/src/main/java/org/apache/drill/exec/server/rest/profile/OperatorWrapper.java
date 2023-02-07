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
package org.apache.drill.exec.server.rest.profile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OperatorMetricRegistry;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.proto.UserBitShared.MetricValue;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.exec.proto.UserBitShared.StreamProfile;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Wrapper class for profiles of ALL operator instances of the same operator type within a major fragment.
 */
public class OperatorWrapper {
  @SuppressWarnings("unused")
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperatorWrapper.class);

  private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
  private static final String UNKNOWN_OPERATOR = "UNKNOWN_OPERATOR";
  //Negative valued constant used for denoting invalid index to indicate absence of metric
  private static final int NO_SPILL_METRIC_INDEX = Integer.MIN_VALUE;
  private final int major;
  private final List<ImmutablePair<ImmutablePair<OperatorProfile, Integer>, String>> opsAndHosts; // [(operatorProfile --> minorFragment number,host), ...]
  private final OperatorProfile firstProfile;
  private final String operatorType;
  private final String operatorName;
  private final int size;
  private final int timeSkewMin;
  private final double timeSkewRatio;
  private final int scanWaitMin;
  private final double waitSkewRatio;

  public OperatorWrapper(int major, List<ImmutablePair<ImmutablePair<OperatorProfile, Integer>, String>> opsAndHostsList, Map<String, String> phyOperMap, DrillConfig config) {
    //Threshold to track if the slowest operator ran relatively slow
    timeSkewMin = config.getInt(ExecConstants.PROFILE_WARNING_TIME_SKEW_MIN);
    timeSkewRatio = config.getDouble(ExecConstants.PROFILE_WARNING_TIME_SKEW_RATIO_PROCESS);
    //Threshold to track if the slowest SCAN operator spent more time in wait than processing
    scanWaitMin = config.getInt(ExecConstants.PROFILE_WARNING_SCAN_WAIT_MIN);
    waitSkewRatio = config.getDouble(ExecConstants.PROFILE_WARNING_TIME_SKEW_RATIO_WAIT);

    Preconditions.checkArgument(opsAndHostsList.size() > 0);
    this.major = major;
    firstProfile = opsAndHostsList.get(0).getLeft().getLeft();
    if (firstProfile.hasOperatorTypeName()) {
      operatorType = firstProfile.getOperatorTypeName();
    } else {
      CoreOperatorType operatorType = CoreOperatorType.valueOf(firstProfile.getOperatorType());
      this.operatorType = operatorType != null
          ? Objects.requireNonNull(operatorType).name()
          : null;
    }
    //Update Name from Physical Map
    String path = new OperatorPathBuilder().setMajor(major).setOperator(firstProfile).build();
    //Use Plan Extracted Operator Names if available
    String extractedOpName = phyOperMap.get(path);
    String inferredOpName = operatorType == null ? UNKNOWN_OPERATOR : operatorType;
    //Revert to inferred names for exceptional cases
    // 1. Extracted 'FLATTEN' operator is NULL
    // 2. Extracted 'SCAN' could be a PARQUET_ROW_GROUP_SCAN, or KAFKA_SUB_SCAN, or etc.
    // 3. Extracted 'UNION_EXCHANGE' could be a SINGLE_SENDER or UNORDERED_RECEIVER
    if (extractedOpName == null || inferredOpName.contains(extractedOpName) || extractedOpName.endsWith("_EXCHANGE")) {
      operatorName =  inferredOpName;
    } else {
      operatorName =  extractedOpName;
    }
    this.opsAndHosts = opsAndHostsList;
    size = opsAndHostsList.size();
  }

  public String getDisplayName() {
    final String path = new OperatorPathBuilder().setMajor(major).setOperator(firstProfile).build();
    return String.format("%s - %s", path, operatorName);
  }

  public String getId() {
    return String.format("operator-%d-%d", major, opsAndHosts.get(0).getLeft().getLeft().getOperatorId());
  }

  public static final String [] OPERATOR_COLUMNS = {
      OperatorTblTxt.MINOR_FRAGMENT, OperatorTblTxt.HOSTNAME, OperatorTblTxt.SETUP_TIME, OperatorTblTxt.PROCESS_TIME, OperatorTblTxt.WAIT_TIME,
      OperatorTblTxt.MAX_BATCHES, OperatorTblTxt.MAX_RECORDS, OperatorTblTxt.PEAK_MEMORY
  };

  public static final String [] OPERATOR_COLUMNS_TOOLTIP = {
      OperatorTblTooltip.MINOR_FRAGMENT, OperatorTblTooltip.HOSTNAME, OperatorTblTooltip.SETUP_TIME, OperatorTblTooltip.PROCESS_TIME, OperatorTblTooltip.WAIT_TIME,
      OperatorTblTooltip.MAX_BATCHES, OperatorTblTooltip.MAX_RECORDS, OperatorTblTooltip.PEAK_MEMORY
  };

  public String getContent() {
    TableBuilder builder = new TableBuilder(OPERATOR_COLUMNS, OPERATOR_COLUMNS_TOOLTIP, true);

    Map<String, String> attributeMap = new HashMap<>(); //Reusing for different fragments
    for (ImmutablePair<ImmutablePair<OperatorProfile, Integer>, String> ip : opsAndHosts) {
      int minor = ip.getLeft().getRight();
      OperatorProfile op = ip.getLeft().getLeft();

      attributeMap.put(HtmlAttribute.DATA_ORDER, String.valueOf(minor)); //Overwrite values from previous fragments
      String path = new OperatorPathBuilder().setMajor(major).setMinor(minor).setOperator(op).build();
      builder.appendCell(path, attributeMap);
      builder.appendCell(ip.getRight());
      builder.appendNanos(op.getSetupNanos());
      builder.appendNanos(op.getProcessNanos());
      builder.appendNanos(op.getWaitNanos());

      long maxBatches = Long.MIN_VALUE;
      long maxRecords = Long.MIN_VALUE;
      for (StreamProfile sp : op.getInputProfileList()) {
        maxBatches = Math.max(sp.getBatches(), maxBatches);
        maxRecords = Math.max(sp.getRecords(), maxRecords);
      }

      builder.appendFormattedInteger(maxBatches);
      builder.appendFormattedInteger(maxRecords);
      builder.appendBytes(op.getPeakLocalMemoryAllocated());
    }
    return builder.build();
  }

  public static final String[] OPERATORS_OVERVIEW_COLUMNS = {
      OverviewTblTxt.OPERATOR_ID, OverviewTblTxt.TYPE_OF_OPERATOR,
      OverviewTblTxt.AVG_SETUP_TIME, OverviewTblTxt.MAX_SETUP_TIME,
      OverviewTblTxt.AVG_PROCESS_TIME, OverviewTblTxt.MAX_PROCESS_TIME,
      OverviewTblTxt.MIN_WAIT_TIME, OverviewTblTxt.AVG_WAIT_TIME, OverviewTblTxt.MAX_WAIT_TIME,
      OverviewTblTxt.PERCENT_FRAGMENT_TIME, OverviewTblTxt.PERCENT_QUERY_TIME, OverviewTblTxt.ROWS.concat(OverviewTblTxt.ESTIMATED_ROWS),
      OverviewTblTxt.AVG_PEAK_MEMORY, OverviewTblTxt.MAX_PEAK_MEMORY
  };

  public static final String[] OPERATORS_OVERVIEW_COLUMNS_TOOLTIP = {
      OverviewTblTooltip.OPERATOR_ID, OverviewTblTooltip.TYPE_OF_OPERATOR,
      OverviewTblTooltip.AVG_SETUP_TIME, OverviewTblTooltip.MAX_SETUP_TIME,
      OverviewTblTooltip.AVG_PROCESS_TIME, OverviewTblTooltip.MAX_PROCESS_TIME,
      OverviewTblTooltip.MIN_WAIT_TIME, OverviewTblTooltip.AVG_WAIT_TIME, OverviewTblTooltip.MAX_WAIT_TIME,
      OverviewTblTooltip.PERCENT_FRAGMENT_TIME, OverviewTblTooltip.PERCENT_QUERY_TIME, OverviewTblTooltip.ROWS,
      OverviewTblTooltip.AVG_PEAK_MEMORY, OverviewTblTooltip.MAX_PEAK_MEMORY
  };

  //Palette to help shade operators sharing a common major fragment
  private static final String[] OPERATOR_OVERVIEW_BGCOLOR_PALETTE = {"#ffffff","#f2f2f2"};

  public void addSummary(TableBuilder tb, Map<String, Long> majorFragmentBusyTally, long majorFragmentBusyTallyTotal) {
    //Select background color from palette
    String opTblBgColor = OPERATOR_OVERVIEW_BGCOLOR_PALETTE[major%OPERATOR_OVERVIEW_BGCOLOR_PALETTE.length];
    String path = new OperatorPathBuilder().setMajor(major).setOperator(firstProfile).build();
    tb.appendCell(path, opTblBgColor, null);
    tb.appendCell(operatorName);

    //Check if spill information is available
    int spillCycleMetricIndex = getSpillCycleMetricIndex(operatorType);
    boolean isSpillableOp = (spillCycleMetricIndex != NO_SPILL_METRIC_INDEX);
    boolean hasSpilledToDisk = false;
    boolean isScanOp = operatorName.endsWith("SCAN");

    //Get MajorFragment Busy+Wait Time Tally
    long majorBusyNanos = majorFragmentBusyTally.get(new OperatorPathBuilder().setMajor(major).build());

    double setupSum = 0.0;
    double processSum = 0.0;
    double waitSum = 0.0;
    double memSum = 0.0;
    double spillCycleSum = 0.0;
    long spillCycleMax = 0L;
    long recordSum = 0L;

    //Construct list for sorting purposes (using legacy Comparators)
    final List<ImmutablePair<OperatorProfile, Integer>> opList = new ArrayList<>();

    for (ImmutablePair<ImmutablePair<OperatorProfile, Integer>,String> ip : opsAndHosts) {
      OperatorProfile profile = ip.getLeft().getLeft();
      setupSum += profile.getSetupNanos();
      processSum += profile.getProcessNanos();
      waitSum += profile.getWaitNanos();
      memSum += profile.getPeakLocalMemoryAllocated();
      for (final StreamProfile sp : profile.getInputProfileList()) {
        recordSum += sp.getRecords();
      }
      opList.add(ip.getLeft());

      //Capture Spill Info
      //Check to ensure index < #metrics (old profiles have less metrics); else reset isSpillableOp
      if (isSpillableOp) {
        //NOTE: We get non-zero value for non-existent metrics, so we can't use getMetric(index)
        //profile.getMetric(spillCycleMetricIndex).getLongValue();
        //Forced to iterate list
        for (MetricValue metricVal : profile.getMetricList()) {
          if (metricVal.getMetricId() == spillCycleMetricIndex) {
            long spillCycles = metricVal.getLongValue();
            spillCycleMax = Math.max(spillCycles, spillCycleMax);
            spillCycleSum += spillCycles;
            hasSpilledToDisk = (spillCycleSum > 0.0);
          }
        }
      }
    }

    final ImmutablePair<OperatorProfile, Integer> longSetup = Collections.max(opList, Comparators.setupTime);
    tb.appendNanos(Math.round(setupSum / size));
    tb.appendNanos(longSetup.getLeft().getSetupNanos());

    Map<String, String> timeSkewMap = null;
    final ImmutablePair<OperatorProfile, Integer> longProcess = Collections.max(opList, Comparators.processTime);
    //Calculating average processing time
    long avgProcTime = Math.round(processSum / size);
    tb.appendNanos(avgProcTime);
    long maxProcTime = longProcess.getLeft().getProcessNanos();
    //Calculating skew of longest processing fragment w.r.t. average
    double maxSkew = (avgProcTime > 0) ? maxProcTime/ (double) avgProcTime : 0.0d;
    //Marking skew if both thresholds are crossed
    if (avgProcTime > TimeUnit.SECONDS.toNanos(timeSkewMin) && maxSkew > timeSkewRatio ) {
      timeSkewMap = new HashMap<>();
      timeSkewMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_TIME_SKEW_TAG);
      timeSkewMap.put(HtmlAttribute.TITLE,  "One fragment took " + DECIMAL_FORMATTER.format(maxSkew) + " longer than average");
      timeSkewMap.put(HtmlAttribute.STYLE, HtmlAttribute.STYLE_VALUE_CURSOR_HELP);
    }
    tb.appendNanos(maxProcTime, timeSkewMap);

    final ImmutablePair<OperatorProfile, Integer> shortWait = Collections.min(opList, Comparators.waitTime);
    final ImmutablePair<OperatorProfile, Integer> longWait = Collections.max(opList, Comparators.waitTime);
    tb.appendNanos(shortWait.getLeft().getWaitNanos());
    //Calculating average wait time for fragment
    long avgWaitTime = Math.round(waitSum / size);

    //Slow Scan Warning
    Map<String, String> slowScanMap = null;
    //Marking slow scan if threshold is crossed and wait was longer than processing
    if (isScanOp && (avgWaitTime > TimeUnit.SECONDS.toNanos(scanWaitMin)) && (avgWaitTime > avgProcTime)) {
      slowScanMap = new HashMap<>();
      slowScanMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_SCAN_WAIT_TAG);
      slowScanMap.put(HtmlAttribute.TITLE, "Avg Wait Time &gt; Avg Processing Time");
      slowScanMap.put(HtmlAttribute.STYLE, HtmlAttribute.STYLE_VALUE_CURSOR_HELP);
    }
    tb.appendNanos(avgWaitTime, slowScanMap);

    long maxWaitTime = longWait.getLeft().getWaitNanos();
    //Skewed Wait Warning
    timeSkewMap = null; //Resetting
    //Calculating skew of longest waiting fragment w.r.t. average
    maxSkew = (avgWaitTime > 0) ? maxWaitTime/ (double) avgWaitTime : 0.0d;
    //Marking skew if both thresholds are crossed
    if (avgWaitTime > TimeUnit.SECONDS.toNanos(timeSkewMin) && maxSkew > waitSkewRatio) {
      timeSkewMap = new HashMap<>();
      timeSkewMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_TIME_SKEW_TAG);
      timeSkewMap.put(HtmlAttribute.TITLE, "One fragment waited " + DECIMAL_FORMATTER.format(maxSkew) + " longer than average");
      timeSkewMap.put(HtmlAttribute.STYLE, HtmlAttribute.STYLE_VALUE_CURSOR_HELP);
    }
    tb.appendNanos(maxWaitTime, timeSkewMap);

    tb.appendPercent(processSum / majorBusyNanos);
    tb.appendPercent(processSum / majorFragmentBusyTallyTotal);

    Map<String, String> estRowcountMap = new HashMap<>();
    estRowcountMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_EST_ROWS_ANCHOR);
    estRowcountMap.put(HtmlAttribute.KEY, path.replaceAll("-xx-", "-"));
    tb.appendFormattedInteger(recordSum, estRowcountMap);

    final ImmutablePair<OperatorProfile, Integer> peakMem = Collections.max(opList, Comparators.operatorPeakMemory);

    //Inject spill-to-disk attributes
    Map<String, String> avgSpillMap = null;
    Map<String, String> maxSpillMap = null;
    if (hasSpilledToDisk) {
      avgSpillMap = new HashMap<>();
      //Average SpillCycle
      double avgSpillCycle = spillCycleSum/size;
      avgSpillMap.put(HtmlAttribute.TITLE, DECIMAL_FORMATTER.format(avgSpillCycle) + " spills on average");
      avgSpillMap.put(HtmlAttribute.STYLE, HtmlAttribute.STYLE_VALUE_CURSOR_HELP);
      avgSpillMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_SPILL_TAG); //JScript will inject Icon
      avgSpillMap.put(HtmlAttribute.SPILLS, DECIMAL_FORMATTER.format(avgSpillCycle)); //JScript will inject Count
      maxSpillMap = new HashMap<>();
      maxSpillMap.put(HtmlAttribute.TITLE, "Most # spills: " + spillCycleMax);
      maxSpillMap.put(HtmlAttribute.STYLE, HtmlAttribute.STYLE_VALUE_CURSOR_HELP);
      maxSpillMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_SPILL_TAG); //JScript will inject Icon
      maxSpillMap.put(HtmlAttribute.SPILLS, String.valueOf(spillCycleMax)); //JScript will inject Count
    }

    tb.appendBytes(Math.round(memSum / size), avgSpillMap);
    tb.appendBytes(peakMem.getLeft().getPeakLocalMemoryAllocated(), maxSpillMap);
  }

  /**
   * Returns index of Spill Count/Cycle metric
   * @param operatorType
   * @return index of spill metric
   */
  private int getSpillCycleMetricIndex(String operatorType) {
    // TODO: DRILL-6642, replace null values for ProtocolMessageEnum with UNRECOGNIZED NullValue to avoid null checks
    if (operatorType == null) {
      return NO_SPILL_METRIC_INDEX;
    }
    String metricName;

    switch (operatorType) {
    case ExternalSort.OPERATOR_TYPE:
      metricName = "SPILL_COUNT";
      break;
    case HashAggregate.OPERATOR_TYPE:
    case HashJoinPOP.OPERATOR_TYPE:
      metricName = "SPILL_CYCLE";
      break;
    default:
      return NO_SPILL_METRIC_INDEX;
    }

    int metricIndex = 0; //Default
    String[] metricNames = OperatorMetricRegistry.getMetricNames(operatorType);
    for (String name : metricNames) {
      if (name.equalsIgnoreCase(metricName)) {
        return metricIndex;
      }
      metricIndex++;
    }
    //Backward compatibility with rendering older profiles. Ideally we should never touch this if an expected metric is not there
    return NO_SPILL_METRIC_INDEX;
  }

  public String getMetricsTable() {
    if (operatorType == null) {
      return "";
    }
    final String[] metricNames = OperatorMetricRegistry.getMetricNames(operatorType);
    if (metricNames == null) {
      return "";
    }

    final String[] metricsTableColumnNames = new String[metricNames.length + 1];
    metricsTableColumnNames[0] = "Minor Fragment";
    int i = 1;
    for (final String metricName : metricNames) {
      metricsTableColumnNames[i++] = metricName;
    }
    final TableBuilder builder = new TableBuilder(metricsTableColumnNames, null);

    for (final ImmutablePair<ImmutablePair<OperatorProfile, Integer>,String> ip : opsAndHosts) {
      final OperatorProfile op = ip.getLeft().getLeft();

      builder.appendCell(
          new OperatorPathBuilder()
          .setMajor(major)
          .setMinor(ip.getLeft().getRight())
          .setOperator(op)
          .build());

      final Number[] values = new Number[metricNames.length];
      //Track new/Unknown Metrics
      final Set<Integer> unknownMetrics = new TreeSet<>();
      for (final MetricValue metric : op.getMetricList()) {
        if (metric.getMetricId() < metricNames.length) {
          if (metric.hasLongValue()) {
            values[metric.getMetricId()] = metric.getLongValue();
          } else if (metric.hasDoubleValue()) {
            values[metric.getMetricId()] = metric.getDoubleValue();
          }
        } else {
          //Tracking unknown metric IDs
          unknownMetrics.add(metric.getMetricId());
        }
      }
      for (final Number value : values) {
        if (value != null) {
          builder.appendFormattedNumber(value);
        } else {
          builder.appendCell("");
        }
      }
    }
    return builder.build();
  }

  private class OperatorTblTxt {
    static final String MINOR_FRAGMENT = "Minor Fragment";
    static final String HOSTNAME = "Hostname";
    static final String SETUP_TIME = "Setup Time";
    static final String PROCESS_TIME = "Process Time";
    static final String WAIT_TIME = "Wait Time";
    static final String MAX_BATCHES = "Max Batches";
    static final String MAX_RECORDS = "Max Records";
    static final String PEAK_MEMORY = "Peak Memory";
  }

  private class OperatorTblTooltip {
    static final String MINOR_FRAGMENT = "Operator's Minor Fragment";
    static final String HOSTNAME = "Host on which the minor fragment ran";
    static final String SETUP_TIME = "Setup Time for the minor fragment's operator";
    static final String PROCESS_TIME = "Process Time for the minor fragment's operator";
    static final String WAIT_TIME = "Wait Time for the minor fragment's operator";
    static final String MAX_BATCHES = "Max Batches processed by the minor fragment's operator";
    static final String MAX_RECORDS = "Max Records processed by the minor fragment's operator";
    static final String PEAK_MEMORY = "Peak Memory usage by the minor fragment's operator";
  }

  private class OverviewTblTxt {
    static final String OPERATOR_ID = "Operator ID";
    static final String TYPE_OF_OPERATOR = "Type";
    static final String AVG_SETUP_TIME = "Avg Setup Time";
    static final String MAX_SETUP_TIME = "Max Setup Time";
    static final String AVG_PROCESS_TIME = "Avg Process Time";
    static final String MAX_PROCESS_TIME = "Max Process Time";
    static final String MIN_WAIT_TIME = "Min Wait Time";
    static final String AVG_WAIT_TIME = "Avg Wait Time";
    static final String MAX_WAIT_TIME = "Max Wait Time";
    static final String PERCENT_FRAGMENT_TIME = "% Fragment Time";
    static final String PERCENT_QUERY_TIME = "% Query Time";
    static final String ROWS = "Rows";
    static final String ESTIMATED_ROWS = "<div class='estRows' title='Estimated'>(Estimated)</div>";
    static final String AVG_PEAK_MEMORY = "Avg Peak Memory";
    static final String MAX_PEAK_MEMORY = "Max Peak Memory";
  }

  private class OverviewTblTooltip {
    static final String OPERATOR_ID = "Operator ID";
    static final String TYPE_OF_OPERATOR = "Operator Type";
    static final String AVG_SETUP_TIME = "Average time in setting up fragments";
    static final String MAX_SETUP_TIME = "Longest time a fragment took in setup";
    static final String AVG_PROCESS_TIME = "Average process time for a fragment";
    static final String MAX_PROCESS_TIME = "Longest process time of any fragment";
    static final String MIN_WAIT_TIME = "Shortest time a fragment spent in waiting";
    static final String AVG_WAIT_TIME = "Average wait time for a fragment";
    static final String MAX_WAIT_TIME = "Longest time a fragment spent in waiting";
    static final String PERCENT_FRAGMENT_TIME = "Percentage of the total fragment time that was spent on the operator";
    static final String PERCENT_QUERY_TIME = "Percentage of the total query time that was spent on the operator";
    static final String ROWS = "Rows emitted by scans, or consumed by other operators";
    static final String AVG_PEAK_MEMORY  =  "Average memory consumption by a fragment";
    static final String MAX_PEAK_MEMORY  =  "Highest memory consumption by a fragment";
  }
}
