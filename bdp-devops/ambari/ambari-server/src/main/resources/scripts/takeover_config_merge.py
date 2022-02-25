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
import tempfile
import json
import re
import base64
import time
import xml
import xml.etree.ElementTree as ET
import StringIO
import ConfigParser
from optparse import OptionGroup

logger = logging.getLogger('AmbariTakeoverConfigMerge')

CONFIG_MAPPING_HELP_TEXT = """
JSON file should content map with {regex_path : <service>-log4j}
Example:
{".+/hadoop/.+/log4j.properties" : "hdfs-log4j",
".+/etc/zookeeper/conf/log4j.properties" : "zookeeper-log4j"
"c6401.ambari.apache.org/etc/hive/conf/log4j.properties" : "hive-log4j"}
"""

LICENSE = """

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""


class Parser:
  pass

class ShParser(Parser):
  def read_data_to_map(self, path):
    with open(path, 'r') as file:
      file_content = file.read()
    return {"content" : file_content}, None

class YamlParser(Parser): # Used Yaml parser to read data into a map
  def read_data_to_map(self, path):
    try:
      import yaml
    except ImportError:
      logger.error("Module PyYAML not installed. Please try to execute \"pip install pyyaml\" for installing PyYAML module.")
      sys.exit(1)

    configurations = {}
    with open(path, 'r') as file:
      try:
        for name, value in yaml.load(file).iteritems():
          if name != None:
            configurations[name] = str(value)
      except:
        logger.error("Couldn't parse {0} file. Skipping ...".format(path))
        return None, None
    return configurations, None

class PropertiesParser(Parser): # Used ConfigParser parser to read data into a map
  def read_data_to_map(self, path):
    configurations = {}
    try :
      #Adding dummy section to properties file content for use ConfigParser
      properties_file_content = StringIO.StringIO()
      properties_file_content.write('[dummysection]\n')
      properties_file_content.write(open(path).read())
      properties_file_content.seek(0, os.SEEK_SET)

      cp = ConfigParser.ConfigParser()
      cp.optionxform = str
      cp.readfp(properties_file_content)

      for section in cp._sections:
        for name, value in cp._sections[section].iteritems():
          if name != None:
            configurations[name] = value
        del configurations['__name__']
    except:
      logger.exception("ConfigParser error: ")
    return configurations, None


class XmlParser(Parser):  # Used DOM parser to read data into a map
  def read_data_to_map(self, path):
    configurations = {}
    properties_attributes = {}
    tree = ET.parse(path)
    root = tree.getroot()
    for properties in root.getiterator('property'):
      name = properties.find('name')
      value = properties.find('value')
      #TODO support all properties attributes
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
    logger.debug("Following configurations found in {0}:\n{1}".format(path, configurations))
    return configurations, properties_attributes

class ConfigMerge:

  CONTENT_UNKNOWN_FILES_MAPPING_FILE = {}
  LEFT_INPUT_DIR = "/tmp/left"
  RIGHT_INPUT_DIR = "/tmp/right"
  INPUT_DIR = '/etc/hadoop'
  OUTPUT_DIR = '/tmp'
  OUT_FILENAME = 'ambari_takeover_config_merge.out'
  JSON_FILENAME = 'ambari_takeover_config_merge.json'
  PARSER_BY_EXTENSIONS = {'.xml' : XmlParser(), '.yaml' : YamlParser(), '.properties' : PropertiesParser(), '.sh' : ShParser()}
  SUPPORTED_EXTENSIONS = ['.xml', '.yaml', '.properties', '.sh']
  SUPPORTED_FILENAME_ENDINGS = {".sh" : "-env"}
  UNKNOWN_FILES_MAPPING_FILE = None

  CONFIGS_WITH_CONTENT = ['pig-properties', '-log4j']

  NOT_MAPPED_FILES = ['log4j.properties']


  config_files_map = {}
  left_file_paths = None
  right_file_paths = None

  def __init__(self, config_files_map=None, left_file_paths=None, right_file_paths=None):
    self.config_files_map = config_files_map
    self.left_file_paths = left_file_paths
    self.right_file_paths = right_file_paths

  @staticmethod
  def get_all_supported_files_grouped_by_name(extensions=SUPPORTED_EXTENSIONS, directory=INPUT_DIR):
    filePaths = {}
    for dirName, subdirList, fileList in os.walk(directory, followlinks=True):
      for file in fileList:
        root, ext = os.path.splitext(file)
        if ext in extensions:
          file_path = os.path.join(dirName, file)
          if ext in ConfigMerge.SUPPORTED_FILENAME_ENDINGS and not ConfigMerge.SUPPORTED_FILENAME_ENDINGS[ext] in root:
            logger.warn("File {0} is not configurable by Ambari. Skipping...".format(file_path))
            continue
          config_name = None

          if ConfigMerge.UNKNOWN_FILES_MAPPING_FILE:
            for path_regex, name in ConfigMerge.CONTENT_UNKNOWN_FILES_MAPPING_FILE.iteritems():
              match = re.match(path_regex, os.path.relpath(file_path, ConfigMerge.INPUT_DIR))
              if match:
                config_name = name
                break

          if not config_name:
            if file in ConfigMerge.NOT_MAPPED_FILES:
              if ConfigMerge.UNKNOWN_FILES_MAPPING_FILE:
                logger.error("File {0} doesn't match any regex from {1}".format(file_path, ConfigMerge.UNKNOWN_FILES_MAPPING_FILE))
              else:
                logger.error("Cannot map {0} to Ambari config type. Please use -u option to specify config mapping for this file. \n"
                             "For more information use --help option for script".format(file_path))
              continue
            else:
              config_name = file

          if not config_name in filePaths:
            filePaths[config_name] = []
          filePaths[config_name].append((file_path, ConfigMerge.PARSER_BY_EXTENSIONS[ext]))
    return filePaths

  @staticmethod
  def merge_configurations(filepath_to_configurations):
    configuration_information_dict = {}
    property_name_to_value_to_filepaths = {}
    merged_configurations = {}

    for path, configurations in filepath_to_configurations.iteritems():
      for configuration_name, value in configurations.iteritems():
        if not configuration_name in property_name_to_value_to_filepaths:
          property_name_to_value_to_filepaths[configuration_name] = {}
        if not value in property_name_to_value_to_filepaths[configuration_name]:
          property_name_to_value_to_filepaths[configuration_name][value] = []

        logger.debug("Iterating over '{0}' with value '{1}' in file '{2}'".format(configuration_name, value, path))
        property_name_to_value_to_filepaths[configuration_name][value].append(path)
        merged_configurations[configuration_name] = value

    return merged_configurations, property_name_to_value_to_filepaths

  @staticmethod
  def format_for_blueprint(configurations, attributes):
    all_configs = []
    for configuration_type, configuration_properties in configurations.iteritems():
      is_content = False
      all_configs.append({})

      for config_with_content in ConfigMerge.CONFIGS_WITH_CONTENT:
        if config_with_content in configuration_type:
          is_content = True
          break

      if is_content:
        content = LICENSE
        for property_name, property_value in configuration_properties.iteritems():
          content+=property_name + "=" + property_value + "\n"
        all_configs[-1][configuration_type] = {'properties': {"content" : content}}
      else:
        all_configs[-1][configuration_type] = {'properties' :configuration_properties}

      for configuration_type_attributes, properties_attributes in attributes.iteritems():
        if properties_attributes and configuration_type == configuration_type_attributes:
          all_configs[-1][configuration_type].update({"properties_attributes" : {"final" : properties_attributes}})

    return {
      "configurations": all_configs,
      "host_groups": [],
      "Blueprints": {}
    }

  @staticmethod
  def format_conflicts_output(property_name_to_value_to_filepaths):
    output = ""
    for property_name, value_to_filepaths in property_name_to_value_to_filepaths.iteritems():
      if len(value_to_filepaths) == 1:
        continue

      first_item = False
      for value, filepaths in value_to_filepaths.iteritems():
        if not first_item:
          first_item = True
          output += "\n\n=== {0} | {1} | {2} |\nHas conflicts with:\n\n".format(property_name,filepaths[0], value)
          continue
        for filepath in filepaths:
          output += "| {0} | {1} | {2} |\n".format(property_name, filepath, value)

    return output

  def perform_merge(self):
    result_configurations = {}
    result_property_attributes = {}
    has_conflicts = False
    for filename, paths_and_parsers in self.config_files_map.iteritems():
      filepath_to_configurations = {}
      filepath_to_property_attributes = {}
      configuration_type = os.path.splitext(filename)[0]
      for path_and_parser in paths_and_parsers:
        path, parser = path_and_parser
        logger.debug("Read data from {0}".format(path))
        parsed_configurations_from_path, parsed_properties_attributes = parser.read_data_to_map(path)
        if parsed_configurations_from_path != None:
          filepath_to_configurations[path] = parsed_configurations_from_path
        if parsed_properties_attributes != None:
          filepath_to_property_attributes[path] = parsed_properties_attributes

      #configs merge
      merged_configurations, property_name_to_value_to_filepaths = ConfigMerge.merge_configurations(
        filepath_to_configurations)

      #properties attributes merge
      merged_attributes, property_name_to_attribute_to_filepaths = ConfigMerge.merge_configurations(
        filepath_to_property_attributes)

      configuration_conflicts_output = ConfigMerge.format_conflicts_output(property_name_to_value_to_filepaths)
      attribute_conflicts_output = ConfigMerge.format_conflicts_output(property_name_to_attribute_to_filepaths)

      if configuration_conflicts_output:
        has_conflicts = True
        conflict_filename = os.path.join(self.OUTPUT_DIR, configuration_type + "-conflicts.txt")
        logger.warn(
          "You have configurations conflicts for {0}. Please check {1}".format(configuration_type, conflict_filename))
        with open(conflict_filename, "w") as fp:
          fp.write(configuration_conflicts_output)

      if attribute_conflicts_output:
        has_conflicts = True
        conflict_filename = os.path.join(self.OUTPUT_DIR, configuration_type + "-attributes-conflicts.txt")
        logger.warn(
          "You have property attribute conflicts for {0}. Please check {1}".format(configuration_type, conflict_filename))
        with open(conflict_filename, "w") as fp:
          fp.write(attribute_conflicts_output)

      result_configurations[configuration_type] = merged_configurations
      result_property_attributes[configuration_type] = merged_attributes


    result_json_file = os.path.join(self.OUTPUT_DIR, "blueprint.json")
    logger.info("Using '{0}' file as output for blueprint template".format(result_json_file))

    with open(result_json_file, 'w') as outfile:
      outfile.write(json.dumps(ConfigMerge.format_for_blueprint(result_configurations, result_property_attributes), sort_keys=True, indent=4,
                               separators=(',', ': ')))
    if has_conflicts:
      logger.info("Script finished with configurations conflicts, please resolve them before using the blueprint")
      return 1
    else:
      logger.info("Script successfully finished")
      return 0

  def perform_diff(self):
    configurations_conflicts = {}
    attributes_conflicts = {}
    file_conflicts = []
    matches_configs = []

    for right_configs_names in self.right_file_paths:
      for left_configs_names in self.left_file_paths:
        if right_configs_names == left_configs_names:
          matches_configs.append(right_configs_names)

    for match_config in matches_configs:
      configurations_conflicts[match_config], attributes_conflicts[match_config] = ConfigMerge.configuration_diff(self.left_file_paths[match_config], self.right_file_paths[match_config])

    file_conflicts = ConfigMerge.get_missing_files(self.right_file_paths, matches_configs, ConfigMerge.LEFT_INPUT_DIR) + \
                     ConfigMerge.get_missing_files(self.left_file_paths, matches_configs, ConfigMerge.RIGHT_INPUT_DIR)

    configuration_diff_output = None
    configuration_diff_output = ConfigMerge.format_diff_output(file_conflicts, configurations_conflicts, attributes_conflicts)

    if configuration_diff_output and configuration_diff_output != "":
      conflict_filename = os.path.join(ConfigMerge.OUTPUT_DIR, "file-diff.txt")
      logger.warn(
        "You have file diff conflicts. Please check {0}".format(conflict_filename))
      with open(conflict_filename, "w") as fp:
        fp.write(configuration_diff_output)

    logger.info("Script successfully finished")
    return 0

  @staticmethod
  def format_diff_output(file_conflicts, configurations_conflicts, attributes_conflicts):
    output = ""
    if file_conflicts:
      output += "======= File diff conflicts ====== \n\n"
      for file_conflict in file_conflicts:
        output+=str(file_conflict)+"\n"

    if configurations_conflicts:
      output += "\n\n======= Property diff conflicts ====== "
      for config_name, property in configurations_conflicts.iteritems():
          if property:
            output+= "\n\n||| " + config_name + " |||\n"
            output+= "\n".join(str(p) for p in property)

    if attributes_conflicts:
      output += "\n\n======= Final attribute diff conflicts ====== "
      for config_name, property_with_attribute in attributes_conflicts.iteritems():
        if property_with_attribute:
          output+= "\n\n||| " + config_name + " |||\n"
          output+= "\n".join(str(p) for p in property_with_attribute)

    return output

  @staticmethod
  def configuration_diff(left, right):
    properties_conflicts = []
    attributes_conflicts = []
    left_path, left_parser = left[0]
    left_configurations, left_attributes = left_parser.read_data_to_map(left_path)
    right_path, right_parser = right[0]
    right_configurations, right_attributes = right_parser.read_data_to_map(right_path)

    matches_configs = []
    matches_attributes = []

    matches_configs, properties_conflicts = ConfigMerge.get_conflicts_and_matches(left_configurations, right_configurations, left_path, right_path)
    properties_conflicts += ConfigMerge.get_missing_properties(left_configurations, matches_configs, right_path) + \
                            ConfigMerge.get_missing_properties(right_configurations, matches_configs, left_path)

    if left_attributes and right_attributes:
      matches_attributes, attributes_conflicts = ConfigMerge.get_conflicts_and_matches(left_attributes, right_attributes, left_path, right_path)
      attributes_conflicts += ConfigMerge.get_missing_attributes(left_attributes, matches_attributes, right_path) + \
                              ConfigMerge.get_missing_attributes(right_attributes, matches_attributes, left_path)
    elif left_attributes:
      attributes_conflicts = ConfigMerge.get_missing_attributes(left_attributes, matches_attributes, right_path)

    elif right_attributes:
      attributes_conflicts = ConfigMerge.get_missing_attributes(right_attributes, matches_attributes, left_path)

    return properties_conflicts, attributes_conflicts

  @staticmethod
  def get_conflicts_and_matches(left_items, right_items, left_path, right_path):
    matches = []
    conflicts = []
    for left_key, left_value in left_items.iteritems():
      for right_key, right_value in right_items.iteritems():
        if left_key == right_key:
          matches.append(right_key)
          if left_value != right_value:
            conflicts.append({right_key : [{left_path : left_value}, {right_path :right_value}]})
    return matches, conflicts

  @staticmethod
  def get_missing_attributes(attributes, matches, file_path):
    conflicts = []
    for key, value in attributes.iteritems():
      if not key in matches:
        conflicts.append({key : "Final attribute is missing in {0} file".format(file_path)})
    return conflicts

  @staticmethod
  def get_missing_properties(configurations, matches, file_path):
    conflicts = []
    for key, value in configurations.iteritems():
      if not key in matches:
        conflicts.append({key : "Property is missing in {0} file".format(file_path)})
    return conflicts

  @staticmethod
  def get_missing_files(config_file_paths, matches, input_dir):
    conflicts = []
    for file_name in config_file_paths:
      if file_name not in matches:
        conflicts.append({file_name : "Configurations file is missing for {0} directory".format(input_dir)})
    return conflicts

def main():
  tempDir = tempfile.gettempdir()
  outputDir = os.path.join(tempDir)

  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is an Ambari thin client and '
                         'supports Ambari cluster takeover by generating a '
                         'configuration json that can be used with a '
                         'blueprint.\n\nIt reads actual hadoop configs '
                         'from a target directory and produces an out file '
                         'with problems found that need to be addressed and '
                         'the json file which can be used to create the '
                         'blueprint.\n\nThis script only works with *.xml *.yaml '
                         'and *.properties extensions of files.')

  parser.add_option("-a", "--action", dest="action", default = "merge",
                    help="Script action. (merge/diff) [default: merge]")

  parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                    default=False, help="output verbosity.")
  parser.add_option("-o", "--outputdir", dest="outputDir", default=outputDir,
                    metavar="FILE", help="Output directory. [default: /tmp]")
  parser.add_option("-u", '--unknown-files-mapping-file',dest="unknown_files_mapping_file",
                    metavar="FILE", help=CONFIG_MAPPING_HELP_TEXT, default="takeover_files_mapping.json")


  merge_options_group = OptionGroup(parser, "Required options for action 'merge'")
  merge_options_group.add_option("-i", "--inputdir", dest="inputDir", help="Input directory.")

  parser.add_option_group(merge_options_group)

  diff_options_group = OptionGroup(parser, "Required options for action 'diff'")
  diff_options_group.add_option("-l", "--leftInputDir", dest="leftInputDir", help="Left input directory.")
  diff_options_group.add_option("-r", "--rightInputDir", dest="rightInputDir", help="Right input directory.")

  parser.add_option_group(diff_options_group)

  (options, args) = parser.parse_args()

  # set verbose
  if options.verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)

  ConfigMerge.OUTPUT_DIR = options.outputDir

  if not os.path.exists(ConfigMerge.OUTPUT_DIR):
    os.makedirs(ConfigMerge.OUTPUT_DIR)

  logegr_file_name = os.path.join(ConfigMerge.OUTPUT_DIR, "takeover_config_merge.log")

  file_handler = logging.FileHandler(logegr_file_name, mode="w")
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  file_handler.setFormatter(formatter)
  logger.addHandler(file_handler)
  stdout_handler = logging.StreamHandler(sys.stdout)
  stdout_handler.setLevel(logging.INFO)
  stdout_handler.setFormatter(formatter)
  logger.addHandler(stdout_handler)

  #unknown file mapping
  if options.unknown_files_mapping_file and os.path.exists(options.unknown_files_mapping_file):
    ConfigMerge.UNKNOWN_FILES_MAPPING_FILE = options.unknown_files_mapping_file
    with open(options.unknown_files_mapping_file) as f:
      ConfigMerge.CONTENT_UNKNOWN_FILES_MAPPING_FILE = json.load(f)
  else:
    logger.warning("Config mapping file was not found at {0}. "
                   "Please provide it at the given path or provide a different path to it using -u option.".format(options.unknown_files_mapping_file))
  if options.action == "merge" :
    ConfigMerge.INPUT_DIR = options.inputDir
    file_paths = ConfigMerge.get_all_supported_files_grouped_by_name(directory=ConfigMerge.INPUT_DIR)
    logger.info("Writing logs into '{0}' file".format(logegr_file_name))
    logger.debug("Following configuration files found:\n{0}".format(file_paths.items()))
    config_merge = ConfigMerge(config_files_map=file_paths)
    return config_merge.perform_merge()

  elif options.action == "diff" :
    if options.leftInputDir and os.path.isdir(options.leftInputDir):
      ConfigMerge.LEFT_INPUT_DIR = options.leftInputDir
    else:
      logger.error("Directory \"{0}\" doesn't exist. Use option \"-h\" for details".format(options.leftInputDir))
      return -1

    if options.rightInputDir and os.path.isdir(options.rightInputDir):
      ConfigMerge.RIGHT_INPUT_DIR = options.rightInputDir
    else:
      logger.error("Directory \"{0}\" doesn't exist. Use option \"-h\" for details".format(options.rightInputDir))
      return -1

    logger.info("Writing logs into '{0}' file".format(logegr_file_name))

    left_file_paths = ConfigMerge.get_all_supported_files_grouped_by_name(directory=ConfigMerge.LEFT_INPUT_DIR)
    logger.debug("Following configuration files found:\n{0} for left directory".format(left_file_paths.items()))
    right_file_paths = ConfigMerge.get_all_supported_files_grouped_by_name(directory=ConfigMerge.RIGHT_INPUT_DIR)
    logger.debug("Following configuration files found:\n{0} for right directory".format(right_file_paths.items()))
    config_merge = ConfigMerge(left_file_paths=left_file_paths , right_file_paths=right_file_paths)
    return config_merge.perform_diff()

  else:
    logger.error("Action \"{0}\" doesn't supports by script. Use option \"-h\" for details".format(options.action))
    return -1

if __name__ == "__main__":
  try:
    sys.exit(main())
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
