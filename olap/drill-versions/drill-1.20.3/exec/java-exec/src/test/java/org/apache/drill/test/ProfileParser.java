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
package org.apache.drill.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.drill.exec.server.rest.profile.CoreOperatorType;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a query profile and provides access to various bits of the profile
 * for diagnostic purposes during tests.
 */

public class ProfileParser {
  private static final Logger logger = LoggerFactory.getLogger(ProfileParser.class);

  /**
   * The original JSON profile.
   */

  JsonObject profile;

  /**
   * Query text parsed out of the profile.
   */

  String query;

  /**
   * List of operator plans in the order in which they appear in the query
   * plan section of the profile. This is an intermediate representation used
   * to create the more fully analyzed structures.
   */

  List<String> plans;

  /**
   * Operations sorted by operator ID. The Operator ID serves as
   * an index into the list to get the information for that operator.
   * Operator ID is the one shown in the plan: xx-nn, where nn is the
   * operator ID. This is NOT the same as the operator type.
   */

  List<OperatorSummary> operations;

  /**
   * Map from major fragment number to fragment information. The major
   * fragment number is the nn in the nn-xx notation in the plan.
   */

  Map<Integer,FragInfo> fragments = new HashMap<>();

  /**
   * Operations in the original topological order as shown in the text
   * version of the query plan in the query profile.
   */
  private List<OperatorSummary> topoOrder;

  public ProfileParser(File file) throws IOException {
    try (FileReader fileReader = new FileReader(file);
         JsonReader reader = Json.createReader(fileReader)) {
      profile = (JsonObject) reader.read();
    }

    parse();
  }

  private void parse() {
    parseQuery();
    parsePlans();
    buildFrags();
    parseFragProfiles();
    mapOpProfiles();
    aggregateOpers();
    buildTree();
  }

  private void parseQuery() {
    query = profile.getString("query");
    query = query.replace("//n", "\n");
  }

  /**
   * Parse a text version of the plan as it appears in the JSON
   * query profile.
   */

  private static class PlanParser {

    List<String> plans = new ArrayList<>();
    List<OperatorSummary> operations = new ArrayList<>();
    List<OperatorSummary> sorted = new ArrayList<>();

    public void parsePlans(String plan) {
      plans = new ArrayList<>();
      String[] parts = plan.split("\n");
      for (String part : parts) {
        plans.add(part);
        OperatorSummary opDef = new OperatorSummary(part);
        operations.add(opDef);
      }
      sortList();
    }

    private void sortList() {
      List<OperatorSummary> raw = new ArrayList<>();
      raw.addAll(operations);
      Collections.sort(raw, new Comparator<OperatorSummary>() {
        @Override
        public int compare(OperatorSummary o1, OperatorSummary o2) {
          int result = Integer.compare(o1.majorId, o2.majorId);
          if (result == 0) {
            result = Integer.compare(o1.stepId, o2.stepId);
          }
          return result;
        }
      });
      int currentFrag = 0;
      int currentStep = 0;
      for (OperatorSummary opDef : raw) {
        if (currentFrag < opDef.majorId) {
          currentFrag++;
          OperatorSummary sender = new OperatorSummary(currentFrag, 0);
          sender.isInferred = true;
          sender.name = "Sender";
          sorted.add(sender);
          currentStep = 1;
          opDef.inferredParent = sender;
          sender.children.add(opDef);
        }
        if (opDef.stepId > currentStep) {
          OperatorSummary unknown = new OperatorSummary(currentFrag, currentStep);
          unknown.isInferred = true;
          unknown.name = "Unknown";
          sorted.add(unknown);
          opDef.inferredParent = unknown;
          unknown.children.add(opDef);
        }
        sorted.add(opDef);
        currentStep = opDef.stepId + 1;
      }
    }
  }

  /**
   * Parse the plan portion of the query profile. Unfortunately,
   * the plan is in text form an is awkward to parse. Also, there is no ID
   * to correlate operators shown in the plan with those referenced in the
   * profile JSON. Inference is needed.
   */

