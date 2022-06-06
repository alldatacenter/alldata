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
import multiprocessing
import os
import sys
import re
import traceback
from Queue import Empty
from random import shuffle
import fnmatch
import tempfile
import shutil

from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

#excluded directories with non-test staff from stack and service scanning,
#also we can add service or stack to skip here
STACK_EXCLUDE = ["utils", "1.3.2"]
SERVICE_EXCLUDE = ["configs"]

TEST_MASK = '[Tt]est*.py'
CUSTOM_TEST_MASK = '_[Tt]est*.py'

oldtmpdirpath = tempfile.gettempdir()
newtmpdirpath = os.path.join(oldtmpdirpath, "ambari-test")
tempfile.tempdir = newtmpdirpath

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
    done = True if os.path.split(base)[-1] == directory_name else False
  return base


def get_test_files(path, mask=None, recursive=True):
  """
  Returns test files for path recursively
  """
  current = []
  directory_items = os.listdir(path)

  for item in directory_items:
    add_to_pythonpath = False
    p = os.path.join(path, item)
    if os.path.isfile(p):
      if fnmatch.fnmatch(item, mask):
        add_to_pythonpath = True
        current.append(item)
    elif os.path.isdir(p):
      if recursive:
        current.extend(get_test_files(p, mask = mask))
    if add_to_pythonpath:
      sys.path.append(path)
  return current


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def get_stack_name():
  return "HDPWIN"

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def get_stack_name():
  return "HDP"

def extract_extends_field_from_file(metainfo):
  pattern = re.compile(r"^\s*?<extends>(.*?)</extends>", re.IGNORECASE)
  if os.path.isfile(metainfo):
    with open(metainfo) as f:
        for line in f.readlines():
          m = pattern.match(line)
          if m and len(m.groups()) == 1:
            extract = m.groups(1)[0]
            return extract
  return None

def get_extends_field_from_metainfo(service_metainfo):
  """
  Parse the metainfo.xml file to retrieve the <extends> value.

  @param service_metainfo: Path to the service metainfo.xml file
  :return Extract the "extends" field if it exists and return its value. Otherwise, return None.
  """
  extract = extract_extends_field_from_file(service_metainfo)
  if extract is not None:
    return extract

  # If couldn't find it in the service's metainfo.xml, check the stack's metainfo.xml
  stack_metainfo = os.path.join(os.path.dirname(service_metainfo), "..", "..", "metainfo.xml")
  extract = extract_extends_field_from_file(stack_metainfo)

  return extract

def resolve_paths_to_import_from_common_services(metainfo_file, base_stack_folder, common_services_parent_dir, service):
  """
  Get a list of paths to append to sys.path in order to import all of the needed modules.
  This is important when a service has  multiple definitions in common-services so that we import the correct one
  instead of a higher version.

  @param metainfo_file: Path to the metainfo.xml file.
  @param base_stack_folder: Path to stacks folder that does not include the version number. This can potentially be None.
  @param common_services_parent_dir: Path to the common-services directory for a specified service, not including the version.
  @param service: Service name
  :return A list of paths to insert to sys.path by follwing the chain of inheritence.
  """
  paths_to_import = []

  if metainfo_file is None or service is None:
    return paths_to_import

  # This could be either a version number of a path to common-services
  extract = get_extends_field_from_metainfo(metainfo_file)
  if extract is not None:
    if "common-services" in extract:
      # If in common-services, we are done.
      scripts_path = os.path.join(common_services_parent_dir, extract, "package", "scripts")
      paths_to_import.append(scripts_path)
    else:
      # If a version number, we need to import it and check that version as well.
      inherited_from_older_version_path = os.path.join(base_stack_folder, "..", extract)

      metainfo_file = os.path.join(inherited_from_older_version_path, "services", service, "metainfo.xml")
      if os.path.isdir(inherited_from_older_version_path):
        paths_to_import += resolve_paths_to_import_from_common_services(metainfo_file, inherited_from_older_version_path, common_services_parent_dir, service)
  else:
    print "Service %s. Could not get extract <extends></extends> from metainfo file: %s. This may prevent modules from being imported." % (service, str(metainfo_file))

  return paths_to_import

