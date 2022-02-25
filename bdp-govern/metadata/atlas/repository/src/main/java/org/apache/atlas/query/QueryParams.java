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
package org.apache.atlas.query;


import org.apache.atlas.AtlasConfiguration;

public class QueryParams {
    private int limit;
    private int offset;

    public QueryParams() {
        this.limit  = -1;
        this.offset = 0;
    }

    public QueryParams(int limit, int offset) {
        this.limit  = limit;
        this.offset = offset;
    }

    public int limit() {
        return limit;
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public int offset() {
        return offset;
    }

    public void offset(int offset) {
        this.offset = offset;
    }

    public static QueryParams getNormalizedParams(int suppliedLimit, int suppliedOffset) {
        int defaultLimit = AtlasConfiguration.SEARCH_DEFAULT_LIMIT.getInt();
        int maxLimit     = AtlasConfiguration.SEARCH_MAX_LIMIT.getInt();

        int limit = defaultLimit;
        if (suppliedLimit > 0 && suppliedLimit <= maxLimit) {
            limit = suppliedLimit;
        }

        int offset = 0;
        if (suppliedOffset > 0) {
            offset = suppliedOffset;
        }

        return new QueryParams(limit, offset);
    }
}
