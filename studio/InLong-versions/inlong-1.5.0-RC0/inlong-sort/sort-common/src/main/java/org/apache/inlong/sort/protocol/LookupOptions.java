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

package org.apache.inlong.sort.protocol;

import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nullable;

/**
 * Lookup options
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LookupOptions.class, name = "lookupOptions")
})
@Data
public class LookupOptions {

    /**
     * Lookup cache max rows
     */
    @Nullable
    @JsonProperty("lookupCacheMaxRows")
    private Long lookupCacheMaxRows;
    /**
     * Lookup cache ttl
     */
    @Nullable
    @JsonProperty("lookupCacheTtl")
    private Long lookupCacheTtl;
    /**
     * Lookup max retries
     */
    @Nullable
    @JsonProperty("lookupMaxRetries")
    private Integer lookupMaxRetries;
    /**
     * Lookup async
     */
    @Nullable
    @JsonProperty("lookupAsync")
    private Boolean lookupAsync;

    @JsonCreator
    public LookupOptions(@JsonProperty("lookupCacheMaxRows") @Nullable Long lookupCacheMaxRows,
            @JsonProperty("lookupCacheTtl") @Nullable Long lookupCacheTtl,
            @JsonProperty("lookupMaxRetries") @Nullable Integer lookupMaxRetries,
            @JsonProperty("lookupAsync") @Nullable Boolean lookupAsync) {
        this.lookupCacheMaxRows = lookupCacheMaxRows;
        this.lookupCacheTtl = lookupCacheTtl;
        this.lookupMaxRetries = lookupMaxRetries;
        this.lookupAsync = lookupAsync;
    }
}