  private void parsePlans() {
    PlanParser parser = new PlanParser();
    String plan = getPlan();
    parser.parsePlans(plan);
    plans = parser.plans;
    topoOrder = parser.operations;
    operations = parser.sorted;
  }

  private void buildFrags() {
    for (OperatorSummary opDef : operations) {
      FragInfo major = fragments.get(opDef.majorId);
      if (major == null) {
        major = new FragInfo(opDef.majorId);
        fragments.put(opDef.majorId, major);
      }
      major.ops.add(opDef);
    }
  }

  private static List<FieldDef> parseCols(String cols) {
    String[] parts = cols.split(", ");
    List<FieldDef> fields = new ArrayList<>();
    for (String part : parts) {
      String[] halves = part.split(" ");
      fields.add(new FieldDef(halves[1], halves[0]));
    }
    return fields;
  }

  private void parseFragProfiles() {
    JsonArray frags = getFragmentProfile();
    for (JsonObject fragProfile : frags.getValuesAs(JsonObject.class)) {
      int mId = fragProfile.getInt("majorFragmentId");
      FragInfo major = fragments.get(mId);
      major.parse(fragProfile);
    }
  }

  private void mapOpProfiles() {
    for (FragInfo major : fragments.values()) {
      for (MinorFragInfo minor : major.minors) {
        minor.mapOpProfiles(major);
      }
    }
  }

  /**
   * A typical plan has many operator details across multiple
   * minor fragments. Aggregate these totals to the "master"
   * definition of each operator.
   */

  private void aggregateOpers() {
    for (FragInfo major : fragments.values()) {
      for (OperatorSummary opDef : major.ops) {
        long sumPeak = 0;
        opDef.execCount = opDef.opExecs.size();
        for (OperatorProfile op : opDef.opExecs) {
          Preconditions.checkState(major.id == op.majorFragId);
          Preconditions.checkState(opDef.stepId == op.opId);
          opDef.actualRows += op.records;
          opDef.actualBatches += op.batches;
          opDef.setupMs += op.setupMs;
          opDef.processMs += op.processMs;
          sumPeak += op.peakMem;
        }
        opDef.actualMemory = sumPeak * 1024 * 1024;
      }
    }
  }

  /**
   * Reconstruct the operator tree from parsed information.
   */

  public void buildTree() {
    int currentLevel = 0;
    OperatorSummary[] opStack = new OperatorSummary[topoOrder.size()];
    for (OperatorSummary opDef : topoOrder) {
      currentLevel = opDef.globalLevel;
      opStack[currentLevel] = opDef;
      if (opDef.inferredParent == null) {
        if (currentLevel > 0) {
          opStack[currentLevel-1].children.add(opDef);
        }
      } else {
        opStack[currentLevel-1].children.add(opDef.inferredParent);
      }
    }
  }


  public String getQuery() {
    return profile.getString("query");
  }

  public String getPlan() {
    return profile.getString("plan");
  }

  public List<String> getPlans() {
    return plans;
  }

  public List<String> getScans() {
    List<String> scans = new ArrayList<>();
    int n = getPlans().size();
    for (int i = n-1; i >= 0;  i--) {
      String plan = plans.get(i);
      if (plan.contains(" Scan(")) {
        scans.add(plan);
      }
    }
    return scans;
  }

  public List<FieldDef> getColumns(String plan) {
    Pattern p = Pattern.compile("RecordType\\((.*)\\):");
    Matcher m = p.matcher(plan);
    if (! m.find()) { return null; }
    String frag = m.group(1);
    String[] parts = frag.split(", ");
    List<FieldDef> fields = new ArrayList<>();
    for (String part : parts) {
      String[] halves = part.split(" ");
      fields.add(new FieldDef(halves[1], halves[0]));
    }
    return fields;
  }

  public Map<Integer,String> getOperators() {
    Map<Integer,String> ops = new HashMap<>();
    int n = getPlans().size();
    Pattern p = Pattern.compile("\\d+-(\\d+)\\s+(\\w+)");
    for (int i = n-1; i >= 0;  i--) {
      String plan = plans.get(i);
      Matcher m = p.matcher(plan);
      if (! m.find()) { continue; }
      int index = Integer.parseInt(m.group(1));
      String op = m.group(2);
      ops.put(index,op);
    }
    return ops;
  }

