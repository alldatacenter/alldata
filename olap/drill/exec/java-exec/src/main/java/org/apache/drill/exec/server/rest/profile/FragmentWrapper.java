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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.MajorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.exec.proto.UserBitShared.StreamProfile;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Collections2;

/**
 * Wrapper class for a major fragment profile.
 */
public class FragmentWrapper {
  private final MajorFragmentProfile major;
  private final long start;
  private final int runningProfileProgressThreshold;

  public FragmentWrapper(final MajorFragmentProfile major, final long start, DrillConfig config) {
    this.major = Preconditions.checkNotNull(major);
    this.start = start;
    //Threshold to track if query made no progress in specified elapsed time
    runningProfileProgressThreshold = config.getInt(ExecConstants.PROFILE_WARNING_PROGRESS_THRESHOLD);
  }

  public String getDisplayName() {
    return String.format("Major Fragment: %s", new OperatorPathBuilder().setMajor(major).build());
  }

  public String getId() {
    return String.format("fragment-%s", major.getMajorFragmentId());
  }

  public static final String[] ACTIVE_FRAGMENT_OVERVIEW_COLUMNS = {
      OverviewTblTxt.MAJOR_FRAGMENT, OverviewTblTxt.MINOR_FRAGMENTS_REPORTING,
      OverviewTblTxt.FIRST_START, OverviewTblTxt.LAST_START, OverviewTblTxt.FIRST_END, OverviewTblTxt.LAST_END,
      OverviewTblTxt.MIN_RUNTIME, OverviewTblTxt.AVG_RUNTIME, OverviewTblTxt.MAX_RUNTIME,
      OverviewTblTxt.PERCENT_BUSY,
      OverviewTblTxt.LAST_UPDATE, OverviewTblTxt.LAST_PROGRESS,
      OverviewTblTxt.MAX_PEAK_MEMORY
  };

  public static final String[] ACTIVE_FRAGMENT_OVERVIEW_COLUMNS_TOOLTIP = {
      OverviewTblTooltip.MAJOR_FRAGMENT, OverviewTblTooltip.MINOR_FRAGMENTS_REPORTING,
      OverviewTblTooltip.FIRST_START, OverviewTblTooltip.LAST_START, OverviewTblTooltip.FIRST_END, OverviewTblTooltip.LAST_END,
      OverviewTblTooltip.MIN_RUNTIME, OverviewTblTooltip.AVG_RUNTIME, OverviewTblTooltip.MAX_RUNTIME,
      OverviewTblTooltip.PERCENT_BUSY,
      OverviewTblTooltip.LAST_UPDATE, OverviewTblTooltip.LAST_PROGRESS,
      OverviewTblTooltip.MAX_PEAK_MEMORY
  };

  // Not including Major Fragment ID and Minor Fragments Reporting
  public static final int NUM_NULLABLE_ACTIVE_OVERVIEW_COLUMNS = ACTIVE_FRAGMENT_OVERVIEW_COLUMNS.length - 2;

