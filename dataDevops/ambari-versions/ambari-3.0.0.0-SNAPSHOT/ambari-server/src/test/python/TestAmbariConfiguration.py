"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import os

from unittest import TestCase

class TestAmbariConfiguration(TestCase):

  def setUp(self):
    import imp
    self.test_directory = os.path.dirname(os.path.abspath(__file__))

    relative_path = '../../main/resources/stacks/ambari_configuration.py'
    ambari_configuration_path = os.path.abspath(os.path.join(self.test_directory, relative_path))
    class_name = 'AmbariConfiguration'

    with open(ambari_configuration_path, 'rb') as fp:
      ambari_configuration_impl = imp.load_module('ambari_configuration', fp,
                                                  ambari_configuration_path,
                                                  ('.py', 'rb', imp.PY_SOURCE))

    self.ambari_configuration_class = getattr(ambari_configuration_impl, class_name)

  def testMissingData(self):
    ambari_configuration = self.ambari_configuration_class('{}')
    self.assertIsNone(ambari_configuration.get_ambari_server_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_ldap_configuration())

  def testMissingSSOConfiguration(self):
    services_json = {
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())
    self.assertIsNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertIsNone(ambari_sso_details.get_jwt_audiences())
    self.assertIsNone(ambari_sso_details.get_jwt_cookie_name())
    self.assertIsNone(ambari_sso_details.get_sso_provider_url())
    self.assertIsNone(ambari_sso_details.get_sso_provider_original_parameter_name())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))

  def testAmbariSSOConfigurationNotManagingServices(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "false",
          "ambari.sso.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "false",
          "ambari.sso.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertFalse(ambari_sso_details.is_managing_services())
    self.assertFalse(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

  def testAmbariSSOConfigurationManagingServices(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertTrue(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.manage_services": "true",
          "ambari.sso.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertTrue(ambari_sso_details.is_managing_services())
    self.assertTrue(ambari_sso_details.should_enable_sso("AMBARI"))
    self.assertFalse(ambari_sso_details.should_disable_sso("AMBARI"))
    self.assertTrue(ambari_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(ambari_sso_details.should_disable_sso("RANGER"))

  def testAmbariJWTProperties(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": "-----BEGIN CERTIFICATE-----\nMIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==\n-----END CERTIFICATE-----",
          "ambari.sso.authentication.enabled": "true",
          "ambari.sso.provider.url": "https://knox.ambari.apache.org",
          "ambari.sso.jwt.cookieName": "hadoop-jwt",
          "ambari.sso.jwt.audiences": ""
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_sso_configuration())

    ambari_sso_details = ambari_configuration.get_ambari_sso_details()
    self.assertIsNotNone(ambari_sso_details)
    self.assertEquals('', ambari_sso_details.get_jwt_audiences())
    self.assertEquals('hadoop-jwt', ambari_sso_details.get_jwt_cookie_name())
    self.assertEquals('https://knox.ambari.apache.org', ambari_sso_details.get_sso_provider_url())
    self.assertEquals('MIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==',
                      ambari_sso_details.get_sso_provider_certificate())

  def testCertWithHeaderAndFooter(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": '-----BEGIN CERTIFICATE-----\n'
                                             'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                                             '................................................................\n'
                                             'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                                             '-----END CERTIFICATE-----\n'
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, True))

  def testCertWithoutHeaderAndFooter(self):
    services_json = {
      "ambari-server-configuration": {
        "sso-configuration": {
          "ambari.sso.provider.certificate": 'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                                             '................................................................\n'
                                             'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n',
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details()

    self.assertEquals('-----BEGIN CERTIFICATE-----\n'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, False))

    self.assertEquals('-----BEGIN CERTIFICATE-----'
                      'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy'
                      '-----END CERTIFICATE-----',
                      ambari_sso_details.get_sso_provider_certificate(True, True))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n'
                      '................................................................\n'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, False))

    self.assertEquals('MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD'
                      '................................................................'
                      'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy',
                      ambari_sso_details.get_sso_provider_certificate(False, True))

  def testMissingLDAPConfiguration(self):
    services_json = {
      "ambari-server-configuration": {
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertFalse(ambari_ldap_details.is_ldap_enabled())
    self.assertIsNone(ambari_ldap_details.get_server_host())
    self.assertIsNone(ambari_ldap_details.get_server_port())
    self.assertIsNone(ambari_ldap_details.get_server_url())
    self.assertIsNone(ambari_ldap_details.get_secondary_server_host())
    self.assertIsNone(ambari_ldap_details.get_secondary_server_port())
    self.assertIsNone(ambari_ldap_details.get_secondary_server_url())
    self.assertFalse(ambari_ldap_details.is_use_ssl())
    self.assertFalse(ambari_ldap_details.is_anonymous_bind())
    self.assertIsNone(ambari_ldap_details.get_bind_dn())
    self.assertIsNone(ambari_ldap_details.get_bind_password())
    self.assertIsNone(ambari_ldap_details.get_dn_attribute())
    self.assertIsNone(ambari_ldap_details.get_user_object_class())
    self.assertIsNone(ambari_ldap_details.get_user_name_attribute())
    self.assertIsNone(ambari_ldap_details.get_user_search_base())
    self.assertIsNone(ambari_ldap_details.get_group_object_class())
    self.assertIsNone(ambari_ldap_details.get_group_name_attribute())
    self.assertIsNone(ambari_ldap_details.get_group_member_attribute())
    self.assertIsNone(ambari_ldap_details.get_group_search_base())
    self.assertIsNone(ambari_ldap_details.get_group_mapping_rules())
    self.assertIsNone(ambari_ldap_details.get_user_search_filter())
    self.assertIsNone(ambari_ldap_details.get_user_member_replace_pattern())
    self.assertIsNone(ambari_ldap_details.get_user_member_filter())
    self.assertIsNone(ambari_ldap_details.get_group_search_filter())
    self.assertIsNone(ambari_ldap_details.get_group_member_replace_pattern())
    self.assertIsNone(ambari_ldap_details.get_group_member_filter())
    self.assertFalse(ambari_ldap_details.is_force_lower_case_user_names())
    self.assertFalse(ambari_ldap_details.is_pagination_enabled())
    self.assertFalse(ambari_ldap_details.is_follow_referral_handling())
    self.assertFalse(ambari_ldap_details.is_disable_endpoint_identification())
    self.assertFalse(ambari_ldap_details.is_ldap_alternate_user_search_enabled())
    self.assertIsNone(ambari_ldap_details.get_alternate_user_search_filter())
    self.assertIsNone(ambari_ldap_details.get_sync_collision_handling_behavior())

  def testNotEmtpyLDAPConfiguration(self):
    services_json = {
        "ambari-server-configuration": {
          "ldap-configuration": {
            "ambari.ldap.authentication.enabled" : "true",
            "ambari.ldap.connectivity.server.host" : "host1",
            "ambari.ldap.connectivity.server.port" : "336",
            "ambari.ldap.connectivity.secondary.server.host" : "host2",
            "ambari.ldap.connectivity.secondary.server.port" : "337",
            "ambari.ldap.connectivity.use_ssl" : "true",
            "ambari.ldap.connectivity.anonymous_bind" : "true",
            "ambari.ldap.connectivity.bind_dn" : "bind_dn",
            "ambari.ldap.connectivity.bind_password" : "bind_password",
            "ambari.ldap.attributes.dn_attr" : "dn_attr",
            "ambari.ldap.attributes.user.object_class" : "user.object_class",
            "ambari.ldap.attributes.user.name_attr" : "user.name_attr",
            "ambari.ldap.attributes.user.search_base" : "user.search_base",
            "ambari.ldap.attributes.group.object_class" : "group.object_class",
            "ambari.ldap.attributes.group.name_attr" : "group.name_attr",
            "ambari.ldap.attributes.group.member_attr" : "group.member_attr",
            "ambari.ldap.attributes.group.search_base" : "group.search_base",
            "ambari.ldap.advanced.group_mapping_rules" : "group_mapping_rules",
            "ambari.ldap.advanced.user_search_filter" : "user_search_filter",
            "ambari.ldap.advanced.user_member_replace_pattern" : "user_member_replace_pattern",
            "ambari.ldap.advanced.user_member_filter" : "user_member_filter",
            "ambari.ldap.advanced.group_search_filter" : "group_search_filter",
            "ambari.ldap.advanced.group_member_replace_pattern" : "group_member_replace_pattern",
            "ambari.ldap.advanced.group_member_filter" : "group_member_filter",
            "ambari.ldap.advanced.force_lowercase_usernames" : "true",
            "ambari.ldap.advanced.pagination_enabled" : "true",
            "ambari.ldap.advanced.referrals" : "true",
            "ambari.ldap.advanced.disable_endpoint_identification" : "true",
            "ambari.ldap.advanced.alternate_user_search_enabled" : "true",
            "ambari.ldap.advanced.alternate_user_search_filter" : "alternate_user_search_filter",
            "ambari.ldap.advanced.collision_behavior" : "collision_behavior"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())
    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)

    self.assertTrue(ambari_ldap_details.is_ldap_enabled())
    self.assertEquals(ambari_ldap_details.get_server_host(), "host1")
    self.assertEquals(ambari_ldap_details.get_server_port(), 336)
    self.assertEquals(ambari_ldap_details.get_server_url(), "host1:336")
    self.assertEquals(ambari_ldap_details.get_secondary_server_host(), "host2")
    self.assertEquals(ambari_ldap_details.get_secondary_server_port(), 337)
    self.assertEquals(ambari_ldap_details.get_secondary_server_url(), "host2:337")
    self.assertTrue(ambari_ldap_details.is_use_ssl())
    self.assertTrue(ambari_ldap_details.is_anonymous_bind())
    self.assertEquals(ambari_ldap_details.get_bind_dn(), "bind_dn")
    self.assertEquals(ambari_ldap_details.get_bind_password(), "bind_password")
    self.assertEquals(ambari_ldap_details.get_dn_attribute(), "dn_attr")
    self.assertEquals(ambari_ldap_details.get_user_object_class(), "user.object_class")
    self.assertEquals(ambari_ldap_details.get_user_name_attribute(), "user.name_attr")
    self.assertEquals(ambari_ldap_details.get_user_search_base(), "user.search_base")
    self.assertEquals(ambari_ldap_details.get_group_object_class(), "group.object_class")
    self.assertEquals(ambari_ldap_details.get_group_name_attribute(), "group.name_attr")
    self.assertEquals(ambari_ldap_details.get_group_member_attribute(), "group.member_attr")
    self.assertEquals(ambari_ldap_details.get_group_search_base(), "group.search_base")
    self.assertEquals(ambari_ldap_details.get_group_mapping_rules(), "group_mapping_rules")
    self.assertEquals(ambari_ldap_details.get_user_search_filter(), "user_search_filter")
    self.assertEquals(ambari_ldap_details.get_user_member_replace_pattern(), "user_member_replace_pattern")
    self.assertEquals(ambari_ldap_details.get_user_member_filter(), "user_member_filter")
    self.assertEquals(ambari_ldap_details.get_group_search_filter(), "group_search_filter")
    self.assertEquals(ambari_ldap_details.get_group_member_replace_pattern(), "group_member_replace_pattern")
    self.assertEquals(ambari_ldap_details.get_group_member_filter(), "group_member_filter")
    self.assertTrue(ambari_ldap_details.is_force_lower_case_user_names())
    self.assertTrue(ambari_ldap_details.is_pagination_enabled())
    self.assertTrue(ambari_ldap_details.is_follow_referral_handling())
    self.assertTrue(ambari_ldap_details.is_disable_endpoint_identification())
    self.assertTrue(ambari_ldap_details.is_ldap_alternate_user_search_enabled())
    self.assertEquals(ambari_ldap_details.get_alternate_user_search_filter(), "alternate_user_search_filter")
    self.assertEquals(ambari_ldap_details.get_sync_collision_handling_behavior(), "collision_behavior")

  def testAmbariNotMangingLdapConfiguration(self):
    ## Case 1: missing the boolean flag indicating that Ambari manages LDAP configuration
    services_json = {
      "ambari-server-configuration": {
        "ldap-configuration": {
          "ambari.ldap.enabled_services": "AMBARI"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertFalse(ambari_ldap_details.is_managing_services())
    self.assertFalse(ambari_ldap_details.should_enable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("AMBARI"))

    ## Case 2: setting the boolean flag to false indicating that Ambari shall NOT manage LDAP configuration
    services_json = {
      "ambari-server-configuration": {
        "ldap-configuration": {
          "ambari.ldap.manage_services": "false",
          "ambari.ldap.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertFalse(ambari_ldap_details.is_managing_services())
    self.assertFalse(ambari_ldap_details.should_enable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("RANGER"))

    ## Case 3: setting the boolean flag to false indicating that Ambari shall NOT manage LDAP configuration and indicating it should be done for ALL services
    services_json = {
      "ambari-server-configuration": {
        "ldap-configuration": {
          "ambari.ldap.manage_services": "false",
          "ambari.ldap.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertFalse(ambari_ldap_details.is_managing_services())
    self.assertFalse(ambari_ldap_details.should_enable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("RANGER"))

  def testAmbariMangingLdapConfiguration(self):
    ## Case 1: setting the boolean flag to false indicating that Ambari shall manage LDAP configuration for AMBARI and RANGER
    services_json = {
      "ambari-server-configuration": {
        "ldap-configuration": {
          "ambari.ldap.manage_services": "true",
          "ambari.ldap.enabled_services": "AMBARI, RANGER"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertTrue(ambari_ldap_details.is_managing_services())
    self.assertTrue(ambari_ldap_details.should_enable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("AMBARI"))
    self.assertTrue(ambari_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("RANGER"))

    ## Case 2: setting the boolean flag to false indicating that Ambari shall manage LDAP configuration for ALL services
    services_json = {
      "ambari-server-configuration": {
        "ldap-configuration": {
          "ambari.ldap.manage_services": "true",
          "ambari.ldap.enabled_services": "*"
        }
      }
    }

    ambari_configuration = self.ambari_configuration_class(services_json)
    self.assertIsNotNone(ambari_configuration.get_ambari_ldap_configuration())

    ambari_ldap_details = ambari_configuration.get_ambari_ldap_details()
    self.assertIsNotNone(ambari_ldap_details)
    self.assertTrue(ambari_ldap_details.is_managing_services())
    self.assertTrue(ambari_ldap_details.should_enable_ldap("AMBARI"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("AMBARI"))
    self.assertTrue(ambari_ldap_details.should_enable_ldap("HDFS"))
    self.assertFalse(ambari_ldap_details.should_disable_ldap("HDFS"))