  public JsonArray getFragmentProfile() {
    return profile.getJsonArray("fragmentProfile");
  }

  /**
   * Information for a fragment, including the operators
   * in that fragment and the set of minor fragments.
   */

  public static class FragInfo {
    public int baseLevel;
    public int id;
    public List<OperatorSummary> ops = new ArrayList<>();
    public List<MinorFragInfo> minors = new ArrayList<>();

    public FragInfo(int majorId) {
      this.id = majorId;
    }

    public OperatorSummary getRootOperator() {
      return ops.get(0);
    }

    public void parse(JsonObject fragProfile) {
      JsonArray minorList = fragProfile.getJsonArray("minorFragmentProfile");
      for (JsonObject minorProfile : minorList.getValuesAs(JsonObject.class)) {
        minors.add(new MinorFragInfo(id, minorProfile));
      }
    }
  }

  /**
   * Information about a minor fragment as parsed from the profile.
   */

  public static class MinorFragInfo {
    public final int majorId;
    public final int id;
    public final List<OperatorProfile> ops = new ArrayList<>();

    public MinorFragInfo(int majorId, JsonObject minorProfile) {
      this.majorId = majorId;
      id = minorProfile.getInt("minorFragmentId");
      JsonArray opList = minorProfile.getJsonArray("operatorProfile");
      for (JsonObject opProfile : opList.getValuesAs(JsonObject.class)) {
        ops.add(new OperatorProfile(majorId, id, opProfile));
      }
    }

    /**
     * Map each operator execution profiles back to the definition of that
     * operator. The only common key is the xx-yy value where xx is the fragment
     * number and yy is the operator ID.
     *
     * @param major major fragment that corresponds to the xx portion of the
     * operator id
     */

    public void mapOpProfiles(FragInfo major) {
      for (OperatorProfile op : ops) {
        OperatorSummary opDef = major.ops.get(op.opId);
        if (opDef == null) {
          logger.info("Can't find operator def: {}-{}", major.id, op.opId);
          continue;
        }
        op.opName = op.type.replace("_", " ");
        op.name = opDef.name;
        if (op.name.equalsIgnoreCase(op.opName)) {
          op.opName = null;
        }
        op.defn = opDef;
        opDef.opName = op.opName;
        opDef.type = op.type;
        opDef.opExecs.add(op);
      }
    }
  }

  /**
   * Detailed information about each operator within a minor fragment
   * for a major fragment. Gathers the detailed information from
   * the profile.
   */

  public static class OperatorProfile {
    public OperatorSummary defn;
    public String opName;
    public int majorFragId;
    public int minorFragId;
    public int opId;
    public String type;
    public String name;
    public long processMs;
    public long waitMs;
    public long setupMs;
    public long peakMem;
    public Map<Integer,JsonNumber> metrics = new HashMap<>();
    public long records;
    public int batches;
    public int schemas;

    public OperatorProfile(int majorId, int minorId, JsonObject opProfile) {
      majorFragId = majorId;
      minorFragId = minorId;
      opId = opProfile.getInt("operatorId");
      JsonValue.ValueType valueType = opProfile.get("operatorType").getValueType();
      type = valueType == JsonValue.ValueType.STRING
          ? opProfile.getString("operatorType")
          : Objects.requireNonNull(CoreOperatorType.valueOf(opProfile.getInt("operatorType"))).name();
      processMs = opProfile.getJsonNumber("processNanos").longValue() / 1_000_000;
      waitMs = opProfile.getJsonNumber("waitNanos").longValue() / 1_000_000;
      setupMs = opProfile.getJsonNumber("setupNanos").longValue() / 1_000_000;
      peakMem = opProfile.getJsonNumber("peakLocalMemoryAllocated").longValue() / (1024 * 1024);
      JsonArray array = opProfile.getJsonArray("inputProfile");
      if (array != null) {
        for (int i = 0; i < array.size(); i++) {
          JsonObject obj = array.getJsonObject(i);
          records += obj.getJsonNumber("records").longValue();
          batches += obj.getInt("batches");
          schemas += obj.getInt("schemas");
        }
      }
      array = opProfile.getJsonArray("metric");
      if (array != null) {
        for (int i = 0; i < array.size(); i++) {
          JsonObject metric = array.getJsonObject(i);
          metrics.put(metric.getJsonNumber("metricId").intValue(), metric.getJsonNumber("longValue"));
        }
      }
    }

