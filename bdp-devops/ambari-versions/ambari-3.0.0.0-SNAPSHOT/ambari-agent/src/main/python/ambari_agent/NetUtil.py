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


from urlparse import urlparse
import logging
import httplib
import ssl
from ssl import SSLError
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_commons.inet_utils import ensure_ssl_using_protocol

ERROR_SSL_WRONG_VERSION = "SSLError: Failed to connect. Please check openssl library versions. \n" +\
              "Refer to: https://bugzilla.redhat.com/show_bug.cgi?id=1022468 for more details."
LOG_REQUEST_MESSAGE = "GET %s -> %s, body: %s"

logger = logging.getLogger(__name__)

ensure_ssl_using_protocol(
  AmbariConfig.get_resolved_config().get_force_https_protocol_name(),
  AmbariConfig.get_resolved_config().get_ca_cert_file_path()
)

class NetUtil:

  DEFAULT_CONNECT_RETRY_DELAY_SEC = 10
  HEARTBEAT_IDLE_INTERVAL_DEFAULT_MIN_SEC = 1
  HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC = 10
  MINIMUM_INTERVAL_BETWEEN_HEARTBEATS = 0.1

  # Url within server to request during status check. This url
  # should return HTTP code 200
  SERVER_STATUS_REQUEST = "{0}/ca"
  # For testing purposes
  DEBUG_STOP_RETRIES_FLAG = False

  def __init__(self, config, stop_event=None):
    self.stop_event = stop_event
    self.config = config
    self.connect_retry_delay = int(config.get('server','connect_retry_delay',
                                              default=self.DEFAULT_CONNECT_RETRY_DELAY_SEC))

  def checkURL(self, url):
    """Try to connect to a given url. Result is True if url returns HTTP code 200, in any other case
    (like unreachable server or wrong HTTP code) result will be False.

       Additionally returns body of request, if available
    """
    logger.info("Connecting to " + url)
    responseBody = ""

    ssl_verify_cert = self.config.get("security","ssl_verify_cert", "0") != "0"

    try:
      parsedurl = urlparse(url)

      # hasattr being true means that current python version has default cert verification enabled.
      if hasattr(ssl, '_create_unverified_context') and not ssl_verify_cert:
          ca_connection = httplib.HTTPSConnection(parsedurl[1], context=ssl._create_unverified_context())
      else:
          ca_connection = httplib.HTTPSConnection(parsedurl[1])

      ca_connection.request("GET", parsedurl[2])
      response = ca_connection.getresponse()
      status = response.status

      if status == 200:
        responseBody = response.read()
        logger.debug(LOG_REQUEST_MESSAGE, url, str(status), responseBody)
        return True, responseBody
      else:
        logger.warning(LOG_REQUEST_MESSAGE, url, str(status), responseBody)
        return False, responseBody
    except SSLError as slerror:
      logger.error(str(slerror))
      logger.error(ERROR_SSL_WRONG_VERSION)
      return False, responseBody

    except Exception, e:
      logger.warning("Failed to connect to " + str(url) + " due to " + str(e) + "  ")
      return False, responseBody

  def try_to_connect(self, server_url, max_retries, logger=None):
    """Try to connect to a given url, sleeping for connect_retry_delay seconds
    between retries. No more than max_retries is performed. If max_retries is -1, connection
    attempts will be repeated forever until server is not reachable

    Returns count of retries
    """
    connected = False
    if logger is not None:
      logger.debug("Trying to connect to %s", server_url)

    retries = 0
    while (max_retries == -1 or retries < max_retries) and not self.DEBUG_STOP_RETRIES_FLAG:
      server_is_up, responseBody = self.checkURL(self.SERVER_STATUS_REQUEST.format(server_url))
      if server_is_up:
        connected = True
        break
      else:
        if logger is not None:
          logger.warn('Server at {0} is not reachable, sleeping for {1} seconds...'.format(server_url,
            self.connect_retry_delay))
        retries += 1

      self.stop_event.wait(self.connect_retry_delay)

      if self.stop_event.is_set():
        #stop waiting
        if logger is not None:
          logger.info("Stop event received")
        break

    return retries, connected, self.DEBUG_STOP_RETRIES_FLAG

  def get_agent_heartbeat_idle_interval_sec(self, heartbeat_idle_interval_min, heartbeat_idle_interval_max, cluster_size):
    """
    Returns the interval in seconds to be used between agent heartbeats when
    there are pending stages which requires higher heartbeat rate to reduce the latency
    between the completion of the last command of the current stage and the starting of first
    command of next stage.

    The heartbeat intervals for elevated heartbeats is calculated as a function of the size of the cluster.

    Using a higher hearbeat rate in case of large clusters will cause agents to flood
    the server with heartbeat messages thus the calculated heartbeat interval is restricted to
    [heartbeat_idle_interval_min, heartbeat_idle_interval_max] range.

    :param cluster_size: the number of nodes the cluster consists of
    :return: the heartbeat interval in seconds
    """

    heartbeat_idle_interval = cluster_size // heartbeat_idle_interval_max

    if heartbeat_idle_interval < heartbeat_idle_interval_min:
      return heartbeat_idle_interval_min

    if heartbeat_idle_interval > heartbeat_idle_interval_max:
      return heartbeat_idle_interval_max

    return heartbeat_idle_interval
