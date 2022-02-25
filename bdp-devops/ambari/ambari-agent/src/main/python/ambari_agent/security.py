#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from StringIO import StringIO
import gzip
import httplib
import urllib2
import socket
import copy
import ssl
import os
import logging
from ambari_commons import subprocess32
import ambari_simplejson as json
import pprint
import traceback
import hostname
import platform
import ambari_stomp
import threading
from ambari_stomp.adapter.websocket import WsConnection
from socket import error as socket_error

logger = logging.getLogger(__name__)

GEN_AGENT_KEY = 'openssl req -new -newkey rsa -nodes -keyout "%(keysdir)s' \
                + os.sep + '%(hostname)s.key" -subj /OU=%(hostname)s/ ' \
                '-out "%(keysdir)s' + os.sep + '%(hostname)s.csr"'
KEY_FILENAME = '%(hostname)s.key'


class VerifiedHTTPSConnection:
  """ Connecting using ssl wrapped sockets """
  def __init__(self, host, connection_url, config):
    self.two_way_ssl_required = False
    self.host = host
    self.connection_url = connection_url
    self.config = config

  def connect(self):
    self.two_way_ssl_required = self.config.isTwoWaySSLConnection(self.host)
    logger.debug("Server two-way SSL authentication required: %s", self.two_way_ssl_required)
    if self.two_way_ssl_required is True:
      logger.info(
        'Server require two-way SSL authentication. Use it instead of one-way...')

    logging.info("Connecting to {0}".format(self.connection_url))


    if not self.two_way_ssl_required:
      conn = AmbariStompConnection(self.connection_url)
      self.establish_connection(conn)
      logger.info('SSL connection established. Two-way SSL authentication is '
                  'turned off on the server.')
      return conn
    else:
      self.certMan = CertificateManager(self.config, self.host)
      self.certMan.initSecurity()
      agent_key = self.certMan.getAgentKeyName()
      agent_crt = self.certMan.getAgentCrtName()
      server_crt = self.certMan.getSrvrCrtName()

      ssl_options = {
        'keyfile': agent_key,
        'certfile': agent_crt,
        'cert_reqs': ssl.CERT_REQUIRED,
        'ca_certs': server_crt
      }

      conn = AmbariStompConnection(self.connection_url, ssl_options=ssl_options)

      try:
        self.establish_connection(conn)
        logger.info('SSL connection established. Two-way SSL authentication '
                    'completed successfully.')
      except ssl.SSLError:
        logger.error('Two-way SSL authentication failed. Ensure that '
                     'server and agent certificates were signed by the same CA '
                     'and restart the agent. '
                     '\nIn order to receive a new agent certificate, remove '
                     'existing certificate file from keys directory. As a '
                     'workaround you can turn off two-way SSL authentication in '
                     'server configuration(ambari.properties) '
                     '\nExiting..')
        raise
      return conn

  def establish_connection(self, conn):
    """
    Create a stomp connection
    """
    try:
      conn.start()
      conn.connect(wait=True)
    except Exception as ex:
      try:
        conn.disconnect()
      except:
        logger.exception("Exception during conn.disconnect()")

      if isinstance(ex, socket_error):
        logger.warn("Could not connect to {0}. {1}".format(self.connection_url, str(ex)))

      raise

class AmbariStompConnection(WsConnection):
  def __init__(self, *args, **kwargs):
    self.lock = threading.RLock()
    self.correlation_id = -1
    WsConnection.__init__(self, *args, **kwargs)

  def send(self, destination, message, content_type=None, headers=None, log_message_function=lambda x:x, presend_hook=None, **keyword_headers):
    with self.lock:
      self.correlation_id += 1
      correlation_id = self.correlation_id
      
    if presend_hook:
      presend_hook(correlation_id)

    logged_message = log_message_function(copy.deepcopy(message))
    logger.info("Event to server at {0} (correlation_id={1}): {2}".format(destination, correlation_id, logged_message))

    body = json.dumps(message)
    WsConnection.send(self, destination, body, content_type=content_type, headers=headers, correlationId=correlation_id, **keyword_headers)

    return correlation_id

  def add_listener(self, listener):
    self.set_listener(listener.__class__.__name__, listener)