def append_paths(server_src_dir, base_stack_folder, service):
  """
  Append paths to sys.path in order to import modules.
  
  @param server_src_dir: Server source directory
  @param base_stack_folder: If present, the directory of the stack.
  @param service: Service name.
  """
  paths_to_add = []
  if base_stack_folder is not None:
    # Append paths
    metainfo_file = None
    if base_stack_folder is not None:
      metainfo_file = os.path.join(base_stack_folder, "services", service, "metainfo.xml")

    common_services_parent_dir = os.path.join(server_src_dir, "main", "resources")
    paths_to_add = resolve_paths_to_import_from_common_services(metainfo_file, base_stack_folder, common_services_parent_dir, service)
  else:
    # We couldn't add paths using the base directory, be greedy and add all available for this service in common-services.
    # Add the common-services scripts directories to the PATH
    base_common_services_folder = os.path.join(server_src_dir, "main", "resources", "common-services")
    for folder, subFolders, files in os.walk(os.path.join(base_common_services_folder, service)):
      if "package/scripts" in folder:
        paths_to_add.append(folder)

  for path in paths_to_add:
    if os.path.exists(path) and path not in sys.path:
      sys.path.append(path)

def stack_test_executor(base_folder, service, stack, test_mask, executor_result):
  """
  Stack tests executor. Must be executed in separate process to prevent module
  name conflicts in different stacks.
  """
  #extract stack scripts folders
  server_src_dir = get_parent_path(base_folder, 'src')
  script_folders = set()

  base_stack_folder = None
  if stack is not None:
    base_stack_folder = os.path.join(server_src_dir,
                                     "main", "resources", "stacks", get_stack_name(), stack)

    for root, subFolders, files in os.walk(os.path.join(base_stack_folder,
                                                        "services", service)):
      if os.path.split(root)[-1] in ["scripts", "files"] and service in root:
        script_folders.add(root)

  append_paths(server_src_dir, base_stack_folder, service)
  tests = get_test_files(base_folder, mask = test_mask)

  #TODO Add an option to randomize the tests' execution
  #shuffle(tests)
  modules = [os.path.basename(s)[:-3] for s in tests]
  try:
    suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in
      modules]
  except:
    executor_result.put({'exit_code': 1,
                         'tests_run': 0,
                         'errors': [("Failed to load test files {0}".format(str(modules)), traceback.format_exc(), "ERROR")],
                         'failures': []})
    executor_result.put(1)
    return

  testSuite = unittest.TestSuite(suites)
  textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)

  #for pretty output
  sys.stdout.flush()
  sys.stderr.flush()
  exit_code = 0 if textRunner.wasSuccessful() else 1
  executor_result.put({'exit_code': exit_code,
                       'tests_run': textRunner.testsRun,
                       'errors': [(str(item[0]), str(item[1]), "ERROR") for item
                                  in textRunner.errors],
                       'failures': [(str(item[0]), str(item[1]), "FAIL") for
                                    item in textRunner.failures]})
  executor_result.put(0) if textRunner.wasSuccessful() else executor_result.put(1)

