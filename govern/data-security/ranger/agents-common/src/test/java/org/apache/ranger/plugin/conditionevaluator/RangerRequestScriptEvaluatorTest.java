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
import org.apache.ranger.plugin.util.ScriptEngineUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.script.ScriptEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangerRequestScriptEvaluatorTest {
    final ScriptEngine scriptEngine = ScriptEngineUtil.createScriptEngine(null);

    @Test
    public void testRequestAttributes() {
        RangerTag                    tagPII    = new RangerTag("PII", Collections.singletonMap("attr1", "PII_value"));
        RangerTag                    tagPCI    = new RangerTag("PCI", Collections.singletonMap("attr1", "PCI_value"));
        RangerAccessRequest          request   = createRequest("test-user", new HashSet<>(Arrays.asList("test-group1", "test-group2")), new HashSet<>(Arrays.asList("test-role1", "test-role2")), Arrays.asList(tagPII, tagPCI));
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

        Assert.assertEquals("GET_TAG_NAMES()",           "PCI,PII", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES()"));
        Assert.assertEquals("GET_TAG_NAMES(null)",       "PCI,PII", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES(null)"));
        Assert.assertEquals("GET_TAG_NAMES(null, '|')",  "PCI|PII", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES(null, '|')"));
        Assert.assertEquals("GET_TAG_NAMES(null, null)", "PCIPII", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES(null, null)"));

        Assert.assertEquals("GET_TAG_NAMES_Q()",                    "'PCI','PII'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q()"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null)",                "'PCI','PII'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null)"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null, '|')",           "'PCI'|'PII'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null, null)",          "'PCI''PII'",  evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null, '|', null)",     "PCI|PII",     evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null, '|', null)"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null, ',', '{', '}')", "{PCI},{PII}", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_TAG_ATTR_NAMES()",           "attr1", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES()"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES(null)",       "attr1", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES(null)"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES(null, '|',)", "attr1", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES(null, '|')"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES(null, null)", "attr1", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES(null, null)"));

        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q()",                    "'attr1'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q()"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q(null)",                "'attr1'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q(null)"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q(null, '|')",           "'attr1'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q(null, null)",          "'attr1'",   evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q(null, '|', null)",     "attr1",   evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q(null, '|', null)"));
        Assert.assertEquals("GET_TAG_ATTR_NAMES_Q(null, ',', '{', '}')", "{attr1}", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_TAG_ATTR('attr1')",            "PCI_value,PII_value", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('attr1')"));
        Assert.assertEquals("GET_TAG_ATTR('attr1', null)",      "PCI_value,PII_value", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('attr1', null)"));
        Assert.assertEquals("GET_TAG_ATTR('attr1', null, '|')", "PCI_value|PII_value", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('attr1', null, '|')"));
        Assert.assertEquals("GET_TAG_ATTR('attr1', null, null)", "PCI_valuePII_value", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('attr1', null, null)"));

        Assert.assertEquals("GET_TAG_ATTR_Q('attr1')",                      "'PCI_value','PII_value'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1')"));
        Assert.assertEquals("GET_TAG_ATTR_Q('attr1', null)",                "'PCI_value','PII_value'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1', null)"));
        Assert.assertEquals("GET_TAG_ATTR_Q('attr1', null, null)",          "'PCI_value''PII_value'",  evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1', null, null)"));
        Assert.assertEquals("GET_TAG_ATTR_Q('attr1', null, '|')",           "'PCI_value'|'PII_value'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1', null, '|')"));
        Assert.assertEquals("GET_TAG_ATTR_Q('attr1', null, ',', null)",     "PCI_value,PII_value",     evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1', null, ',', null)"));
        Assert.assertEquals("GET_TAG_ATTR_Q('attr1', null, ',', '{', '}')", "{PCI_value},{PII_value}", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('attr1', null, ',', '{', '}')"));

        Assert.assertEquals("GET_UG_NAMES()",           "test-group1,test-group2", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES()"));
        Assert.assertEquals("GET_UG_NAMES(null)",       "test-group1,test-group2", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES(null)"));
        Assert.assertEquals("GET_UG_NAMES(null, '|')",  "test-group1|test-group2", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES(null, '|')"));
        Assert.assertEquals("GET_UG_NAMES(null, null)", "test-group1test-group2",  evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES(null, null)"));

        Assert.assertEquals("GET_UG_NAMES_Q()",                    "'test-group1','test-group2'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q()"));
        Assert.assertEquals("GET_UG_NAMES_Q(null)",                "'test-group1','test-group2'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null)"));
        Assert.assertEquals("GET_UG_NAMES_Q(null, null)",          "'test-group1''test-group2'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_UG_NAMES_Q(null, '|')",           "'test-group1'|'test-group2'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_UG_NAMES_Q(null, ',', null)",     "test-group1,test-group2",     evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null, ',', null)"));
        Assert.assertEquals("GET_UG_NAMES_Q(null, ',', '{', '}')", "{test-group1},{test-group2}", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_UG_ATTR_NAMES()",           "dept,site", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES()"));
        Assert.assertEquals("GET_UG_ATTR_NAMES(null)",       "dept,site", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES(null)"));
        Assert.assertEquals("GET_UG_ATTR_NAMES(null, '|')",  "dept|site", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES(null, '|')"));
        Assert.assertEquals("GET_UG_ATTR_NAMES(null, null)", "deptsite",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES(null, null)"));

        Assert.assertEquals("GET_UG_ATTR_NAMES_Q()",                    "'dept','site'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q()"));
        Assert.assertEquals("GET_UG_ATTR_NAMES_Q(null)",                "'dept','site'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q(null)"));
        Assert.assertEquals("GET_UG_ATTR_NAMES_Q(null, null)",          "'dept''site'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_UG_ATTR_NAMES_Q(null, '|')",           "'dept'|'site'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_UG_ATTR_NAMES_Q(null, ',', null)",     "dept,site",     evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q(null, ',', null)"));
        Assert.assertEquals("GET_UG_ATTR_NAMES_Q(null, ',', '{', '}')", "{dept},{site}", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_UG_ATTR('dept')",             "ENGG,PROD", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('dept')"));
        Assert.assertEquals("GET_UG_ATTR('dept', null)",       "ENGG,PROD", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('dept', null)"));
        Assert.assertEquals("GET_UG_ATTR('dept', null, '|')",  "ENGG|PROD", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('dept', null, '|')"));
        Assert.assertEquals("GET_UG_ATTR('dept', null, null)", "ENGGPROD",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('dept', null, null)"));

        Assert.assertEquals("GET_UG_ATTR_Q('dept')",                      "'ENGG','PROD'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept')"));
        Assert.assertEquals("GET_UG_ATTR_Q('dept', null)",                "'ENGG','PROD'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('dept', null, null)",          "'ENGG''PROD'",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept', null, null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('dept', null, '|')",           "'ENGG'|'PROD'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept', null, '|')"));
        Assert.assertEquals("GET_UG_ATTR_Q('dept', null, ',', null)",     "ENGG,PROD",     evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept', null, ',', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('dept', null, ',', '{', '}')", "{ENGG},{PROD}", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('dept', null, ',', '{', '}')"));

        Assert.assertEquals("GET_UG_ATTR('site')",             "10,20", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('site')"));
        Assert.assertEquals("GET_UG_ATTR('site', null)",       "10,20", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('site', null)"));
        Assert.assertEquals("GET_UG_ATTR('site', null, '|')",  "10|20", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('site', null, '|')"));
        Assert.assertEquals("GET_UG_ATTR('site', null, null)", "1020",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('site', null, null)"));

        Assert.assertEquals("GET_UG_ATTR_Q('site')",                      "'10','20'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site')"));
        Assert.assertEquals("GET_UG_ATTR_Q('site', null)",                "'10','20'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('site', null, null)",          "'10''20'",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site', null, null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('site', null, '|')",           "'10'|'20'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site', null, '|')"));
        Assert.assertEquals("GET_UG_ATTR_Q('site', null, ',', null)",     "10,20",     evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site', null, ',', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('site', null, ',', '{', '}')", "{10},{20}", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('site', null, ',', '{', '}')"));

        Assert.assertEquals("GET_UR_NAMES()",           "test-role1,test-role2", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES()"));
        Assert.assertEquals("GET_UR_NAMES(null)",       "test-role1,test-role2", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES(null)"));
        Assert.assertEquals("GET_UR_NAMES(null, '|')",  "test-role1|test-role2", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES(null, '|')"));
        Assert.assertEquals("GET_UR_NAMES(null, null)", "test-role1test-role2",  evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES(null, null)"));

        Assert.assertEquals("GET_UR_NAMES_Q()",                    "'test-role1','test-role2'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q()"));
        Assert.assertEquals("GET_UR_NAMES_Q(null)",                "'test-role1','test-role2'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null)"));
        Assert.assertEquals("GET_UR_NAMES_Q(null, null)",          "'test-role1''test-role2'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_UR_NAMES_Q(null, '|')",           "'test-role1'|'test-role2'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_UR_NAMES_Q(null, ',', null)",     "test-role1,test-role2",     evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null, ',', null)"));
        Assert.assertEquals("GET_UR_NAMES_Q(null, ',', '{', '}')", "{test-role1},{test-role2}", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_USER_ATTR_NAMES()",           "state", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES()"));
        Assert.assertEquals("GET_USER_ATTR_NAMES(null)",       "state", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES(null)"));
        Assert.assertEquals("GET_USER_ATTR_NAMES(null, '|')",  "state", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES(null, '|')"));
        Assert.assertEquals("GET_USER_ATTR_NAMES(null, null)", "state",  evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES(null, null)"));

        Assert.assertEquals("GET_USER_ATTR_NAMES_Q()",                    "'state'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q()"));
        Assert.assertEquals("GET_USER_ATTR_NAMES_Q(null)",                "'state'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q(null)"));
        Assert.assertEquals("GET_USER_ATTR_NAMES_Q(null, null)",          "'state'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q(null, null)"));
        Assert.assertEquals("GET_USER_ATTR_NAMES_Q(null, '|')",           "'state'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q(null, '|')"));
        Assert.assertEquals("GET_USER_ATTR_NAMES_Q(null, ',', null)",     "state",   evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q(null, ',', null)"));
        Assert.assertEquals("GET_USER_ATTR_NAMES_Q(null, ',', '{', '}')", "{state}", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_NAMES_Q(null, ',', '{', '}')"));

        Assert.assertEquals("GET_USER_ATTR('state')",             "CA", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('state')"));
        Assert.assertEquals("GET_USER_ATTR('state', null)",       "CA", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('state', null)"));
        Assert.assertEquals("GET_USER_ATTR('state', null, '|')",  "CA", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('state', null, '|')"));
        Assert.assertEquals("GET_USER_ATTR('state', null, null)", "CA",  evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('state', null, null)"));

        Assert.assertEquals("GET_USER_ATTR_Q('state')",                      "'CA'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state')"));
        Assert.assertEquals("GET_USER_ATTR_Q('state', null)",                "'CA'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state', null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('state', null, null)",          "'CA'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state', null, null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('state', null, '|')",           "'CA'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state', null, '|')"));
        Assert.assertEquals("GET_USER_ATTR_Q('state', null, ',', null)",     "CA",   evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state', null, ',', null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('state', null, ',', '{', '}')", "{CA}", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('state', null, ',', '{', '}')"));
    }

    @Test
    public void testNonExistentValues() {
        RangerAccessRequest          request   = createRequest("test-user", Collections.emptySet(), Collections.emptySet(), Collections.emptyList());
        RangerRequestScriptEvaluator evaluator = new RangerRequestScriptEvaluator(request);

        // empty TAG names
        Assert.assertEquals("GET_TAG_NAMES()",              "",      evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES()"));
        Assert.assertEquals("GET_TAG_NAMES(null)",          "",      evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES(null)"));
        Assert.assertEquals("GET_TAG_NAMES('empty')",       "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES('empty')"));
        Assert.assertEquals("GET_TAG_NAMES('empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES('empty', '|')"));
        Assert.assertEquals("GET_TAG_NAMES('empty', null)", "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES('empty', null)"));

        // empty TAG names
        Assert.assertEquals("GET_TAG_NAMES_Q()",                       "",        evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q()"));
        Assert.assertEquals("GET_TAG_NAMES_Q(null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q(null)"));
        Assert.assertEquals("GET_TAG_NAMES_Q('empty')",                "'empty'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q('empty')"));
        Assert.assertEquals("GET_TAG_NAMES_Q('empty', ',')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q('empty', ',')"));
        Assert.assertEquals("GET_TAG_NAMES_Q('empty', '|', null)",     "'empty'", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q('empty', '|')"));
        Assert.assertEquals("GET_TAG_NAMES_Q('empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_TAG_NAMES_Q('empty', ',', '{', '}')"));

        // empty UG names
        Assert.assertEquals("GET_UG_NAMES()",              "",      evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES()"));
        Assert.assertEquals("GET_UG_NAMES(null)",          "",      evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES(null)"));
        Assert.assertEquals("GET_UG_NAMES('empty')",       "empty", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES('empty')"));
        Assert.assertEquals("GET_UG_NAMES('empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES('empty', '|')"));
        Assert.assertEquals("GET_UG_NAMES('empty', null)", "empty", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES('empty', null)"));

        // empty UG names
        Assert.assertEquals("GET_UG_NAMES_Q()",                       "",        evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q()"));
        Assert.assertEquals("GET_UG_NAMES_Q(null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q(null)"));
        Assert.assertEquals("GET_UG_NAMES_Q('empty')",                "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q('empty')"));
        Assert.assertEquals("GET_UG_NAMES_Q('empty', ',')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q('empty', ',')"));
        Assert.assertEquals("GET_UG_NAMES_Q('empty', '|', null)",     "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q('empty', '|')"));
        Assert.assertEquals("GET_UG_NAMES_Q('empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_UG_NAMES_Q('empty', ',', '{', '}')"));

        // empty UR names
        Assert.assertEquals("GET_UR_NAMES()",              "",      evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES()"));
        Assert.assertEquals("GET_UR_NAMES(null)",          "",      evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES(null)"));
        Assert.assertEquals("GET_UR_NAMES('empty')",       "empty", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES('empty')"));
        Assert.assertEquals("GET_UR_NAMES('empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES('empty', '|')"));
        Assert.assertEquals("GET_UR_NAMES('empty', null)", "empty", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES('empty', null)"));

        // empty UR names
        Assert.assertEquals("GET_UR_NAMES_Q()",                       "",        evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q()"));
        Assert.assertEquals("GET_UR_NAMES_Q(null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q(null)"));
        Assert.assertEquals("GET_UR_NAMES_Q('empty')",                "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q('empty')"));
        Assert.assertEquals("GET_UR_NAMES_Q('empty', ',')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q('empty', ',')"));
        Assert.assertEquals("GET_UR_NAMES_Q('empty', '|', null)",     "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q('empty', '|')"));
        Assert.assertEquals("GET_UR_NAMES_Q('empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_UR_NAMES_Q('empty', ',', '{', '}')"));

        // non-existent attribute
        Assert.assertEquals("GET_TAG_ATTR('noattr')",                "",      evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('noattr')"));
        Assert.assertEquals("GET_TAG_ATTR('noattr', null)",          "",      evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('noattr', null)"));
        Assert.assertEquals("GET_TAG_ATTR('noattr', 'empty')",       "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('noattr', 'empty')"));
        Assert.assertEquals("GET_TAG_ATTR('noattr', 'empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('noattr', 'empty', '|')"));
        Assert.assertEquals("GET_TAG_ATTR('noattr', 'empty', null)", "empty", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR('noattr', 'empty', null)"));

        // non-existent attribute
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr')",                         "",        evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr')"));
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr', null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr', null)"));
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr', 'empty')",                "'empty'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr', 'empty')"));
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr', 'empty', ',')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr', 'empty', ',')"));
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr', 'empty', '|', null)",     "empty",   evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr', 'empty', '|', null)"));
        Assert.assertEquals("GET_TAG_ATTR_Q('noattr', 'empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_TAG_ATTR_Q('noattr', 'empty', ',', '{', '}')"));

        // non-existent attribute
        Assert.assertEquals("GET_UG_ATTR('noattr')",             "", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('noattr')"));
        Assert.assertEquals("GET_UG_ATTR('noattr', null)",       "", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('noattr', null)"));
        Assert.assertEquals("GET_UG_ATTR('noattr', 'empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('noattr', 'empty', '|')"));
        Assert.assertEquals("GET_UG_ATTR('noattr', 'empty', null)", "empty",  evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR('noattr', 'empty', null)"));

        // non-existent attribute
        Assert.assertEquals("GET_UG_ATTR_Q('noattr')",                         "",        evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr')"));
        Assert.assertEquals("GET_UG_ATTR_Q('noattr', null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('noattr', 'empty', null)",          "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr', 'empty', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('noattr', 'empty', '|')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr', 'empty', '|')"));
        Assert.assertEquals("GET_UG_ATTR_Q('noattr', 'empty', ',', null)",     "empty",   evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr', 'empty', ',', null)"));
        Assert.assertEquals("GET_UG_ATTR_Q('noattr', 'empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_UG_ATTR_Q('noattr', 'empty', ',', '{', '}')"));

        // non-existent attribute
        Assert.assertEquals("GET_USER_ATTR('noattr')",                "",      evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('noattr')"));
        Assert.assertEquals("GET_USER_ATTR('noattr', null)",          "",      evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('noattr', null)"));
        Assert.assertEquals("GET_USER_ATTR('noattr', 'empty', '|')",  "empty", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('noattr', 'empty', '|')"));
        Assert.assertEquals("GET_USER_ATTR('noattr', 'empty', null)", "empty", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR('noattr', 'empty', null)"));

        // non-existent attribute
        Assert.assertEquals("GET_USER_ATTR_Q('noattr')",                         "",        evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr')"));
        Assert.assertEquals("GET_USER_ATTR_Q('noattr', null)",                   "",        evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr', null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('noattr', 'empty', null)",          "'empty'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr', 'empty', null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('noattr', 'empty', '|')",           "'empty'", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr', 'empty', '|')"));
        Assert.assertEquals("GET_USER_ATTR_Q('noattr', 'empty', ',', null)",     "empty",   evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr', 'empty', ',', null)"));
        Assert.assertEquals("GET_USER_ATTR_Q('noattr', 'empty', ',', '{', '}')", "{empty}", evaluator.evaluateScript(scriptEngine, "GET_USER_ATTR_Q('noattr', 'empty', ',', '{', '}')"));
    }

    @Test
    public void testIntersectsIncludes() {
        RangerTag                    tagPartners = new RangerTag("PARTNERS", Collections.singletonMap("names", "partner-1,partner-2"));
        RangerTag                    tagDepts    = new RangerTag("DEPTS", Collections.singletonMap("names", "ENGG,SALES"));
        RangerAccessRequest          request     = createRequest("test-user2", Collections.singleton("test-group2"), Collections.singleton("test-role2"), Arrays.asList(tagPartners, tagDepts));
        RangerRequestScriptEvaluator evaluator   = new RangerRequestScriptEvaluator(request);

        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['sales'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['sales'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['mktg'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['mktg'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['products'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['products'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['sales', 'engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['sales', 'engineering'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['mktg', 'engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['mktg', 'engineering'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['products', 'engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['products', 'engineering'])"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].intersects(['engineering', 'hr', 'sales'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['engineering', 'hr', 'sales'])"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].intersects(['engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(['engineering'])"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].intersects([])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects([])"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].intersects(null)", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].intersects(null)"));
        Assert.assertFalse("test: [].intersects(['engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "[].intersects(['engineering'])"));
        Assert.assertFalse("test: [].intersects([])", (Boolean) evaluator.evaluateScript(scriptEngine, "[].intersects([])"));
        /*
         TAGS.PARTNERS.names = partner-1,partner-2
         USER.partners       = partner-1,partner-2,partners-3
         */
        Assert.assertTrue("test: TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_USER_ATTR('partners') && TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))"));


        Assert.assertTrue("test: ['sales', 'mktg', 'products'].includes('sales')", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes('sales')"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].includes('mktg')", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes('mktg')"));
        Assert.assertTrue("test: ['sales', 'mktg', 'products'].includes('products')", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes('products')"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].includes(['engineering'])", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes('engineering')"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].includes('')", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes('')"));
        Assert.assertFalse("test: ['sales', 'mktg', 'products'].includes(null)", (Boolean) evaluator.evaluateScript(scriptEngine, "['sales', 'mktg', 'products'].includes(null)"));
        Assert.assertFalse("test: [].includes('engineering')", (Boolean) evaluator.evaluateScript(scriptEngine, "[].includes('engineering')"));
        Assert.assertFalse("test: [].includes([])", (Boolean) evaluator.evaluateScript(scriptEngine, "[].includes([])"));
        /*
         TAGS.DEPTS.names = ENGG,SALES
         USER.dept        = ENGG
         */
        Assert.assertTrue("test: TAGS.DEPTS.names.split(',').includes(USER.dept)", (Boolean) evaluator.evaluateScript(scriptEngine, "TAGS.DEPTS.names.split(',').includes(USER.dept)"));

        // switch context to user test-user3, who has different attribute values for partners and dept
        request   = createRequest("test-user3", Collections.singleton("test-group3"), Collections.singleton("test-role3"), Arrays.asList(tagPartners, tagDepts));
        evaluator = new RangerRequestScriptEvaluator(request);

        /*
         TAGS.PARTNERS.names = partner-1,partner-2
         USER.partners       = partner-3
         */
        Assert.assertFalse("test: TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_USER_ATTR('partners') && TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))"));

        /*
         TAGS.DEPTS.names = ENGG,SALES
         USER.dept        = MKTG
         */
        Assert.assertFalse("test: TAGS.DEPTS.names.split(',').includes(USER.dept)", (Boolean) evaluator.evaluateScript(scriptEngine, "TAGS.DEPTS.names.split(',').includes(USER.dept)"));

        // switch context to user test-user4, who doesn't have attribute partners and dept
        request   = createRequest("test-user4", Collections.singleton("test-group4"), Collections.singleton("test-role4"), Arrays.asList(tagPartners, tagDepts));
        evaluator = new RangerRequestScriptEvaluator(request);

        /*
         TAGS.PARTNERS.names = partner-1,partner-2
         USER.partners       = null
         */
        Assert.assertFalse("test: TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))", (Boolean) evaluator.evaluateScript(scriptEngine, "HAS_USER_ATTR('partners') && TAGS.PARTNERS.names.split(',').intersects(USER.partners.split(','))"));

        /*
         TAGS.DEPTS.names = ENGG,SALES
         USER.dept        = null
         */
        Assert.assertFalse("test: TAGS.DEPTS.names.split(',').includes(USER.dept)", (Boolean) evaluator.evaluateScript(scriptEngine, "TAGS.DEPTS.names.split(',').includes(USER.dept)"));
    }

    @Test
    public void testBlockJavaClassReferences() {
        RangerAccessRequest          request   = createRequest("test-user", Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_LIST);
        RangerRequestScriptEvaluator evaluator = new RangerRequestScriptEvaluator(request);

        Assert.assertNull("test: java.lang.System.out.println(\"test\");", evaluator.evaluateScript(scriptEngine, "java.lang.System.out.println(\"test\");"));
        Assert.assertNull("test: java.lang.Runtime.getRuntime().exec(\"bash\");", evaluator.evaluateScript(scriptEngine, "java.lang.Runtime.getRuntime().exec(\"bash\");"));
    }


    RangerAccessRequest createRequest(String userName, Set<String> userGroups, Set<String> userRoles, List<RangerTag> resourceTags) {
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
        request.setUser(userName);
        request.setUserGroups(userGroups);
        request.setUserRoles(userRoles);

        RangerAccessRequestUtil.setCurrentResourceInContext(request.getContext(), resource);

        if (resourceTags != null) {
            Set<RangerTagForEval> rangerTagForEvals = new HashSet<>();
            RangerTagForEval      currentTag        = null;

            for (RangerTag tag : resourceTags) {
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

        Map<String, Map<String, String>> userAttrMapping  = new HashMap<>();
        Map<String, Map<String, String>> groupAttrMapping = new HashMap<>();

        userAttrMapping.put("test-user", Collections.singletonMap("state", "CA"));
        userAttrMapping.put("test-user2", new HashMap<String, String>() {{ put("partners", "partner-1,partner-2,partner-3"); put("dept", "ENGG"); }});
        userAttrMapping.put("test-user3", new HashMap<String, String>() {{ put("partners", "partner-3"); put("dept", "MKTG"); }});

        groupAttrMapping.put("test-group1", new HashMap<String, String>() {{ put("dept", "ENGG"); put("site", "10"); }});
        groupAttrMapping.put("test-group2", new HashMap<String, String>() {{ put("dept", "PROD"); put("site", "20"); }});
        groupAttrMapping.put("test-group3", new HashMap<String, String>() {{ put("dept", "SALES"); put("site", "30"); }});

        when(userStore.getUserAttrMapping()).thenReturn(userAttrMapping);
        when(userStore.getGroupAttrMapping()).thenReturn(groupAttrMapping);

        return request;
    }

}