class CachedHTTPSConnection:
  """ Caches a ssl socket and uses a single https connection to the server. """

  def __init__(self, config, server_hostname):
    self.connected = False
    self.config = config
    self.server = server_hostname
    self.port = config.get('server', 'secured_url_port')
    self.connect()

  def connect(self):
    if not self.connected:
      self.httpsconn = VerifiedHTTPSConnection(self.server, self.port,
                                               self.config)
      self.httpsconn.connect()
      self.connected = True
    # possible exceptions are caught and processed in Controller

  def forceClear(self):
    self.httpsconn = VerifiedHTTPSConnection(self.server, self.port,
                                             self.config)
    self.connect()

  def request(self, req):
    self.connect()
    try:
      self.httpsconn.request(req.get_method(), req.get_full_url(),
                             req.get_data(), req.headers)
      response = self.httpsconn.getresponse()
      # Ungzip if gzipped
      if response.getheader('Content-Encoding') == 'gzip':
        buf = StringIO(response.read())
        response = gzip.GzipFile(fileobj=buf)
      readResponse = response.read()
    except Exception as ex:
      # This exception is caught later in Controller
      logger.debug("Error in sending/receving data from the server " +
                   traceback.format_exc())
      logger.info("Encountered communication error. Details: " + repr(ex))
      self.connected = False
      raise IOError("Error occured during connecting to the server: " + str(ex))
    return readResponse


class CertificateManager():
  def __init__(self, config, server_hostname):
    self.config = config
    self.keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    self.server_crt = self.config.get('security', 'server_crt')
    self.server_url = 'https://' + server_hostname + ':' \
                      + self.config.get('server', 'url_port')

  def getAgentKeyName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".key"

  def getAgentCrtName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".crt"

  def getAgentCrtReqName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".csr"

  def getSrvrCrtName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + "ca.crt"

  def checkCertExists(self):

    s = os.path.abspath(
      self.config.get('security', 'keysdir')) + os.sep + "ca.crt"

    server_crt_exists = os.path.exists(s)

    if not server_crt_exists:
      logger.info("Server certicate not exists, downloading")
      self.loadSrvrCrt()
    else:
      logger.info("Server certicate exists, ok")

    agent_key_exists = os.path.exists(self.getAgentKeyName())

    if not agent_key_exists:
      logger.info("Agent key not exists, generating request")
      self.genAgentCrtReq(self.getAgentKeyName())
    else:
      logger.info("Agent key exists, ok")

    agent_crt_exists = os.path.exists(self.getAgentCrtName())

    if not agent_crt_exists:
      logger.info("Agent certificate not exists, sending sign request")
      self.reqSignCrt()
    else:
      logger.info("Agent certificate exists, ok")

  def loadSrvrCrt(self):
    get_ca_url = self.server_url + '/cert/ca/'
    logger.info("Downloading server cert from " + get_ca_url)
    proxy_handler = urllib2.ProxyHandler({})
    opener = urllib2.build_opener(proxy_handler)
    stream = opener.open(get_ca_url)
    response = stream.read()
    stream.close()
    srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
    srvr_crt_f.write(response)
    srvr_crt_f.close()

  def reqSignCrt(self):
    sign_crt_req_url = self.server_url + '/certs/' + hostname.hostname(
      self.config)
    agent_crt_req_f = open(self.getAgentCrtReqName())
    agent_crt_req_content = agent_crt_req_f.read()
    agent_crt_req_f.close()
    passphrase_env_var = self.config.get('security', 'passphrase_env_var_name')
    passphrase = os.environ[passphrase_env_var]
    register_data = {'csr': agent_crt_req_content,
                     'passphrase': passphrase}
    data = json.dumps(register_data)
    proxy_handler = urllib2.ProxyHandler({})
    opener = urllib2.build_opener(proxy_handler)
    urllib2.install_opener(opener)
    req = urllib2.Request(sign_crt_req_url, data,
                          {'Content-Type': 'application/json'})
    f = urllib2.urlopen(req)
    response = f.read()
    f.close()
    try:
      data = json.loads(response)
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("Sign response from Server: \n" + pprint.pformat(data))
    except Exception:
      logger.warn("Malformed response! data: %s", data)
      data = {'result': 'ERROR'}
    result = data['result']
    if result == 'OK':
      agentCrtContent = data['signedCa']
      agentCrtF = open(self.getAgentCrtName(), "w")
      agentCrtF.write(agentCrtContent)
    else:
      # Possible exception is catched higher at Controller
      logger.error('Certificate signing failed.'
                   '\nIn order to receive a new agent'
                   ' certificate, remove existing certificate file from keys '
                   'directory. As a workaround you can turn off two-way SSL '
                   'authentication in server configuration(ambari.properties) '
                   '\nExiting..')
      raise ssl.SSLError

  def genAgentCrtReq(self, keyname):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    generate_script = GEN_AGENT_KEY % {
      'hostname': hostname.hostname(self.config),
      'keysdir': keysdir}

    logger.info(generate_script)
    if platform.system() == 'Windows':
      p = subprocess32.Popen(generate_script, stdout=subprocess32.PIPE)
      p.communicate()
    else:
      p = subprocess32.Popen([generate_script], shell=True,
                           stdout=subprocess32.PIPE)
      p.communicate()
    # this is required to be 600 for security concerns.
    os.chmod(keyname, 0600)

  def initSecurity(self):
    self.checkCertExists()
