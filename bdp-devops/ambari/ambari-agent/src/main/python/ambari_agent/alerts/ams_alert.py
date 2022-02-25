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

import imp
import time
import urllib
from alerts.metric_alert import MetricAlert, REALCODE_REGEXP
import ambari_simplejson as json
import logging
import re
import uuid

from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from ambari_commons import inet_utils

logger = logging.getLogger(__name__)

AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics?%s"

class AmsAlert(MetricAlert):
  """
  Allow alerts to fire based on an AMS metrics.
  Alert is triggered if the aggregated function of the specified metric has
  grown beyond the specified threshold within a given time interval.
  """
  def __init__(self, alert_meta, alert_source_meta, config):
    super(AmsAlert, self).__init__(alert_meta, alert_source_meta, config)

    self.metric_info = None
    if 'ams' in alert_source_meta:
      self.metric_info = AmsMetric(alert_source_meta['ams'])

  def _collect(self):
    """
    Low level function to collect alert data.  The result is a tuple as:
    res[0] = the result code
    res[1] = the list of arguments supplied to the reporting text for the result code
    """

    if self.metric_info is None:
      raise Exception("Could not determine result. Specific metric collector is not defined.")

    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("[Alert][{0}] Calculated metric URI to be {1} (ssl={2})".format(
        self.get_name(), alert_uri.uri, str(alert_uri.is_ssl_enabled)))

    host = inet_utils.get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    try:
      port = int(get_port_from_url(alert_uri.uri))
    except:
      port = 6188

    collect_result = None
    value_list = []

    if isinstance(self.metric_info, AmsMetric):
      raw_data_points, http_code = self._load_metric(alert_uri.is_ssl_enabled, host, port, self.metric_info)
      if not raw_data_points and http_code not in [200, 307]:
        collect_result = self.RESULT_UNKNOWN
        value_list.append('HTTP {0} response (metrics unavailable)'.format(str(http_code)))
      elif not raw_data_points and http_code in [200, 307]:
        raise Exception("[Alert][{0}] Unable to extract JSON from HTTP response".format(self.get_name()))
      else:

        data_points = self.metric_info.calculate_value(raw_data_points)
        compute_result = self.metric_info.calculate_compute(data_points)
        value_list.append(compute_result)

        collect_result = self._get_result(value_list[0] if compute_result is None else compute_result)

        if logger.isEnabledFor(logging.DEBUG):
          logger.debug("[Alert][{0}] Computed result = {1}".format(self.get_name(), str(value_list)))

    return (collect_result, value_list)

  def _load_metric(self, ssl, host, port, ams_metric):
    """ creates a AmsMetric object that holds info about ams-based metrics """

    if "0.0.0.0" in str(host):
      host = self.host_name


    current_time = int(time.time()) * 1000
    interval = ams_metric.interval
    get_metrics_parameters = {
      "metricNames": ",".join(ams_metric.metric_list),
      "appId": ams_metric.app_id,
      "hostname": self.host_name,
      "startTime": current_time - 60 * 1000 * interval,
      "endTime": current_time,
      "precision": "seconds",
      "grouped": "true",
      }
    encoded_get_metrics_parameters = urllib.urlencode(get_metrics_parameters)

    url = AMS_METRICS_GET_URL % encoded_get_metrics_parameters

    try:
      # TODO Implement HTTPS support
      conn = httplib.HTTPConnection(host, port,
                                    timeout=self.connection_timeout)
      conn.request("GET", url)
      response = conn.getresponse()
      data = response.read()
    except Exception, exception:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Unable to retrieve metrics from AMS: {1}".format(self.get_name(), str(exception)))
      status = response.status if 'response' in vars() else None
      return (None, status)
    finally:
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("""
        AMS request parameters - {0}
        AMS response - {1}
        """.format(encoded_get_metrics_parameters, data))
      # explicitely close the connection as we've seen python hold onto these
      if conn is not None:
        try:
          conn.close()
        except:
          logger.debug("[Alert][{0}] Unable to close URL connection to {1}".format(self.get_name(), url))
    json_is_valid = True
    try:
      data_json = json.loads(data)
    except Exception, exception:
      json_is_valid = False
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Convert response to json failed or json doesn't contain needed data: {1}".
                         format(self.get_name(), str(exception)))

    metrics = []

    if json_is_valid:
      metric_dict = {}
      for metrics_data in data_json["metrics"]:
        metric_dict[metrics_data["metricname"]] = metrics_data["metrics"]

      for metric_name in self.metric_info.metric_list:
        if metric_name in metric_dict:
          # TODO sorted data points by timestamp
          # OrderedDict was implemented in Python2.7
          sorted_data_points = metric_dict[metric_name]
          metrics.append(sorted_data_points)
      pass

    return (metrics, response.status)


class AmsMetric:
  DYNAMIC_CODE_VALUE_TEMPLATE = """
# ensure that division yields a float, use // for integer division
from __future__ import division

def f(args):
  l = []
  for k in args[0]:
    try:
      data_point = {0}
      l.append(data_point)
    except:
      continue

  return l
"""

  DYNAMIC_CODE_COMPUTE_TEMPLATE = """
# ensure that division yields a float, use // for integer division
from __future__ import division
from ambari_commons.aggregate_functions import sample_standard_deviation_percentage
from ambari_commons.aggregate_functions import sample_standard_deviation
from ambari_commons.aggregate_functions import mean
from ambari_commons.aggregate_functions import count

def f(args):
  func = {0}
  return func(args)
"""

  def __init__(self, metric_info):
    self.custom_value_module = None
    self.custom_compute_module = None
    self.metric_list = metric_info['metric_list']
    self.interval = metric_info['interval'] # in minutes
    self.app_id = metric_info['app_id']
    self.minimum_value = metric_info['minimum_value']

    if 'value' in metric_info:
      realcode = REALCODE_REGEXP.sub('args[\g<2>][k]', metric_info['value'])

      self.custom_value_module =  imp.new_module(str(uuid.uuid4()))
      code = self.DYNAMIC_CODE_VALUE_TEMPLATE.format(realcode)
      exec code in self.custom_value_module.__dict__

    if 'compute' in metric_info:
      realcode = metric_info['compute']
      self.custom_compute_module =  imp.new_module(str(uuid.uuid4()))
      code = self.DYNAMIC_CODE_COMPUTE_TEMPLATE.format(realcode)
      exec code in self.custom_compute_module.__dict__


  def calculate_value(self, args):
    data_points = None
    if self.custom_value_module is not None:
      data_points = self.custom_value_module.f(args)
      if self.minimum_value:
        data_points = [data_point for data_point in data_points if data_point > self.minimum_value]
    return data_points

  def calculate_compute(self, args):
    compute_result = None
    if self.custom_compute_module is not None:
      compute_result = self.custom_compute_module.f(args)
    return compute_result
