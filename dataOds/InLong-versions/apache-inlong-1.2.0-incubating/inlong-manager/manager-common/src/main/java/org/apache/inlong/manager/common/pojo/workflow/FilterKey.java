/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.pojo.workflow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.inlong.manager.common.util.InlongCollectionUtils;

/**
 * Approvers filter key of workflow
 */
public enum FilterKey {

    DEFAULT;

    private static final List<FilterKey> FILTER_KEY_ORDER = ImmutableList.of(DEFAULT);
    private static final Map<String, FilterKey> NAME_MAP = InlongCollectionUtils.transformToImmutableMap(
            Lists.newArrayList(FilterKey.values()),
            FilterKey::name,
            Function.identity()
    );

    /**
     * Filter order-from small to large range
     *
     * @return orderly filtering KEY
     */
    public static List<FilterKey> getFilterKeyByOrder() {
        return FILTER_KEY_ORDER;
    }

    public static FilterKey fromName(String name) {
        return NAME_MAP.get(name);
    }
}
