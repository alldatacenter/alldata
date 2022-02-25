#!/usr/bin/env python

'''
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
'''

import ambari_commons.network as network
import ambari_simplejson as json
import logging
import os
import random
import urllib
from ambari_agent.AmbariConfig import AmbariConfig
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.is_empty import is_empty

logger = logging.getLogger(__name__)


DEFAULT_COLLECTOR_SUFFIX = '.sink.timeline.collector.hosts'
DEFAULT_METRICS2_PROPERTIES_FILE_NAME = 'hadoop-metrics2.properties'

AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics?%s"

METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY = '{{ams-site/timeline.metrics.service.webapp.address}}'
METRICS_COLLECTOR_VIP_HOST_KEY = '{{cluster-env/metrics_collector_external_hosts}}'
METRICS_COLLECTOR_VIP_PORT_KEY = '{{cluster-env/metrics_collector_external_port}}'
AMS_METRICS_COLLECTOR_USE_SSL_KEY = '{{ams-site/timeline.metrics.service.http.policy}}'
CONNECTION_TIMEOUT_KEY = 'http.connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0


def select_metric_collector_for_sink(sink_name):
  # TODO check '*' sink_name

  all_collectors_string = get_metric_collectors_from_properties_file(sink_name)
  return select_metric_collector_hosts_from_hostnames(all_collectors_string)

def select_metric_collector_hosts_from_hostnames(comma_separated_hosts):
  if comma_separated_hosts:
    hosts = comma_separated_hosts.split(',')
    return get_random_host(hosts)
  else:
    return 'localhost'

def get_random_host(hosts):
  return random.choice(hosts)

def get_metric_collectors_from_properties_file(sink_name):
  try:
    hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
  except Exception as e:
    raise Exception("Couldn't define hadoop_conf_dir: {0}".format(e))
  properties_filepath = os.path.join(hadoop_conf_dir, DEFAULT_METRICS2_PROPERTIES_FILE_NAME)

  if not os.path.exists(properties_filepath):
    raise Exception("Properties file doesn't exist : {0}. Can't define metric collector hosts".format(properties_filepath))
  props = load_properties_from_file(properties_filepath)

  property_key = sink_name + DEFAULT_COLLECTOR_SUFFIX
  if property_key in props:
    return props.get(property_key)
  else:
    raise Exception("Properties file doesn't contain {0}. Can't define metric collector hosts".format(property_key))

def load_properties_from_file(filepath, sep='=', comment_char='#'):
  """
  Read the file passed as parameter as a properties file.
  """
  props = {}
  with open(filepath, "rt") as f:
    for line in f:
      l = line.strip()
      if l and not l.startswith(comment_char):
        key_value = l.split(sep)
        key = key_value[0].strip()
        value = sep.join(key_value[1:]).strip('" \t')
        props[key] = value
  return props


def get_ams_tokens():
  return (METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, AMS_METRICS_COLLECTOR_USE_SSL_KEY, METRICS_COLLECTOR_VIP_HOST_KEY, METRICS_COLLECTOR_VIP_PORT_KEY)


def create_ams_client(alert_id, ams_app_id, configurations, parameters):
  if METRICS_COLLECTOR_VIP_HOST_KEY in configurations and METRICS_COLLECTOR_VIP_PORT_KEY in configurations:
    ams_collector_hosts = configurations[METRICS_COLLECTOR_VIP_HOST_KEY].split(',')
    ams_collector_port = int(configurations[METRICS_COLLECTOR_VIP_PORT_KEY])
  else:
    # ams-site/timeline.metrics.service.webapp.address is required
    if not METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY in configurations:
      raise Exception('{0} is a required parameter for the script'.format(METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY))

    collector_webapp_address = configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY].split(":")
    if not _valid_collector_webapp_address(collector_webapp_address):
      raise Exception('{0} value should be set as "fqdn_hostname:port", but set to {1}'.format(
        METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY]))

    ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
    if not ams_collector_hosts:
      raise Exception("Ambari metrics is not available: ams_collector_hosts is None")
    ams_collector_port = int(collector_webapp_address[1])

  use_ssl = False
  if AMS_METRICS_COLLECTOR_USE_SSL_KEY in configurations:
    use_ssl = configurations[AMS_METRICS_COLLECTOR_USE_SSL_KEY] == 'HTTPS_ONLY'

  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])
  return AmsClient(alert_id, ams_collector_hosts, ams_collector_port, use_ssl, connection_timeout, ams_app_id)

