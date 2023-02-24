/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.security;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.web.TestUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FileAuthenticationTest {

    private static ApplicationContext applicationContext = null;
    private static AtlasAuthenticationProvider authProvider = null;
    private String originalConf;
    private static final Logger LOG = LoggerFactory
            .getLogger(FileAuthenticationTest.class);
    @Mock
    Authentication authentication;

    @BeforeMethod
    public void setup1() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public void setup() throws Exception {

        String persistDir = TestUtils.getTempDirectory();
        setUpPolicyStore(persistDir);
        setupUserCredential(persistDir);

        setUpAltasApplicationProperties(persistDir);
        
        originalConf = System.getProperty("atlas.conf");
        System.setProperty("atlas.conf", persistDir);

        applicationContext = new ClassPathXmlApplicationContext(
                "test-spring-security.xml");
        authProvider = applicationContext
                .getBean(org.apache.atlas.web.security.AtlasAuthenticationProvider.class);

    }

    private void setUpAltasApplicationProperties(String persistDir) throws Exception {
        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("atlas.authentication.method.file", "true");
        configuration.setProperty("atlas.authentication.method.file.filename", persistDir
                + "/users-credentials");
        configuration.setProperty("atlas.auth.policy.file",persistDir
                + "/policy-store.txt" );
        TestUtils.writeConfiguration(configuration, persistDir + File.separator
                + ApplicationProperties.APPLICATION_PROPERTIES);
    }

    private void setupUserCredential(String tmpDir) throws Exception {

        StringBuilder credentialFileStr = new StringBuilder(1024);
        credentialFileStr.append("adminv3=ADMIN::$2a$10$ZVnkc2if06JMLCJEAhTKbOPeWDXTCFLL8zMA6FzZoP.bu8ThT43ha\n");
        credentialFileStr.append("admin=ADMIN::a4a88c0872bf652bb9ed803ece5fd6e82354838a9bf59ab4babb1dab322154e1\n");
        credentialFileStr.append("adminv1=ADMIN::8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918\n");
        credentialFileStr.append("michael=DATA_SCIENTIST::95bfb24de17d285d734b9eaa9109bfe922adc85f20d2e5e66a78bddb4a4ebddb\n");
        credentialFileStr.append("paul=DATA_STEWARD::e7c0dcf5f8a93e93791e9bac1ae454a691c1d2a902fc4256d489e96c1b9ac68c\n");
        credentialFileStr.append("user=  \n");
        credentialFileStr.append("user12=  ::43d864d8f9b53cd913fc6a665c8470595cefa4a360edeb78cf6c4eac00c0a3a0\n");
        File credentialFile = new File(tmpDir, "users-credentials");
        FileUtils.write(credentialFile, credentialFileStr.toString());
    }

    private void setUpPolicyStore(String tmpDir) throws Exception {
        StringBuilder policyStr = new StringBuilder(1024);
        policyStr.append("adminPolicy;;admin:rwud;;ROLE_ADMIN:rwud;;type:*,entity:*,operation:*");
        File policyFile = new File(tmpDir, "policy-store.txt");
        FileUtils.write(policyFile, policyStr.toString());
    }



    @Test
    public void testValidUserLoginWithV3password() {

        when(authentication.getName()).thenReturn("adminv3");
        when(authentication.getCredentials()).thenReturn("admin");

        Authentication auth = authProvider.authenticate(authentication);
        LOG.debug(" {}", auth);

        assertTrue(auth.isAuthenticated());
    }

    @Test
    public void testValidUserLogin() {

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getCredentials()).thenReturn("admin");

        Authentication auth = authProvider.authenticate(authentication);
        LOG.debug(" {}", auth);

        assertTrue(auth.isAuthenticated());
    }

    @Test
    public void testValidUserLoginWithV1password() {

        when(authentication.getName()).thenReturn("adminv1");
        when(authentication.getCredentials()).thenReturn("admin");

        Authentication auth = authProvider.authenticate(authentication);
        LOG.debug(" {}", auth);

        assertTrue(auth.isAuthenticated());
    }

    @Test
    public void testInValidPasswordLogin() {

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getCredentials()).thenReturn("wrongpassword");

       try {
            Authentication auth = authProvider.authenticate(authentication);
           LOG.debug(" {}", auth);
        } catch (BadCredentialsException bcExp) {
            assertEquals("Wrong password", bcExp.getMessage());
        }
    }

    @Test
    public void testInValidUsernameLogin() {
   
        when(authentication.getName()).thenReturn("wrongUserName");
        when(authentication.getCredentials()).thenReturn("wrongpassword");
      try {
            Authentication auth = authProvider.authenticate(authentication);
          LOG.debug(" {}", auth);
        } catch (UsernameNotFoundException uExp) {
            assertTrue(uExp.getMessage().contains("Username not found."));
        }
    }

    @Test
    public void testLoginWhenRoleIsNotSet() {

        when(authentication.getName()).thenReturn("user12"); // for this user role is not set properly
        when(authentication.getCredentials()).thenReturn("user12");
        try {
            Authentication auth = authProvider.authenticate(authentication);
            LOG.debug(" {}", auth);
        } catch (AtlasAuthenticationException uExp) {
            assertTrue(uExp.getMessage().startsWith("User role credentials is not set properly for"));
        }
    }


    @Test
    public void testLoginWhenRolePasswordNotSet() {

        when(authentication.getName()).thenReturn("user"); // for this user password details are set blank
        when(authentication.getCredentials()).thenReturn("P@ssword");
        try {
            Authentication auth = authProvider.authenticate(authentication);
            LOG.debug(" {}", auth);
        } catch (UsernameNotFoundException uExp) {
            assertTrue(uExp.getMessage().startsWith("Username not found"));
        }
    }

    @Test
    public void testUserRoleMapping() {

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getCredentials()).thenReturn("admin");

        Authentication auth = authProvider.authenticate(authentication);
        LOG.debug(" {}", auth);

        assertTrue(auth.isAuthenticated());

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        String role = "";
        for (GrantedAuthority gauth : authorities) {
            role = gauth.getAuthority();
        }
        assertTrue("ADMIN".equals(role));
    }


    @AfterClass
    public void tearDown() throws Exception {

        if (originalConf != null) {
            System.setProperty("atlas.conf", originalConf);
        }
        applicationContext = null;
        authProvider = null;
    }
}