    public long getMetric(int id) {
      JsonNumber value = metrics.get(id);
      if (value == null) {
        return 0;
      }
      return value.longValue();
    }

    @Override
    public String toString() {
      return String.format("[OperatorProfile %02d-%02d-%02d, type: %s, name: %s]",
          majorFragId, opId, minorFragId, type,
          (name == null) ? "null" : name);
    }
  }

  /**
   * Information about an operator definition: the plan-time information
   * that appears in the plan portion of the profile. Also holds the
   * "actuals" from the minor fragment portion of the profile.
   * Allows integrating the "planned" vs. "actual" performance of the
   * query.
   * <p>
   * There is one operator definition (represented here), each of which may
   * give rise to multiple operator executions (housed in minor fragments.)
   * The {@link #opExecs} field provides the list of operator executions
   * (which provides access to operator metrics.)
   */

  public static class OperatorSummary {
    public String type;
    public long processMs;
    public long setupMs;
    public int execCount;
    public String opName;
    public boolean isInferred;
    public int majorId;
    public int stepId;
    public String args;
    public List<FieldDef> columns;
    public int globalLevel;
    public int localLevel;
    public int id;
    public int branchId;
    public boolean isBranchRoot;
    public double estMemoryCost;
    public double estNetCost;
    public double estIOCost;
    public double estCpuCost;
    public double estRowCost;
    public double estRows;
    public String name;
    public long actualMemory;
    public int actualBatches;
    public long actualRows;
    public OperatorSummary inferredParent;
    public List<OperatorProfile> opExecs = new ArrayList<>();
    public List<OperatorSummary> children = new ArrayList<>();

    // 00-00    Screen : rowType = RecordType(VARCHAR(10) Year, VARCHAR(65536) Month, VARCHAR(100) Devices, VARCHAR(100) Tier, VARCHAR(100) LOB, CHAR(10) Gateway, BIGINT Day, BIGINT Hour, INTEGER Week, VARCHAR(100) Week_end_date, BIGINT Usage_Cnt): \
    // rowcount = 100.0, cumulative cost = {7.42124276972414E9 rows, 7.663067406383167E10 cpu, 0.0 io, 2.24645048816E10 network, 2.692766612982188E8 memory}, id = 129302
    //
    // 00-01      Project(Year=[$0], Month=[$1], Devices=[$2], Tier=[$3], LOB=[$4], Gateway=[$5], Day=[$6], Hour=[$7], Week=[$8], Week_end_date=[$9], Usage_Cnt=[$10]) :
    // rowType = RecordType(VARCHAR(10) Year, VARCHAR(65536) Month, VARCHAR(100) Devices, VARCHAR(100) Tier, VARCHAR(100) LOB, CHAR(10) Gateway, BIGINT Day, BIGINT Hour, INTEGER Week, VARCHAR(100) Week_end_date, BIGINT Usage_Cnt): rowcount = 100.0, cumulative cost = {7.42124275972414E9 rows, 7.663067405383167E10 cpu, 0.0 io, 2.24645048816E10 network, 2.692766612982188E8 memory}, id = 129301

