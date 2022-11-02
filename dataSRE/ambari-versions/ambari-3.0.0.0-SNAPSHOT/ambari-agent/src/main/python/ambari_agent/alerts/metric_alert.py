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

import imp
import ambari_simplejson as json
import logging
import re
import urllib2
import uuid

from  tempfile import gettempdir
from alerts.base_alert import BaseAlert
from ambari_commons.urllib_handlers import RefreshHeaderProcessor
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from ambari_commons import inet_utils
from ambari_commons.constants import AGENT_TMP_DIR

logger = logging.getLogger(__name__)

SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

# default timeout
DEFAULT_CONNECTION_TIMEOUT = 5.0
REALCODE_REGEXP = re.compile('(\{(\d+)\})')

class MetricAlert(BaseAlert):

  def __init__(self, alert_meta, alert_source_meta, config):
    super(MetricAlert, self).__init__(alert_meta, alert_source_meta, config)

    connection_timeout = DEFAULT_CONNECTION_TIMEOUT

    self.metric_info = None
    if 'jmx' in alert_source_meta:
      self.metric_info = JmxMetric(alert_source_meta['jmx'])

    # extract any lookup keys from the URI structure
    self.uri_property_keys = None
    if 'uri' in alert_source_meta:
      uri = alert_source_meta['uri']
      self.uri_property_keys = self._lookup_uri_property_keys(uri)

      if 'connection_timeout' in uri:
        connection_timeout = uri['connection_timeout']

    # python uses 5.0, CURL uses "5"
    self.connection_timeout = float(connection_timeout)
    self.curl_connection_timeout = int(connection_timeout)

    # will force a kinit even if klist says there are valid tickets (4 hour default)
    self.kinit_timeout = long(config.get('agent', 'alert_kinit_timeout', BaseAlert._DEFAULT_KINIT_TIMEOUT))


  def _collect(self):
    if self.metric_info is None:
      raise Exception("Could not determine result. Specific metric collector is not defined.")

    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)

    logger.debug("[Alert][{0}] Calculated metric URI to be {1} (ssl={2})".format(
        self.get_name(), alert_uri.uri, str(alert_uri.is_ssl_enabled)))

    host = inet_utils.get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    port = 80 # probably not very realistic
    try:
      port = int(get_port_from_url(alert_uri.uri))
    except:
      pass

    collect_result = None
    value_list = []

    if isinstance(self.metric_info, JmxMetric):
      jmx_property_values, http_code = self._load_jmx(alert_uri.is_ssl_enabled, host, port, self.metric_info)
      if not jmx_property_values and http_code in [200, 307]:
        collect_result = self.RESULT_UNKNOWN
        value_list.append('HTTP {0} response (metrics unavailable)'.format(str(http_code)))
      elif not jmx_property_values and http_code not in [200, 307]:
        raise Exception("[Alert][{0}] Unable to extract JSON from JMX response".format(self.get_name()))
      else:
        value_list.extend(jmx_property_values)
        check_value = self.metric_info.calculate(value_list)
        value_list.append(check_value)

        collect_result = self._get_result(value_list[0] if check_value is None else check_value)

        if logger.isEnabledFor(logging.DEBUG):
          logger.debug("[Alert][{0}] Resolved values = {1}".format(self.get_name(), str(value_list)))
    return (collect_result, value_list)


  def _get_result(self, value):
    ok_value = self.__find_threshold('ok')
    warn_value = self.__find_threshold('warning')
    crit_value = self.__find_threshold('critical')

    # critical values are higher
    critical_direction_up = crit_value >= warn_value

    if critical_direction_up:
      # critical values are higher
      if value >= crit_value:
        return self.RESULT_CRITICAL
      elif value >= warn_value:
        return self.RESULT_WARNING
      else:
        if ok_value is not None:
          if value >= ok_value and value < warn_value:
            return self.RESULT_OK
          else:
            return self.RESULT_UNKNOWN
        else:
          return self.RESULT_OK
    else:
      # critical values are lower
      if value <= crit_value:
        return self.RESULT_CRITICAL
      elif value <= warn_value:
        return self.RESULT_WARNING
      else:
        if ok_value is not None:
          if value <= ok_value and value > warn_value:
            return self.RESULT_OK
          else:
            return self.RESULT_UNKNOWN
        else:
          return self.RESULT_OK


  def __find_threshold(self, reporting_type):
    """ find the defined thresholds for alert values """

    if not 'reporting' in self.alert_source_meta:
      return None

    if not reporting_type in self.alert_source_meta['reporting']:
      return None

    if not 'value' in self.alert_source_meta['reporting'][reporting_type]:
      return None

    return self.alert_source_meta['reporting'][reporting_type]['value']


  def _load_jmx(self, ssl, host, port, jmx_metric):
    """ creates a JmxMetric object that holds info about jmx-based metrics """
    value_list = []
    kerberos_keytab = None
    kerberos_principal = None

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug(str(jmx_metric.property_map))

    configurations = self.configuration_builder.get_configuration(self.cluster_id, None, None)

    security_enabled = str(self._get_configuration_value(configurations, SECURITY_ENABLED_KEY)).upper() == 'TRUE'

    if self.uri_property_keys.kerberos_principal is not None:
      kerberos_principal = self._get_configuration_value(configurations,
      self.uri_property_keys.kerberos_principal)

      if kerberos_principal is not None:
        # substitute _HOST in kerberos principal with actual fqdn
        kerberos_principal = kerberos_principal.replace('_HOST', self.host_name)

    if self.uri_property_keys.kerberos_keytab is not None:
      kerberos_keytab = self._get_configuration_value(configurations, self.uri_property_keys.kerberos_keytab)

    if "0.0.0.0" in str(host):
      host = self.host_name

    for jmx_property_key, jmx_property_value in jmx_metric.property_map.iteritems():
      url = "{0}://{1}:{2}/jmx?qry={3}".format(
        "https" if ssl else "http", host, str(port), jmx_property_key)

      # use a customer header processor that will look for the non-standard
      # "Refresh" header and attempt to follow the redirect
      response = None
      content = ''
      try:
        if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
          tmp_dir = AGENT_TMP_DIR
          if tmp_dir is None:
            tmp_dir = gettempdir()

          kerberos_executable_search_paths = self._get_configuration_value(configurations, '{{kerberos-env/executable_search_paths}}')
          smokeuser = self._get_configuration_value(configurations, '{{cluster-env/smokeuser}}')

          response, error_msg, time_millis = curl_krb_request(tmp_dir, kerberos_keytab, kerberos_principal, url,
            "metric_alert", kerberos_executable_search_paths, False, self.get_name(), smokeuser,
            connection_timeout=self.curl_connection_timeout, kinit_timer_ms = self.kinit_timeout)

          content = response
        else:
          url_opener = urllib2.build_opener(RefreshHeaderProcessor())
          response = url_opener.open(url, timeout=self.connection_timeout)
          content = response.read()
      except Exception, exception:
        if logger.isEnabledFor(logging.DEBUG):
          logger.exception("[Alert][{0}] Unable to make a web request: {1}".format(self.get_name(), str(exception)))
      finally:
        # explicitely close the connection as we've seen python hold onto these
        if response is not None:
          try:
            response.close()
          except:
            logger.debug("[Alert][{0}] Unable to close JMX URL connection to {1}".format
              (self.get_name(), url))

      json_is_valid = True
      try:
        json_response = json.loads(content)
        json_data = json_response['beans'][0]
      except Exception, exception:
        json_is_valid = False
        if logger.isEnabledFor(logging.DEBUG):
          logger.exception("[Alert][{0}] Convert response to json failed or json doesn't contain needed data: {1}".
                         format(self.get_name(), str(exception)))

      if json_is_valid:
        for attr in jmx_property_value:
          if attr not in json_data:
            beans = json_response['beans']
            for jmx_prop_list_item in beans:
              if "name" in jmx_prop_list_item and jmx_prop_list_item["name"] == jmx_property_key:
                if attr not in jmx_prop_list_item:
                  raise Exception("Unable to find {0} in JSON from {1} ".format(attr, url))
                json_data = jmx_prop_list_item

          value_list.append(json_data[attr])

      http_response_code = None
      if not json_is_valid and security_enabled and kerberos_principal is not None and kerberos_keytab is not None:
        http_response_code, error_msg, time_millis = curl_krb_request(tmp_dir, kerberos_keytab,
          kerberos_principal, url, "metric_alert", kerberos_executable_search_paths, True,
          self.get_name(), smokeuser, connection_timeout=self.curl_connection_timeout,
          kinit_timer_ms = self.kinit_timeout)

    return (value_list, http_response_code)

  def _get_reporting_text(self, state):
    '''
    Always returns {0} since the result of the script alert is a rendered string.
    This will ensure that the base class takes the result string and just uses
    it directly.

    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    return '{0}'


class JmxMetric:
  DYNAMIC_CODE_TEMPLATE = """
# ensure that division yields a float, use // for integer division
from __future__ import division

def f(args):
  return {0}
"""
  def __init__(self, jmx_info):
    self.custom_module = None
    self.property_list = jmx_info['property_list']
    self.property_map = {}

    if 'value' in jmx_info:
      realcode = REALCODE_REGEXP.sub('args[\g<2>]', jmx_info['value'])

      self.custom_module =  imp.new_module(str(uuid.uuid4()))
      code = self.DYNAMIC_CODE_TEMPLATE.format(realcode)
      exec code in self.custom_module.__dict__

    for p in self.property_list:
      parts = p.split('/')
      if not parts[0] in self.property_map:
        self.property_map[parts[0]] = []
      self.property_map[parts[0]].append(parts[1])


  def calculate(self, args):
    if self.custom_module is not None:
      return self.custom_module.f(args)
    return None
