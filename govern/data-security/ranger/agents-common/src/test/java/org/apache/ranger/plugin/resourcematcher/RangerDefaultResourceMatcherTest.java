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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.resourcematcher;

import com.google.common.collect.Lists;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RangerDefaultResourceMatcherTest {

    Object[][] data = {
            // { resource, policy, excludes, result
            { "*",  "*",  false, true, "user" },  // resource is all values
            { "*",  "*",  true,  false, "user" },
            { "*",  "a*", false, false, "user" }, // but, policy is not match any
            { "*",  "a*", true,  false, "user" }, // ==> compare with above: exclude flag has no effect here
            { "a*", "a",  false, false, "user" }, // resource has regex marker!
            { "a*", "a",  true,  true, "user" },
            { "a",  "a",  false, true, "user" },  // exact match
            { "a",  "a",  true,  false, "user" },
            { "a1", "a*", false, true, "user" },  // trivial regex match
            { "a1", "a*", true,  false, "user" },
    };

    @Test
    public void testIsMatch() throws Exception {
        for (Object[] row : data) {
            String resource = (String)row[0];
            String policyValue = (String)row[1];
            boolean excludes = (boolean)row[2];
            boolean result = (boolean)row[3];
            String user = (String) row[4];

            Map<String, Object> evalContext = new HashMap<>();
            RangerAccessRequestUtil.setCurrentUserInContext(evalContext, user);

            MatcherWrapper matcher = new MatcherWrapper(policyValue, excludes);
            assertEquals(getMessage(row), result, matcher.isMatch(resource, evalContext));
        }
    }

    String getMessage(Object[] row) {
        return String.format("Resource=%s, Policy=%s, excludes=%s, result=%s",
                (String)row[0], (String)row[1], (boolean)row[2], (boolean)row[3]);
    }

    static class MatcherWrapper extends RangerDefaultResourceMatcher {
        MatcherWrapper(String policyValue, boolean exclude) {
            RangerResourceDef   resourceDef    = new RangerResourceDef();
            Map<String, String> matcherOptions = new HashMap<>();

            matcherOptions.put(OPTION_WILD_CARD, Boolean.toString(policyValue.contains(WILDCARD_ASTERISK)));
            matcherOptions.put(OPTION_IGNORE_CASE, Boolean.toString(false));

            resourceDef.setMatcherOptions(matcherOptions);

            setResourceDef(resourceDef);

            RangerPolicy.RangerPolicyResource policyResource = new RangerPolicy.RangerPolicyResource();
            policyResource.setIsExcludes(exclude);
            policyResource.setValues(Lists.newArrayList(policyValue));
            setPolicyResource(policyResource);


            init();
        }
    }

}