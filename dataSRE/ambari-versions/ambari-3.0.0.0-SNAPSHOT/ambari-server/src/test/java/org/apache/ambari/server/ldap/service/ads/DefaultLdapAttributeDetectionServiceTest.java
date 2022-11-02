/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.ChainedAttributeDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private AttributeDetectorFactory attributeDetectorFactoryMock;

  @Mock
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactoryMock;

  @Mock
  private LdapConnectionTemplate ldapConnectionTemplateMock;

  @Mock
  private SearchRequest searchRequestMock;

  @TestSubject
  private DefaultLdapAttributeDetectionService defaultLdapAttributeDetectionService = new DefaultLdapAttributeDetectionService();

  @Before
  public void before() {
    resetAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldLdapUserAttributeDetection() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(AmbariServerConfigurationKey.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration(configMap);

    List<Object> entryList = Lists.newArrayList(new DefaultEntry("uid=gauss"));

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(entryMapperMock().getClass())))
      .andReturn(entryList);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyString(), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(searchRequestMock);

    EasyMock.expect(attributeDetectorFactoryMock.userAttributDetector())
      .andReturn(new ChainedAttributeDetector(Sets.newHashSet(new UserNameAttrDetector())));

    EasyMock.expect(searchRequestMock.setSizeLimit(50)).andReturn(searchRequestMock);

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapUserAttributes(ldapConfiguration);

    // THEN
    Assert.assertNotNull(decorated);
    Assert.assertEquals("N/A", ldapConfiguration.userNameAttribute());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldUserAttributeDetectionFailWhenLdapOerationFails() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(AmbariServerConfigurationKey.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration(configMap);

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andThrow(new AmbariLdapException("Testing ..."));

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapUserAttributes(ldapConfiguration);

    // THEN
    // exception is thrown

  }


  @Test
  @SuppressWarnings("unchecked")
  public void shouldLdapGroupAttributeDetection() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(AmbariServerConfigurationKey.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration(configMap);

    List<Object> entryList = Lists.newArrayList(new DefaultEntry("uid=gauss"));

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(entryMapperMock().getClass())))
      .andReturn(entryList);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyString(), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(searchRequestMock);

    EasyMock.expect(attributeDetectorFactoryMock.groupAttributeDetector())
      .andReturn(new ChainedAttributeDetector(Sets.newHashSet(new GroupMemberAttrDetector())));

    EasyMock.expect(searchRequestMock.setSizeLimit(50)).andReturn(searchRequestMock);

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapGroupAttributes(ldapConfiguration);

    // THEN
    Assert.assertNotNull(decorated);
    Assert.assertEquals("N/A", ldapConfiguration.groupMemberAttribute());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldGroupAttributeDetectionFailWhenLdapOerationFails() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(AmbariServerConfigurationKey.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration(configMap);

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andThrow(new AmbariLdapException("Testing ..."));

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapGroupAttributes(ldapConfiguration);

    // THEN
    // exception is thrown

  }

  private EntryMapper<Entry> entryMapperMock() {
    return new EntryMapper<Entry>() {
      @Override
      public Entry map(Entry entry) throws LdapException {
        return null;
      }
    };
  }

}