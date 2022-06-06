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
import time
import urllib2
import traceback
import logging

from resource_management.core.environment import Environment
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import DEFAULT_KERBEROS_KINIT_TIMER_MS
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER


RESULT_CODE_OK = "OK"
RESULT_CODE_CRITICAL = "CRITICAL"
RESULT_CODE_UNKNOWN = "UNKNOWN"

OK_MESSAGE = "WebHCat status was OK ({0:.3f}s response from {1})"
CRITICAL_CONNECTION_MESSAGE = "Connection failed to {0} + \n{1}"
CRITICAL_HTTP_MESSAGE = "HTTP {0} response from {1} \n{2}"
CRITICAL_WEBHCAT_STATUS_MESSAGE = 'WebHCat returned an unexpected status of "{0}"'
CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE = "Unable to determine WebHCat health from unexpected JSON response"

TEMPLETON_PORT_KEY = '{{webhcat-site/templeton.port}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
WEBHCAT_PRINCIPAL_KEY = '{{webhcat-site/templeton.kerberos.principal}}'
WEBHCAT_KEYTAB_KEY = '{{webhcat-site/templeton.kerberos.keytab}}'

SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_PRINCIPAL_KEY = '{{cluster-env/smokeuser_principal_name}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

WEBHCAT_OK_RESPONSE = 'ok'
WEBHCAT_PORT_DEFAULT = 50111

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0
CURL_CONNECTION_TIMEOUT_DEFAULT = str(int(CONNECTION_TIMEOUT_DEFAULT))

# default keytab location
SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY = 'default.smoke.keytab'
SMOKEUSER_KEYTAB_DEFAULT = '/etc/security/keytabs/smokeuser.headless.keytab'

# default smoke principal
SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY = 'default.smoke.principal'
SMOKEUSER_PRINCIPAL_DEFAULT = 'ambari-qa@EXAMPLE.COM'

# default smoke user
SMOKEUSER_DEFAULT = 'ambari-qa'
logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (TEMPLETON_PORT_KEY, SECURITY_ENABLED_KEY, SMOKEUSER_KEYTAB_KEY,SMOKEUSER_PRINCIPAL_KEY,
          KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, SMOKEUSER_KEY)


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

  webhcat_port = WEBHCAT_PORT_DEFAULT
  if TEMPLETON_PORT_KEY in configurations:
    webhcat_port = int(configurations[TEMPLETON_PORT_KEY])

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = configurations[SECURITY_ENABLED_KEY].lower() == 'true'

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  curl_connection_timeout = CURL_CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])
    curl_connection_timeout = str(int(connection_timeout))


  # the alert will always run on the webhcat host
  if host_name is None:
    host_name = socket.getfqdn()

  smokeuser = SMOKEUSER_DEFAULT

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  # webhcat always uses http, never SSL
  query_url = "http://{0}:{1}/templeton/v1/status?user.name={2}".format(host_name, webhcat_port, smokeuser)

  # initialize
  total_time = 0
  json_response = {}

  if security_enabled:
    try:
      # defaults
      smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
      smokeuser_principal = SMOKEUSER_PRINCIPAL_DEFAULT

      # check script params
      if SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY in parameters:
        smokeuser_principal = parameters[SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY]
      if SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY in parameters:
        smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY]

      # check configurations last as they should always take precedence
      if SMOKEUSER_PRINCIPAL_KEY in configurations:
        smokeuser_principal = configurations[SMOKEUSER_PRINCIPAL_KEY]
      if SMOKEUSER_KEYTAB_KEY in configurations:
        smokeuser_keytab = configurations[SMOKEUSER_KEYTAB_KEY]

      # Get the configured Kerberos executable search paths, if any
      kerberos_executable_search_paths = None
      if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]

      kinit_timer_ms = parameters.get(KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS)

      env = Environment.get_instance()
      stdout, stderr, time_millis = curl_krb_request(env.tmp_dir, smokeuser_keytab, smokeuser_principal,
        query_url, "webhcat_alert_cc_", kerberos_executable_search_paths, True,
        "WebHCat Server Status", smokeuser, connection_timeout=curl_connection_timeout,
        kinit_timer_ms = kinit_timer_ms)

      # check the response code
      response_code = int(stdout)

      # 0 indicates no connection
      if response_code == 0:
        label = CRITICAL_CONNECTION_MESSAGE.format(query_url, traceback.format_exc())
        return (RESULT_CODE_CRITICAL, [label])

      # any other response aside from 200 is a problem
      if response_code != 200:
        label = CRITICAL_HTTP_MESSAGE.format(response_code, query_url, traceback.format_exc())
        return (RESULT_CODE_CRITICAL, [label])

      # now that we have the http status and it was 200, get the content
      stdout, stderr, total_time = curl_krb_request(env.tmp_dir, smokeuser_keytab, smokeuser_principal,
        query_url, "webhcat_alert_cc_", kerberos_executable_search_paths,
        False, "WebHCat Server Status", smokeuser, connection_timeout=curl_connection_timeout,
        kinit_timer_ms = kinit_timer_ms)

      json_response = json.loads(stdout)
    except:
      return (RESULT_CODE_CRITICAL, [traceback.format_exc()])
  else:
    url_response = None

    try:
      # execute the query for the JSON that includes WebHCat status
      start_time = time.time()
      url_response = urllib2.urlopen(query_url, timeout=connection_timeout)
      total_time = time.time() - start_time

      json_response = json.loads(url_response.read())
    except urllib2.HTTPError as httpError:
      label = CRITICAL_HTTP_MESSAGE.format(httpError.code, query_url, traceback.format_exc())
      return (RESULT_CODE_CRITICAL, [label])
    except:
      label = CRITICAL_CONNECTION_MESSAGE.format(query_url, traceback.format_exc())
      return (RESULT_CODE_CRITICAL, [label])
    finally:
      if url_response is not None:
        try:
          url_response.close()
        except:
          pass


  # if status is not in the response, we can't do any check; return CRIT
  if 'status' not in json_response:
    return (RESULT_CODE_CRITICAL, [CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE + str(json_response)])


  # URL response received, parse it
  try:
    webhcat_status = json_response['status']
  except:
    return (RESULT_CODE_CRITICAL, [CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE + "\n" + traceback.format_exc()])


  # proper JSON received, compare against known value
  if webhcat_status.lower() == WEBHCAT_OK_RESPONSE:
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE.format(total_time, query_url)
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_WEBHCAT_STATUS_MESSAGE.format(webhcat_status)

  return (result_code, [label])
