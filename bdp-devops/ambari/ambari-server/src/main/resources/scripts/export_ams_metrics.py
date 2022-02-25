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

import optparse
import sys
import os
import logging
import urllib2
import json
import datetime
import time
import re
import copy
from optparse import OptionGroup
from flask import Flask, Response, jsonify, request, abort
from flask.ext.cors import CORS
from flask_restful import Resource, Api, reqparse



class Params:

  ACTION = None
  AMS_HOSTNAME = 'localhost'
  AMS_PORT = '6188'
  AMS_APP_ID = None
  AMS_APP_ID_FORMATTED = None
  HOSTS_FILE = None
  METRICS_FILE = None
  OUT_DIR = None
  PRECISION = 'minutes'
  START_TIME = None
  END_TIME = None
  METRICS = []
  HOSTS = []
  METRICS_METADATA = {}
  FLASK_SERVER_NAME = None
  METRICS_FOR_HOSTS = {}
  HOSTS_WITH_COMPONENTS = {}
  INPUT_DIR = None
  VERBOSE = None
  AGGREGATE = None

  @staticmethod
  def get_collector_uri(metricNames, hostname=None):
    if hostname:
      return 'http://{0}:{1}/ws/v1/timeline/metrics?metricNames={2}&hostname={3}&appId={4}&startTime={5}&endTime={6}&precision={7}' \
        .format(Params.AMS_HOSTNAME, Params.AMS_PORT, metricNames, hostname, Params.AMS_APP_ID,
                Params.START_TIME, Params.END_TIME, Params.PRECISION)
    else:
      return 'http://{0}:{1}/ws/v1/timeline/metrics?metricNames={2}&appId={3}&startTime={4}&endTime={5}&precision={6}' \
        .format(Params.AMS_HOSTNAME, Params.AMS_PORT, metricNames, Params.AMS_APP_ID, Params.START_TIME,
                Params.END_TIME, Params.PRECISION)

class Utils:

  @staticmethod
  def setup_logger(verbose, log_file):

    global logger
    logger = logging.getLogger('AmbariMetricsExport')
    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    if log_file:
      filehandler = logging.FileHandler(log_file)
    consolehandler = logging.StreamHandler()
    filehandler.setFormatter(formatter)
    consolehandler.setFormatter(formatter)
    logger.addHandler(filehandler)
    logger.addHandler(consolehandler)

    # set verbose
    if verbose:
      # logging.basicConfig(level=logging.DEBUG)
      logger.setLevel(logging.DEBUG)
    else:
      # logging.basicConfig(level=logging.INFO)
      logger.setLevel(logging.INFO)

  @staticmethod
  def get_data_from_url(collector_uri):
    req = urllib2.Request(collector_uri)
    connection = None
    try:
      connection = urllib2.urlopen(req)
    except Exception as e:
      logger.error('Error on metrics GET request: %s' % collector_uri)
      logger.error(str(e))
    # Validate json before dumping
    response_data = None
    if connection:
      try:
        response_data = json.loads(connection.read())
      except Exception as e:
        logger.warn('Error parsing json data returned from URI: %s' % collector_uri)
        logger.debug(str(e))

    return response_data

  @staticmethod
  def get_epoch(input):
    if (len(input) == 13):
      return int(input)
    elif (len(input) == 20):
      return int(time.mktime(datetime.datetime.strptime(input, '%Y-%m-%dT%H:%M:%SZ').timetuple()) * 1000)
    else:
      return -1

  @staticmethod
  def read_json_file(filename):
    with open(filename) as f:
      return json.load(f)

  @staticmethod
  def get_configs():
    conf_file = None
    if Params.INPUT_DIR:
      for metrics_dir in AmsMetricsProcessor.get_metrics_dirs(Params.INPUT_DIR):
        for dir_item in os.listdir(metrics_dir):
          dir_item_path = os.path.join(Params.INPUT_DIR, metrics_dir, dir_item)
          if dir_item == "configs":
            conf_file = dir_item_path
            break
        if conf_file:
          break

      if os.path.exists(conf_file):
        json = Utils.read_json_file(conf_file)
        Params.AMS_APP_ID = json['APP_ID']
        Params.START_TIME = json['START_TIME']
        Params.END_TIME = json['END_TIME']
        Params.AGGREGATE = json['AGGREGATE']
      else:
        logger.warn('Not found config file in {0}'.format(os.path.join(Params.INPUT_DIR), "configs"))
        logger.info('Aborting...')
        sys.exit(1)

  @staticmethod
  def set_configs():
    conf_file = os.path.join(Params.OUT_DIR, "configs")
    aggregate = True if not Params.HOSTS else False
    properties = {"APP_ID" : Params.AMS_APP_ID, "START_TIME" : Params.START_TIME, "END_TIME" : Params.END_TIME, "AGGREGATE" : aggregate}

    with open(conf_file, 'w') as file:
      file.write(json.dumps(properties))