    public OperatorSummary(String plan) {
      Pattern p = Pattern.compile("^(\\d+)-(\\d+)(\\s+)(\\w+)(?:\\((.*)\\))?\\s*:\\s*(.*)$");
      Matcher m = p.matcher(plan);
      if (!m.matches()) {
        throw new IllegalStateException("Could not parse plan: " + plan);
      }
      majorId = Integer.parseInt(m.group(1));
      stepId = Integer.parseInt(m.group(2));
      name = m.group(4);
      args = m.group(5);
      String tail = m.group(6);
      String indent = m.group(3);
      globalLevel = (indent.length() - 4) / 2;

      p = Pattern.compile("rowType = RecordType\\((.*)\\): (rowcount .*)");
      m = p.matcher(tail);
      if (m.matches()) {
        columns = parseCols(m.group(1));
        tail = m.group(2);
      }

      p = Pattern.compile("rowcount = ([\\d.E]+), cumulative cost = \\{([\\d.E]+) rows, ([\\d.E]+) cpu, ([\\d.E]+) io, ([\\d.E]+) network, ([\\d.E]+) memory\\}, id = (\\d+)");
      m = p.matcher(tail);
      if (! m.find()) {
        throw new IllegalStateException("Could not parse costs: " + tail);
      }
      estRows = Double.parseDouble(m.group(1));
      estRowCost = Double.parseDouble(m.group(2));
      estCpuCost = Double.parseDouble(m.group(3));
      estIOCost = Double.parseDouble(m.group(4));
      estNetCost = Double.parseDouble(m.group(5));
      estMemoryCost = Double.parseDouble(m.group(6));
      id = Integer.parseInt(m.group(7));
    }

    public void printTree(String indent) {
      new TreePrinter().visit(this);
    }

    public OperatorSummary(int major, int id) {
      majorId = major;
      stepId = id;
    }

    @Override
    public String toString() {
      String head = "[OpDefInfo " + majorId + "-" + stepId + ": " + name;
      if (isInferred) {
        head += " (" + opName + ")";
      }
      return head + "]";
    }
  }

  /**
   * Visit a tree of operator definitions to support printing,
   * analysis and other tasks.
   */

  public static class TreeVisitor
  {
    public void visit(OperatorSummary root) {
      visit(root, 0);
    }

    public void visit(OperatorSummary node, int indent) {
      visitOp(node, indent);
      if (node.children.isEmpty()) {
        return;
      }
      if (node.children.size() == 1) {
        visit(node.children.get(0), indent);
        return;
      }
      indent++;
      int i = 0;
      for (OperatorSummary child : node.children) {
        visitSubtree(node, i++, indent);
        visit(child, indent+1);
      }
    }

    protected void visitOp(OperatorSummary node, int indent) {
    }

    protected void visitSubtree(OperatorSummary node, int i, int indent) {
    }

    public String indentString(int indent, String pad) {
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < indent; i++) {
        buf.append(pad);
      }
      return buf.toString();
    }

    public String indentString(int indent) {
      return indentString(indent, "  ");
    }

