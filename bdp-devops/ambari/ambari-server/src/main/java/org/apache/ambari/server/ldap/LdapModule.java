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


package org.apache.ambari.server.ldap;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.ldap.service.AmbariLdapFacade;
import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapConfigurationService;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapConnectionConfigService;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupObjectClassDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserGroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserObjectClassDetector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * GUICE configuration module for setting up LDAP related infrastructure.
 */
public class LdapModule extends AbstractModule {

  public static final String USER_ATTRIBUTES_DETECTORS = "UserAttributesDetectors";
  public static final String GROUP_ATTRIBUTES_DETECTORS = "GroupAttributesDetectors";

  @Override
  protected void configure() {
    bind(LdapFacade.class).to(AmbariLdapFacade.class);
    bind(LdapConfigurationService.class).to(DefaultLdapConfigurationService.class);
    bind(LdapAttributeDetectionService.class).to(DefaultLdapAttributeDetectionService.class);
    bind(LdapConnectionConfigService.class).to(DefaultLdapConnectionConfigService.class);

    // this binding requires the JPA module!
    bind(AmbariLdapConfiguration.class).toProvider(AmbariLdapConfigurationProvider.class);

    bind(AttributeDetectorFactory.class);

    // binding the set of user attributes detector
    Multibinder<AttributeDetector> userAttributeDetectorBinder = Multibinder.newSetBinder(binder(), AttributeDetector.class,
      Names.named(USER_ATTRIBUTES_DETECTORS));
    userAttributeDetectorBinder.addBinding().to(UserObjectClassDetector.class);
    userAttributeDetectorBinder.addBinding().to(UserNameAttrDetector.class);
    userAttributeDetectorBinder.addBinding().to(UserGroupMemberAttrDetector.class);


    // binding the set of group attributes detector
    Multibinder<AttributeDetector> groupAttributeDetectorBinder = Multibinder.newSetBinder(binder(), AttributeDetector.class,
      Names.named(GROUP_ATTRIBUTES_DETECTORS));
    groupAttributeDetectorBinder.addBinding().to(GroupObjectClassDetector.class);
    groupAttributeDetectorBinder.addBinding().to(GroupNameAttrDetector.class);
    groupAttributeDetectorBinder.addBinding().to(GroupMemberAttrDetector.class);

  }

}