class AmsMetricsProcessor:

  @staticmethod
  def write_metrics_to_file(metrics, host=None):

    for metric in metrics:
      uri = Params.get_collector_uri(metric, host)
      logger.info('Request URI: %s' % str(uri))
      metrics_json = Utils.get_data_from_url(uri)
      if metrics_json:
        if host:
          path = os.path.join(Params.OUT_DIR, host, metric)
        else:
          path = os.path.join(Params.OUT_DIR, metric)
        logger.info('Writing metric file: %s' % path)
        with open(path, 'w') as file:
          file.write(json.dumps(metrics_json))

  @staticmethod
  def get_metrics_metadata():
    app_metrics_metadata = []
    for metric in Params.METRICS:
      if not Params.AGGREGATE:
        app_metrics_metadata.append({"metricname": metric, "seriesStartTime": Params.START_TIME, "supportsAggregation": "false", "type": "UNDEFINED"})
      else:
        app_metrics_metadata.append({"metricname": metric, "seriesStartTime": Params.START_TIME, "supportsAggregation": "false"})
    logger.debug("Adding {0} to metadata".format(metric))

    return {Params.AMS_APP_ID : app_metrics_metadata}

  @staticmethod
  def get_hosts_with_components():
    hosts_with_components = {}
    if Params.AGGREGATE:
      return {"fakehostname" : [Params.AMS_APP_ID]}
    else:
      for host in Params.HOSTS:
        hosts_with_components[host] = [Params.AMS_APP_ID]
      return hosts_with_components

  @staticmethod
  def get_metrics_dirs(d):
    for o in os.listdir(d):
      if 'ambari_metrics_export_' in o and os.path.isdir(os.path.join(d, o)):
        yield os.path.join(d, o)


  @staticmethod
  def ger_metrics_from_input_dir():
    metrics_for_hosts = {}

    for metrics_dir in AmsMetricsProcessor.get_metrics_dirs(Params.INPUT_DIR):
      for dir_item in os.listdir(metrics_dir):
        dir_item_path = os.path.join(metrics_dir, dir_item)
        if os.path.isdir(dir_item_path):
          if dir_item not in Params.HOSTS:
            Params.HOSTS.append(os.path.basename(dir_item))
          metrics_for_hosts[dir_item] = {}
          for metric in os.listdir(dir_item_path):
            if metric not in Params.METRICS:
              Params.METRICS.append(os.path.basename(metric))
            metric_file = os.path.join(dir_item_path, metric)
            metrics_for_hosts[dir_item][metric] = Utils.read_json_file(metric_file)
        elif os.path.isfile(dir_item_path) and dir_item != "configs":
          if dir_item not in Params.METRICS:
            Params.METRICS.append(os.path.basename(dir_item))
          metric_file = os.path.join(Params.INPUT_DIR, dir_item_path)
          metrics_for_hosts[dir_item] = Utils.read_json_file(metric_file)

    return metrics_for_hosts

  @staticmethod
  def export_ams_metrics():
    if not os.path.exists(Params.METRICS_FILE):
      logger.error('Metrics file is required.')
      sys.exit(1)
    logger.info('Reading metrics file.')
    with open(Params.METRICS_FILE, 'r') as file:
      for line in file:
        Params.METRICS.append(line.strip())
    logger.info('Reading hosts file.')

    logger.info('Reading hosts file.')
    if Params.HOSTS_FILE and os.path.exists(Params.HOSTS_FILE):
      with open(Params.HOSTS_FILE, 'r') as file:
        for line in file:
          Params.HOSTS.append(line.strip())
    else:
      logger.info('No hosts file found, aggregate metrics will be exported.')

    if Params.HOSTS:
      for host in Params.HOSTS:
        os.makedirs(os.path.join(Params.OUT_DIR, host)) # create host dir
        AmsMetricsProcessor.write_metrics_to_file(Params.METRICS, host)
    else:
      os.makedirs(os.path.join(Params.OUT_DIR))
      AmsMetricsProcessor.write_metrics_to_file(Params.METRICS, None)

  def process(self):
    if Params.ACTION == "export":
      self.export_ams_metrics()
      Utils.set_configs()
    else:
      Utils.get_configs()
      self.metrics_for_hosts = self.ger_metrics_from_input_dir()
      self.metrics_metadata = self.get_metrics_metadata()
      self.hosts_with_components = self.get_hosts_with_components()