def _valid_collector_webapp_address(webapp_address):
  if len(webapp_address) == 2 \
          and webapp_address[0] != '127.0.0.1' \
          and webapp_address[1].isdigit():
    return True

  return False

class AmsClient:

  def __init__(self, alert_id, ams_collector_hosts, ams_collector_port, use_ssl, connection_timeout, ams_app_id):
    self.alert_id = alert_id
    self.ams_collector_hosts = ams_collector_hosts
    self.ams_collector_port = ams_collector_port
    self.use_ssl = use_ssl
    self.connection_timeout = connection_timeout
    self.ams_app_id = ams_app_id

  def load_metric(self, ams_metric, host_filter):
    metric_dict = None
    http_code = None
    for ams_collector_host in self.ams_collector_hosts:
      try:
        metric_dict, http_code = self._load_metric(ams_collector_host, ams_metric, host_filter)
        if http_code == 200 and metric_dict:
          break
      except Exception, exception:
        if logger.isEnabledFor(logging.DEBUG):
          logger.exception("[Alert][{0}] Unable to retrieve metrics from AMS ({1}:{2}): {3}".format(self.alert_id, ams_collector_host, self.ams_collector_port, str(exception)))

    if not http_code:
      raise Exception("Ambari metrics is not available: no response")
    if http_code not in [200, 299]:
      raise Exception("Ambari metrics is not available: http status code = " + str(http_code))
    if not metric_dict:
      raise Exception("Ambari metrics is not available: no metrics were found.")

    return metric_dict


  def _load_metric(self, ams_collector_host, ams_metric, host_filter):
    get_metrics_parameters = {
      "metricNames": ams_metric,
      "appId": self.ams_app_id,
      "hostname": host_filter,
      "precision": "seconds",
      "grouped": "true",
    }
    encoded_get_metrics_parameters = urllib.urlencode(get_metrics_parameters)
    url = AMS_METRICS_GET_URL % encoded_get_metrics_parameters

    _ssl_version = AmbariConfig.get_resolved_config().get_force_https_protocol_value()

    ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf"
    metric_truststore_ca_certs='ca.pem'
    ca_certs = os.path.join(ams_monitor_conf_dir, metric_truststore_ca_certs)

    conn = None
    response = None
    data = None
    try:
      conn = network.get_http_connection(
        ams_collector_host,
        int(self.ams_collector_port),
        self.use_ssl,
        ca_certs,
        ssl_version=_ssl_version
      )
      conn.request("GET", url)
      response = conn.getresponse()
      data = response.read()
    except Exception, exception:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Unable to retrieve metrics from AMS: {1}".format(self.alert_id, str(exception)))
      status = response.status if response else None
      return None, status
    finally:
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("""
        AMS request parameters - {0}
        AMS response - {1}
        """.format(encoded_get_metrics_parameters, data))
      # explicitly close the connection as we've seen python hold onto these
      if conn is not None:
        try:
          conn.close()
        except:
          logger.debug("[Alert][{0}] Unable to close URL connection to {1}".format(self.alert_id, url))

    data_json = None
    try:
      data_json = json.loads(data)
    except Exception, exception:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Convert response to json failed or json doesn't contain needed data: {1}".
                         format(self.alert_id, str(exception)))

    if not data_json:
      return None, response.status

    metric_dict = {}
    if "metrics" not in data_json:
      return None, response.status
    for metrics_data in data_json["metrics"]:
      metric_dict[metrics_data["metricname"]] = metrics_data["metrics"]

    return metric_dict, response.status
