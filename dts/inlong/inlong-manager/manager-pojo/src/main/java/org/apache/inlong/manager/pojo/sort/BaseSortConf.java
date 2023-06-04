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

package org.apache.inlong.manager.pojo.sort;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Sort configuration for inlong group.
 */
@Data
@ApiModel("Sort configuration for inlong group")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "sortType")
public abstract class BaseSortConf {

    public abstract SortType getType();

    public static final String SORT_FLINK = "flink";
    public static final String SORT_LOCAL = "local";
    public static final String SORT_USER_DEFINED = "user_defined";

    public enum SortType {

        FLINK(SORT_FLINK),
        LOCAL(SORT_LOCAL),
        USER_DEFINED(SORT_USER_DEFINED);

        private final String type;

        SortType(String type) {
            this.type = type;
        }

        public static SortType forType(String type) {
            for (SortType sortType : values()) {
                if (sortType.getType().equals(type)) {
                    return sortType;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported sortType=%s", type));
        }

        public String getType() {
            return this.type;
        }

    }
}
