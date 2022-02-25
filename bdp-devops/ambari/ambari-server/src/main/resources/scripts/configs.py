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
from optparse import OptionGroup
from collections import OrderedDict
import sys
import urllib2, ssl
import time
import json
import base64
import xml
import xml.etree.ElementTree as ET
import os
import logging

logger = logging.getLogger('AmbariConfig')

HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'

SET_ACTION = 'set'
GET_ACTION = 'get'
DELETE_ACTION = 'delete'

GET_REQUEST_TYPE = 'GET'
PUT_REQUEST_TYPE = 'PUT'

# JSON Keywords
PROPERTIES = 'properties'
ATTRIBUTES = 'properties_attributes'
CLUSTERS = 'Clusters'
DESIRED_CONFIGS = 'desired_configs'
SERVICE_CONFIG_NOTE = 'service_config_version_note'
TYPE = 'type'
TAG = 'tag'
ITEMS = 'items'
TAG_PREFIX = 'version'

CLUSTERS_URL = '/api/v1/clusters/{0}'
DESIRED_CONFIGS_URL = CLUSTERS_URL + '?fields=Clusters/desired_configs'
CONFIGURATION_URL = CLUSTERS_URL + '/configurations?type={1}&tag={2}'

FILE_FORMAT = \
"""
"properties": {
  "key1": "value1"
  "key2": "value2"
},
"properties_attributes": {
  "attribute": {
    "key1": "value1"
    "key2": "value2"
  }
}
"""

class UsageException(Exception):
  pass


def api_accessor(host, login, password, protocol, port, unsafe=None):
  def do_request(api_url, request_type=GET_REQUEST_TYPE, request_body=''):
    try:
      url = '{0}://{1}:{2}{3}'.format(protocol, host, port, api_url)
      admin_auth = base64.encodestring('%s:%s' % (login, password)).replace('\n', '')
      request = urllib2.Request(url)
      request.add_header('Authorization', 'Basic %s' % admin_auth)
      request.add_header('X-Requested-By', 'ambari')
      request.add_data(request_body)
      request.get_method = lambda: request_type

      if unsafe:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        response = urllib2.urlopen(request, context=ctx)
      else:
        response = urllib2.urlopen(request)

      response_body = response.read()
    except Exception as exc:
      raise Exception('Problem with accessing api. Reason: {0}'.format(exc))
    return response_body
  return do_request

def get_config_tag(cluster, config_type, accessor):
  response = accessor(DESIRED_CONFIGS_URL.format(cluster))
  try:
    desired_tags = json.loads(response)
    current_config_tag = desired_tags[CLUSTERS][DESIRED_CONFIGS][config_type][TAG]
  except Exception as exc:
    raise Exception('"{0}" not found in server response. Response:\n{1}'.format(config_type, response))
  return current_config_tag

def create_new_desired_config(cluster, config_type, properties, attributes, accessor, version_note):
  new_tag = TAG_PREFIX + str(int(time.time() * 1000000))
  new_config = {
    CLUSTERS: {
      DESIRED_CONFIGS: {
        TYPE: config_type,
        TAG: new_tag,
        SERVICE_CONFIG_NOTE:version_note,
        PROPERTIES: properties
      }
    }
  }
  if len(attributes.keys()) > 0:
    new_config[CLUSTERS][DESIRED_CONFIGS][ATTRIBUTES] = attributes
  request_body = json.dumps(new_config)
  new_file = 'doSet_{0}.json'.format(new_tag)
  logger.info('### PUTting json into: {0}'.format(new_file))
  output_to_file(new_file)(new_config)
  accessor(CLUSTERS_URL.format(cluster), PUT_REQUEST_TYPE, request_body)
  logger.info('### NEW Site:{0}, Tag:{1}'.format(config_type, new_tag))

def get_current_config(cluster, config_type, accessor):
  config_tag = get_config_tag(cluster, config_type, accessor)
  logger.info("### on (Site:{0}, Tag:{1})".format(config_type, config_tag))
  response = accessor(CONFIGURATION_URL.format(cluster, config_type, config_tag))
  config_by_tag = json.loads(response, object_pairs_hook=OrderedDict)
  current_config = config_by_tag[ITEMS][0]
  return current_config[PROPERTIES], current_config.get(ATTRIBUTES, {})

def update_config(cluster, config_type, config_updater, accessor, version_note):
  properties, attributes = config_updater(cluster, config_type, accessor)
  create_new_desired_config(cluster, config_type, properties, attributes, accessor, version_note)