    public String subtreeLabel(OperatorSummary node, int branch) {
      if (node.name.equals("HashJoin")) {
        return (branch == 0) ? "Probe" : "Build";
      } else {
        return "Input " + (branch + 1);
      }
    }
  }

  /**
   * Print the operator tree for analysis.
   */

  public static class TreePrinter extends TreeVisitor
  {
    @Override
    protected void visitOp(OperatorSummary node, int indent) {
      logger.info("{}{}", indentString(indent), node.toString());
    }

    @Override
    protected void visitSubtree(OperatorSummary node, int i, int indent) {
      logger.info("{}{}", indentString(indent), subtreeLabel(node, i));
    }
  }

  /**
   * Print out the tree showing a comparison of estimated vs.
   * actual costs. Example:
   * <p><pre>
   * 03-05 HashJoin (HASH JOIN)
   *                                 Estimate:       2,521,812 rows,      1 MB
   *                                 Actual:           116,480 rows,     52 MB
   *         Probe
   * 03-07 . . Project
   *                                 Estimate:       2,521,812 rows,      1 MB
   *                                 Actual:                 0 rows,      0 MB
   * </pre>
   */

  public static class CostPrinter extends TreeVisitor
  {
    @Override
    protected void visitOp(OperatorSummary node, int indentLevel) {
      final StringBuilder nodeBuilder = new StringBuilder();
      nodeBuilder.append(String.format("%02d-%02d ", node.majorId, node.stepId));
      String indent = indentString(indentLevel, ". ");
      nodeBuilder.append(indent).append(node.name);
      if (node.opName != null) {
        nodeBuilder.append(" (").append(node.opName).append(")");
      }
      logger.info(nodeBuilder.toString());

      final StringBuilder costBuilder = new StringBuilder();
      indent = indentString(15);
      costBuilder.append(indent);
      costBuilder.append(String.format("  Estimate: %,15.0f rows, %,7.0f MB",
                         node.estRows, node.estMemoryCost / 1024 / 1024));
      costBuilder.append(indent);
      costBuilder.append(String.format("  Actual:   %,15d rows, %,7d MB",
                         node.actualRows, node.actualMemory / 1024 / 1024));
      logger.info(nodeBuilder.toString());
    }

    @Override
    protected void visitSubtree(OperatorSummary node, int i, int indent) {
      logger.info("{}      {}", indentString(indent), subtreeLabel(node, i));
    }
  }

  public static class FindOpVisitor extends TreeVisitor
  {
    private List<OperatorSummary> ops;
    private String type;

    public List<OperatorSummary> find(String type, OperatorSummary node) {
      ops = new ArrayList<>();
      this.type = type;
      visit(node);
      return ops;
    }

    @Override
    protected void visitOp(OperatorSummary node, int indentLevel) {
      if (type.equals(node.type)) {
        ops.add(node);
      }
    }
  }

  /**
   * We often run test queries single threaded to make analysis of the profile
   * easier. For a single-threaded (single slice) query, get a map from
   * operator ID to operator information as preparation for additional
   * analysis.
   *
   * @return
   */

  public Map<Integer,OperatorProfile> getOpInfo() {
    Map<Integer,String> ops = getOperators();
    Map<Integer,OperatorProfile> info = new HashMap<>();
    JsonArray frags = getFragmentProfile();
    JsonObject fragProfile = frags.getJsonObject(0).getJsonArray("minorFragmentProfile").getJsonObject(0);
    JsonArray opList = fragProfile.getJsonArray("operatorProfile");
    for (JsonObject opProfile : opList.getValuesAs(JsonObject.class)) {
      parseOpProfile(ops, info, opProfile);
    }
    return info;
  }

  /**
   * For a single-slice query, get all operators of a given numeric operator
   * type.
   * @param type the operator type
   * @return a list of operators of the given type
   */

  public List<OperatorProfile> getOpsOfType(String type) {
    List<OperatorProfile> ops = new ArrayList<>();
    List<OperatorSummary> opDefs = getOpDefsOfType(type);
    for (OperatorSummary opDef : opDefs) {
      ops.addAll(opDef.opExecs);
    }
    return ops;
  }

  public List<OperatorSummary> getOpDefsOfType(String type) {
    return new FindOpVisitor().find(type, topoOrder.get(0));
  }

  private void parseOpProfile(Map<Integer, String> ops,
      Map<Integer, OperatorProfile> info, JsonObject opProfile) {
    OperatorProfile opInfo = new OperatorProfile(0, 0, opProfile);
    opInfo.name = ops.get(opInfo.opId);
    info.put(opInfo.opId, opInfo);
  }

  public void printPlan() {
    new CostPrinter().visit(topoOrder.get(0));
  }

  public void printTime() {
    new TimePrinter().visit(topoOrder.get(0));
  }

  public static class Aggregator extends TreeVisitor
  {
    protected int n;
    protected long totalSetup;
    protected long totalProcess;
    protected long total;
    protected int maxFrag;
    protected boolean isTree;

    @Override
    public void visit(OperatorSummary root) {
      super.visit(root, 0);
      total = totalSetup + totalProcess;
    }

    @Override
    protected void visitOp(OperatorSummary node, int indentLevel) {
      n++;
      totalSetup += node.setupMs;
      totalProcess += node.processMs;
      maxFrag = Math.max(maxFrag, node.majorId);
      isTree |= (node.children.size() > 1);
    }
  }

  public static class TimePrinter extends TreeVisitor
  {
    private Aggregator totals;
    private boolean singleThread;
    private boolean singleFragment;

    @Override
    public void visit(OperatorSummary root) {
      totals = new Aggregator();
      totals.visit(root);
      singleThread = ! totals.isTree;
      singleFragment = (totals.maxFrag == 0);
      super.visit(root, 0);
      logger.info("Total:");
      String indent = singleThread? "  " : indentString(15);
      logger.info("{}{}", indent, String.format("Setup:   %,6d ms", totals.totalSetup));
      logger.info("{}{}", indent, String.format("Process: %,6d ms", totals.totalProcess));
    }

    @Override
    protected void visitOp(OperatorSummary node, int indentLevel) {
      if (singleThread) {
        printSimpleFormat(node);
      } else {
        printTreeFormat(node, indentLevel);
      }
    }

    private void printSimpleFormat(OperatorSummary node) {
      final StringBuilder sb = new StringBuilder();

      if (singleFragment) {
        sb.append(String.format("%02d ", node.stepId));
      } else {
        sb.append(String.format("%02d-%02d ", node.majorId, node.stepId));
      }
      sb.append(node.name);
      if (node.opName != null) {
        sb.append(" (").append(node.opName).append(")");
      }
      logger.info(sb.toString());
      printTimes(node, "  ");
    }

    private void printTimes(OperatorSummary node, String indent) {
      logger.info("{}{}", indent, String.format("Setup:   %,6d ms - %3d%%, %3d%%", node.setupMs,
                         percent(node.setupMs, totals.totalSetup),
                         percent(node.setupMs, totals.total)));
      logger.info("{}{}", indent, String.format("Process: %,6d ms - %3d%%, %3d%%", node.processMs,
                         percent(node.processMs, totals.totalProcess),
                         percent(node.processMs, totals.total)));
    }

    private void printTreeFormat(OperatorSummary node, int indentLevel) {
      final StringBuilder sb = new StringBuilder();
      sb.append(String.format("%02d-%02d ", node.majorId, node.stepId));
      String indent = indentString(indentLevel, ". ");
      sb.append(indent).append(node.name);
      if (node.opName != null) {
        sb.append(" (").append(node.opName).append(")");
      }
      logger.info(sb.toString());
      indent = indentString(15);
      printTimes(node, indent);
    }
  }

  /**
   * For a single-slice query, print a summary of the operator stack
   * and costs. At present, works for a linear query with on single-input
   * operators.
   */

  public void print() {
    printTime();
  }

  public void simplePrint() {
    Map<Integer, OperatorProfile> opInfo = getOpInfo();
    int n = opInfo.size();
    long totalSetup = 0;
    long totalProcess = 0;
    for (int i = 0;  i <= n;  i++) {
      OperatorProfile op = opInfo.get(i);
      if (op == null) { continue; }
      totalSetup += op.setupMs;
      totalProcess += op.processMs;
    }
    long total = totalSetup + totalProcess;
    for (int i = 0;  i <= n;  i++) {
      OperatorProfile op = opInfo.get(i);
      if (op == null) { continue; }
      logger.info("Op: {} {}", op.opId, op.name);
      logger.info("Setup:   {} - {}%, {}%", op.setupMs, percent(op.setupMs, totalSetup), percent(op.setupMs, total));
      logger.info("Process: {} - {}%, {}%", op.processMs, percent(op.processMs, totalProcess), percent(op.processMs, total));
      if (op.type.equals("EXTERNAL_SORT")) {
        long value = op.getMetric(0);
        logger.info("  Spills: {}", value);
      }
      if (op.waitMs > 0) {
        logger.info("  Wait:    {}", op.waitMs);
      }
      if (op.peakMem > 0) {
        logger.info("  Memory: {}", op.peakMem);
      }
    }
    logger.info("Total:");
    logger.info("  Setup:   {}", totalSetup);
    logger.info("  Process: {}", totalProcess);
  }

  public static long percent(long value, long total) {
    if (total == 0) {
      return 0; }
    return Math.round(value * 100 / (double)total);
  }

  public List<OperatorSummary> getOpDefn(String target) {
    List<OperatorSummary> ops = new ArrayList<>();
    for (OperatorSummary opDef : operations) {
      if (opDef.name.startsWith(target)) {
        ops.add(opDef);
      }
    }
    return ops;
  }
}
