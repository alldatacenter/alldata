/**
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
package org.apache.atlas.discovery;

import org.apache.atlas.model.discovery.AtlasAggregationEntry;
import org.apache.atlas.type.AtlasStructType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is an interface to search aggregation mwntrics providers.
 */
public interface SearchAggregator {
    /**
     * returns aggregation metrics for passed in aggregation fields.
     * @param aggregationFields the set of aggregation attribute names.
     * @param aggregationAttrbutes the set of aggregationAttributes
     * @return  the result of aggreggations by aggregation fields.
     */
    Map<String, List<AtlasAggregationEntry>> getAggregatedMetrics(Set<String> aggregationFields,
                                                                  Set<AtlasStructType.AtlasAttribute> aggregationAttrbutes);
}