def update_specific_property(config_name, config_value):
  def update(cluster, config_type, accessor):
    properties, attributes = get_current_config(cluster, config_type, accessor)
    properties[config_name] = config_value
    return properties, attributes
  return update

def update_from_xml(config_file):
  def update(cluster, config_type, accessor):
    return read_xml_data_to_map(config_file)
  return update

# Used DOM parser to read data into a map
def read_xml_data_to_map(path):
  configurations = {}
  properties_attributes = {}
  tree = ET.parse(path)
  root = tree.getroot()
  for properties in root.getiterator('property'):
    name = properties.find('name')
    value = properties.find('value')
    final = properties.find('final')

    if name != None:
      name_text = name.text if name.text else ""
    else:
      logger.warn("No name is found for one of the properties in {0}, ignoring it".format(path))
      continue

    if value != None:
      value_text = value.text if value.text else ""
    else:
      logger.warn("No value is found for \"{0}\" in {1}, using empty string for it".format(name_text, path))
      value_text = ""

    if final != None:
      final_text = final.text if final.text else ""
      properties_attributes[name_text] = final_text

    configurations[name_text] = value_text
  return configurations, {"final" : properties_attributes}

def update_from_file(config_file):
  def update(cluster, config_type, accessor):
    try:
      with open(config_file) as in_file:
        file_content = in_file.read()
    except Exception as e:
      raise Exception('Cannot find file "{0}" to PUT'.format(config_file))
    try:
      file_properties = json.loads(file_content)
    except Exception as e:
      raise Exception('File "{0}" should be in the following JSON format ("properties_attributes" is optional):\n{1}'.format(config_file, FILE_FORMAT))
    new_properties = file_properties.get(PROPERTIES, {})
    new_attributes = file_properties.get(ATTRIBUTES, {})
    logger.info('### PUTting file: "{0}"'.format(config_file))
    return new_properties, new_attributes
  return update

def delete_specific_property(config_name):
  def update(cluster, config_type, accessor):
    properties, attributes = get_current_config(cluster, config_type, accessor)
    properties.pop(config_name, None)
    for attribute_values in attributes.values():
      attribute_values.pop(config_name, None)
    return properties, attributes
  return update

def output_to_file(filename):
  def output(config):
    with open(filename, 'w') as out_file:
      json.dump(config, out_file, indent=2)
  return output

def output_to_console(config):
  print json.dumps(config, indent=2)

def get_config(cluster, config_type, accessor, output):
  properties, attributes = get_current_config(cluster, config_type, accessor)
  config = {PROPERTIES: properties}
  if len(attributes.keys()) > 0:
    config[ATTRIBUTES] = attributes
  output(config)

def set_properties(cluster, config_type, args, accessor, version_note):
  logger.info('### Performing "set":')

  if len(args) == 1:
    config_file = args[0]
    root, ext = os.path.splitext(config_file)
    if ext == ".xml":
      updater = update_from_xml(config_file)
    elif ext == ".json":
      updater = update_from_file(config_file)
    else:
      logger.error("File extension {0} is not supported".format(ext))
      return -1
    logger.info('### from file {0}'.format(config_file))
  else:
    config_name = args[0]
    config_value = args[1]
    updater = update_specific_property(config_name, config_value)
    logger.info('### new property - "{0}":"{1}"'.format(config_name, config_value))
  update_config(cluster, config_type, updater, accessor, version_note)
  return 0

def delete_properties(cluster, config_type, args, accessor, version_note):
  logger.info('### Performing "delete":')
  if len(args) == 0:
    logger.error("Not enough arguments. Expected config key.")
    return -1

  config_name = args[0]
  logger.info('### on property "{0}"'.format(config_name))
  update_config(cluster, config_type, delete_specific_property(config_name), accessor, version_note)
  return 0


def get_properties(cluster, config_type, args, accessor):
  logger.info("### Performing \"get\" content:")
  if len(args) > 0:
    filename = args[0]
    output = output_to_file(filename)
    logger.info('### to file "{0}"'.format(filename))
  else:
    output = output_to_console
  get_config(cluster, config_type, accessor, output)
  return 0