  public void addSummary(TableBuilder tb) {
    // Use only minor fragments that have complete profiles
    // Complete iff the fragment profile has at least one operator profile, and start and end times.
    final List<MinorFragmentProfile> complete = new ArrayList<>(
        Collections2.filter(major.getMinorFragmentProfileList(), Filters.hasOperatorsAndTimes));

    tb.appendCell(new OperatorPathBuilder().setMajor(major).build());
    tb.appendCell(complete.size() + " / " + major.getMinorFragmentProfileCount());

    // If there are no stats to aggregate, create an empty row
    if (complete.size() < 1) {
      tb.appendRepeated("", NUM_NULLABLE_ACTIVE_OVERVIEW_COLUMNS);
      return;
    }

    final MinorFragmentProfile firstStart = Collections.min(complete, Comparators.startTime);
    final MinorFragmentProfile lastStart = Collections.max(complete, Comparators.startTime);
    tb.appendMillis(firstStart.getStartTime() - start);
    tb.appendMillis(lastStart.getStartTime() - start);

    final MinorFragmentProfile firstEnd = Collections.min(complete, Comparators.endTime);
    final MinorFragmentProfile lastEnd = Collections.max(complete, Comparators.endTime);
    tb.appendMillis(firstEnd.getEndTime() - start);
    tb.appendMillis(lastEnd.getEndTime() - start);

    long cumulativeFragmentDurationInMillis = 0L;
    long cumulativeProcessInNanos = 0L;
    long cumulativeWaitInNanos = 0L;
    for (final MinorFragmentProfile p : complete) {
      cumulativeFragmentDurationInMillis += p.getEndTime() - p.getStartTime();
      //Capture Busy & Wait Time
      List<OperatorProfile> opProfileList = p.getOperatorProfileList();
      for (OperatorProfile operatorProfile : opProfileList) {
        cumulativeProcessInNanos += operatorProfile.getProcessNanos();
        cumulativeWaitInNanos += operatorProfile.getWaitNanos();
      }
    }
    double totalProcessInMillis = Math.round(cumulativeProcessInNanos/1E6);
    double totalWaitInMillis = Math.round(cumulativeWaitInNanos/1E6);

    final MinorFragmentProfile shortRun = Collections.min(complete, Comparators.runTime);
    final MinorFragmentProfile longRun = Collections.max(complete, Comparators.runTime);
    tb.appendMillis(shortRun.getEndTime() - shortRun.getStartTime());
    tb.appendMillis(cumulativeFragmentDurationInMillis / complete.size());
    tb.appendMillis(longRun.getEndTime() - longRun.getStartTime());

    Map<String, String> percBusyAttrMap = new HashMap<>();
    //#8721 is the summation sign: sum(Busy): ## + sum(Wait): ##
    percBusyAttrMap.put(HtmlAttribute.TITLE,
        String.format("&#8721;Busy: %,.2fs + &#8721;Wait: %,.2fs", totalProcessInMillis/1E3, totalWaitInMillis/1E3));
    tb.appendPercent(totalProcessInMillis / (totalProcessInMillis + totalWaitInMillis), percBusyAttrMap);

    final MinorFragmentProfile lastUpdate = Collections.max(complete, Comparators.lastUpdate);
    tb.appendMillis(System.currentTimeMillis()-lastUpdate.getLastUpdate());

    final MinorFragmentProfile lastProgress = Collections.max(complete, Comparators.lastProgress);
    long elapsedSinceLastProgress = System.currentTimeMillis()-lastProgress.getLastProgress();
    Map<String, String> lastProgressAttrMap = null;
    if (elapsedSinceLastProgress > TimeUnit.SECONDS.toMillis(runningProfileProgressThreshold)) {
      lastProgressAttrMap = new HashMap<>();
      lastProgressAttrMap.put(HtmlAttribute.CLASS, HtmlAttribute.CLASS_VALUE_NO_PROGRESS_TAG);
    }
    tb.appendMillis(elapsedSinceLastProgress, lastProgressAttrMap);

    // TODO(DRILL-3494): Names (maxMem, getMaxMemoryUsed) are misleading; the value is peak memory allocated to fragment
    final MinorFragmentProfile maxMem = Collections.max(complete, Comparators.fragmentPeakMemory);
    tb.appendBytes(maxMem.getMaxMemoryUsed());
  }

  public static final String[] COMPLETED_FRAGMENT_OVERVIEW_COLUMNS = {
      OverviewTblTxt.MAJOR_FRAGMENT, OverviewTblTxt.MINOR_FRAGMENTS_REPORTING,
      OverviewTblTxt.FIRST_START, OverviewTblTxt.LAST_START, OverviewTblTxt.FIRST_END, OverviewTblTxt.LAST_END,
      OverviewTblTxt.MIN_RUNTIME, OverviewTblTxt.AVG_RUNTIME, OverviewTblTxt.MAX_RUNTIME,
      OverviewTblTxt.PERCENT_BUSY, OverviewTblTxt.MAX_PEAK_MEMORY
  };

