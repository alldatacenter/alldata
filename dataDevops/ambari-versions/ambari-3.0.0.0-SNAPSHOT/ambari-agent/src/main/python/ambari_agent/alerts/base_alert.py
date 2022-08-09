#!/usr/bin/env python

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

import logging
import re
import time
from collections import namedtuple

logger = logging.getLogger(__name__)

# create a named tuple to return both the concrete URI and SSL flag
AlertUri = namedtuple('AlertUri', 'uri is_ssl_enabled')

class BaseAlert(object):
  CONFIG_KEY_REGEXP = re.compile('{{(\S+?)}}')
  # will force a kinit even if klist says there are valid tickets (4 hour default)
  _DEFAULT_KINIT_TIMEOUT = 14400000

  RESULT_OK = "OK"
  RESULT_WARNING = "WARNING"
  RESULT_CRITICAL = "CRITICAL"
  RESULT_UNKNOWN = "UNKNOWN"
  RESULT_SKIPPED = "SKIPPED"

  HA_NAMESERVICE_PARAM = "{{ha-nameservice}}"
  HA_ALIAS_PARAM = "{{alias}}"

  def __init__(self, alert_meta, alert_source_meta, config):
    self.alert_meta = alert_meta
    self.alert_source_meta = alert_source_meta
    self.cluster_name = ''
    self.cluster_id = None
    self.host_name = ''
    self.public_host_name = ''
    self.config = config

  def interval(self):
    """ gets the defined interval this check should run """
    if not self.alert_meta.has_key('interval'):
      return 1
    else:
      interval = self.alert_meta['interval']
      return 1 if interval < 1 else interval

  def get_definition_id(self):
    """
    gets the id of definition (a number)
    """
    return self.alert_meta['definitionId']

  def is_enabled(self):
    """
    gets whether the definition is enabled
    """
    return self.alert_meta['enabled']


  def get_name(self):
    """
    gets the unique name of the alert definition
    """
    return self.alert_meta['name']


  def get_uuid(self):
    """
    gets the unique has of the alert definition
    """
    return self.alert_meta['uuid']


  def set_helpers(self, collector, cluster_configuration_cache, configuration_builder):
    """
    sets helper objects for alerts without having to use them in a constructor
    """
    self.collector = collector
    self.cluster_configuration_cache = cluster_configuration_cache
    self.configuration_builder = configuration_builder


  def set_cluster(self, cluster_name, cluster_id, host_name, public_host_name=None):
    """ sets cluster information for the alert """
    self.cluster_name = cluster_name
    self.cluster_id = str(cluster_id)
    self.host_name = host_name
    self.public_host_name = host_name
    if public_host_name:
      self.public_host_name = public_host_name


  def _get_alert_meta_value_safely(self, meta_key):
    """
    safe way to get a value when outputting result json.  will not throw an exception
    """
    if self.alert_meta.has_key(meta_key):
      return self.alert_meta[meta_key]
    else:
      return None


  def collect(self):
    """ method used for collection.  defers to _collect() """

    res = (BaseAlert.RESULT_UNKNOWN, [])
    res_base_text = None

    try:
      res = self._collect()
      result_state = res[0]
      reporting_state = result_state.lower()

      # it's possible that the alert definition doesn't have reporting; safely
      # check for it and fallback to default text if it doesn't exist
      if ('reporting' in self.alert_source_meta) and \
          (reporting_state in self.alert_source_meta['reporting']) and \
          ('text' in self.alert_source_meta['reporting'][reporting_state]):
          res_base_text = self.alert_source_meta['reporting'][reporting_state]['text']

      if res_base_text is None:
        res_base_text = self._get_reporting_text(result_state)

    except Exception as exception:
      message = "[Alert][{0}] Unable to execute alert. {1}".format(
        self.get_name(), str(exception))

      # print the exception if in DEBUG, otherwise just log the warning
      # if logger.isEnabledFor(logging.DEBUG):
      logger.exception(message)
      # else:
      #  logger.warning(message)

      res = (BaseAlert.RESULT_UNKNOWN, [str(exception)])
      res_base_text = "{0}"


    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("[Alert][{0}] result = {1}".format(self.get_name(), str(res)))

    data = {}
    data['name'] = self._get_alert_meta_value_safely('name')
    data['clusterId'] = self.cluster_id
    data['timestamp'] = long(time.time() * 1000)
    data['definitionId'] = self.get_definition_id()

    try:
      data['state'] = res[0]

      # * is the splat operator, which flattens a collection into positional arguments
      # flatten the array and then try formatting it
      try:
        data['text'] = res_base_text.format(*res[1])
      except ValueError, value_error:
        logger.warn("[Alert][{0}] - {1}".format(self.get_name(), str(value_error)))

        # if there is a ValueError, it's probably because the text doesn't match the type of
        # positional arguemtns (ie {0:d} with a float)
        res_base_text = res_base_text.replace("d}", "s}")
        data_as_strings = map(str, res[1])
        data['text'] = res_base_text.format(*data_as_strings)

      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("[Alert][{0}] text = {1}".format(self.get_name(), data['text']))
    except Exception, exception:
      logger.exception("[Alert][{0}] - The alert's data is not properly formatted".format(self.get_name()))

      # if there's a problem with getting the data returned from collect() then mark this
      # alert as UNKNOWN
      data['state'] = self.RESULT_UNKNOWN
      data['text'] = "There is a problem with the alert definition: {0}".format(str(exception))
    finally:
      # put the alert into the collector so it can be collected on the next run
      self.collector.put(self.cluster_name, data)


  def _get_configuration_value(self, configurations, key):
    """
    Gets the value of the specified configuration key from the cache. The key
    should be of the form {{foo-bar/baz}}. If the key given is not a lookup key
    and is instead a constant, such as "foo" or "5", then the constant is
    returned.

    If the key contains more than 1 parameter to lookup, then each match is
    looked up and replaced.

    If the value does not exist in the configs, then return None to indicate
    that this key could not be found.

    This should turn {{hdfs-site/value}}/whatever/{{hdfs-site/value2}}
    into
    value/whatever/value2

    :return:  the resolved value or None if any of the placeholder parameters
              does not exist in the configs
    """
    if key is None:
      return None

    # parse {{foo-bar/baz}}/whatever/{{foobar-site/blah}}
    # into
    # ['foo-bar/baz', 'foobar-site/blah']
    placeholder_keys = BaseAlert.CONFIG_KEY_REGEXP.findall(key)

    # if none found, then return the original
    if placeholder_keys is None or len(placeholder_keys) == 0:
      return key

    # for every match, get its configuration value and replace it in the key
    resolved_key = key
    for placeholder_key in placeholder_keys:
      value = self.get_configuration_value(configurations, placeholder_key)

      # if any of the placeholder keys is missing from the configuration, then
      # return None as per the contract of this function
      if value is None:
        return None

      # it's possible that a dictionary was request (ie {{hdfs-site}} instead
      # of {{hdfs-site/foo}} - in which case, we should just return the
      # dictionary as is
      if isinstance(value, dict):
        return value

      # {{foo-bar/baz}}/whatever -> http://server/whatever
      resolved_key = resolved_key.replace("{{%s}}" % placeholder_key, value)

    return resolved_key

  def get_configuration_value(self, configurations, key):
    """
    Gets a value from the cluster configuration map for the given cluster and
    key. The key is expected to be of the form 'foo-bar/baz' or
    'foo-bar/bar-baz/foobarbaz' where every / denotes a new mapping
    :param key:  a lookup key, like 'foo-bar/baz'
    :return: the value, or None if not found
    """
    if not key.startswith("/"):
      key = "/configurations/" + key

    try:
      curr_dict = configurations
      subdicts = filter(None, key.split('/'))

      for layer_key in subdicts:
        curr_dict = curr_dict[layer_key]

      return curr_dict
    except KeyError:
      logger.debug("Cache miss for configuration property {0}".format(key))
      return None


  def _lookup_uri_property_keys(self, uri_structure):
    """
    Loads the configuration lookup keys that the URI structure needs. This
    will return a named tuple that contains the keys needed to lookup
    parameterized URI values from the cached configuration.
    The URI structure looks something like:

    "uri":{
      "http": foo,
      "https": bar,
      ...
    }
    """

    if uri_structure is None:
      return None

    acceptable_codes_key = None
    http_key = None
    https_key = None
    https_property_key = None
    https_property_value_key = None
    default_port = None
    kerberos_keytab = None
    kerberos_principal = None
    ha_nameservice = None
    ha_alias_key = None
    ha_http_pattern = None
    ha_https_pattern = None

    if 'acceptable_codes' in uri_structure:
      acceptable_codes_key = uri_structure['acceptable_codes']

    if 'http' in uri_structure:
      http_key = uri_structure['http']

    if 'https' in uri_structure:
      https_key = uri_structure['https']

    if 'https_property' in uri_structure:
      https_property_key = uri_structure['https_property']

    if 'https_property_value' in uri_structure:
      https_property_value_key = uri_structure['https_property_value']

    if 'default_port' in uri_structure:
      default_port = uri_structure['default_port']

    if 'kerberos_keytab' in uri_structure:
      kerberos_keytab = uri_structure['kerberos_keytab']

    if 'kerberos_principal' in uri_structure:
      kerberos_principal = uri_structure['kerberos_principal']

    if 'high_availability' in uri_structure:
      ha = uri_structure['high_availability']

      if 'nameservice' in ha:
        ha_nameservice = ha['nameservice']

      if 'alias_key' in ha:
        ha_alias_key = ha['alias_key']

      if 'http_pattern' in ha:
        ha_http_pattern = ha['http_pattern']

      if 'https_pattern' in ha:
        ha_https_pattern = ha['https_pattern']


    AlertUriLookupKeys = namedtuple('AlertUriLookupKeys',
      'acceptable_codes http https https_property https_property_value default_port '
      'kerberos_keytab kerberos_principal '
      'ha_nameservice ha_alias_key ha_http_pattern ha_https_pattern')

    alert_uri_lookup_keys = AlertUriLookupKeys(
      acceptable_codes=acceptable_codes_key,
      http=http_key,
      https=https_key,
      https_property=https_property_key,
      https_property_value=https_property_value_key, default_port=default_port,
      kerberos_keytab=kerberos_keytab, kerberos_principal=kerberos_principal,
      ha_nameservice=ha_nameservice, ha_alias_key=ha_alias_key,
      ha_http_pattern=ha_http_pattern, ha_https_pattern=ha_https_pattern
    )

    return alert_uri_lookup_keys


  def _get_uri_from_structure(self, alert_uri_lookup_keys):
    """
    Gets the URI to use by examining the URI structure from the definition.
    This will return a named tuple that has the uri and the SSL flag. The
    URI structure looks something like:

    "uri":{
      "http": foo,
      "https": bar,
      ...
    }
    """

    if alert_uri_lookup_keys is None:
      return None

    http_uri = None
    https_uri = None

    configurations = self.configuration_builder.get_configuration(self.cluster_id, None, None)

    # first thing is first; if there are HA keys then try to dynamically build
    # the property which is used to get the actual value of the uri
    # (ie dfs.namenode.http-address.c1ha.nn2)
    if alert_uri_lookup_keys.ha_nameservice is not None or alert_uri_lookup_keys.ha_alias_key is not None:
      alert_uri = self._get_uri_from_ha_structure(alert_uri_lookup_keys, configurations)
      if alert_uri is not None:
        return alert_uri

    # attempt to parse and parameterize the various URIs; properties that
    # do not exist int he lookup map are returned as None
    if alert_uri_lookup_keys.http is not None:
      http_uri = self._get_configuration_value(configurations, alert_uri_lookup_keys.http)

    if alert_uri_lookup_keys.https is not None:
      https_uri = self._get_configuration_value(configurations, alert_uri_lookup_keys.https)

    # without a URI, there's no way to create the structure we need - return
    # the default port if specified, otherwise throw an exception
    if http_uri is None and https_uri is None:
      if alert_uri_lookup_keys.default_port is not None:
        alert_uri = AlertUri(uri=alert_uri_lookup_keys.default_port, is_ssl_enabled=False)
        return alert_uri
      else:
        raise Exception("Could not determine result. Either the http or https URI must be specified.")

    # start out assuming plaintext
    uri = http_uri
    is_ssl_enabled = False

    if https_uri is not None:
      # https without http implies SSL, otherwise look it up based on the properties
      if http_uri is None:
        is_ssl_enabled = True
        uri = https_uri
      elif self._check_uri_ssl_property(alert_uri_lookup_keys, configurations):
        is_ssl_enabled = True
        uri = https_uri

    alert_uri = AlertUri(uri=uri, is_ssl_enabled=is_ssl_enabled)
    return alert_uri


  def _get_uri_from_ha_structure(self, alert_uri_lookup_keys, configurations):
    """
    Attempts to parse the HA URI structure in order to build a dynamic key
    that represents the correct host URI to check.
    :param alert_uri_lookup_keys:
    :return: the AlertUri named tuple if there is a valid HA URL, otherwise None
    """
    if alert_uri_lookup_keys is None:
      return None

    logger.debug("[Alert][{0}] HA URI structure detected in definition, attempting to lookup dynamic HA properties".format(self.get_name()))

    ha_nameservice = self._get_configuration_value(configurations, alert_uri_lookup_keys.ha_nameservice)
    ha_alias_key = alert_uri_lookup_keys.ha_alias_key
    ha_http_pattern = alert_uri_lookup_keys.ha_http_pattern
    ha_https_pattern = alert_uri_lookup_keys.ha_https_pattern
    ha_nameservice_aliases = {}

    # if HA alias key is not defined then it's not HA environment
    if ha_alias_key is None:
      return None

    if alert_uri_lookup_keys.ha_nameservice is not None:
      # if there is a HA nameservice defined, but it can not be evaluated then it's not HA environment
      if ha_nameservice is None:
        return None

      # convert dfs.ha.namenodes.{{ha-nameservice}} into dfs.ha.namenodes.c1ha
      ha_nameservices = filter(None, ha_nameservice.split(','))

      for nameservice in ha_nameservices:
        ha_alias_key_nameservice = ha_alias_key.replace(self.HA_NAMESERVICE_PARAM, nameservice)
        ha_nameservice_alias = self._get_configuration_value(configurations, ha_alias_key_nameservice)


        if ha_nameservice_alias:
          ha_nameservice_aliases[nameservice] = ha_nameservice_alias

      if not ha_nameservice_aliases:
        logger.warning("[Alert][{0}] HA nameservice value is present but there are no aliases for {1}".format(
          self.get_name(), ha_alias_key))
        return None
    else:
      ha_nameservice_aliases = {None: self._get_configuration_value(configurations, ha_alias_key)}

      # if HA nameservice is not defined then the fact that the HA alias_key could not be evaluated shows that it's not HA environment
      if ha_nameservice_aliases[None] is None:
        return None

    # determine which pattern to use (http or https)
    ha_pattern = ha_http_pattern
    is_ssl_enabled = self._check_uri_ssl_property(alert_uri_lookup_keys, configurations)
    if is_ssl_enabled:
      ha_pattern = ha_https_pattern

    # no pattern found
    if ha_pattern is None:
      logger.warning("[Alert][{0}] There is no matching http(s) pattern for the HA URI".format(
        self.get_name()))

      return None

    if self.HA_NAMESERVICE_PARAM in ha_pattern and ha_nameservice is None:
      logger.warning("[Alert][{0}] An HA URI pattern of {1} was detected, but there is no nameservice key".format(
        self.get_name(), ha_pattern))

      return None

    # for each alias, grab it and check to see if this host matches
    for nameservice, aliases in ha_nameservice_aliases.iteritems():
      for alias in aliases.split(','):

        # convert dfs.namenode.http-address.{{ha-nameservice}}.{{alias}} into
        # dfs.namenode.http-address.c1ha.{{alias}}
        ha_pattern_current = ha_pattern
        if nameservice is not None:
          ha_pattern_current = ha_pattern_current.replace(self.HA_NAMESERVICE_PARAM, nameservice)
        # convert dfs.namenode.http-address.c1ha.{{alias}} into
        # dfs.namenode.http-address.c1ha.nn1
        key = ha_pattern_current.replace(self.HA_ALIAS_PARAM, alias.strip())

        # get the host for dfs.namenode.http-address.c1ha.nn1 and see if it's
        # this host
        value = self._get_configuration_value(configurations, key)

        if value is not None and (self.host_name.lower() in value.lower() or self.public_host_name.lower() in value.lower()):
          return AlertUri(uri=value, is_ssl_enabled=is_ssl_enabled)

    return None


  def _check_uri_ssl_property(self, alert_uri_lookup_keys, configurations):
    """
    Gets whether the SSL property and value on the URI indicate an SSL
    connection.
    :param alert_uri_lookup_keys:
    :return:  True if the SSL check property and value are defined and match
              otherwise False
    """
    https_property = None
    https_property_value = None

    if alert_uri_lookup_keys.https_property is not None:
      https_property = self._get_configuration_value(configurations, alert_uri_lookup_keys.https_property)

    if alert_uri_lookup_keys.https_property_value is not None:
      https_property_value = self._get_configuration_value(configurations, alert_uri_lookup_keys.https_property_value)

    if https_property is None:
      return False

    return https_property == https_property_value


  def _collect(self):
    """
    Low level function to collect alert data.  The result is a tuple as:
    res[0] = the result code
    res[1] = the list of arguments supplied to the reporting text for the result code
    """
    # TODO: After implementation uncomment /src/test/python/ambari_agent/TestMetricAlert.py:194
    # and /src/test/python/ambari_agent/TestScriptAlert.py:52
    raise NotImplementedError


  def _get_reporting_text(self, state):
    '''
    Gets the default reporting text to use when the alert definition does not
    contain any. Subclasses can override this to return specific text.
    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    return '{0}'
