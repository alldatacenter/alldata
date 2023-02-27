/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.authorize.simple;

import org.apache.atlas.authorize.*;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AtlasSimpleAuthorizerTest {
    private static Logger LOG = LoggerFactory.getLogger(AtlasSimpleAuthorizerTest.class);

    private static final String USER_ADMIN            = "admin";
    private static final String USER_DATA_SCIENTIST   = "dataScientist";
    private static final String USER_DATA_STEWARD     = "dataSteward";
    private static final String USER_DATA_STEWARD_EX  = "dataStewardEx";
    private static final String USER_FINANCE          = "finance";
    private static final String USER_FINANCE_PII      = "financePII";
    private static final String USER_IN_ADMIN_GROUP   = "admin-group-user";
    private static final String USER_IN_UNKNOWN_GROUP = "unknown-group-user";

    private static final Map<String, Set<String>> USER_GROUPS = new HashMap<String, Set<String>>() {{
                                                                        put(USER_ADMIN, Collections.singleton("ROLE_ADMIN"));
                                                                        put(USER_DATA_STEWARD, Collections.emptySet());
                                                                        put(USER_DATA_SCIENTIST, Collections.emptySet());
                                                                        put(USER_DATA_STEWARD_EX, Collections.singleton("DATA_STEWARD_EX"));
                                                                        put(USER_FINANCE, Collections.singleton("FINANCE"));
                                                                        put(USER_FINANCE_PII, Collections.singleton("FINANCE_PII"));
                                                                        put(USER_IN_ADMIN_GROUP, Collections.singleton("ROLE_ADMIN"));
                                                                        put(USER_IN_UNKNOWN_GROUP, Collections.singleton("UNKNOWN_GROUP"));
                                                                    }};

    private static final List<AtlasPrivilege> ENTITY_PRIVILEGES = Arrays.asList(AtlasPrivilege.ENTITY_CREATE,
                                                                                AtlasPrivilege.ENTITY_UPDATE,
                                                                                AtlasPrivilege.ENTITY_READ,
                                                                                AtlasPrivilege.ENTITY_ADD_CLASSIFICATION,
                                                                                AtlasPrivilege.ENTITY_UPDATE_CLASSIFICATION,
                                                                                AtlasPrivilege.ENTITY_REMOVE_CLASSIFICATION,
                                                                                AtlasPrivilege.ENTITY_READ_CLASSIFICATION,
                                                                                AtlasPrivilege.ENTITY_ADD_LABEL,
                                                                                AtlasPrivilege.ENTITY_REMOVE_LABEL,
                                                                                AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA);

    private static final List<AtlasPrivilege> LABEL_PRIVILEGES = Arrays.asList(AtlasPrivilege.ENTITY_ADD_LABEL, AtlasPrivilege.ENTITY_REMOVE_LABEL);

    private String          originalConf;
    private AtlasAuthorizer authorizer;

    @BeforeMethod
    public void setup1() {
        originalConf = System.getProperty("atlas.conf");

        System.setProperty("atlas.conf", "src/test/resources");

        try {
            authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest setup failed", e);
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (originalConf != null) {
            System.setProperty("atlas.conf", originalConf);
        }

        authorizer = null;
    }

    @Test(enabled = true)
    public void testAllAllowedForAdminUser() {
        try {
            for (AtlasPrivilege privilege : AtlasPrivilege.values()) {
                AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege);

                setUser(request, USER_ADMIN);

                boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                AssertJUnit.assertEquals(privilege.name() + " should have been allowed for user " + USER_DATA_SCIENTIST, true, isAccessAllowed);
            }
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAddPIIForStewardExUser() {
        try {
            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null , AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, null,  new AtlasClassification("PII"));

            setUser(request, USER_DATA_STEWARD_EX);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_DATA_STEWARD_EX + " should have been allowed to add PII", true, isAccessAllowed);
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAddClassificationOnEntityWithClassificationForStewardExUser() {
        try {

            AtlasEntityHeader entityHeader = new AtlasEntityHeader();
            entityHeader.setClassifications(Arrays.asList(new AtlasClassification("PII_1"), new AtlasClassification("PII_2")));

            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, entityHeader, new AtlasClassification("PII"));

            setUser(request, USER_DATA_STEWARD_EX);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_DATA_STEWARD_EX + " should have been allowed to add PII", true, isAccessAllowed);
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAddClassificationOnEntityWithClassificationForStewardExUserShouldFail() {
        try {

            AtlasEntityHeader entityHeader = new AtlasEntityHeader();
            entityHeader.setClassifications(Arrays.asList(new AtlasClassification("TAG1"), new AtlasClassification("TAG2")));

            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, entityHeader, new AtlasClassification("PII"));

            setUser(request, USER_DATA_STEWARD_EX);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_DATA_STEWARD_EX + " should have not been allowed to add PII on entity with TAG1,TAG2 classification ", false, isAccessAllowed);
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAddPIIForStewardUser() {
        try {
            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null , AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, null,  new AtlasClassification("PII"));

            setUser(request, USER_DATA_STEWARD);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_DATA_STEWARD + " should not have been allowed to add PII", false, isAccessAllowed);
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testFinancePIIEntityAccessForFinancePIIUser() {
        try {
            AtlasEntityHeader entity = new AtlasEntityHeader() {{
                                                setClassifications(Arrays.asList(new AtlasClassification("FINANCE"), new AtlasClassification("PII")));
                                            }};

            for (AtlasPrivilege privilege : ENTITY_PRIVILEGES) {
                AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege, entity, new AtlasClassification("PII"));

                setUser(request, USER_FINANCE_PII);

                boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                AssertJUnit.assertEquals("user " + USER_FINANCE_PII + " should have been allowed " + privilege + " on entity with FINANCE & PII", true, isAccessAllowed);
            }
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testFinancePIIEntityAccessForFinanceUser() {
        try {
            AtlasEntityHeader entity = new AtlasEntityHeader() {{
                                                setClassifications(Arrays.asList(new AtlasClassification("FINANCE"), new AtlasClassification("PII")));
                                            }};

            for (AtlasPrivilege privilege : ENTITY_PRIVILEGES) {
                AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege, entity, new AtlasClassification("PII"));

                setUser(request, USER_FINANCE);

                boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                AssertJUnit.assertEquals("user " + USER_FINANCE + " should not have been allowed " + privilege + " on entity with FINANCE & PII", false, isAccessAllowed);
            }
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testFinanceEntityAccess() {
        try {
            AtlasEntityHeader entity = new AtlasEntityHeader() {{
                setClassifications(Arrays.asList(new AtlasClassification("FINANCE")));
            }};

            for (String userName : Arrays.asList(USER_FINANCE_PII, USER_FINANCE)) {
                for (AtlasPrivilege privilege : ENTITY_PRIVILEGES) {
                    AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege, entity, new AtlasClassification("FINANCE"));

                    setUser(request, userName);

                    boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                    AssertJUnit.assertEquals("user " + userName + " should have been allowed " + privilege + " on entity with FINANCE", true, isAccessAllowed);
                }
            }
        } catch (Exception e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAccessForUserInAdminGroup() {
        try {
            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_UPDATE);

            setUser(request, USER_IN_ADMIN_GROUP);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_IN_ADMIN_GROUP + " should have been allowed " + AtlasPrivilege.ENTITY_UPDATE, true, isAccessAllowed);
        } catch (AtlasAuthorizationException e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testAccessForUserInUnknownGroup() {
        try {
            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_UPDATE);

            setUser(request, USER_IN_UNKNOWN_GROUP);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_IN_UNKNOWN_GROUP + " should not have been allowed " + AtlasPrivilege.ENTITY_UPDATE, false, isAccessAllowed);
        } catch (AtlasAuthorizationException e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testLabels() {
        try {
            for (AtlasPrivilege privilege : LABEL_PRIVILEGES) {
                for (String userName : Arrays.asList(USER_DATA_SCIENTIST, USER_DATA_STEWARD)) {
                    AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege);

                    setUser(request, userName);

                    boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                    AssertJUnit.assertEquals("user " + userName + " should not have been allowed " + privilege, false, isAccessAllowed);
                }

                AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, privilege);

                setUser(request, USER_DATA_STEWARD_EX);

                boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                AssertJUnit.assertEquals("user " + USER_DATA_STEWARD_EX + " should have been allowed " + privilege, true, isAccessAllowed);
            }
        } catch (AtlasAuthorizationException e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    @Test(enabled = true)
    public void testBusinessMetadata() {
        try {
            for (String userName : Arrays.asList(USER_DATA_SCIENTIST, USER_DATA_STEWARD)) {
                AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA);

                setUser(request, userName);

                boolean isAccessAllowed = authorizer.isAccessAllowed(request);

                AssertJUnit.assertEquals("user " + userName + " should not have been allowed " + AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA, false, isAccessAllowed);
            }

            AtlasEntityAccessRequest request = new AtlasEntityAccessRequest(null, AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA);

            setUser(request, USER_DATA_STEWARD_EX);

            boolean isAccessAllowed = authorizer.isAccessAllowed(request);

            AssertJUnit.assertEquals("user " + USER_DATA_STEWARD_EX + " should have been allowed " + AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA, true, isAccessAllowed);
        } catch (AtlasAuthorizationException e) {
            LOG.error("Exception in AtlasSimpleAuthorizerTest", e);

            AssertJUnit.fail();
        }
    }

    private void setUser(AtlasAccessRequest request, String userName) {
        Set<String> userGroups = USER_GROUPS.get(userName);

        request.setUser(userName, userGroups != null ? userGroups : Collections.emptySet());
    }
}