  public static final String[] COMPLETED_FRAGMENT_OVERVIEW_COLUMNS_TOOLTIP = {
      OverviewTblTooltip.MAJOR_FRAGMENT, OverviewTblTooltip.MINOR_FRAGMENTS_REPORTING,
      OverviewTblTooltip.FIRST_START, OverviewTblTooltip.LAST_START, OverviewTblTooltip.FIRST_END, OverviewTblTooltip.LAST_END,
      OverviewTblTooltip.MIN_RUNTIME, OverviewTblTooltip.AVG_RUNTIME, OverviewTblTooltip.MAX_RUNTIME,
      OverviewTblTooltip.PERCENT_BUSY, OverviewTblTooltip.MAX_PEAK_MEMORY
  };

  //Not including Major Fragment ID and Minor Fragments Reporting
  public static final int NUM_NULLABLE_COMPLETED_OVERVIEW_COLUMNS = COMPLETED_FRAGMENT_OVERVIEW_COLUMNS.length - 2;

  public void addFinalSummary(TableBuilder tb) {

    // Use only minor fragments that have complete profiles
    // Complete iff the fragment profile has at least one operator profile, and start and end times.
    final List<MinorFragmentProfile> complete = new ArrayList<>(
        Collections2.filter(major.getMinorFragmentProfileList(), Filters.hasOperatorsAndTimes));

    tb.appendCell(new OperatorPathBuilder().setMajor(major).build());
    tb.appendCell(complete.size() + " / " + major.getMinorFragmentProfileCount());

    // If there are no stats to aggregate, create an empty row
    if (complete.size() < 1) {
      tb.appendRepeated("", NUM_NULLABLE_COMPLETED_OVERVIEW_COLUMNS);
      return;
    }

    final MinorFragmentProfile firstStart = Collections.min(complete, Comparators.startTime);
    final MinorFragmentProfile lastStart = Collections.max(complete, Comparators.startTime);
    tb.appendMillis(firstStart.getStartTime() - start);
    tb.appendMillis(lastStart.getStartTime() - start);

    final MinorFragmentProfile firstEnd = Collections.min(complete, Comparators.endTime);
    final MinorFragmentProfile lastEnd = Collections.max(complete, Comparators.endTime);
    tb.appendMillis(firstEnd.getEndTime() - start);
    tb.appendMillis(lastEnd.getEndTime() - start);

    long totalDuration = 0L;
    double totalProcessInMillis = 0.0d;
    double totalWaitInMillis = 0.0d;
    for (final MinorFragmentProfile p : complete) {
      totalDuration += p.getEndTime() - p.getStartTime();
      //Capture Busy & Wait Time
      List<OperatorProfile> opProfileList = p.getOperatorProfileList();
      for (OperatorProfile operatorProfile : opProfileList) {
        totalProcessInMillis += operatorProfile.getProcessNanos()/1E6;
        totalWaitInMillis += operatorProfile.getWaitNanos()/1E6;
      }
    }

    final MinorFragmentProfile shortRun = Collections.min(complete, Comparators.runTime);
    final MinorFragmentProfile longRun = Collections.max(complete, Comparators.runTime);
    tb.appendMillis(shortRun.getEndTime() - shortRun.getStartTime());
    tb.appendMillis(totalDuration / complete.size());
    tb.appendMillis(longRun.getEndTime() - longRun.getStartTime());

    Map<String, String> percBusyAttrMap = new HashMap<>();
    //#8721 is the summation sign: sum(Busy): ## + sum(Wait): ##
    percBusyAttrMap.put(HtmlAttribute.TITLE,
        String.format("&#8721;Busy: %,.2fs + &#8721;Wait: %,.2fs", totalProcessInMillis/1E3, totalWaitInMillis/1E3));
    tb.appendPercent(totalProcessInMillis / (totalProcessInMillis + totalWaitInMillis), percBusyAttrMap);

    // TODO(DRILL-3494): Names (maxMem, getMaxMemoryUsed) are misleading; the value is peak memory allocated to fragment
    final MinorFragmentProfile maxMem = Collections.max(complete, Comparators.fragmentPeakMemory);
    tb.appendBytes(maxMem.getMaxMemoryUsed());
  }

