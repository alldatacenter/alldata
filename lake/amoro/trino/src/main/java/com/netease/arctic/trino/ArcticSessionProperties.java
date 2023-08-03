
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino;

import com.google.common.collect.ImmutableList;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.iceberg.IcebergSessionProperties;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.session.PropertyMetadata;

import javax.inject.Inject;
import java.util.List;

import static io.trino.spi.session.PropertyMetadata.booleanProperty;
import static io.trino.spi.session.PropertyMetadata.doubleProperty;

/**
 * Arctic supporting session properties
 */
public final class ArcticSessionProperties
    implements SessionPropertiesProvider {

  private static final String ARCTIC_STATISTICS_ENABLED = "arctic_table_statistics_enabled";

  private static final String ARCTIC_SPLIT_TASK_BY_DELETE_RATIO = "arctic_split_task_by_delete_ratio";
  private static final String ARCTIC_ENABLE_SPLIT_TASK_BY_DELETE_RATIO = "arctic_enable_split_task_by_delete_ratio";
  private final List<PropertyMetadata<?>> sessionProperties;

  @Inject
  public ArcticSessionProperties(
      ArcticConfig arcticConfig,
      IcebergSessionProperties icebergSessionProperties) {
    sessionProperties = ImmutableList.<PropertyMetadata<?>>builder()
        .addAll(icebergSessionProperties.getSessionProperties())
        .add(booleanProperty(
            ARCTIC_STATISTICS_ENABLED,
            "Expose table statistics for Arctic table",
            arcticConfig.isTableStatisticsEnabled(),
            false))
        .add(doubleProperty(
            ARCTIC_SPLIT_TASK_BY_DELETE_RATIO,
            "If task delete ratio less than this value will be split to more task",
            arcticConfig.getSplitTaskByDeleteRatio(),
            false
        ))
        .add(booleanProperty(
          ARCTIC_ENABLE_SPLIT_TASK_BY_DELETE_RATIO,
            "Enable task split by ratio",
            arcticConfig.isEnableSplitTaskByDeleteRatio(),
            false
        ))
        .build();
  }

  @Override
  public List<PropertyMetadata<?>> getSessionProperties() {
    return sessionProperties;
  }

  public static boolean isArcticStatisticsEnabled(ConnectorSession session) {
    return session.getProperty(ARCTIC_STATISTICS_ENABLED, Boolean.class);
  }

  public static boolean enableSplitTaskByDeleteRatio(ConnectorSession session) {
    return session.getProperty(ARCTIC_ENABLE_SPLIT_TASK_BY_DELETE_RATIO, Boolean.class);
  }

  public static double splitTaskByDeleteRatio(ConnectorSession session) {
    return session.getProperty(ARCTIC_SPLIT_TASK_BY_DELETE_RATIO, Double.class);
  }
}
