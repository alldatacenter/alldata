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
package org.apache.drill.exec.pop;

import java.io.IOException;
import java.util.Properties;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.Fragment.ExchangeFragmentPair;
import org.apache.drill.exec.planner.fragment.MakeFragmentsVisitor;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.junit.BeforeClass;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

public abstract class PopUnitTestBase  extends ExecTest{
//  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PopUnitTestBase.class);

  protected static DrillConfig CONFIG;

  @BeforeClass
  public static void setup() {
    Properties props = new Properties();

    // Properties here mimic those in drill-root/pom.xml, Surefire plugin
    // configuration. They allow tests to run successfully in Eclipse.

    props.put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, "false");
    props.put(ExecConstants.HTTP_ENABLE, "false");
    props.put(Drillbit.SYSTEM_OPTIONS_NAME, "org.apache.drill.exec.compile.ClassTransformer.scalar_replacement=on");
    props.put("drill.catastrophic_to_standard_out", "true");
    CONFIG = DrillConfig.create(props);
  }


  public static int getFragmentCount(Fragment b) {
    int i = 1;
    for (ExchangeFragmentPair p : b) {
      i += getFragmentCount(p.getNode());
    }
    return i;
  }

  public static Fragment getRootFragment(PhysicalPlanReader reader, String file) throws FragmentSetupException,
      IOException, ForemanSetupException {
    return getRootFragmentFromPlanString(reader, Files.asCharSource(DrillFileUtils.getResourceAsFile(file), Charsets.UTF_8).read());
  }


  public static Fragment getRootFragmentFromPlanString(PhysicalPlanReader reader, String planString)
      throws FragmentSetupException, IOException, ForemanSetupException {
    PhysicalPlan plan = reader.readPhysicalPlan(planString);
    PhysicalOperator o = plan.getSortedOperators(false).iterator().next();
    return o.accept(MakeFragmentsVisitor.INSTANCE, null);
  }
}
