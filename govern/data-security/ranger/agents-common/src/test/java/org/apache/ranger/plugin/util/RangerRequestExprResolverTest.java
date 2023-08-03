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

package org.apache.ranger.plugin.util;

import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangerRequestExprResolverTest {
    @Test
    public void testRequestAttributes() {
        RangerAccessRequest request = createRequest(Arrays.asList("PII", "PCI"));

        Map<String, String> exprValue = new HashMap<>();

        exprValue.put("s3://mybucket/users/${{USER._name}}/${{USER.state}}/test.txt", "s3://mybucket/users/test-user/CA/test.txt");
        exprValue.put("state == '${{USER.state}}' AND dept == '${{UGA.sVal.dept}}'", "state == 'CA' AND dept == 'ENGG'");
        exprValue.put("attr1 == '${{TAG.attr1}}'", "attr1 == 'PII_value'");

        exprValue.put("${{USER._name}}", "test-user");
        exprValue.put("${{USER['state']}}", "CA");
        exprValue.put("${{USER.state}}", "CA");

        exprValue.put("${{UGNAMES.indexOf('test-group1') != -1}}", "true");
        exprValue.put("${{UGNAMES.indexOf('test-group2') != -1}}", "true");
        exprValue.put("${{UGNAMES.indexOf('test-group3') == -1}}", "true");

        exprValue.put("${{UG['test-group1'].dept}}", "ENGG");
        exprValue.put("${{UG['test-group1'].site}}", "10");
        exprValue.put("${{UG['test-group2'].dept}}", "PROD");
        exprValue.put("${{UG['test-group2'].site}}", "20");
        exprValue.put("${{UG['test-group3']}}", "null");
        exprValue.put("${{UG['test-group1'].notExists}}", "null");

        exprValue.put("${{URNAMES.indexOf('test-role1') != -1}}", "true");
        exprValue.put("${{URNAMES.indexOf('test-role2') != -1}}", "true");
        exprValue.put("${{URNAMES.indexOf('test-role3') == -1}}", "true");

        exprValue.put("${{UGA.sVal.dept}}", "ENGG");
        exprValue.put("${{UGA.sVal.site}}", "10");
        exprValue.put("${{UGA.sVal.notExists}}", "null");
        exprValue.put("${{J(UGA.mVal.dept)}}", "[\"ENGG\",\"PROD\"]");
        exprValue.put("${{J(UGA.mVal.site)}}", "[\"10\",\"20\"]");
        exprValue.put("${{J(UGA.mVal.notExists)}}", "null");
        exprValue.put("${{UGA.mVal['dept'].indexOf('ENGG') != -1}}", "true");
        exprValue.put("${{UGA.mVal['dept'].indexOf('PROD') != -1}}", "true");
        exprValue.put("${{UGA.mVal['dept'].indexOf('EXEC') == -1}}", "true");

        exprValue.put("${{REQ.accessType}}", "select");
        exprValue.put("${{REQ.action}}", "query");

        exprValue.put("${{RES._ownerUser}}", "testUser");
        exprValue.put("${{RES.database}}", "db1");
        exprValue.put("${{RES.table}}", "tbl1");
        exprValue.put("${{RES.column}}", "col1");

        exprValue.put("${{TAG._type}}", "PII");
        exprValue.put("${{TAG.attr1}}", "PII_value");
        exprValue.put("${{Object.keys(TAGS).length}}", "2");
        exprValue.put("${{TAGS.PCI._type}}", "PCI");
        exprValue.put("${{TAGS.PCI.attr1}}", "PCI_value");
        exprValue.put("${{TAGS.PII._type}}", "PII");
        exprValue.put("${{TAGS.PII.attr1}}", "PII_value");

        exprValue.put("${{TAGNAMES.length}}", "2");
        exprValue.put("${{TAGNAMES.indexOf('PII') != -1}}", "true");
        exprValue.put("${{TAGNAMES.indexOf('PCI') != -1}}", "true");

        for (Map.Entry<String, String> entry : exprValue.entrySet()) {
            String                    expr        = entry.getKey();
            String                    value       = entry.getValue();
            RangerRequestExprResolver resolver    = new RangerRequestExprResolver(expr, null);
            String                    resolvedVal = resolver.resolveExpressions(request);

            Assert.assertEquals(expr, value, resolvedVal);
        }
    }


    RangerAccessRequest createRequest(List<String> resourceTags) {
        RangerAccessResource resource = mock(RangerAccessResource.class);

        Map<String, Object> resourceMap = new HashMap<>();

        resourceMap.put("database", "db1");
        resourceMap.put("table", "tbl1");
        resourceMap.put("column", "col1");

        when(resource.getAsString()).thenReturn("db1/tbl1/col1");
        when(resource.getOwnerUser()).thenReturn("testUser");
        when(resource.getAsMap()).thenReturn(resourceMap);
        when(resource.getReadOnlyCopy()).thenReturn(resource);

        RangerAccessRequestImpl request = new RangerAccessRequestImpl();

        request.setResource(resource);
        request.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF);
        request.setAccessType("select");
        request.setAction("query");
        request.setUser("test-user");
        request.setUserGroups(new HashSet<>(Arrays.asList("test-group1", "test-group2")));
        request.setUserRoles(new HashSet<>(Arrays.asList("test-role1", "test-role2")));

        RangerAccessRequestUtil.setCurrentResourceInContext(request.getContext(), resource);

        if (resourceTags != null) {
            Set<RangerTagForEval> rangerTagForEvals = new HashSet<>();
            RangerTagForEval      currentTag        = null;

            for (String resourceTag : resourceTags) {
                RangerTag tag        = new RangerTag(UUID.randomUUID().toString(), resourceTag, Collections.singletonMap("attr1", resourceTag + "_value"), null, null, null);
                RangerTagForEval tagForEval = new RangerTagForEval(tag, RangerPolicyResourceMatcher.MatchType.SELF);

                rangerTagForEvals.add(tagForEval);

                if (currentTag == null) {
                    currentTag = tagForEval;
                }
            }

            RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), rangerTagForEvals);
            RangerAccessRequestUtil.setCurrentTagInContext(request.getContext(), currentTag);
        }  else {
            RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), null);
        }

        RangerUserStore userStore = mock(RangerUserStore.class);

        RangerAccessRequestUtil.setRequestUserStoreInContext(request.getContext(), userStore);

        Map<String, Map<String, String>> userAttrMapping  = Collections.singletonMap("test-user", Collections.singletonMap("state", "CA"));
        Map<String, Map<String, String>> groupAttrMapping = new HashMap<>();

        groupAttrMapping.put("test-group1", new HashMap<String, String>() {{ put("dept", "ENGG"); put("site", "10"); }});
        groupAttrMapping.put("test-group2", new HashMap<String, String>() {{ put("dept", "PROD"); put("site", "20"); }});

        when(userStore.getUserAttrMapping()).thenReturn(userAttrMapping);
        when(userStore.getGroupAttrMapping()).thenReturn(groupAttrMapping);

        return request;
    }

}