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

import unittest
import os
import sys
from random import shuffle
import fnmatch

#excluded directories with non-test staff from stack and service scanning,
#also we can add service or stack to skip here
STACK_EXCLUDE = ["utils"]
SERVICE_EXCLUDE = ["configs"]

TEST_MASK = '[Tt]est*.py'
CUSTOM_TEST_MASK = '_[Tt]est*.py'
def get_parent_path(base, directory_name):
  """
  Returns absolute path for directory_name, if directory_name present in base.
  For example, base=/home/user/test2, directory_name=user - will return /home/user
  """
  done = False
  while not done:
    base = os.path.dirname(base)
    if base == "/":
      return None
    if os.path.split(base)[-1] == directory_name:
      done = True
    else:
      done = False
  return base

def get_test_files(path, mask = None, recursive=True):
  """
  Returns test files for path recursively
  """
  current = []
  directory_items = os.listdir(path)
  directory_items.sort()

  for item in directory_items:
    add_to_pythonpath = False
    item_path = os.path.join(path, item)
    if os.path.isfile(item_path):
      if fnmatch.fnmatch(item, mask):
        add_to_pythonpath = True
        current.append(item)
    elif os.path.isdir(item_path):
      if recursive:
        current.extend(get_test_files(item_path, mask = mask))
    if add_to_pythonpath:
      sys.path.append(path)
  return current


def main():
  custom_tests = False
  if len(sys.argv) > 1:
    if sys.argv[1] == "true":
      custom_tests = True
  pwd = os.path.abspath(os.path.dirname(__file__))

  project_folder = get_parent_path(pwd,'isilon-onefs-mpack')
  sys.path.append(project_folder + "/src/main/resources/addon-services/ONEFS/1.0.0")
  sys.path.append(project_folder + "/src/test/python")
  sys.path.append(project_folder + "/../../../ambari-agent/src/main/python")
  sys.path.append(project_folder + "/../../../ambari-common/src/main/python")

  has_failures = False
  test_runs = 0
  test_failures = []
  test_errors = []
  sys.stderr.write("Running tests\n")
  if custom_tests:
    test_mask = CUSTOM_TEST_MASK
  else:
    test_mask = TEST_MASK

  tests = get_test_files(pwd, mask=test_mask, recursive=True)
  shuffle(tests)
  modules = [os.path.basename(s)[:-3] for s in tests]
  suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in
            modules]
  testSuite = unittest.TestSuite(suites)
  textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)
  test_runs += textRunner.testsRun
  test_errors.extend([(str(item[0]),str(item[1]),"ERROR") for item in textRunner.errors])
  test_failures.extend([(str(item[0]),str(item[1]),"FAIL") for item in textRunner.failures])
  tests_status = textRunner.wasSuccessful() and not has_failures

  if not tests_status:
    sys.stderr.write("----------------------------------------------------------------------\n")
    sys.stderr.write("Failed tests:\n")
  for failed_tests in [test_errors,test_failures]:
    for err in failed_tests:
      sys.stderr.write("{0}: {1}\n".format(err[2],err[0]))
      sys.stderr.write("----------------------------------------------------------------------\n")
      sys.stderr.write("{0}\n".format(err[1]))
  sys.stderr.write("----------------------------------------------------------------------\n")
  sys.stderr.write("Total run:{0}\n".format(test_runs))
  sys.stderr.write("Total errors:{0}\n".format(len(test_errors)))
  sys.stderr.write("Total failures:{0}\n".format(len(test_failures)))

  if tests_status:
    sys.stderr.write("OK\n")
    exit_code = 0
  else:
    sys.stderr.write("ERROR\n")
    exit_code = 1
  return exit_code


if __name__ == "__main__":
  sys.exit(main())

