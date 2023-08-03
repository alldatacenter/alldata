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

package org.apache.ranger.common;

import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestContextUtil {

        @InjectMocks
        ContextUtil contextUtil = new ContextUtil();

        UserSessionBase currentUserSession = new UserSessionBase();
        XXPortalUser gjUser = new XXPortalUser();
        RangerSecurityContext context = new RangerSecurityContext();

        @Before
        public void setup(){
                gjUser.setId(1L);
                currentUserSession.setXXPortalUser(gjUser);
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);
        }

        @SuppressWarnings("static-access")
        @Test
        public void testGetCurrentUserId(){
                Long expectedId = 1L;
                Long id = contextUtil.getCurrentUserId();

                Assert.assertEquals(expectedId, id);
        }

        @SuppressWarnings("static-access")
        @Test
        public void testGetCurrentUserPublicName(){
                String expectedName = "rangerAdmin";
                gjUser.setPublicScreenName("rangerAdmin");

                String publicName = contextUtil.getCurrentUserPublicName();
                Assert.assertEquals(expectedName, publicName);

        }

        @SuppressWarnings("static-access")
        @Test
        public void testCurrentUserSession(){
                UserSessionBase expectedUserSession = contextUtil.getCurrentUserSession();
                Assert.assertNotNull(expectedUserSession);
        }

        @SuppressWarnings("static-access")
        @Test
        public void testCurrentUserSessionAsNull(){
                context.setUserSession(null);
                UserSessionBase expectedUserSession = contextUtil.getCurrentUserSession();
                Assert.assertNull(expectedUserSession);
        }

        @SuppressWarnings("static-access")
        @Test
        public void testCurrentRequestContext(){
                RequestContext requestContext = new RequestContext();
                context.setRequestContext(requestContext);
                RequestContext expectedContext = contextUtil.getCurrentRequestContext();
                Assert.assertNotNull(expectedContext);

        }

        @SuppressWarnings("static-access")
        @Test
        public void testCurrentUserLoginId(){
                String expectedLoginId = "rangerAdmin";
                gjUser.setLoginId("rangerAdmin");
                String loginId = contextUtil.getCurrentUserLoginId();
                Assert.assertEquals(expectedLoginId, loginId);

        }

}
