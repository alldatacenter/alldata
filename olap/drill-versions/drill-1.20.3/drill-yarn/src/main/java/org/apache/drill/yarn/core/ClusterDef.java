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
package org.apache.drill.yarn.core;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.apache.drill.yarn.appMaster.TaskSpec;
import org.mortbay.log.Log;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;

public class ClusterDef {
  // The following keys are relative to the cluster group definition

  public static final String GROUP_NAME = "name";
  public static final String GROUP_TYPE = "type";
  public static final String GROUP_SIZE = "count";

  // For the labeled pool

  public static final String DRILLBIT_LABEL = "drillbit-label-expr";
  public static final String AM_LABEL = "am-label-expr";

  /**
   * Defined cluster tier types. The value of the type appears as the value of
   * the {@link $CLUSTER_TYPE} parameter in the config file.
   */

  public enum GroupType {
    BASIC,
    LABELED;

    public static GroupType toEnum(String value) {
      return GroupType.valueOf( value.toUpperCase() );
    }

    public String toValue() {
      return name().toLowerCase();
    }
  }

  public static class ClusterGroup {
    private final String name;
    private final int count;
    private final GroupType type;

    public ClusterGroup( Map<String, Object> group, int index, GroupType type ) {
      this.type = type;

      // Config system has already parsed the value. We insist that the value,
      // when parsed, was interpreted as an integer. That is, the value had
      // to be, say 10. Not "10", not 10.0, but just a plain integer.

      try {
        count = (Integer) group.get(GROUP_SIZE);
      } catch (ClassCastException e) {
        throw new IllegalArgumentException(
            "Expected an integer for " + GROUP_SIZE + " for tier " + index);
      }
      Object nameValue = group.get(GROUP_NAME);
      String theName = null;
      if (nameValue != null) {
        theName = nameValue.toString();
      }
      if (DoYUtil.isBlank(theName)) {
        theName = "tier-" + Integer.toString(index);
      }
      name = theName;
    }


    public String getName( ) { return name; }
    public int getCount( ) { return count; }
    public GroupType getType( ) { return type; }

    public void getPairs(int index, List<NameValuePair> pairs) {
      String key = DrillOnYarnConfig.append(DrillOnYarnConfig.CLUSTERS,
          Integer.toString(index));
      addPairs(pairs, key);
    }

    protected void addPairs(List<NameValuePair> pairs, String key) {
      pairs.add(
          new NameValuePair(DrillOnYarnConfig.append(key, GROUP_NAME), name));
      pairs.add(
          new NameValuePair(DrillOnYarnConfig.append(key, GROUP_TYPE), type));
      pairs.add(
          new NameValuePair(DrillOnYarnConfig.append(key, GROUP_SIZE), count));
    }

    public void dump(String prefix, PrintStream out) {
      out.print(prefix);
      out.print("name = ");
      out.println(name);
      out.print(prefix);
      out.print("type = ");
      out.println(type.toValue());
      out.print(prefix);
      out.print("count = ");
      out.println(count);
    }

    public void modifyTaskSpec(TaskSpec taskSpec) {
    }
  }

  public static class BasicGroup extends ClusterGroup {

    public BasicGroup(Map<String, Object> pool, int index) {
      super(pool, index, GroupType.BASIC);
    }

  }

  public static class LabeledGroup extends ClusterGroup {

    private final String drillbitLabelExpr;

    public LabeledGroup(Map<String, Object> pool, int index) {
      super(pool, index, GroupType.LABELED);
      drillbitLabelExpr = (String) pool.get(DRILLBIT_LABEL);
      if (drillbitLabelExpr == null) {
        Log.warn("Labeled pool is missing the drillbit label expression ("
            + DRILLBIT_LABEL + "), will treat pool as basic.");
      }
    }

    public String getLabelExpr( ) { return drillbitLabelExpr; }

    @Override
    public void dump(String prefix, PrintStream out) {
      out.print(prefix);
      out.print("Drillbit label expr = ");
      out.println((drillbitLabelExpr == null) ? "<none>" : drillbitLabelExpr);
    }

    @Override
    protected void addPairs(List<NameValuePair> pairs, String key) {
      super.addPairs(pairs, key);
      pairs.add(new NameValuePair(DrillOnYarnConfig.append(key, DRILLBIT_LABEL),
          drillbitLabelExpr));
    }

    @Override
    public void modifyTaskSpec(TaskSpec taskSpec) {
      taskSpec.containerSpec.nodeLabelExpr = drillbitLabelExpr;
    }
  }

  /**
   * Deserialize a node tier from the configuration file.
   *
   * @param n
   * @return
   */

  public static ClusterGroup getCluster(Config config, int n) {
    int index = n + 1;
    ConfigList tiers = config.getList(DrillOnYarnConfig.CLUSTERS);
    ConfigValue value = tiers.get(n);
    if ( value == null ) {
      throw new IllegalArgumentException( "If cluster group is provided, it cannot be null: group " + index );
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> tier = (Map<String, Object>) value.unwrapped();
    String type;
    try {
      type = tier.get(GROUP_TYPE).toString();
    } catch (NullPointerException e) {
      throw new IllegalArgumentException(
          "Pool type is required for cluster group " + index);
    }
    GroupType groupType = GroupType.toEnum(type);
    if (groupType == null) {
      throw new IllegalArgumentException(
          "Undefined type for cluster group " + index + ": " + type);
    }
    ClusterGroup tierDef;
    switch (groupType) {
    case BASIC:
      tierDef = new BasicGroup( tier, index );
      break;
    case LABELED:
      tierDef = new LabeledGroup( tier, index );
      break;
    default:
      assert false;
      throw new IllegalStateException(
          "Undefined cluster group type: " + groupType);
    }
    return tierDef;
  }
}