class FlaskServer():
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor
    app = Flask(__name__)
    api = Api(app)
    cors = CORS(app)

    api.add_resource(HostsResource, '/ws/v1/timeline/metrics/hosts', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})
    api.add_resource(MetadataResource, '/ws/v1/timeline/metrics/metadata', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})
    api.add_resource(MetricsResource, '/ws/v1/timeline/metrics', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})

    logger.info("Start Flask server. Server URL = " + Params.FLASK_SERVER_NAME + ":5000")

    app.run(debug=Params.VERBOSE,
            host=Params.FLASK_SERVER_NAME,
            port=5000)

class MetadataResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    if self.ams_metrics_processor.metrics_metadata:
      return jsonify(self.ams_metrics_processor.metrics_metadata)
    else:
      abort(404)

class HostsResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    if self.ams_metrics_processor.hosts_with_components:
      return jsonify(self.ams_metrics_processor.hosts_with_components)
    else:
      abort(404)

class MetricsResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    args = request.args
    separator = "._"

    if not "metricNames" in args:
      logger.error("Bad request")
      abort(404)

    if separator in args["metricNames"]:
      metric_name, operation = args["metricNames"].split(separator, 1)
    else:
      metric_name = args["metricNames"]
      separator = ""
      operation = ""

    metric_dict = {"metrics" : []}

    if "hostname" in args and args["hostname"] != "":
      host_names = args["hostname"].split(",")
      metric_dict = {"metrics" : []}
      for host_name in host_names:
        if metric_name in self.ams_metrics_processor.metrics_for_hosts[host_name]:
          if len(self.ams_metrics_processor.metrics_for_hosts[host_name][metric_name]["metrics"]) > 0:
            metric_dict["metrics"].append(self.ams_metrics_processor.metrics_for_hosts[host_name][metric_name]["metrics"][0])
        else:
          continue

    elif Params.AGGREGATE:
      for metric in self.ams_metrics_processor.metrics_for_hosts:
        if metric_name == metric:
          metric_dict = self.ams_metrics_processor.metrics_for_hosts[metric_name]
          break

    else:
      for host in self.ams_metrics_processor.metrics_for_hosts:
        for metric in self.ams_metrics_processor.metrics_for_hosts[host]:
            if metric_name == metric and len(self.ams_metrics_processor.metrics_for_hosts[host][metric_name]["metrics"]) > 0:
              metric_dict = self.ams_metrics_processor.metrics_for_hosts[host][metric_name]
              break

    if metric_dict:
      metrics_json_new = copy.deepcopy(metric_dict)
      for i in range (0, len(metrics_json_new["metrics"])):
        metrics_json_new["metrics"][i]["metricname"] += separator + operation
      return jsonify(metrics_json_new)
    else :
      abort(404)
      return

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is a Ambari thin client and '
                         'supports export of ambari metric data for an app '
                         'from Ambari Metrics Service to a output dir. '
                         'The metrics will be exported to a file with name of '
                         'the metric and in a directory with the name as the '
                         'hostname under the output dir. '
                         'Also this python program is a thin REST server '
                         'that implements a subset of the Ambari Metrics Service metrics server interfaces. '
                         'You can use it to visualize information exported by the AMS thin client')

  d = datetime.datetime.now()
  time_suffix = '{0}-{1}-{2}-{3}-{4}-{5}'.format(d.year, d.month, d.day,
                                                 d.hour, d.minute, d.second)
  print 'Time: %s' % time_suffix

  logfile = os.path.join('/tmp', 'ambari_metrics_export.out')

  output_dir = os.path.join('/tmp', 'ambari_metrics_export_' + time_suffix)

  parser.add_option("-a", "--action", dest="action", default="set_action", help="Use action 'export' for exporting AMS metrics. "
                                                                         "Use action 'run' for run REST server")
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                    default=False, help="output verbosity.")
  parser.add_option("-l", "--logfile", dest="log_file", default=logfile,
                    metavar='FILE', help="Log file. [default: %s]" % logfile)

  export_options_group = OptionGroup(parser, "Required options for action 'export'")
  #export metrics -----------------------------------------------------
  export_options_group.add_option("-s", "--host", dest="server_hostname",
                    help="AMS host name.")
  export_options_group.add_option("-p", "--port", dest="server_port",
                    default="6188", help="AMS port. [default: 6188]")
  export_options_group.add_option("-c", "--app-id", dest="app_id",
                    help="AMS app id.")
  export_options_group.add_option("-m", "--metrics-file", dest="metrics_file",
                    help="Metrics file with metric names to query. New line separated.")
  export_options_group.add_option("-f", "--host-file", dest="hosts_file",
                    help="Host file with hostnames to query. New line separated.")
  export_options_group.add_option("-r", "--precision", dest="precision",
                    default='minutes', help="AMS API precision, default = minutes.")
  export_options_group.add_option("-b", "--start_time", dest="start_time",
                    help="Start time in milliseconds since epoch or UTC timestamp in YYYY-MM-DDTHH:mm:ssZ format.")
  export_options_group.add_option("-e", "--end_time", dest="end_time",
                    help="End time in milliseconds since epoch or UTC timestamp in YYYY-MM-DDTHH:mm:ssZ format.")
  export_options_group.add_option("-o", "--output-dir", dest="output_dir", default=output_dir,
                    help="Output dir. [default: %s]" % output_dir)
  parser.add_option_group(export_options_group)
  #start Flask server -----------------------------------------------------

  run_server_option_group = OptionGroup(parser, "Required options for action 'run'")

  run_server_option_group.add_option("-i", "--input-dir", dest="input_dir",
                    default='/tmp', help="Input directory for AMS metrics exports. [default: /tmp]")
  run_server_option_group.add_option("-n", "--flask-server_name", dest="server_name",
                    help="Flask server name, default = 127.0.0.1", default="127.0.0.1")

  parser.add_option_group(run_server_option_group)
  (options, args) = parser.parse_args()


  #export metrics -----------------------------------------------------
  Params.ACTION = options.action

  Params.VERBOSE = options.verbose

  Utils.setup_logger(options.verbose, options.log_file)

  if Params.ACTION == "export":

    Params.AMS_HOSTNAME = options.server_hostname

    Params.AMS_PORT = options.server_port

    Params.AMS_APP_ID = options.app_id

    if Params.AMS_APP_ID != "HOST":
      Params.AMS_APP_ID = Params.AMS_APP_ID.lower()

    Params.METRICS_FILE = options.metrics_file

    Params.HOSTS_FILE = options.hosts_file

    Params.PRECISION = options.precision

    Params.OUT_DIR = output_dir if options.output_dir == output_dir else os.path.join(options.output_dir, 'ambari_metrics_export_' + time_suffix)

    if Params.START_TIME == -1:
      logger.warn('No start time provided, or it is in the wrong format. Please '
                  'provide milliseconds since epoch or a value in YYYY-MM-DDTHH:mm:ssZ format')
      logger.info('Aborting...')
      sys.exit(1)

    Params.END_TIME = Utils.get_epoch(options.end_time)

    if Params.END_TIME == -1:
      logger.warn('No end time provided, or it is in the wrong format. Please '
                  'provide milliseconds since epoch or a value in YYYY-MM-DDTHH:mm:ssZ format')
      logger.info('Aborting...')
      sys.exit(1)

    Params.START_TIME = Utils.get_epoch(options.start_time)

    ams_metrics_processor = AmsMetricsProcessor()
    ams_metrics_processor.process()


  elif Params.ACTION == "run":
  #start Flask server -----------------------------------------------------
    Params.INPUT_DIR = options.input_dir

    Params.FLASK_SERVER_NAME = options.server_name

    ams_metrics_processor = AmsMetricsProcessor()
    ams_metrics_processor.process()
    FlaskServer(ams_metrics_processor)

  else:
    logger.warn('Action \'{0}\' not supported. Please use action \'export\' for exporting AMS metrics '
                'or use action \'run\' for starting REST server'.format(Params.ACTION))
    logger.info('Aborting...')
    sys.exit(1)


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
