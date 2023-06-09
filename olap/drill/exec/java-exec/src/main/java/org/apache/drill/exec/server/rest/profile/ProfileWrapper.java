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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.MajorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.rest.WebServer;
import org.apache.drill.exec.server.rest.WebUtils;
import org.apache.drill.shaded.guava.com.google.common.base.CaseFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;

/**
 * Wrapper class for a {@link #profile query profile}, so it to be presented through web UI.
 */
public class ProfileWrapper {
  private static final String ESTIMATED_LABEL = " (Estimated)";
  private static final String NOT_AVAILABLE_LABEL = "Not Available";
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProfileWrapper.class);
  private static final ObjectMapper mapper = new ObjectMapper().enable(INDENT_OUTPUT);

  private final QueryProfile profile;
  private final String id;
  private final List<FragmentWrapper> fragmentProfiles;
  private final List<OperatorWrapper> operatorProfiles;
  private final Map<String, Long> majorFragmentTallyMap;
  private final long majorFragmentTallyTotal;
  private final OptionList options;
  private final boolean onlyImpersonationEnabled;
  private Map<String, String> physicalOperatorMap;
  private final String noProgressWarningThreshold;
  private final int defaultAutoLimit;
  private final boolean showEstimatedRows;
  private final String csrfToken;

  public ProfileWrapper(final QueryProfile profile, DrillConfig drillConfig, HttpServletRequest request) {
    this.profile = profile;
    this.id = profile.hasQueryId() ? profile.getQueryId() : QueryIdHelper.getQueryId(profile.getId());
    this.defaultAutoLimit = drillConfig.getInt(ExecConstants.HTTP_WEB_CLIENT_RESULTSET_AUTOLIMIT_ROWS);
    //Generating Operator Name map (DRILL-6140)
    String profileTextPlan = profile.hasPlan()? profile.getPlan(): "";
    generateOpMap(profileTextPlan);
    csrfToken = WebUtils.getCsrfTokenFromHttpRequest(request);

    final List<FragmentWrapper> fragmentProfiles = new ArrayList<>();

    final List<MajorFragmentProfile> majors = new ArrayList<>(profile.getFragmentProfileList());
    Collections.sort(majors, Comparators.majorId);

    for (final MajorFragmentProfile major : majors) {
      fragmentProfiles.add(new FragmentWrapper(major, profile.getStart(), drillConfig));
    }
    this.fragmentProfiles = fragmentProfiles;
    this.majorFragmentTallyMap = new HashMap<>(majors.size());
    this.majorFragmentTallyTotal = tallyMajorFragmentCost(majors);

    final List<OperatorWrapper> ows = new ArrayList<>();
    // temporary map to store (major_id, operator_id) -> [((op_profile, minor_id),minorFragHostname)]
    final Map<ImmutablePair<Integer, Integer>, List<ImmutablePair<ImmutablePair<OperatorProfile, Integer>, String>>> opmap = new HashMap<>();

    Collections.sort(majors, Comparators.majorId);
    for (final MajorFragmentProfile major : majors) {

      final List<MinorFragmentProfile> minors = new ArrayList<>(major.getMinorFragmentProfileList());
      Collections.sort(minors, Comparators.minorId);
      for (final MinorFragmentProfile minor : minors) {
        String fragmentHostName = minor.getEndpoint().getAddress();
        final List<OperatorProfile> ops = new ArrayList<>(minor.getOperatorProfileList());
        Collections.sort(ops, Comparators.operatorId);

        for (final OperatorProfile op : ops) {

          final ImmutablePair<Integer, Integer> ip = new ImmutablePair<>(
              major.getMajorFragmentId(), op.getOperatorId());
          if (!opmap.containsKey(ip)) {
            final List<ImmutablePair<ImmutablePair<OperatorProfile, Integer>, String>> l = new ArrayList<>();
            opmap.put(ip, l);
          }
          opmap.get(ip).add(new ImmutablePair<>(
              new ImmutablePair<>(op, minor.getMinorFragmentId()),
              fragmentHostName));
        }
      }
    }

    final List<ImmutablePair<Integer, Integer>> keys = new ArrayList<>(opmap.keySet());
    Collections.sort(keys);

    for (final ImmutablePair<Integer, Integer> ip : keys) {
      ows.add(new OperatorWrapper(ip.getLeft(), opmap.get(ip), physicalOperatorMap, drillConfig));
    }
    this.operatorProfiles = ows;

    OptionList options;
    try {
      options = mapper.readValue(profile.getOptionsJson(), OptionList.class);
    } catch (Exception e) {
      logger.error("Unable to deserialize query options", e);
      options = new OptionList();
    }
    this.options = options;

    this.onlyImpersonationEnabled = WebServer.isOnlyImpersonationEnabled(drillConfig);
    this.noProgressWarningThreshold = String.valueOf(drillConfig.getInt(ExecConstants.PROFILE_WARNING_PROGRESS_THRESHOLD));
    this.showEstimatedRows = drillConfig.getBoolean(ExecConstants.PROFILE_STATISTICS_ESTIMATED_ROWS_SHOW);
  }

  private long tallyMajorFragmentCost(List<MajorFragmentProfile> majorFragments) {
    long globalProcessNanos = 0L;
    for (MajorFragmentProfile majorFP : majorFragments) {
      String majorFragmentId = new OperatorPathBuilder().setMajor(majorFP).build();
      long processNanos = 0L;
      for (MinorFragmentProfile minorFP : majorFP.getMinorFragmentProfileList()) {
        for (OperatorProfile op : minorFP.getOperatorProfileList()) {
          processNanos += op.getProcessNanos();
        }
      }
      majorFragmentTallyMap.put(majorFragmentId, processNanos);
      globalProcessNanos += processNanos;
    }
    return globalProcessNanos;
  }

  public boolean hasAutoLimit() {
    return profile.hasAutoLimit();
  }

  public int getAutoLimit() {
    return profile.getAutoLimit();
  }

  public int getDefaultAutoLimit() {
    return defaultAutoLimit;
  }

  public boolean hasError() {
    return profile.hasError() && profile.getError() != null;
  }

  public QueryProfile getProfile() {
    return profile;
  }

  public String getProfileDuration() {
    return (new SimpleDurationFormat(profile.getStart(), profile.getEnd())).verbose();
  }

  public String getQueryId() {
    return id;
  }

  public String getQueryStateDisplayName() {
    return ProfileUtil.getQueryStateDisplayName(profile.getState());
  }

  public String getPlanningDuration() {
    //Check if Planning End is known
    if (profile.getPlanEnd() > 0L) {
      return (new SimpleDurationFormat(profile.getStart(), profile.getPlanEnd())).verbose();
    }

    //Check if any fragments have started
    if (profile.getFragmentProfileCount() > 0) {
      //Init Planning End Time
      long estimatedPlanEnd = Long.MAX_VALUE;
      //Using Screen MajorFragment as reference
      MajorFragmentProfile majorFrag0 = profile.getFragmentProfile(0);
      //Searching for earliest starting fragment
      for (MinorFragmentProfile fragmentWrapper : majorFrag0.getMinorFragmentProfileList()) {
        long minorFragmentStart = fragmentWrapper.getStartTime();
        if (minorFragmentStart > 0 && minorFragmentStart < estimatedPlanEnd) {
          estimatedPlanEnd = minorFragmentStart;
        }
      }
      //Provide estimated plan time
      return (new SimpleDurationFormat(profile.getStart(), estimatedPlanEnd)).verbose() + ESTIMATED_LABEL;
    }

    //Unable to  estimate/calculate Specific Time spent in Planning
    return NOT_AVAILABLE_LABEL;
  }

  public String getQueuedDuration() {
    //Check if State is ENQUEUED
    if (profile.getState() == QueryState.ENQUEUED) {
      return (new SimpleDurationFormat(profile.getPlanEnd(), System.currentTimeMillis())).verbose();
    }

    //Check if Queue Wait End is known
    if (profile.getQueueWaitEnd() > 0L) {
      return (new SimpleDurationFormat(profile.getPlanEnd(), profile.getQueueWaitEnd())).verbose();
    }

    //Unable to  estimate/calculate Specific Time spent in Queue
    return NOT_AVAILABLE_LABEL;
  }

  public String getExecutionDuration() {
    //Check if State is PREPARING, PLANNING, STARTING or ENQUEUED
    if (profile.getState() == QueryState.PREPARING ||
        profile.getState() == QueryState.PLANNING ||
        profile.getState() == QueryState.STARTING ||
        profile.getState() == QueryState.ENQUEUED) {
      return NOT_AVAILABLE_LABEL;
    }

    long queryEndTime;

    // Check if State is RUNNING, set end time to current time
    if (profile.getState() == QueryState.RUNNING) {
      queryEndTime = System.currentTimeMillis();
    } else {
       queryEndTime = profile.getEnd();
    }

    //Check if QueueEnd is known
    if (profile.getQueueWaitEnd() > 0L) {
      //Execution time [end(QueueWait) - endTime(Query)]
      return (new SimpleDurationFormat(profile.getQueueWaitEnd(), queryEndTime)).verbose();
    }

    //Check if Plan End is known
    if (profile.getPlanEnd() > 0L) {
      //Execution time [end(Planning) - endTime(Query)]
      return (new SimpleDurationFormat(profile.getPlanEnd(), queryEndTime)).verbose();
    }

    //Check if any fragments have started
    if (profile.getFragmentProfileCount() > 0) {
      //Providing Invalid Planning End Time (Will update later)
      long estimatedPlanEnd = Long.MAX_VALUE;
      //Using Screen MajorFragment as reference
      MajorFragmentProfile majorFrag0 = profile.getFragmentProfile(0);
      //Searching for earliest starting fragment
      for (MinorFragmentProfile fragmentWrapper : majorFrag0.getMinorFragmentProfileList()) {
        long minorFragmentStart = fragmentWrapper.getStartTime();
        if (minorFragmentStart > 0 && minorFragmentStart < estimatedPlanEnd) {
          estimatedPlanEnd = minorFragmentStart;
        }
      }
      //Execution time [start(rootFragment) - endTime(Query)]
      return (new SimpleDurationFormat(estimatedPlanEnd, queryEndTime)).verbose() + ESTIMATED_LABEL;
    }

    //Unable to  estimate/calculate Specific Execution Time
    return NOT_AVAILABLE_LABEL;
  }

  //Threshold to be used by WebServer in issuing warning

  public String getNoProgressWarningThreshold() {
    return this.noProgressWarningThreshold;
  }
  public List<FragmentWrapper> getFragmentProfiles() {
    return fragmentProfiles;
  }

  public String getFragmentsOverview() {
    TableBuilder tb;
    if (profile.getState() == QueryState.STARTING
        || profile.getState() == QueryState.RUNNING) {
      tb = new TableBuilder(FragmentWrapper.ACTIVE_FRAGMENT_OVERVIEW_COLUMNS, FragmentWrapper.ACTIVE_FRAGMENT_OVERVIEW_COLUMNS_TOOLTIP);
      for (final FragmentWrapper fw : fragmentProfiles) {
        fw.addSummary(tb);
      }
    } else {
      tb = new TableBuilder(FragmentWrapper.COMPLETED_FRAGMENT_OVERVIEW_COLUMNS, FragmentWrapper.COMPLETED_FRAGMENT_OVERVIEW_COLUMNS_TOOLTIP);
      for (final FragmentWrapper fw : fragmentProfiles) {
        fw.addFinalSummary(tb);
      }
    }
    return tb.build();
  }

  public List<OperatorWrapper> getOperatorProfiles() {
    return operatorProfiles;
  }

  public String getOperatorsOverview() {
    final TableBuilder tb = new TableBuilder(OperatorWrapper.OPERATORS_OVERVIEW_COLUMNS,
        OperatorWrapper.OPERATORS_OVERVIEW_COLUMNS_TOOLTIP);

    for (final OperatorWrapper ow : operatorProfiles) {
      ow.addSummary(tb, this.majorFragmentTallyMap, this.majorFragmentTallyTotal);
    }
    return tb.build();
  }

  public Map<String, String> getOptions() {
    return getOptions(o -> true);
  }

  public Map<String, String> getSessionOptions() {
    return getOptions(o -> OptionValue.OptionScope.SESSION == o.getScope());
  }

  public Map<String, String> getQueryOptions() {
    return getOptions(o -> OptionValue.OptionScope.QUERY == o.getScope());
  }

  /**
   * Generates sorted map with properties used to display on Web UI,
   * where key is property name and value is property string value.
   * Options are filtered based on {@link OptionValue.OptionScope}.
   * <p/>
   * When property value is null, it would be replaced with 'null',
   * this is achieved using {@link String#valueOf(Object)} method.
   * Options will be stored in ascending key order, sorted according
   * to the natural order for the option name represented by {@link String}.
   *
   * @param filter filter based on {@link OptionValue.OptionScope}
   * @return map with properties names and string values
   */
  private Map<String, String> getOptions(Predicate<OptionValue> filter) {
    return options.stream()
      .filter(filter)
      .collect(Collectors.toMap(
        OptionValue::getName,
        o -> String.valueOf(o.getValue()),
        (o, n) -> n,
        TreeMap::new));
  }

  /**
   * @return true if impersonation is enabled without authentication,
   *         is needed to indicated if user name should be included when re-running the query
   */
  public boolean isOnlyImpersonationEnabled() {
    return onlyImpersonationEnabled;
  }

  //Generates operator names inferred from physical plan

  private void generateOpMap(String plan) {
    this.physicalOperatorMap = new HashMap<>();
    if (plan.isEmpty()) {
      return;
    }
    //[e.g ] operatorLine = "01-03 Flatten(flattenField=[$1]) : rowType = RecordType(ANY rfsSpecCode, ..."
    String[] operatorLine = plan.split("\\n");
    for (String line : operatorLine) {
      String[] lineToken = line.split("\\s+", 3);
      if (lineToken.length < 2) {
        continue; //Skip due to possible invalid entry
      }
      //[e.g ] operatorPath = "01-xx-03"
      String operatorPath = lineToken[0].trim().replaceFirst("-", "-xx-"); //Required format for lookup
      //[e.g ] extractedOperatorName = "FLATTEN"
      String extractedOperatorName = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, lineToken[1].split("\\(", 2)[0].trim());
      physicalOperatorMap.put(operatorPath, extractedOperatorName);
    }
  }
  public boolean showEstimatedRows() {
    return showEstimatedRows;
  }

  public String getCsrfToken() {
    return csrfToken;
  }
}
