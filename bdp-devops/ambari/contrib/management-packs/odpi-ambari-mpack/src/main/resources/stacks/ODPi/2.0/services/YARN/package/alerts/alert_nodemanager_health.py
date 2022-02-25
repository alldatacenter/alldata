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

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import socket
import urllib2
import logging
import traceback
from ambari_commons import OSCheck
from ambari_commons.inet_utils import resolve_address
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import DEFAULT_KERBEROS_KINIT_TIMER_MS
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER
from resource_management.core.environment import Environment

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

NODEMANAGER_HTTP_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.address}}'
NODEMANAGER_HTTPS_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.https.address}}'
YARN_HTTP_POLICY_KEY = '{{yarn-site/yarn.http.policy}}'

OK_MESSAGE = 'NodeManager Healthy'
CRITICAL_CONNECTION_MESSAGE = 'Connection failed to {0} ({1})'
CRITICAL_HTTP_STATUS_MESSAGE = 'HTTP {0} returned from {1} ({2}) \n{3}'
CRITICAL_NODEMANAGER_STATUS_MESSAGE = 'NodeManager returned an unexpected status of "{0}"'
CRITICAL_NODEMANAGER_UNKNOWN_JSON_MESSAGE = 'Unable to determine NodeManager health from unexpected JSON response'

KERBEROS_KEYTAB = '{{yarn-site/yarn.nodemanager.webapp.spnego-keytab-file}}'
KERBEROS_PRINCIPAL = '{{yarn-site/yarn.nodemanager.webapp.spnego-principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'

NODEMANAGER_DEFAULT_PORT = 8042

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0

LOGGER_EXCEPTION_MESSAGE = "[Alert] NodeManager Health on {0} fails:"
logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NODEMANAGER_HTTP_ADDRESS_KEY,NODEMANAGER_HTTPS_ADDRESS_KEY, EXECUTABLE_SEARCH_PATHS,
  YARN_HTTP_POLICY_KEY, SMOKEUSER_KEY, KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY)
  

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  result_code = RESULT_CODE_UNKNOWN

  if configurations is None:
    return (result_code, ['There were no configurations supplied to the script.'])

  if host_name is None:
    host_name = socket.getfqdn()

  scheme = 'http'
  http_uri = None
  https_uri = None
  http_policy = 'HTTP_ONLY'

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  executable_paths = None
  if EXECUTABLE_SEARCH_PATHS in configurations:
    executable_paths = configurations[EXECUTABLE_SEARCH_PATHS]

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  kerberos_keytab = None
  if KERBEROS_KEYTAB in configurations:
    kerberos_keytab = configurations[KERBEROS_KEYTAB]

  kerberos_principal = None
  if KERBEROS_PRINCIPAL in configurations:
    kerberos_principal = configurations[KERBEROS_PRINCIPAL]
    kerberos_principal = kerberos_principal.replace('_HOST', host_name)

  if NODEMANAGER_HTTP_ADDRESS_KEY in configurations:
    http_uri = configurations[NODEMANAGER_HTTP_ADDRESS_KEY]

  if NODEMANAGER_HTTPS_ADDRESS_KEY in configurations:
    https_uri = configurations[NODEMANAGER_HTTPS_ADDRESS_KEY]

  if YARN_HTTP_POLICY_KEY in configurations:
    http_policy = configurations[YARN_HTTP_POLICY_KEY]


  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])


  # determine the right URI and whether to use SSL
  host_port = http_uri
  if http_policy == 'HTTPS_ONLY':
    scheme = 'https'

    if https_uri is not None:
      host_port = https_uri

  label = ''
  url_response = None
  node_healthy = 'false'
  total_time = 0

  # replace hostname on host fqdn to make it work on all environments
  if host_port is not None:
    if ":" in host_port:
      uri_host, uri_port = host_port.split(':')
      host_port = '{0}:{1}'.format(host_name, uri_port)
    else:
      host_port = host_name

  # some yarn-site structures don't have the web ui address
  if host_port is None:
    host_port = '{0}:{1}'.format(host_name, NODEMANAGER_DEFAULT_PORT)

  query = "{0}://{1}/ws/v1/node/info".format(scheme, host_port)

  try:
    if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
      env = Environment.get_instance()

      # curl requires an integer timeout
      curl_connection_timeout = int(connection_timeout)

      kinit_timer_ms = parameters.get(KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS)

      url_response, error_msg, time_millis  = curl_krb_request(env.tmp_dir, kerberos_keytab, kerberos_principal,
        query, "nm_health_alert", executable_paths, False, "NodeManager Health", smokeuser,
        connection_timeout=curl_connection_timeout, kinit_timer_ms = kinit_timer_ms)

      json_response = json.loads(url_response)
    else:
      # execute the query for the JSON that includes templeton status
      url_response = urllib2.urlopen(query, timeout=connection_timeout)
      json_response = json.loads(url_response.read())
  except urllib2.HTTPError, httpError:
    label = CRITICAL_HTTP_STATUS_MESSAGE.format(str(httpError.code), query,
      str(httpError), traceback.format_exc())

    return (RESULT_CODE_CRITICAL, [label])
  except:
    label = CRITICAL_CONNECTION_MESSAGE.format(query, traceback.format_exc())
    return (RESULT_CODE_CRITICAL, [label])

  # URL response received, parse it
  try:
    node_healthy = json_response['nodeInfo']['nodeHealthy']
    node_healthy_report = json_response['nodeInfo']['healthReport']

    # convert boolean to string
    node_healthy = str(node_healthy)
  except:
    return (RESULT_CODE_CRITICAL, [query + "\n" + traceback.format_exc()])
  finally:
    if url_response is not None:
      try:
        url_response.close()
      except:
        pass

  # proper JSON received, compare against known value
  if node_healthy.lower() == 'true':
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE
  elif node_healthy.lower() == 'false':
    result_code = RESULT_CODE_CRITICAL
    label = node_healthy_report
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_NODEMANAGER_STATUS_MESSAGE.format(node_healthy)

  return (result_code, [label])