def main():
  if not os.path.exists(newtmpdirpath): os.makedirs(newtmpdirpath)

  if len(sys.argv) > 1 and sys.argv[1] == "true": # handle custom_tests for backward-compatibility
    test_mask = CUSTOM_TEST_MASK
  elif len(sys.argv) > 2:
    test_mask = sys.argv[2]
  else:
    test_mask = TEST_MASK

  pwd = os.path.abspath(os.path.dirname(__file__))

  ambari_server_folder = get_parent_path(pwd, 'ambari-server')
  ambari_agent_folder = os.path.normpath(os.path.join(ambari_server_folder, "../ambari-agent"))
  ambari_common_folder = os.path.normpath(os.path.join(ambari_server_folder, "../ambari-common"))
  sys.path.append(os.path.join(ambari_common_folder, "src/main/python"))
  sys.path.append(os.path.join(ambari_common_folder, "src/main/python/ambari_jinja2"))
  sys.path.append(os.path.join(ambari_common_folder, "src/test/python"))
  sys.path.append(os.path.join(ambari_agent_folder,  "src/main/python"))
  sys.path.append(os.path.join(ambari_server_folder, "src/test/python"))
  sys.path.append(os.path.join(ambari_server_folder, "src/main/python"))
  sys.path.append(os.path.join(ambari_server_folder, "src/main/resources/scripts"))
  sys.path.append(os.path.join(ambari_server_folder, "src/main/resources/custom_actions/scripts"))
  sys.path.append(os.path.join(ambari_server_folder, "src/main/resources/host_scripts"))

  stacks_folder = os.path.join(pwd, 'stacks')
  #generate test variants(path, service, stack)
  test_variants = []
  for stack in os.listdir(stacks_folder):
    current_stack_dir = os.path.join(stacks_folder, stack)
    if os.path.isdir(current_stack_dir) and stack not in STACK_EXCLUDE:
      for service in os.listdir(current_stack_dir):
        current_service_dir = os.path.join(current_stack_dir, service)
        if os.path.isdir(current_service_dir) and service not in SERVICE_EXCLUDE:
          if service == 'hooks':
            for hook in os.listdir(current_service_dir):
              test_variants.append({'directory': os.path.join(current_service_dir, hook),
                                    'service': hook,
                                    'stack': stack})
          else:
            test_variants.append({'directory': current_service_dir,
                                  'service': service,
                                  'stack': stack})

  #run tests for every service in every stack in separate process
  has_failures = False
  test_runs = 0
  test_failures = []
  test_errors = []
  for variant in test_variants:
    executor_result = multiprocessing.Queue()
    sys.stderr.write( "Running tests for stack:{0} service:{1}\n"
                      .format(variant['stack'], variant['service']))
    process = multiprocessing.Process(target=stack_test_executor,
                                      args=(variant['directory'],
                                            variant['service'],
                                            variant['stack'],
                                            test_mask,
                                            executor_result)
          )
    process.start()
    while process.is_alive():
      process.join(10)

      #for pretty output
      sys.stdout.flush()
      sys.stderr.flush()

      try:
        variant_result = executor_result.get_nowait()
        break
      except Empty as ex:
        pass

    test_runs += variant_result['tests_run']
    test_errors.extend(variant_result['errors'])
    test_failures.extend(variant_result['failures'])

    if variant_result['exit_code'] != 0:
      has_failures = True

  #run base ambari-server tests
  sys.stderr.write("Running tests for ambari-server\n")

  test_dirs = [
    (os.path.join(pwd, 'custom_actions'), "\nRunning tests for custom actions\n"),
    (os.path.join(pwd, 'host_scripts'), "\nRunning tests for host scripts\n"),
    (pwd, "\nRunning tests for ambari-server\n"),
  ]

  for test_dir, msg in test_dirs:
    sys.stderr.write(msg)
    tests = get_test_files(test_dir, mask=test_mask, recursive=False)

    # TODO Add an option to randomize the tests' execution
    # shuffle(tests)

    modules = [os.path.basename(s)[:-3] for s in tests]
    suites = [unittest.defaultTestLoader.loadTestsFromName(name) for name in modules]
    testSuite = unittest.TestSuite(suites)
    textRunner = unittest.TextTestRunner(verbosity=2).run(testSuite)
    test_runs += textRunner.testsRun

    test_errors.extend(
      [(str(item[0]), str(item[1]), "ERROR") for item in textRunner.errors])

    test_failures.extend(
      [(str(item[0]), str(item[1]), "FAIL") for item in textRunner.failures])

  if len(test_errors) > 0 or len(test_failures) > 0:
    has_failures = True

  if has_failures:
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

  try:
    shutil.rmtree(newtmpdirpath)
  except:
    #Swallow the errors, nothing to do if the dir is being held by a dangling process
    pass
  tempfile.tempdir = oldtmpdirpath
  tempfile.oldtmpdirpath = None

  if not has_failures:
    sys.stderr.write("OK\n")
    exit_code = 0
  else:
    sys.stderr.write("ERROR\n")
    exit_code = 1

  return exit_code


if __name__ == "__main__":
  sys.exit(main())

