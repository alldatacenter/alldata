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

"""
SAMPLE USAGE:

python unitTests.py
python unitTests.py NameOfFile.py
python unitTests.py NameOfFileWithoutExtension  (this will append .* to the end, so it can match other file names too)

prepend _ to test file name(s) and run "python unitTests.py": execute only
  test files whose name begins with _ (useful for quick debug)

SETUP:
To run in Linux from command line,
cd to this same directory. Then make sure PYTHONPATH is correct.

export PYTHONPATH=$PYTHONPATH:$(pwd)/ambari-agent/src/test/python:
$(pwd)/ambari-common/src/test/python:
$(pwd)/ambari-agent/src/test/python/ambari_agent:
$(pwd)/ambari-common/src/main/python:
$(pwd)/ambari-server/src/main/resources/common-services/HDFS/2.1.0.2.0/package/files:
$(pwd)/ambari-agent/src/test/python/resource_management:
$(pwd)/ambari-common/src/main/python/ambari_jinja2
"""

import re
import unittest
import fnmatch
from os.path import isdir
import logging
from only_for_platform import get_platform, PLATFORM_WINDOWS
from resource_management.core.logger import Logger
#TODO Add an option to randomize the tests' execution
#from random import shuffle

LOG_FILE_NAME='tests.log'
SELECTED_PREFIX = "_"
PY_EXT='.py'


class TestAgent(unittest.TestSuite):
  def run(self, result):
    run = unittest.TestSuite.run
    run(self, result)
    return result


def parent_dir(path):
  if isdir(path):
    if path.endswith(os.sep):
      path = os.path.dirname(path)
    parent_dir = os.path.dirname(path)
  else:
    parent_dir = os.path.dirname(os.path.dirname(path))

  return parent_dir

def get_test_files(path, mask=None, recursive=True):
  """
  Returns test files for path recursively
  """
  # Must convert mask so it can match a file
  if mask and mask != "" and not mask.endswith("*"):
    mask=mask+"*"

  file_list = []
  directory_items = os.listdir(path)

  for item in directory_items:
    add_to_pythonpath = False
    p = os.path.join(path, item)
    if os.path.isfile(p):
      if mask is not None and fnmatch.fnmatch(item, mask) or \
        mask is None and re.search(r"^_?[Tt]est.*\.py$", item):
          add_to_pythonpath = True
          file_list.append(item)
    elif os.path.isdir(p):
      if recursive:
        file_list.extend(get_test_files(p, mask=mask))
    if add_to_pythonpath:
      sys.path.append(path)

  return file_list


def all_tests_suite(custom_test_mask):
  test_mask = custom_test_mask if custom_test_mask else None

  src_dir = os.getcwd()
  files_list = get_test_files(src_dir, mask=test_mask)

  #TODO Add an option to randomize the tests' execution
  #shuffle(files_list)
  tests_list = []

  logger.info('------------------------TESTS LIST:-------------------------------------')
  # If test with special name exists, run only this test
  selected_test = None
  for file_name in files_list:
    if file_name.endswith(PY_EXT) and not file_name == __file__ and file_name.startswith(SELECTED_PREFIX):
      logger.info("Running only selected test " + str(file_name))
      selected_test = file_name
  if selected_test is not None:
      tests_list.append(selected_test.replace(PY_EXT, ''))
  else:
    for file_name in files_list:
      if file_name.endswith(PY_EXT) and not file_name == __file__:
        logger.info(file_name)
        tests_list.append(file_name.replace(PY_EXT, ''))
  logger.info('------------------------------------------------------------------------')

  suite = unittest.TestLoader().loadTestsFromNames(tests_list)
  return unittest.TestSuite([suite])

def main():
  test_mask = None
  if len(sys.argv) >= 2:
    test_mask = sys.argv[1]

  logger.info('------------------------------------------------------------------------')
  logger.info('PYTHON AGENT TESTS')
  logger.info('------------------------------------------------------------------------')
  runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
  suite = all_tests_suite(test_mask)
  status = runner.run(suite).wasSuccessful()

  if not status:
    logger.error('-----------------------------------------------------------------------')
    logger.error('Python unit tests failed')
    logger.error('Find detailed logs in ' + path)
    logger.error('-----------------------------------------------------------------------')
    if get_platform() == PLATFORM_WINDOWS:
      os._exit(1)
    else:
      exit(1)
  else:
    logger.info('------------------------------------------------------------------------')
    logger.info('Python unit tests finished successfully')
    logger.info('------------------------------------------------------------------------')

if __name__ == '__main__':
  import os
  import sys

  pwd = os.path.abspath(__file__)
  ambari_agent_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(pwd))))
  src_dir = os.path.dirname(ambari_agent_dir)
  ambari_common_dir = os.path.join(src_dir, "ambari-common")

  sys.path.insert(0, os.path.join(ambari_agent_dir, "src", "main", "python"))
  sys.path.insert(0, os.path.join(ambari_agent_dir, "src", "main", "python", "ambari_agent"))
  sys.path.insert(0, os.path.join(ambari_common_dir, "src", "main", "python"))
  sys.path.insert(0, os.path.join(ambari_common_dir, "src", "main", "python", "ambari_jinja2"))
  sys.path.insert(0, os.path.join(ambari_common_dir, "src", "test", "python"))

  logger = logging.getLogger()
  logger.setLevel(logging.INFO)
  formatter = logging.Formatter("[%(levelname)s] %(message)s")
  src_dir = os.getcwd()
  target_dir = parent_dir(parent_dir(parent_dir(src_dir))) + os.sep + 'target'
  if not os.path.exists(target_dir):
    os.mkdir(target_dir)
  path = target_dir + os.sep + LOG_FILE_NAME
  file=open(path, "w")
  consoleLog = logging.StreamHandler(file)
  consoleLog.setFormatter(formatter)
  logger.addHandler(consoleLog)
  Logger.initialize_logger(logging_level = logging.WARNING)

  main()
