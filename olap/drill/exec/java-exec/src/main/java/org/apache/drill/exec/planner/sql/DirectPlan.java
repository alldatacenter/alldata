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
package org.apache.drill.exec.planner.sql;

import org.apache.drill.common.logical.PlanProperties;
import org.apache.drill.common.logical.PlanProperties.Generator.ResultMode;
import org.apache.drill.common.logical.PlanProperties.PlanPropertiesBuilder;
import org.apache.drill.common.logical.PlanProperties.PlanType;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.planner.sql.handlers.DefaultSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.SimpleCommandResult;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.direct.DirectGroupScan;
import org.apache.drill.exec.store.pojo.PojoRecordReader;

import java.util.Collections;
import java.util.List;

public class DirectPlan {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectPlan.class);


  public static PhysicalPlan createDirectPlan(QueryContext context, boolean result, String message){
    return createDirectPlan(context, new SimpleCommandResult(result, message));
  }

  @SuppressWarnings("unchecked")
  public static <T> PhysicalPlan createDirectPlan(QueryContext context, T obj){
    return createDirectPlan(context.getCurrentEndpoint(), Collections.singletonList(obj), (Class<T>) obj.getClass());

  }

  public static <T> PhysicalPlan createDirectPlan(DrillbitEndpoint endpoint, List<T> records, Class<T> clazz){
    PojoRecordReader<T> reader = new PojoRecordReader<>(clazz, records);
    DirectGroupScan scan = new DirectGroupScan(reader);
    Screen screen = new Screen(scan, endpoint);

    PlanPropertiesBuilder propsBuilder = PlanProperties.builder();
    propsBuilder.type(PlanType.APACHE_DRILL_PHYSICAL);
    propsBuilder.version(1);
    propsBuilder.resultMode(ResultMode.EXEC);
    propsBuilder.generator(DirectPlan.class.getSimpleName(), "");
    return new PhysicalPlan(propsBuilder.build(), DefaultSqlHandler.getPops(screen));

  }
}
