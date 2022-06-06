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

import httplib
import ssl
import socket
import urllib2

from ambari_commons.logging_utils import print_warning_msg
from resource_management.core.exceptions import Fail

# overrides default httplib.HTTPSConnection implementation to use specified ssl version
class HTTPSConnectionWithCustomSslVersion(httplib.HTTPSConnection):
  def __init__(self, host, port, ssl_version, **kwargs):
    httplib.HTTPSConnection.__init__(self, host, port, **kwargs)
    self.ssl_version = ssl_version

  def connect(self):
    conn_socket = socket.create_connection((self.host, self.port),
                                    self.timeout)
    if getattr(self, '_tunnel_host', None):
      self.sock = conn_socket
      self._tunnel()

    self.sock = ssl.wrap_socket(conn_socket, self.key_file, self.cert_file,
                                ssl_version=self.ssl_version)

def get_http_connection(host, port, https_enabled=False, ca_certs=None, ssl_version = ssl.PROTOCOL_SSLv23):
  if https_enabled:
    if ca_certs:
      check_ssl_certificate_and_return_ssl_version(host, port, ca_certs, ssl_version)
    return HTTPSConnectionWithCustomSslVersion(host, port, ssl_version)
  else:
    return httplib.HTTPConnection(host, port)

def check_ssl_certificate_and_return_ssl_version(host, port, ca_certs, ssl_version = ssl.PROTOCOL_SSLv23):
  try:
    ssl.get_server_certificate((host, port), ssl_version=ssl_version, ca_certs=ca_certs)
  except ssl.SSLError as ssl_error:
    raise Fail("Failed to verify the SSL certificate for https://{0}:{1} with CA certificate in {2}. Error : {3}"
             .format(host, port, ca_certs, str(ssl_error)))
  return ssl_version


def reconfigure_urllib2_opener(ignore_system_proxy=False):
  """
  Reconfigure urllib opener

  :type ignore_system_proxy bool
  """

  if ignore_system_proxy:
    proxy_handler = urllib2.ProxyHandler({})
    opener = urllib2.build_opener(proxy_handler)
    urllib2.install_opener(opener)
