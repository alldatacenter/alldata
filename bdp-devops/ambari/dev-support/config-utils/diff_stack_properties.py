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

import sys
import os

import xml.etree.ElementTree as ET

COMMON = "common-services"
STACKS = "stacks"
CONFIG_DIR = "configuration"
SERVICES_DIR = "services"

SYMLINKS_TXT = "symlinks.txt"
VERSIONS_TXT = "versions.txt"


def main():
  """ Parse arguments from user, check that all required args are passed in and start work."""

  if len(sys.argv) != 3:
    print "usage: diff_stack_properties.py [first_stack_dir] [second_stack_dir]"
    sys.exit(-1)

  args = sys.argv[1:]

  if not os.path.exists(args[0]) or not os.path.exists(args[1]):
    print "usage: diff_stack_properties.py [first_stack_dir] [second_stack_dir]"
    sys.exit(-1)

  args = sys.argv[1:]

  do_work(args)


def do_work(args):
  """
  Compare stack dirs.
  :param args:
  """
  new_stacks = args[0]
  old_stacks = args[1]

  compare_common(new_stacks, old_stacks)

  compare_stacks(new_stacks, old_stacks)


def compare_stacks(new_stacks, old_stacks):
  print "#############[{}]#############".format(STACKS)
  for stack in [stack for stack in os.listdir(os.path.join(new_stacks, STACKS)) if
                os.path.isdir(os.path.join(new_stacks, STACKS, stack))]:
    for version in os.listdir(os.path.join(new_stacks, STACKS, stack)):
      if os.path.exists(os.path.join(new_stacks, STACKS, stack, version, CONFIG_DIR)):
        diff = compare_config_dirs(os.path.join(new_stacks, STACKS, stack, version, CONFIG_DIR),
                                   os.path.join(old_stacks, STACKS, stack, version, CONFIG_DIR))
        if diff != "":
          print "#############{}.{}#############".format(stack, version)
          print diff
      if os.path.exists(os.path.join(new_stacks, STACKS, stack, version, SERVICES_DIR)):
        print "#############{}.{}#############".format(stack, version)
        for service_name in os.listdir(os.path.join(new_stacks, STACKS, stack, version, SERVICES_DIR)):
          new_configs_dir = os.path.join(new_stacks, STACKS, stack, version, SERVICES_DIR, service_name, CONFIG_DIR)
          old_configs_dir = os.path.join(old_stacks, STACKS, stack, version, SERVICES_DIR, service_name, CONFIG_DIR)
          diff = compare_config_dirs(new_configs_dir, old_configs_dir)
          if diff != "":
            print "=========={}==========".format(service_name)
            print diff


def compare_common(new_stacks, old_stacks):
  print "#############[{}]#############".format(COMMON)
  for service_name in os.listdir(os.path.join(new_stacks, COMMON)):
    for version in os.listdir(os.path.join(new_stacks, COMMON, service_name)):
      new_configs_dir = os.path.join(new_stacks, COMMON, service_name, version, CONFIG_DIR)
      old_configs_dir = os.path.join(old_stacks, COMMON, service_name, version, CONFIG_DIR)
      diff = compare_config_dirs(new_configs_dir, old_configs_dir)
      if diff != "":
        print "=========={}.{}==========".format(service_name, version)
        print diff


def compare_config_dirs(new_configs_dir, old_configs_dir):
  result = ""
  if os.path.exists(old_configs_dir) and os.path.exists(new_configs_dir):
    for file_name in os.listdir(new_configs_dir):
      old_file_name = os.path.join(old_configs_dir, file_name)
      if os.path.exists(old_file_name):
        result += compare_config_files(os.path.join(new_configs_dir, file_name),
                                       os.path.join(old_configs_dir, file_name),
                                       file_name)
      else:
        result += "new file {}\n".format(file_name)
  else:
    if os.path.exists(old_configs_dir) or os.path.exists(new_configs_dir):
      if not os.path.exists(new_configs_dir):
        result += "deleted configuration dir {}\n".format(new_configs_dir)
      if not os.path.exists(old_configs_dir):
        result += "new configuration dir {} with files {} \n".format(new_configs_dir, os.listdir(new_configs_dir))
  return result


def compare_config_files(new_configs, old_configs, file_name):
  result = ""
  if os.path.exists(old_configs):
    old_configs_tree = ET.ElementTree(file=old_configs)
    new_configs_tree = ET.ElementTree(file=new_configs)
    for new_property in new_configs_tree.findall("property"):
      name = new_property.find("name").text
      if new_property.find("value") is not None:
        value = new_property.find("value").text
      if new_property.find("on-ambari-upgrade") is not None:
        on_amb_upgrade = new_property.find("on-ambari-upgrade").get("add")
      else:
        on_amb_upgrade = None

      deleted = None
      old_deleted = None
      if new_property.find("deleted") is not None:
        deleted = new_property.find("deleted").text
      old_property = old_configs_tree.find("property[name='{}']".format(name))

      if on_amb_upgrade == "true" and old_property is None:
        result += "add {}\n".format(name)
      else:
        if old_property is not None and old_property.find("deleted") is not None:
          old_deleted = old_property.find("deleted").text
        if deleted == "true" and old_deleted != "true":
          result += "deleted {}\n".format(name)
    if result != "":
      result = "------{}------\n".format(file_name) + result
  else:
    result += "{} not exists\n".format(old_configs, )
  return result


if __name__ == "__main__":
  main()
