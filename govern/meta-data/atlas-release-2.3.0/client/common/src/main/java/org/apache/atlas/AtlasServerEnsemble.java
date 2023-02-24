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

package org.apache.atlas;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import java.util.Arrays;

import java.util.List;

public class AtlasServerEnsemble {

    private final String[] urls;

    public AtlasServerEnsemble(String[] baseUrls) {
        Preconditions.checkArgument((baseUrls!=null && baseUrls.length>0),
                "List of baseURLs cannot be null or empty.");
        for (String baseUrl : baseUrls) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(baseUrl),
                    "Base URL cannot be null or empty.");
        }
        urls = baseUrls;
    }

    public boolean hasSingleInstance() {
        return urls.length==1;
    }

    public String firstURL() {
        return urls[0];
    }

    public List<String> getMembers() {
        return Arrays.asList(urls);
    }
}
