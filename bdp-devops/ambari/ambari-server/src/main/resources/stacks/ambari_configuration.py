#!/usr/bin/env ambari-python-wrap
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

CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----"
CERTIFICATE_FOOTER = "-----END CERTIFICATE-----"


def _get_from_dictionary(dictionary, key):
  """
  Safely returns the value from a dictionary that has the given key.

  if the dictionary is None or does not contain the specified key, None is returned

  :return: a dictionary
  """
  if dictionary and key in dictionary:
    return dictionary[key]
  else:
    return None


class AmbariConfiguration(object):
  """
  AmbariConfiguration is a class the encapsulates the Ambari server configuration data.

  The Ambari server configurations are split into categories, where each category contains 0 or more
  properties. For example, the 'ldap-configuration' category contains the
  "ambari.ldap.authentication.enabled"
  property.

  ...
  "ambari-server-configuration" : {
    ...
    "ldap-configuration" : {
      ...
      "ambari.ldap.authentication.enabled" : "true"
      ...
    },
    ...
    "sso-configuration" : {
      ...
      "ambari.sso.enabled_services" : "ATLAS, AMBARI"
      ...
    },
    ...
  }
  ...
  """

  def __init__(self, services):
    self.services = services

  def get_ambari_server_configuration(self):
    """
    Safely returns the "ambari-server-configurations" dictionary from the services dictionary.

    if the services dictionary is None or does not contain "ambari-server-configuration",
    None is returned

    :return: a dictionary
    """
    return _get_from_dictionary(self.services, "ambari-server-configuration")

  def get_ambari_server_configuration_category(self, category):
    """
    Safely returns a dictionary of the properties for the requested category from the
    "ambari-server-configurations" dictionary.

    If the ambari-server-configurations dictionary is None or does not contain the
    request category name, None is returned

    :param category: the name of a category
    :return: a dictionary
    """
    return _get_from_dictionary(self.get_ambari_server_configuration(), category)

  def get_ambari_sso_configuration(self):
    """
    Safely gets a dictionary of properties for the "sso-configuration" category.

    :return: a dictionary or None, if "sso-configuration" is not available
    """
    return self.get_ambari_server_configuration_category("sso-configuration")

  def get_ambari_sso_details(self):
    """
    Gets a dictionary of properties that may be used to configure a service for SSO integration.
    :return: a dictionary
    """
    return AmbariSSODetails(self.get_ambari_sso_configuration())

  def get_ambari_ldap_configuration(self):
    """
    Safely gets a dictionary of properties for the "ldap-configuration" category.

    :return: a dictionary or None, if "ldap-configuration" is not available
    """
    return self.get_ambari_server_configuration_category("ldap-configuration")

  def get_ambari_ldap_details(self):
    """
    :return: instance of AmbariLDAPConfiguration that may be used to configure a service for LDAP integration
    """
    return AmbariLDAPConfiguration(self.get_ambari_ldap_configuration())

