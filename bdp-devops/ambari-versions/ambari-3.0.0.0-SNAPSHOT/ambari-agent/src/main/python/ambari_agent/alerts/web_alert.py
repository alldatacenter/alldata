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
import time
import urllib2

from urllib2 import HTTPError

from tempfile import gettempdir
from alerts.base_alert import BaseAlert
from collections import namedtuple
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions.get_path_from_url import get_path_from_url
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from ambari_commons import OSCheck
from ambari_commons.inet_utils import resolve_address, ensure_ssl_using_protocol, get_host_from_url
from ambari_commons.constants import AGENT_TMP_DIR
from ambari_agent.AmbariConfig import AmbariConfig

logger = logging.getLogger(__name__)

# default timeout
DEFAULT_CONNECTION_TIMEOUT = 5

WebResponse = namedtuple('WebResponse', 'status_code time_millis error_msg')

ensure_ssl_using_protocol(
    AmbariConfig.get_resolved_config().get_force_https_protocol_name(),
    AmbariConfig.get_resolved_config().get_ca_cert_file_path()
)

class WebAlert(BaseAlert):

  def __init__(self, alert_meta, alert_source_meta, config):
    super(WebAlert, self).__init__(alert_meta, alert_source_meta, config)

    connection_timeout = DEFAULT_CONNECTION_TIMEOUT

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
    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)

    logger.debug("[Alert][{0}] Calculated web URI to be {1} (ssl={2})".format(
      self.get_name(), alert_uri.uri, str(alert_uri.is_ssl_enabled)))

    url = self._build_web_query(alert_uri)

    # substitute 0.0.0.0 in url with actual fqdn
    url = url.replace('0.0.0.0', self.host_name)

    web_response = self._make_web_request(url)
    status_code = web_response.status_code
    time_seconds = web_response.time_millis / 1000
    error_message = web_response.error_msg

    if status_code == 0:
      return (self.RESULT_CRITICAL, [status_code, url, time_seconds, error_message])

    # check explicit listed codes
    if self.uri_property_keys.acceptable_codes and status_code in self.uri_property_keys.acceptable_codes:
      return (self.RESULT_OK, [status_code, url, time_seconds])

    # anything that's less than 400 is OK
    if status_code < 400:
      return (self.RESULT_OK, [status_code, url, time_seconds])

    # everything else is WARNING
    return (self.RESULT_WARNING, [status_code, url, time_seconds, error_message])


  def _build_web_query(self, alert_uri):
    """
    Builds a URL out of the URI structure. If the URI is already a URL of
    the form http[s]:// then this will return the URI as the URL; otherwise,
    it will build the URL from the URI structure's elements
    """
    # shortcut if the supplied URI starts with the information needed
    string_uri = str(alert_uri.uri)
    if string_uri.startswith('http://') or string_uri.startswith('https://'):
      return alert_uri.uri

    uri_path = None
    if string_uri and string_uri != str(None):
      uri_path = get_path_from_url(string_uri)

    # start building the URL manually
    host = get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    # maybe slightly realistic
    port = 80
    if alert_uri.is_ssl_enabled is True:
      port = 443

    # extract the port
    try:
      port = int(get_port_from_url(alert_uri.uri))
    except:
      pass

    scheme = 'http'
    if alert_uri.is_ssl_enabled is True:
      scheme = 'https'

    if OSCheck.is_windows_family():
      # on windows 0.0.0.0 is invalid address to connect but on linux it resolved to 127.0.0.1
      host = resolve_address(host)

    if uri_path:
      return "{0}://{1}:{2}/{3}".format(scheme, host, str(port), uri_path)
    else:
      return "{0}://{1}:{2}".format(scheme, host, str(port))

  def _make_web_request(self, url):
    """
    Makes an http(s) request to a web resource and returns the http code. If
    there was an error making the request, return 0 for the status code.
    """
    error_msg = None
    try:
      response_code = 0
      kerberos_keytab = None
      kerberos_principal = None

      configurations = self.configuration_builder.get_configuration(self.cluster_id, None, None)

      if self.uri_property_keys.kerberos_principal is not None:
        kerberos_principal = self._get_configuration_value(configurations,
          self.uri_property_keys.kerberos_principal)

        if kerberos_principal is not None:
          # substitute _HOST in kerberos principal with actual fqdn
          kerberos_principal = kerberos_principal.replace('_HOST', self.host_name)

      if self.uri_property_keys.kerberos_keytab is not None:
        kerberos_keytab = self._get_configuration_value(configurations, self.uri_property_keys.kerberos_keytab)

      security_enabled = self._get_configuration_value(configurations, '{{cluster-env/security_enabled}}')

      if kerberos_principal is not None and kerberos_keytab is not None \
        and security_enabled is not None and security_enabled.lower() == "true":

        tmp_dir = AGENT_TMP_DIR
        if tmp_dir is None:
          tmp_dir = gettempdir()

        # Get the configured Kerberos executables search paths, if any
        kerberos_executable_search_paths = self._get_configuration_value(configurations, '{{kerberos-env/executable_search_paths}}')
        smokeuser = self._get_configuration_value(configurations, '{{cluster-env/smokeuser}}')

        response_code, error_msg, time_millis = curl_krb_request(tmp_dir, kerberos_keytab, kerberos_principal, url,
          "web_alert", kerberos_executable_search_paths, True, self.get_name(), smokeuser,
          connection_timeout=self.curl_connection_timeout, kinit_timer_ms = self.kinit_timeout)
      else:
        # kerberos is not involved; use urllib2
        response_code, time_millis, error_msg = self._make_web_request_urllib(url)

      return WebResponse(status_code=response_code, time_millis=time_millis,
        error_msg=error_msg)

    except Exception, exception:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Unable to make a web request.".format(self.get_name()))

      return WebResponse(status_code=0, time_millis=0, error_msg=str(exception))


  def _make_web_request_urllib(self, url):
    """
    Make a web request using urllib2. This function does not handle exceptions.
    :param url: the URL to request
    :return: a tuple of the response code and the total time in ms
    """
    response = None
    error_message = None

    start_time = time.time()

    try:
      response = urllib2.urlopen(url, timeout=self.connection_timeout)
      response_code = response.getcode()
      time_millis = time.time() - start_time

      return response_code, time_millis, error_message
    except HTTPError, httpError:
      time_millis = time.time() - start_time
      error_message = str(httpError)

      return httpError.code, time_millis, error_message
    finally:
      if response is not None:
        try:
          response.close()
        except Exception, exception:
          if logger.isEnabledFor(logging.DEBUG):
            logger.exception("[Alert][{0}] Unable to close socket connection".format(self.get_name()))


  def _get_reporting_text(self, state):
    '''
    Gets the default reporting text to use when the alert definition does not
    contain any.
    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    if state == self.RESULT_CRITICAL:
      return 'Connection failed to {1}'

    return 'HTTP {0} response in {2:.4f} seconds'
