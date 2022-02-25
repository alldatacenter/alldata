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

from datetime import datetime, timedelta
from resource_management import Execute, File
from tempfile import mkstemp
import os
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.core.source import StaticFile

FILE_TYPE_XML = 'XML'
FILE_TYPE_PROPERTIES = 'PROPERTIES'
FILE_TYPE_JAAS_CONF = 'JAAS_CONF'

# The property name used by the hadoop credential provider
HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME = 'hadoop.security.credential.provider.path'

# Copy JCEKS provider to service specific location and update the ACL
def update_credential_provider_path(config, config_type, dest_provider_path, file_owner, file_group, use_local_jceks=False):
  """
  Copies the JCEKS file for the specified config from the default location to the given location,
  and sets the ACLs for the specified owner and group. Also updates the config type's configuration
  hadoop credential store provider with the copied file name.
  :param config: configurations['configurations'][config_type]
  :param config_type: Like hive-site, oozie-site, etc.
  :param dest_provider_path: The full path to the file where the JCEKS provider file is to be copied to.
  :param file_owner: File owner
  :param file_group: Group
  :return: A copy of the config that was modified or the input config itself if nothing was modified.
  """
  # Get the path to the provider <config_type>.jceks
  if HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME in config:
    provider_path = config[HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME]
    src_provider_path = provider_path[len('jceks://file'):]
    File(dest_provider_path,
        owner = file_owner,
        group = file_group,
        mode = 0640,
        content = StaticFile(src_provider_path)
    )
    # make a copy of the config dictionary since it is read-only
    config_copy = config.copy()
    # overwrite the provider path with the path specified
    if use_local_jceks:
      config_copy[HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME] = 'localjceks://file{0}'.format(dest_provider_path)
    else:
      config_copy[HADOOP_CREDENTIAL_PROVIDER_PROPERTY_NAME] = 'jceks://file{0}'.format(dest_provider_path)
    return config_copy
  return config

def validate_security_config_properties(params, configuration_rules):
  """
  Generic security configuration validation based on a set of rules and operations
  :param params: The structure where the config parameters are held
  :param configuration_rules: A structure containing rules and expectations,
  Three types of checks are currently supported by this method:
  1. value_checks - checks that a certain value must be set
  2. empty_checks - checks that the property values must not be empty
  3. read_checks - checks that the value represented by the property describes a readable file on the filesystem
  :return: Issues found - should be empty if all is good
  """

  issues = {}

  for config_file, rule_sets in configuration_rules.iteritems():
    # Each configuration rule set may have 0 or more of the following rule sets:
    # - value_checks
    # - empty_checks
    # - read_checks
    try:
      # Each rule set has at least a list of relevant property names to check in some way
      # The rule set for the operation of 'value_checks' is expected to be a dictionary of
      # property names to expected values

      actual_values = params[config_file] if config_file in params else {}

      # Process Value Checks
      # The rules are expected to be a dictionary of property names to expected values
      rules = rule_sets['value_checks'] if 'value_checks' in rule_sets else None
      if rules:
        for property_name, expected_value in rules.iteritems():
          actual_value = get_value(actual_values, property_name, '')
          if actual_value != expected_value:
            issues[config_file] = "Property %s contains an unexpected value. " \
                                  "Expected/Actual: %s/%s" \
                                  % (property_name, expected_value, actual_value)

      # Process Empty Checks
      # The rules are expected to be a list of property names that should not have empty values
      rules = rule_sets['empty_checks'] if 'empty_checks' in rule_sets else None
      if rules:
        for property_name in rules:
          actual_value = get_value(actual_values, property_name, '')
          if not actual_value:
            issues[config_file] = "Property %s must exist and must not be empty" % property_name

      # Process Read Checks
      # The rules are expected to be a list of property names that resolve to files names and must
      # exist and be readable
      rules = rule_sets['read_checks'] if 'read_checks' in rule_sets else None
      if rules:
        for property_name in rules:
          actual_value = get_value(actual_values, property_name, None)
          if not actual_value:
            issues[config_file] = "Property %s does not exist" % property_name
          elif not os.path.isfile(actual_value):
            issues[config_file] = "Property %s points to an inaccessible file - %s" % (property_name, actual_value)
    except Exception as e:
      issues[config_file] = "Exception occurred while validating the config file\nCauses: %s" % str(e)
  return issues


def build_expectations(config_file, value_checks, empty_checks, read_checks):
  """
  Helper method used to build the check expectations dict
  :return:
  """
  configs_expectations = {}
  configs_expectations[config_file] = {}
  if value_checks:
    configs_expectations[config_file]['value_checks'] = value_checks
  if empty_checks:
    configs_expectations[config_file]['empty_checks'] = empty_checks
  if read_checks:
    configs_expectations[config_file]['read_checks'] = read_checks
  return configs_expectations


