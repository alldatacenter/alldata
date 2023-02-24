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

package org.apache.atlas.web.service;

import com.google.common.base.Charsets;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.zookeeper.data.ACL;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CuratorFactoryTest {

    @Mock
    private Configuration configuration;

    @Mock
    private HAConfiguration.ZookeeperProperties zookeeperProperties;

    @Mock
    private CuratorFrameworkFactory.Builder builder;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldAddAuthorization() {
        when(zookeeperProperties.hasAcl()).thenReturn(true);
        when(zookeeperProperties.getAcl()).thenReturn("sasl:myclient@EXAMPLE.COM");
        when(zookeeperProperties.hasAuth()).thenReturn(true);
        when(zookeeperProperties.getAuth()).thenReturn("sasl:myclient@EXAMPLE.COM");
        CuratorFactory curatorFactory = new CuratorFactory(configuration) {
            @Override
            protected void initializeCuratorFramework() {
            }
        };
        curatorFactory.enhanceBuilderWithSecurityParameters(zookeeperProperties, builder);
        verify(builder).aclProvider(any(ACLProvider.class));
        verify(builder).authorization(eq("sasl"), eq("myclient@EXAMPLE.COM".getBytes(Charsets.UTF_8)));
    }

    @Test
    public void shouldAddAclProviderWithRightACL() {
        when(zookeeperProperties.hasAcl()).thenReturn(true);
        when(zookeeperProperties.getAcl()).thenReturn("sasl:myclient@EXAMPLE.COM");
        when(zookeeperProperties.hasAuth()).thenReturn(false);
        CuratorFactory curatorFactory = new CuratorFactory(configuration) {
            @Override
            protected void initializeCuratorFramework() {
            }
        };
        curatorFactory.enhanceBuilderWithSecurityParameters(zookeeperProperties, builder);
        verify(builder).aclProvider(argThat(new ArgumentMatcher<ACLProvider>() {
            @Override
            public boolean matches(ACLProvider aclProvider) {
                ACL acl = aclProvider.getDefaultAcl().get(0);
                return acl.getId().getId().equals("myclient@EXAMPLE.COM")
                        && acl.getId().getScheme().equals("sasl");
            }
        }));
    }

    @Test
    public void shouldNotAddAnySecureParameters() {
        when(zookeeperProperties.hasAcl()).thenReturn(false);
        when(zookeeperProperties.hasAuth()).thenReturn(false);
        CuratorFactory curatorFactory = new CuratorFactory(configuration) {
            @Override
            protected void initializeCuratorFramework() {
            }
        };
        verifyZeroInteractions(builder);
    }
}
