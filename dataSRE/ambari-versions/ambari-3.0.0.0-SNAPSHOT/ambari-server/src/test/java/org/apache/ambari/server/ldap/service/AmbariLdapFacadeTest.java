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

package org.apache.ambari.server.ldap.service;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Unit test suite for the LdapFacade operations.
 */
public class AmbariLdapFacadeTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  public LdapConfigurationService ldapConfigurationServiceMock;

  @Mock(type = MockType.STRICT)
  public LdapAttributeDetectionService ldapAttributeDetectionServiceMock;

  @TestSubject
  private LdapFacade ldapFacade = new AmbariLdapFacade();

  private AmbariLdapConfiguration ambariLdapConfiguration;


  private Capture<AmbariLdapConfiguration> ambariLdapConfigurationCapture;

  @Before
  public void before() {
    ambariLdapConfiguration = new AmbariLdapConfiguration(Maps.newHashMap());
    ambariLdapConfigurationCapture = Capture.newInstance();


    resetAll();
  }

  /**
   * Tests whether the facade method call delegates to the proper service call.
   * The thest is success if the same instance is passed to the service.
   *
   * @throws Exception
   */
  @Test
  public void testShouldConfigurationCheckDelegateToTheRightServiceCall() throws Exception {
    // GIVEN
    // the mocks are set up
    ldapConfigurationServiceMock.checkConnection(EasyMock.capture(ambariLdapConfigurationCapture));
    replayAll();
    // WHEN
    // the facade method is called
    ldapFacade.checkConnection(ambariLdapConfiguration);

    // THEN
    // the captured configuration instance is the same the facade method got called with
    Assert.assertEquals("The configuration instance souldn't change before passing it to the service",
        ambariLdapConfiguration, ambariLdapConfigurationCapture.getValue());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldConfigurationCheckFailureResultInAmbariLdapException() throws Exception {
    // GIVEN
    ldapConfigurationServiceMock.checkConnection(EasyMock.anyObject(AmbariLdapConfiguration.class));
    EasyMock.expectLastCall().andThrow(new AmbariLdapException("Testing ..."));
    replayAll();

    // WHEN
    ldapFacade.checkConnection(ambariLdapConfiguration);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldLdapAttributesCheckDelegateToTheRightServiceCalls() throws Exception {
    // GIVEN

    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");


    Capture<String> testUserCapture = Capture.newInstance();
    Capture<String> testPasswordCapture = Capture.newInstance();
    Capture<String> userDnCapture = Capture.newInstance();

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.capture(testUserCapture), EasyMock.capture(testPasswordCapture),
        EasyMock.capture(ambariLdapConfigurationCapture))).andReturn("userDn");

    EasyMock.expect(ldapConfigurationServiceMock.checkGroupAttributes(EasyMock.capture(userDnCapture),
        EasyMock.capture(ambariLdapConfigurationCapture))).andReturn(Sets.newHashSet("userGroup"));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("testUser", testUserCapture.getValue());
    Assert.assertEquals("testPassword", testPasswordCapture.getValue());
    Assert.assertEquals("userDn", userDnCapture.getValue());

    Assert.assertTrue(testUserGroups.contains("userGroup"));

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldAttributeCheckFailuresResultInAmbariLdapException() throws Exception {
    // GIVEN
    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.anyString(), EasyMock.anyString(),
        EasyMock.anyObject(AmbariLdapConfiguration.class))).andThrow(new AmbariLdapException("Testing ..."));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, ambariLdapConfiguration);
    // THEN
    // Exception is thrown
  }

  @Test
  public void testShouldLdapAttributeDetectionDelegateToTheRightServiceCalls() throws Exception {

    // configuration map with user attributes detected
    Map<String, String> userConfigMap = Maps.newHashMap();
    userConfigMap.put(AmbariServerConfigurationKey.USER_NAME_ATTRIBUTE.key(), "uid");
    AmbariLdapConfiguration userAttrDecoratedConfig = new AmbariLdapConfiguration(userConfigMap);

    // configuration map with user+group attributes detected
    Map<String, String> groupConfigMap = Maps.newHashMap(userConfigMap);
    groupConfigMap.put(AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE.key(), "dn");
    AmbariLdapConfiguration groupAttrDecoratedConfig = new AmbariLdapConfiguration(groupConfigMap);

    Capture<AmbariLdapConfiguration> userAttrDetectionConfigCapture = Capture.newInstance();
    Capture<AmbariLdapConfiguration> groupAttrDetectionConfigCapture = Capture.newInstance();

    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.capture(userAttrDetectionConfigCapture)))
        .andReturn(userAttrDecoratedConfig);

    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapGroupAttributes(EasyMock.capture(groupAttrDetectionConfigCapture)))
        .andReturn(groupAttrDecoratedConfig);

    replayAll();

    // WHEN
    AmbariLdapConfiguration detected = ldapFacade.detectAttributes(ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("User attribute detection called with the wrong configuration", ambariLdapConfiguration,
        userAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Group attribute detection called with the wrong configuration", userAttrDecoratedConfig,
        groupAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Attribute detection returned an invalid configuration", groupAttrDecoratedConfig, detected);

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldAttributeDetectionFailuresResultInAmbariLdapException() throws Exception {
    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.anyObject(AmbariLdapConfiguration.class)))
        .andThrow(new AmbariLdapException("Testing ..."));

    replayAll();

    // WHEN
    ldapFacade.detectAttributes(ambariLdapConfiguration);

    // THEN
    // Exception is thrown
  }
}