def main():

  parser = optparse.OptionParser(usage="usage: %prog [options]")

  login_options_group = OptionGroup(parser, "To specify credentials please use \"-e\" OR \"-u\" and \"-p'\"")
  login_options_group.add_option("-u", "--user", dest="user", default="admin", help="Optional user ID to use for authentication. Default is 'admin'")
  login_options_group.add_option("-p", "--password", dest="password", default="admin", help="Optional password to use for authentication. Default is 'admin'")
  login_options_group.add_option("-e", "--credentials-file", dest="credentials_file", help="Optional file with user credentials separated by new line.")
  parser.add_option_group(login_options_group)

  parser.add_option("-t", "--port", dest="port", default="8080", help="Optional port number for Ambari server. Default is '8080'. Provide empty string to not use port.")
  parser.add_option("-s", "--protocol", dest="protocol", default="http", help="Optional support of SSL. Default protocol is 'http'")
  parser.add_option("--unsafe", action="store_true", dest="unsafe", help="Skip SSL certificate verification.")
  parser.add_option("-a", "--action", dest="action", help="Script action: <get>, <set>, <delete>")
  parser.add_option("-l", "--host", dest="host", help="Server external host name")
  parser.add_option("-n", "--cluster", dest="cluster", help="Name given to cluster. Ex: 'c1'")
  parser.add_option("-c", "--config-type", dest="config_type", help="One of the various configuration types in Ambari. Ex: core-site, hdfs-site, mapred-queue-acls, etc.")
  parser.add_option("-b", "--version-note", dest="version_note", default="", help="Version change notes which will help to know what has been changed in this config. This value is optional and is used for actions <set> and <delete>.")

  config_options_group = OptionGroup(parser, "To specify property(s) please use \"-f\" OR \"-k\" and \"-v'\"")
  config_options_group.add_option("-f", "--file", dest="file", help="File where entire configurations are saved to, or read from. Supported extensions (.xml, .json>)")
  config_options_group.add_option("-k", "--key", dest="key", help="Key that has to be set or deleted. Not necessary for 'get' action.")
  config_options_group.add_option("-v", "--value", dest="value", help="Optional value to be set. Not necessary for 'get' or 'delete' actions.")
  parser.add_option_group(config_options_group)

  (options, args) = parser.parse_args()

  logger.setLevel(logging.INFO)
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  stdout_handler = logging.StreamHandler(sys.stdout)
  stdout_handler.setLevel(logging.INFO)
  stdout_handler.setFormatter(formatter)
  logger.addHandler(stdout_handler)

  # options with default value

  if not options.credentials_file and (not options.user or not options.password):
    parser.error("You should use option (-e) to set file with Ambari user credentials OR use (-u) username and (-p) password")

  if options.credentials_file:
    if os.path.isfile(options.credentials_file):
      try:
        with open(options.credentials_file) as credentials_file:
          file_content = credentials_file.read()
          login_lines = filter(None, file_content.splitlines())
          if len(login_lines) == 2:
            user = login_lines[0]
            password = login_lines[1]
          else:
            logger.error("Incorrect content of {0} file. File should contain Ambari username and password separated by new line.".format(options.credentials_file))
            return -1
      except Exception as e:
        logger.error("You don't have permissions to {0} file".format(options.credentials_file))
        return -1
    else:
      logger.error("File {0} doesn't exist or you don't have permissions.".format(options.credentials_file))
      return -1
  else:
    user = options.user
    password = options.password

  port = options.port
  protocol = options.protocol

  #options without default value
  if None in [options.action, options.host, options.cluster, options.config_type]:
    parser.error("One of required options is not passed")

  action = options.action
  host = options.host
  cluster = options.cluster
  config_type = options.config_type
  version_note = options.version_note

  accessor = api_accessor(host, user, password, protocol, port, options.unsafe)
  if action == SET_ACTION:

    if not options.file and (not options.key or not options.value):
      parser.error("You should use option (-f) to set file where entire configurations are saved OR (-k) key and (-v) value for one property")
    if options.file:
      action_args = [options.file]
    else:
      action_args = [options.key, options.value]
    return set_properties(cluster, config_type, action_args, accessor, version_note)

  elif action == GET_ACTION:
    if options.file:
      action_args = [options.file]
    else:
      action_args = []
    return get_properties(cluster, config_type, action_args, accessor)

  elif action == DELETE_ACTION:
    if not options.key:
      parser.error("You should use option (-k) to set property name witch will be deleted")
    else:
      action_args = [options.key]
    return delete_properties(cluster, config_type, action_args, accessor, version_note)
  else:
    logger.error('Action "{0}" is not supported. Supported actions: "get", "set", "delete".'.format(action))
    return -1

if __name__ == "__main__":
  try:
    sys.exit(main())
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
