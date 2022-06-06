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

Ambari Agent

"""

__all__ = ["curl_krb_request"]
import hashlib
import logging
import os

import time

from get_kinit_path import get_kinit_path
from get_klist_path import get_klist_path
from resource_management.core import global_lock
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.get_user_call_output import get_user_call_output

HASH_ALGORITHM = hashlib.sha224
CONNECTION_TIMEOUT_DEFAULT = 10
MAX_TIMEOUT_DEFAULT = CONNECTION_TIMEOUT_DEFAULT + 2

logger = logging.getLogger()

# a dictionary of the last time that a kinit was performed for a specific cache
# dicionaries are inherently thread-safe in Python via the Global Interpreer Lock
# https://docs.python.org/2/glossary.html#term-global-interpreter-lock
_KINIT_CACHE_TIMES = {}

# the default time in between forced kinit calls (4 hours)
DEFAULT_KERBEROS_KINIT_TIMER_MS = 14400000

# a parameter which can be used to pass around the above timout value
KERBEROS_KINIT_TIMER_PARAMETER = "kerberos.kinit.timer"

def curl_krb_request(tmp_dir, keytab, principal, url, cache_file_prefix,
    krb_exec_search_paths, return_only_http_code, caller_label, user,
    connection_timeout = CONNECTION_TIMEOUT_DEFAULT,
    ca_certs = None,
    kinit_timer_ms=DEFAULT_KERBEROS_KINIT_TIMER_MS, method = '',body='',header=''):
  """
  Makes a curl request using the kerberos credentials stored in a calculated cache file. The
  cache file is created by combining the supplied principal, keytab, user, and request name into
  a unique hash.

  This function will use the klist command to determine if the cache is expired and will perform
  a kinit if necessary. Additionally, it has an internal timer to force a kinit after a
  configurable amount of time. This is to prevent boundary issues where requests hit the edge
  of a ticket's lifetime.

  :param tmp_dir: the directory to use for storing the local kerberos cache for this request.
  :param keytab: the location of the keytab to use when performing a kinit
  :param principal: the principal to use when performing a kinit
  :param url: the URL to request
  :param cache_file_prefix: an identifier used to build the unique cache name for this request.
                            This ensures that multiple requests can use the same cache.
  :param krb_exec_search_paths: the search path to use for invoking kerberos binaries
  :param return_only_http_code: True to return only the HTTP code, False to return GET content
  :param caller_label: an identifier to give context into the caller of this module (used for logging)
  :param user: the user to invoke the curl command as
  :param connection_timeout: if specified, a connection timeout for curl (default 10 seconds)
  :param ca_certs: path to certificates
  :param kinit_timer_ms: if specified, the time (in ms), before forcing a kinit even if the
                         klist cache is still valid.
  :return:
  """

  import uuid
  # backward compatibility with old code and management packs, etc. All new code need pass ca_certs explicitly
  if ca_certs is None:
    try:
      from ambari_agent.AmbariConfig import AmbariConfig
      ca_certs = AmbariConfig.get_resolved_config().get_ca_cert_file_path()
    except:
      pass
  # start off false
  is_kinit_required = False

  # Create the kerberos credentials cache (ccache) file and set it in the environment to use
  # when executing curl. Use a hash of the combination of the principal and keytab file
  # to generate a (relatively) unique cache filename so that we can use it as needed. Scope
  # this file by user in order to prevent sharing of cache files by multiple users.
  ccache_file_name = HASH_ALGORITHM("{0}|{1}".format(principal, keytab)).hexdigest()

  curl_krb_cache_path = os.path.join(tmp_dir, "curl_krb_cache")
  if not os.path.exists(curl_krb_cache_path):
    os.makedirs(curl_krb_cache_path)
  os.chmod(curl_krb_cache_path, 01777)

  ccache_file_path = "{0}{1}{2}_{3}_cc_{4}".format(curl_krb_cache_path, os.sep, cache_file_prefix, user, ccache_file_name)
  kerberos_env = {'KRB5CCNAME': ccache_file_path}

  # concurrent kinit's can cause the following error:
  # Internal credentials cache error while storing credentials while getting initial credentials
  kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
  kinit_lock.acquire()
  try:
    # If there are no tickets in the cache or they are expired, perform a kinit, else use what
    # is in the cache
    if krb_exec_search_paths:
      klist_path_local = get_klist_path(krb_exec_search_paths)
    else:
      klist_path_local = get_klist_path()

    # take a look at the last time kinit was run for the specified cache and force a new
    # kinit if it's time; this helps to avoid problems approaching ticket boundary when
    # executing a klist and then a curl
    last_kinit_time = _KINIT_CACHE_TIMES.get(ccache_file_name, 0)
    current_time = long(time.time())
    if current_time - kinit_timer_ms > last_kinit_time:
      is_kinit_required = True

    # if the time has not expired, double-check that the cache still has a valid ticket
    if not is_kinit_required:
      klist_command = "{0} -s {1}".format(klist_path_local, ccache_file_path)
      is_kinit_required = (shell.call(klist_command, user=user)[0] != 0)

    # if kinit is required, the perform the kinit
    if is_kinit_required:
      if krb_exec_search_paths:
        kinit_path_local = get_kinit_path(krb_exec_search_paths)
      else:
        kinit_path_local = get_kinit_path()

      logger.debug("Enabling Kerberos authentication for %s via GSSAPI using ccache at %s",
        caller_label, ccache_file_path)

      # kinit; there's no need to set a ticket timeout as this will use the default invalidation
      # configured in the krb5.conf - regenerating keytabs will not prevent an existing cache
      # from working correctly
      shell.checked_call("{0} -c {1} -kt {2} {3} > /dev/null".format(kinit_path_local,
        ccache_file_path, keytab, principal), user=user)

      # record kinit time
      _KINIT_CACHE_TIMES[ccache_file_name] = current_time
    else:
      # no kinit needed, use the cache
      logger.debug("Kerberos authentication for %s via GSSAPI already enabled using ccache at %s.",
        caller_label, ccache_file_path)
  finally:
    kinit_lock.release()

  # check if cookies dir exists, if not then create it
  cookies_dir = os.path.join(tmp_dir, "cookies")

  if not os.path.exists(cookies_dir):
    os.makedirs(cookies_dir)

  cookie_file_name = str(uuid.uuid4())
  cookie_file = os.path.join(cookies_dir, cookie_file_name)

  start_time = time.time()
  error_msg = None

  # setup timeouts for the request; ensure we use integers since that is what curl needs
  connection_timeout = int(connection_timeout)
  maximum_timeout = connection_timeout + 2

  ssl_options = ['-k']
  if ca_certs:
    ssl_options = ['--cacert', ca_certs]
  try:
    if return_only_http_code:
      _, curl_stdout, curl_stderr = get_user_call_output(['curl', '--location-trusted'] + ssl_options + ['--negotiate', '-u', ':', '-b', cookie_file, '-c', cookie_file, '-w',
                             '%{http_code}', url, '--connect-timeout', str(connection_timeout), '--max-time', str(maximum_timeout), '-o', '/dev/null'],
                             user=user, env=kerberos_env)
    else:
      curl_command = ['curl', '--location-trusted'] + ssl_options + ['--negotiate', '-u', ':', '-b', cookie_file, '-c', cookie_file,
                      url, '--connect-timeout', str(connection_timeout), '--max-time', str(maximum_timeout)]
      # returns response body
      if len(method) > 0 and len(body) == 0 and len(header) == 0:
        curl_command.extend(['-X', method])

      elif len(method) > 0 and len(body) == 0 and len(header) > 0:
        curl_command.extend(['-H', header, '-X', method])

      elif len(method) > 0 and len(body) > 0 and len(header) == 0:
        curl_command.extend(['-X', method, '-d', body])

      elif len(method) > 0 and len(body) > 0 and len(header) > 0:
        curl_command.extend(['-H', header, '-X', method, '-d', body])

      _, curl_stdout, curl_stderr = get_user_call_output(curl_command, user=user, env=kerberos_env)

  except Fail:
    if logger.isEnabledFor(logging.DEBUG):
      logger.exception("Unable to make a curl request for {0}.".format(caller_label))
    raise
  finally:
    if os.path.isfile(cookie_file):
      os.remove(cookie_file)

  # empty quotes evaluates to false
  if curl_stderr:
    error_msg = curl_stderr

  time_millis = time.time() - start_time

  # empty quotes evaluates to false
  if curl_stdout:
    if return_only_http_code:
      return (int(curl_stdout), error_msg, time_millis)
    else:
      return (curl_stdout, error_msg, time_millis)

  logger.debug("The curl response for %s is empty; standard error = %s",
    caller_label, str(error_msg))

  return ("", error_msg, time_millis)