  public static final String[] FRAGMENT_COLUMNS = {
      FragmentTblTxt.MINOR_FRAGMENT, FragmentTblTxt.HOSTNAME, FragmentTblTxt.START_TIME, FragmentTblTxt.END_TIME,
      FragmentTblTxt.RUNTIME, FragmentTblTxt.MAX_RECORDS, FragmentTblTxt.MAX_BATCHES, FragmentTblTxt.LAST_UPDATE,
      FragmentTblTxt.LAST_PROGRESS, FragmentTblTxt.PEAK_MEMORY, FragmentTblTxt.STATE
  };

  public static final String[] FRAGMENT_COLUMNS_TOOLTIP = {
      FragmentTblTooltip.MINOR_FRAGMENT, FragmentTblTooltip.HOSTNAME, FragmentTblTooltip.START_TIME, FragmentTblTooltip.END_TIME,
      FragmentTblTooltip.RUNTIME, FragmentTblTooltip.MAX_RECORDS, FragmentTblTooltip.MAX_BATCHES, FragmentTblTooltip.LAST_UPDATE,
      FragmentTblTooltip.LAST_PROGRESS, FragmentTblTooltip.PEAK_MEMORY, FragmentTblTooltip.STATE
  };

  // Not including minor fragment ID
  private static final int NUM_NULLABLE_FRAGMENTS_COLUMNS = FRAGMENT_COLUMNS.length - 1;

  public String getContent() {
    final TableBuilder builder = new TableBuilder(FRAGMENT_COLUMNS, FRAGMENT_COLUMNS_TOOLTIP, true);

    // Use only minor fragments that have complete profiles
    // Complete iff the fragment profile has at least one operator profile, and start and end times.
    final List<MinorFragmentProfile> complete = new ArrayList<>(
        Collections2.filter(major.getMinorFragmentProfileList(), Filters.hasOperatorsAndTimes));
    final List<MinorFragmentProfile> incomplete = new ArrayList<>(
        Collections2.filter(major.getMinorFragmentProfileList(), Filters.missingOperatorsOrTimes));

    Collections.sort(complete, Comparators.minorId);

    Map<String, String> attributeMap = new HashMap<>(); //Reusing for different fragments
    for (final MinorFragmentProfile minor : complete) {
      final List<OperatorProfile> ops = new ArrayList<>(minor.getOperatorProfileList());

      long biggestIncomingRecords = 0;
      long biggestBatches = 0;
      for (final OperatorProfile op : ops) {
        long incomingRecords = 0;
        long batches = 0;
        for (final StreamProfile sp : op.getInputProfileList()) {
          incomingRecords += sp.getRecords();
          batches += sp.getBatches();
        }
        biggestIncomingRecords = Math.max(biggestIncomingRecords, incomingRecords);
        biggestBatches = Math.max(biggestBatches, batches);
      }

      attributeMap.put("data-order", String.valueOf(minor.getMinorFragmentId())); //Overwrite values from previous fragments
      builder.appendCell(new OperatorPathBuilder().setMajor(major).setMinor(minor).build(), attributeMap);
      builder.appendCell(minor.getEndpoint().getAddress());
      builder.appendMillis(minor.getStartTime() - start);
      builder.appendMillis(minor.getEndTime() - start);
      builder.appendMillis(minor.getEndTime() - minor.getStartTime());

      builder.appendFormattedInteger(biggestIncomingRecords);
      builder.appendFormattedInteger(biggestBatches);

      builder.appendTime(minor.getLastUpdate());
      builder.appendTime(minor.getLastProgress());

      builder.appendBytes(minor.getMaxMemoryUsed());
      builder.appendCell(minor.getState().name());
    }

    for (final MinorFragmentProfile m : incomplete) {
      builder.appendCell(major.getMajorFragmentId() + "-" + m.getMinorFragmentId());
      builder.appendRepeated(m.getState().toString(), NUM_NULLABLE_FRAGMENTS_COLUMNS);
    }
    return builder.build();
  }