class AmbariSSODetails(object):
  """
  AmbariSSODetails encapsulates the SSO configuration data specified in the ambari-server-configuration data
  """

  def __init__(self, sso_properties):
    self.sso_properties = sso_properties

  def is_managing_services(self):
    """
    Tests the configuration data to determine if Ambari should be configuring servcies to enable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.manage_services", which is expected
    to be a "true" or "false".

    :return: True, if Ambari should manage services' SSO configurations
    """
    return "true" == _get_from_dictionary(self.sso_properties, "ambari.sso.manage_services")

  def get_services_to_enable(self):
    """
    Safely gets the list of services that Ambari should enabled for SSO.

    The returned value is a list of the relevant service names converted to lowercase.

    :return: a list of service names converted to lowercase
    """
    sso_enabled_services = _get_from_dictionary(self.sso_properties, "ambari.sso.enabled_services")

    return [x.strip().lower() for x in sso_enabled_services.strip().split(",")] \
      if sso_enabled_services \
      else []

  def should_enable_sso(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to enable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled.

    :param service_name: the name of the service to test
    :return: True, if SSO should be enabled; False, otherwise
    """
    if self.is_managing_services():
      services_to_enable = self.get_services_to_enable()
      return "*" in services_to_enable or service_name.lower() in services_to_enable
    else:
      return False

  def should_disable_sso(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to disable SSO integration.

    The relevant property is "sso-configuration/ambari.sso.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled.

    :param service_name: the name of the service to test
    :return: true, if SSO should be disabled; false, otherwise
    """
    if self.is_managing_services():
      services_to_enable = self.get_services_to_enable()
      return "*" not in services_to_enable and service_name.lower() not in services_to_enable
    else:
      return False

  def get_jwt_audiences(self):
    """
    Gets the configured JWT audiences list

    The relevant property is "sso-configuration/ambari.sso.jwt.audiences", which is expected
    to be a comma-delimited list of audience names.

    :return the configured JWT audiences list:
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.jwt.audiences')

  def get_jwt_cookie_name(self):
    """
    Gets the configured JWT cookie name

    The relevant property is "sso-configuration/ambari.sso.jwt.cookieName", which is expected
    to be a string.

    :return: the configured JWT cookie name
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.jwt.cookieName')

  def get_sso_provider_url(self):
    """
    Gets the configured SSO provider URL

    The relevant property is "sso-configuration/ambari.sso.provider.url", which is expected
    to be a string.

    :return: the configured SSO provider URL
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.url')

  def get_sso_provider_original_parameter_name(self):
    """
    Gets the configured SSO provider's original URL parameter name

    The relevant property is "sso-configuration/ambari.sso.provider.originalUrlParamName", which is
    expected to be a string.

    :return: the configured SSO provider's original URL parameter name
    """
    return _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.originalUrlParamName')

  def get_sso_provider_certificate(self, include_header_and_footer=False, remove_line_breaks=True):
    """
    Retrieves, formats, and returns the PEM data from the stored 509 certificate.

    The relevant property is "sso-configuration/ambari.sso.provider.certificate", which is expected
    to be a PEM-encoded x509 certificate, including the header and footer.

    If the header and footer need to exist, and do not, the will be added. If they need to be removed,
    they will be removed if they exist.  Any line break characters will be left alone unless the
    caller specifies them to be removed. Line break characters will not be added if missing.

    :param include_header_and_footer: True, to include the standard header and footer; False to remove
    the standard header and footer
    :param remove_line_breaks: True, remove and line breaks from PEM data; False to leave any existing line break as-is
    :return:  formats, and returns the PEM data from an x509 certificate
    """
    public_cert = _get_from_dictionary(self.sso_properties, 'ambari.sso.provider.certificate')

    if public_cert:
      public_cert = public_cert.lstrip().rstrip()

      if include_header_and_footer:
        # Ensure the header and footer are in the string
        if not public_cert.startswith(CERTIFICATE_HEADER):
          public_cert = CERTIFICATE_HEADER + '\n' + public_cert

        if not public_cert.endswith(CERTIFICATE_FOOTER):
          public_cert = public_cert + '\n' + CERTIFICATE_FOOTER
      else:
        # Ensure the header and footer are not in the string
        if public_cert.startswith(CERTIFICATE_HEADER):
          public_cert = public_cert[len(CERTIFICATE_HEADER):]

        if public_cert.endswith(CERTIFICATE_FOOTER):
          public_cert = public_cert[:len(public_cert) - len(CERTIFICATE_FOOTER)]

      # Remove any leading and ending line breaks
      public_cert = public_cert.lstrip().rstrip()

      if remove_line_breaks:
        public_cert = public_cert.replace('\n', '')

    return public_cert


class AmbariLDAPConfiguration:
  """
  AmbariLDAPConfiguration encapsulates the LDAP configuration data specified in the ambari-server-configuration data.
  The public API of class mirrors the following Java class's public API (except for trust store related API and getLdapServerProperties which we do not need in Pyton side):
  org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration
  """

  def __init__(self, ldap_properties):
    self.ldap_properties = ldap_properties

  def is_ldap_enabled(self):
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.authentication.enabled')

  def get_server_host(self):
    '''
    :return: The LDAP URL host used for connecting to an LDAP server when authenticating users or None if ldap-configuration/ambari.ldap.connectivity.server.host is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.server.host')

  def get_server_port(self):
    '''
    :return: The LDAP URL port (as an integer) used for connecting to an LDAP server when authenticating users or None if ldap-configuration/ambari.ldap.connectivity.server.port is not specified
    '''
    ldap_server_port = _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.server.port')
    return int(ldap_server_port) if ldap_server_port is not None else None

  def get_server_url(self):
    '''
    :return: The LDAP URL (host:port) used for connecting to an LDAP server when authenticating users
    '''
    ldap_host = self.get_server_host()
    ldap_port = self.get_server_port()
    return None if ldap_host is None or ldap_port is None else '{}:{}'.format(ldap_host,ldap_port)

  def get_secondary_server_host(self):
    '''
    :return: A second LDAP URL host to use as a backup when authenticating users or None if ldap-configuration/ambari.ldap.connectivity.secondary.server.host is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.secondary.server.host')

  def get_secondary_server_port(self):
    '''
    :return: A second LDAP URL port (as an integer) to use as a backup when authenticating users or None if ldap-configuration/ambari.ldap.connectivity.secondary.server.port is not specified
    '''
    ldap_server_secondary_port = _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.secondary.server.port')
    return int(ldap_server_secondary_port) if ldap_server_secondary_port is not None else None

  def get_secondary_server_url(self):
    '''
    :return: A second LDAP URL (host:port) used for connecting to an LDAP server when authenticating users
    '''
    ldap_host = self.get_secondary_server_host()
    ldap_port = self.get_secondary_server_port()
    return None if ldap_host is None or ldap_port is None else '{}:{}'.format(ldap_host,ldap_port)

  def is_use_ssl(self):
    '''
    :return: Whether to use LDAP over SSL (LDAPS).
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.use_ssl')

  def is_anonymous_bind(self):
    '''
    :return: Whether LDAP requests can connect anonymously or if a managed user is required to connect
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.anonymous_bind')

  def get_bind_dn(self):
    '''
    :return: The DN of the manager account to use when binding to LDAP if anonymous binding is disabled or None if ldap-configuration/ is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.bind_dn')

  def get_bind_password(self):
    '''
    :return: The password for the manager account used to bind to LDAP if anonymous binding is disabled
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.connectivity.bind_password')

  def get_dn_attribute(self):
    '''
    :return: The attribute used for determining what the distinguished name property is or None if ldap-configuration/ambari.ldap.attributes.dn_attr is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.dn_attr')

  def get_user_object_class(self):
    '''
    :return: The class to which user objects in LDAP belong or None if ldap-configuration/ambari.ldap.attributes.user.object_class is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.user.object_class')

  def get_user_name_attribute(self):
    '''
    :return: The attribute used for determining the user name, such as 'uid' or None if ldap-configuration/ambari.ldap.attributes.user.name_attr is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.user.name_attr')

  def get_user_search_base(self):
    '''
    :return: The base DN to use when filtering LDAP users and groups. This is only used when LDAP authentication is enabled or None if ldap-configuration/ambari.ldap.attributes.user.search_base is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.user.search_base')

  def get_group_object_class(self):
    '''
    :return: The LDAP object class value that defines groups in the directory service or None if ldap-configuration/ambari.ldap.attributes.group.object_class is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.group.object_class')

  def get_group_name_attribute(self):
    '''
    :return: The attribute used to determine the group name in LDAP  or None if ldap-configuration/ambari.ldap.attributes.group.name_attr is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.group.name_attr')

  def get_group_member_attribute(self):
    '''
    :return: The LDAP attribute which identifies group membership  or None if ldap-configuration/ambari.ldap.attributes.group.member_attr is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.group.member_attr')

  def get_group_search_base(self):
    '''
    :return: The base DN to use when filtering LDAP users and groups or None if ldap-configuration/ambari.ldap.attributes.group.search_base is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.attributes.group.search_base')

  def get_group_mapping_rules(self):
    '''
    :return: A comma-separate list of groups which would give a user administrative access to Ambari when syncing from LDAP or None if ldap-configuration/ambari.ldap.advanced.group_mapping_rules is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.group_mapping_rules')

  def get_user_search_filter(self):
    '''
    :return: A filter used to lookup a user in LDAP based on the Ambari user name or None if ldap-configuration/ambari.ldap.advanced.user_search_filter is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.user_search_filter')

  def get_user_member_replace_pattern(self):
    '''
    :return: The regex pattern to use when replacing the user member attribute ID value with a placeholder or None if ldap-configuration/ambari.ldap.advanced.user_member_replace_pattern is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.user_member_replace_pattern')

  def get_user_member_filter(self):
    '''
    :return: The filter to use for syncing user members of a group from LDAP or None if ldap-configuration/ambari.ldap.advanced.user_member_filter is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.user_member_filter')

  def get_group_search_filter(self):
    '''
    :return: The DN to use when searching for LDAP groups or None if ldap-configuration/ambari.ldap.advanced.group_search_filter is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.group_search_filter')

  def get_group_member_replace_pattern(self):
    '''
    :return: The regex pattern to use when replacing the group member attribute ID value with a placeholder or None if ldap-configuration/ambari.ldap.advanced.group_member_replace_pattern is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.group_member_replace_pattern')

  def get_group_member_filter(self):
    '''
    :return: The F=filter to use for syncing group members of a group from LDAP or None if ldap-configuration/ambari.ldap.advanced.group_member_filter is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.group_member_filter')

  def is_force_lower_case_user_names(self):
    '''
    :return: Whether to force the ldap user name to be lowercase or leave as-is
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.force_lowercase_usernames')

  def is_pagination_enabled(self):
    '''
    :return: Whether results from LDAP are paginated when requested
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.pagination_enabled')

  def is_follow_referral_handling(self):
    '''
    :return: Whether to follow LDAP referrals to other URLs when the LDAP controller doesn't have the requested object
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.referrals')

  def is_disable_endpoint_identification(self):
    '''
    :return: Whether to disable endpoint identification (hostname verification) during SSL handshake while updating from LDAP
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.disable_endpoint_identification')

  def is_ldap_alternate_user_search_enabled(self):
    '''
    :return: Whether a secondary (alternate) LDAP user search filer is used if the primary filter fails to find a user
    '''
    return "true" == _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.alternate_user_search_enabled')

  def get_alternate_user_search_filter(self):
    '''
    :return: An alternate LDAP user search filter which can be used if 'ldap_alternate_user_search_enabled' is enabled and the primary filter fails to find a user
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.alternate_user_search_filter')

  def get_sync_collision_handling_behavior(self):
    '''
    :return: How to handle username collision while updating from LDAP or None if ldap-configuration/ambari.ldap.advanced.collision_behavior is not specified
    '''
    return _get_from_dictionary(self.ldap_properties, 'ambari.ldap.advanced.collision_behavior')

  def is_managing_services(self):
    """
    Tests the configuration data to determine if Ambari should be configuring services to enable LDAP integration.

    The relevant property is "ldap-configuration/ambari.ldap.manage_services", which is expected
    to be a "true" or "false".

    :return: True, if Ambari should manage services' LDAP configurations
    """
    return "true" == _get_from_dictionary(self.ldap_properties, "ambari.ldap.manage_services")

  def get_services_to_enable(self):
    """
    Safely gets the list of services that Ambari should enabled for LDAP.

    The returned value is a list of the relevant service names converted to lowercase.

    :return: a list of service names converted to lowercase
    """
    ldap_enabled_services = _get_from_dictionary(self.ldap_properties, "ambari.ldap.enabled_services")

    return [x.strip().lower() for x in ldap_enabled_services.strip().split(",")] \
      if ldap_enabled_services \
      else []

  def should_enable_ldap(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to enable LDAP integration.

    The relevant property is "ldap-configuration/ambari.ldap.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled or '*' indicating ALL installed services.

    :param service_name: the name of the service to test
    :return: True, if LDAP should be enabled; False, otherwise
    """
    if self.is_managing_services():
      services_to_enable = self.get_services_to_enable()
      return "*" in services_to_enable or service_name.lower() in services_to_enable
    else:
      return False

  def should_disable_ldap(self, service_name):
    """
    Tests the configuration data to determine if the specified service should be configured by
    Ambari to disable LDAP integration.

    The relevant property is "ldap-configuration/ambari.ldap.enabled_services", which is expected
    to be a comma-delimited list of services to be enabled or '*' indicating ALL installed services.

    :param service_name: the name of the service to test
    :return: True, if LDAP should be disabled; False, otherwise
    """
    if self.is_managing_services():
      services_to_enable = self.get_services_to_enable()
      return "*" not in services_to_enable and service_name.lower() not in services_to_enable
    else:
      return False