def get_params_from_filesystem(conf_dir, config_files):
  """
  Used to retrieve properties from xml config files and build a dict

  The dictionary of configuration files to file types should contain one of the following values"
    'XML'
    'PROPERTIES'

  :param conf_dir:  directory where the configuration files sit
  :param config_files: dictionary of configuration file names to (supported) file types
  :return: a dictionary of config-type to a dictionary of key/value pairs for
  """
  result = {}
  from xml.etree import ElementTree as ET
  import ConfigParser, StringIO
  import re

  for config_file, file_type in config_files.iteritems():
    file_name, file_ext = os.path.splitext(config_file)

    config_filepath = conf_dir + os.sep + config_file

    if not os.path.isfile(config_filepath):
      continue

    if file_type == FILE_TYPE_XML:
      configuration = ET.parse(config_filepath)
      props = configuration.getroot().getchildren()
      config_file_id = file_name if file_name else config_file
      result[config_file_id] = {}
      for prop in props:
        result[config_file_id].update({prop[0].text: prop[1].text})

    elif file_type == FILE_TYPE_PROPERTIES:
      with open(config_filepath, 'r') as f:
        config_string = '[root]\n' + f.read()
      ini_fp = StringIO.StringIO(re.sub(r'\\\s*\n', '\\\n ', config_string))
      config = ConfigParser.RawConfigParser()
      config.readfp(ini_fp)
      props = config.items('root')
      result[file_name] = {}
      for key, value in props:
        result[file_name].update({key : value})

    elif file_type == FILE_TYPE_JAAS_CONF:
      section_header = re.compile('^(\w+)\s+\{\s*$')
      section_data = re.compile('^\s*([^ \s\=\}\{]+)\s*=?\s*"?([^ ";]+)"?;?\s*$')
      section_footer = re.compile('^\}\s*;?\s*$')
      section_name = "root"
      result[file_name] = {}
      with open(config_filepath, 'r') as f:
        for line in f:
          if line:
            line = line.strip()
            m = section_header.search(line)
            if m:
              section_name = m.group(1)
              if section_name not in result[file_name]:
                result[file_name][section_name] = {}
            else:
              m = section_footer.search(line)
              if m:
                section_name = "root"
              else:
                m = section_data.search(line)
                if m:
                  result[file_name][section_name][m.group(1)] = m.group(2)

  return result


def cached_kinit_executor(kinit_path, exec_user, keytab_file, principal, hostname, temp_dir,
                          expiration_time=5):
  """
  Main cached kinit executor - Uses a temporary file on the FS to cache executions. Each command
  will have its own file and only one entry (last successful execution) will be stored
  """
  key = str(hash("%s|%s" % (principal, keytab_file)))
  filename = key + "_tmp.txt"
  file_path = temp_dir + os.sep + "kinit_executor_cache"
  output = None

  # First execution scenario dir file existence check
  if not os.path.exists(file_path):
    os.makedirs(file_path)

  file_path += os.sep + filename

  # If the file does not exist create before read
  if not os.path.isfile(file_path):
    with open(file_path, 'w+') as new_file:
      new_file.write("{}")
  try:
    with open(file_path, 'r') as cache_file:
      output = json.load(cache_file)
  except:
    # In the extraordinary case the temporary file gets corrupted the cache should be reset to avoid error loop
    with open(file_path, 'w+') as cache_file:
      cache_file.write("{}")

  if (not output) or (key not in output) or ("last_successful_execution" not in output[key]):
    new_cached_exec(key, file_path, kinit_path, temp_dir, exec_user, keytab_file, principal, hostname)
  else:
    last_run_time = output[key]["last_successful_execution"]
    now = datetime.now()
    if (now - datetime.strptime(last_run_time, "%Y-%m-%d %H:%M:%S.%f") > timedelta(minutes=expiration_time)):
      new_cached_exec(key, file_path, kinit_path, temp_dir, exec_user, keytab_file, principal, hostname)


def new_cached_exec(key, file_path, kinit_path, temp_dir, exec_user, keytab_file, principal, hostname):
  """
  Entry point of an actual execution - triggered when timeout on the cache expired or on fresh execution
  """
  now = datetime.now()
  temp_kinit_cache_fd, temp_kinit_cache_filename = mkstemp(dir=temp_dir)
  command = "%s -c %s -kt %s %s" % \
            (kinit_path, temp_kinit_cache_filename, keytab_file,
             principal.replace("_HOST", hostname))

  os.close(temp_kinit_cache_fd)

  try:
    # Ensure the proper user owns this file
    File(temp_kinit_cache_filename, owner=exec_user, mode=0600)

    # Execute the kinit
    Execute(command, user=exec_user)

    with open(file_path, 'w+') as cache_file:
      result = {key: {"last_successful_execution": str(now)}}
      json.dump(result, cache_file)
  finally:
    File(temp_kinit_cache_filename, action='delete')

def get_value(values, property_path, default_value):
  names = property_path.split('/')

  current_dict = values
  for name in names:
    if name in current_dict:
      current_dict = current_dict[name]
    else:
      return default_value

  return current_dict
