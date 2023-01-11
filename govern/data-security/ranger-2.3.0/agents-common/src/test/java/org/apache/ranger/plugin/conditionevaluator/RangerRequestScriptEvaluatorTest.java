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

package org.apache.ranger.plugin.conditionevaluator;

import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerRequestScriptEvaluator;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.junit.Assert;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangerRequestScriptEvaluatorTest {
    final String              engineName   = "JavaScript";
    final ScriptEngineManager manager      = new ScriptEngineManager();
    final ScriptEngine        scriptEngine = manager.getEngineByName(engineName);

    @Test
    public void testRequestAttributes() {
        RangerAccessRequest          request   = createRequest(Arrays.asList("PII", "PCI"));
        RangerRequestScriptEvaluator evaluator = new RangerRequestScriptEvaluator(request);

        Assert.assertEquals("test: UG_NAMES_CSV", "test-group1,test-group2", evaluator.evaluateScript(scriptEngine, "UG_NAMES_CSV"));
        Assert.assertEquals("test: UR_NAMES_CSV", "test-role1,test-role2", evaluator.evaluateScript(scriptEngine, "UR_NAMES_CSV"));
        Assert.assertEquals("test: TAG_NAMES_CSV", "PCI,PII", evaluator.evaluateScript(scriptEngine, "TAG_NAMES_CSV"));
        Assert.assertEquals("test: USER_ATTR_NAMES_CSV", "state", evaluator.evaluateScript(scriptEngine, "USER_ATTR_NAMES_CSV"));
        Assert.assertEquals("test: UG_ATTR_NAMES_CSV", "dept,site", evaluator.evaluateScript(scriptEngine, "UG_ATTR_NAMES_CSV"));
        Assert.assertEquals("test: TAG_ATTR_NAMES_CSV", "attr1", evaluator.evaluateScript(scriptEngine, "TAG_ATTR_NAMES_CSV"));
        Assert.assertEquals("test: GET_UG_ATTR_CSV('dept')", "ENGG,PROD", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_CSV('dept')"));
        Assert.assertEquals("test: GET_UG_ATTR_CSV('site')", "10,20", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_CSV('site')"));
        Assert.assertEquals("test: GET_TAG_ATTR_CSV('attr1')", "PCI_value,PII_value", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_CSV('attr1')"));

        Assert.assertEquals("test: UG_NAMES_Q_CSV", "'test-group1','test-group2'", evaluator.evaluateScript(scriptEngine, "UG_NAMES_Q_CSV"));
        Assert.assertEquals("test: UR_NAMES_Q_CSV", "'test-role1','test-role2'", evaluator.evaluateScript(scriptEngine, "UR_NAMES_Q_CSV"));
        Assert.assertEquals("test: TAG_NAMES_Q_CSV", "'PCI','PII'", evaluator.evaluateScript(scriptEngine, "TAG_NAMES_Q_CSV"));
        Assert.assertEquals("test: USER_ATTR_NAMES_Q_CSV", "'state'", evaluator.evaluateScript(scriptEngine, "USER_ATTR_NAMES_Q_CSV"));
        Assert.assertEquals("test: UG_ATTR_NAMES_Q_CSV", "'dept','site'", evaluator.evaluateScript(scriptEngine, "UG_ATTR_NAMES_Q_CSV"));
        Assert.assertEquals("test: TAG_ATTR_NAMES_Q_CSV", "'attr1'", evaluator.evaluateScript(scriptEngine, "TAG_ATTR_NAMES_Q_CSV"));
        Assert.assertEquals("test: GET_UG_ATTR_Q_CSV('dept')", "'ENGG','PROD'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q_CSV('dept')"));
        Assert.assertEquals("test: GET_UG_ATTR_Q_CSV('site')", "'10','20'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q_CSV('site')"));
        Assert.assertEquals("test: GET_TAG_ATTR_Q_CSV('attr1')", "'PCI_value','PII_value'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q_CSV('attr1')"));

        Assert.assertTrue("test: USER._name is 'test-user'", (Boolean) evaluator.evaluateScript(scriptEngine, "USER._name == 'test-user'"));
        Assert.assertTrue("test: HAS_USER_ATTR(state)", (Boolean)evaluator.evaluateScript(scriptEngine, "HAS_USER_ATTR('state')"));
        Assert.assertFalse("test: HAS_USER_ATTR(notExists)", (Boolean)evaluator.evaluateScript(scriptEngine, "HAS_USER_ATTR('notExists')"));
        Assert.assertTrue("test: USER['state'] is 'CA'", (Boolean) evaluator.evaluateScript(scriptEngine, "USER['state'] == 'CA'"));
        Assert.assertTrue("test: USER.state is 'CA'", (Boolean) evaluator.evaluateScript(scriptEngine, "USER.state == 'CA'"));

        Assert.assertTrue("test: IS_IN_GROUP(test-group1)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_GROUP('test-group1')"));
        Assert.assertTrue("test: IS_IN_GROUP(test-group2)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_GROUP('test-group2')"));
        Assert.assertFalse("test: IS_IN_GROUP(notExists)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_GROUP('notExists')"));
        Assert.assertTrue("test: IS_IN_ANY_GROUP", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_ANY_GROUP"));
        Assert.assertFalse("test: IS_NOT_IN_ANY_GROUP", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_NOT_IN_ANY_GROUP"));

        Assert.assertTrue("test: UG['test-group1'].dept is 'ENGG'", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group1'].dept == 'ENGG'"));
        Assert.assertTrue("test: UG['test-group1'].site is 10", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group1'].site == 10"));
        Assert.assertTrue("test: UG['test-group2'].dept is 'PROD'", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group2'].dept == 'PROD'"));
        Assert.assertTrue("test: UG['test-group2'].site is 20", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group2'].site == 20"));
        Assert.assertTrue("test: UG['test-group3'] is null", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group3'] == null"));
        Assert.assertTrue("test: UG['test-group1'].notExists is null", (Boolean) evaluator.evaluateScript(scriptEngine, "UG['test-group1'].notExists == null"));

        Assert.assertTrue("test: IS_IN_ROLE(test-role1)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_ROLE('test-role1')"));
        Assert.assertTrue("test: IS_IN_ROLE(test-role2)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_ROLE('test-role2')"));
        Assert.assertFalse("test: IS_IN_ROLE(notExists)", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_ROLE('notExists')"));
        Assert.assertTrue("test: IS_IN_ANY_ROLE", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_IN_ANY_ROLE"));
        Assert.assertFalse("test: IS_NOT_IN_ANY_ROLE", (Boolean)evaluator.evaluateScript(scriptEngine, "IS_NOT_IN_ANY_ROLE"));

        Assert.assertTrue("test: UGA.sVal['dept'] is 'ENGG'", (Boolean)evaluator.evaluateScript(scriptEngine, "UGA.sVal['dept'] == 'ENGG'"));
        Assert.assertTrue("test: UGA.sVal['site'] is 10", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.sVal['site'] == 10"));
        Assert.assertTrue("test: UGA.sVal['notExists'] is null", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.sVal['notExists'] == null"));
        Assert.assertTrue("test: UGA.mVal['dept'] is [\"ENGG\", \"PROD\"]", (Boolean) evaluator.evaluateScript(scriptEngine, "J(UGA.mVal['dept']) == '[\"ENGG\",\"PROD\"]'"));
        Assert.assertTrue("test: UGA.mVal['site'] is [10, 20]", (Boolean) evaluator.evaluateScript(scriptEngine, "J(UGA.mVal['site']) == '[\"10\",\"20\"]'"));
        Assert.assertTrue("test: UGA.mVal['notExists'] is null", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.mVal['notExists'] == null"));
        Assert.assertTrue("test: UGA.mVal['dept'] has 'ENGG'", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.mVal['dept'].indexOf('ENGG') != -1"));
        Assert.assertTrue("test: UGA.mVal['dept'] has 'PROD'", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.mVal['dept'].indexOf('PROD') != -1"));
        Assert.assertTrue("test: UGA.mVal['dept'] doesn't have 'EXEC'", (Boolean) evaluator.evaluateScript(scriptEngine, "UGA.mVal['dept'].indexOf('EXEC') == -1"));
        Assert.assertTrue("test: HAS_UG_ATTR(dept)", (Boolean)evaluator.evaluateScript(scriptEngine, "HAS_UG_ATTR('dept')"));
        Assert.assertTrue("test: HAS_UG_ATTR(site)", (Boolean)evaluator.evaluateScript(scriptEngine, "HAS_UG_ATTR('site')"));
        Assert.assertFalse("test: HAS_UG_ATTR(notExists)", (Boolean)evaluator.evaluateScript(scriptEngine, "HAS_UG_ATTR('notExists')"));

        Assert.assertTrue("test: REQ.accessTyp is 'select'", (Boolean) evaluator.evaluateScript(scriptEngine, "REQ.accessType == 'select'"));
        Assert.assertTrue("test: REQ.action is 'query'", (Boolean) evaluator.evaluateScript(scriptEngine, "REQ.action == 'query'"));

        Assert.assertTrue("test: RES._ownerUser is 'testUser'", (Boolean) evaluator.evaluateScript(scriptEngine, "RES._ownerUser == 'testUser'"));
        Assert.assertTrue("test: RES.database is 'db1'", (Boolean) evaluator.evaluateScript(scriptEngine, "RES.database == 'db1'"));
        Assert.assertTrue("test: RES.table is 'tbl1'", (Boolean) evaluator.evaluateScript(scriptEngine, "RES.table == 'tbl1'"));
        Assert.assertTrue("test: RES.column is 'col1'", (Boolean) evaluator.evaluateScript(scriptEngine, "RES.column == 'col1'"));

        Assert.assertTrue("test: TAG._type is 'PII'", (Boolean) evaluator.evaluateScript(scriptEngine, "TAG._type == 'PII'"));
        Assert.assertTrue("test: TAG.attr1 is 'PII_value'", (Boolean) evaluator.evaluateScript(scriptEngine, "TAG.attr1 == 'PII_value'"));
        Assert.assertTrue("test: TAGS.length is 2", (Boolean) evaluator.evaluateScript(scriptEngine, "Object.keys(TAGS).length == 2"));
        Assert.assertEquals("test: TAG PII has attr1=PII_value", evaluator.evaluateScript(scriptEngine, "TAGS['PII'].attr1"), "PII_value");
        Assert.assertEquals("test: TAG PCI has attr1=PCI_value", evaluator.evaluateScript(scriptEngine, "TAGS['PCI'].attr1"), "PCI_value");
        Assert.assertTrue("test: TAG PII doesn't have PII.notExists", (Boolean) evaluator.evaluateScript(scriptEngine, "TAGS['PII'].notExists == undefined"));
        Assert.assertTrue("test: HAS_TAG_ATTR(attr1)", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_TAG_ATTR('attr1')"));
        Assert.assertFalse("test: HAS_TAG_ATTR(notExists)", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_TAG_ATTR('notExists')"));

        Assert.assertTrue("test: TAGNAMES.length is 2", (Boolean) evaluator.evaluateScript(scriptEngine, "TAGNAMES.length == 2"));
        Assert.assertTrue("test: HAS_TAG(PII)", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_TAG('PII')"));
        Assert.assertTrue("test: HAS_TAG(PCI)", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_TAG('PCI')"));
        Assert.assertFalse("test: HAS_TAG(notExists)", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_TAG('notExists')"));
        Assert.assertTrue("test: HAS_ANY_TAG", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_ANY_TAG"));
        Assert.assertFalse("test: HAS_NO_TAG", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_NO_TAG"));
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