  private class FragmentTblTxt {
    static final String MINOR_FRAGMENT = "Minor Fragment";
    static final String HOSTNAME = "Hostname";
    static final String START_TIME = "Start";
    static final String END_TIME = "End";
    static final String RUNTIME = "Runtime";
    static final String MAX_BATCHES = "Max Batches";
    static final String MAX_RECORDS = "Max Records";
    static final String LAST_UPDATE = "Last Update";
    static final String LAST_PROGRESS = "Last Progress";
    static final String PEAK_MEMORY = "Peak Memory";
    static final String STATE = "State";
  }

  private class FragmentTblTooltip {
    static final String MINOR_FRAGMENT = "Minor Fragment ID";
    static final String HOSTNAME = "Host on which the fragment ran";
    static final String START_TIME = "Fragment Start Time";
    static final String END_TIME = "Fragment End Time";
    static final String RUNTIME = "Duration of the Fragment";
    static final String MAX_BATCHES = "Max Records processed by the fragment";
    static final String MAX_RECORDS = "Max Batches processed by the fragment";
    static final String LAST_UPDATE = "Last time fragment reported heartbeat";
    static final String LAST_PROGRESS = "Last time fragment reported back metrics";
    static final String PEAK_MEMORY = "Fragment Peak Memory Usage";
    static final String STATE = "State of the fragment";
  }

  private class OverviewTblTxt {
    static final String MAJOR_FRAGMENT = "Major Fragment";
    static final String MINOR_FRAGMENTS_REPORTING = "Minor Fragments Reporting";
    static final String FIRST_START = "First Start";
    static final String LAST_START = "Last Start";
    static final String FIRST_END = "First End";
    static final String LAST_END = "Last End";
    static final String MIN_RUNTIME = "Min Runtime";
    static final String AVG_RUNTIME = "Avg Runtime";
    static final String MAX_RUNTIME = "Max Runtime";
    static final String PERCENT_BUSY = "% Busy";
    static final String LAST_UPDATE = "Last Update";
    static final String LAST_PROGRESS = "Last Progress";
    static final String MAX_PEAK_MEMORY = "Max Peak Memory";
  }

  private class OverviewTblTooltip {
    static final String MAJOR_FRAGMENT = "Major fragment ID seen in the visual plan";
    static final String MINOR_FRAGMENTS_REPORTING = "Number of minor fragments started";
    static final String FIRST_START = "Time at which the first fragment started";
    static final String LAST_START = "Time at which the last fragment started";
    static final String FIRST_END = "Time at which the first fragment completed";
    static final String LAST_END = "Time at which the last fragment completed";
    static final String MIN_RUNTIME = "Shortest fragment runtime";
    static final String AVG_RUNTIME = "Average fragment runtime";
    static final String MAX_RUNTIME = "Longest fragment runtime";
    static final String PERCENT_BUSY = "Percentage of run time that fragments were busy doing work";
    static final String LAST_UPDATE = "Time since most recent heartbeat from a fragment";
    static final String LAST_PROGRESS = "Time since most recent update from a fragment";
    static final String MAX_PEAK_MEMORY = "Highest memory consumption by a fragment";
  }
}

