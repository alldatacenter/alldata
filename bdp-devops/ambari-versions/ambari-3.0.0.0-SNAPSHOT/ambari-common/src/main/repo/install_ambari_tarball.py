#!/usr/bin/env python2
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

import re
import os
import sys
import logging
from ambari_commons import subprocess32
from optparse import OptionParser
import ConfigParser

USAGE = "Usage: %prog [OPTION]... URL"
DESCRIPTION = "URL should point to full tar.gz location e.g.: https://public-repo-1.hortonworks.com/something/ambari-server.tar.gz"

logger = logging.getLogger("install_ambari_tarball")

PREINST_SCRIPT = "preinst"
PRERM_SCRIPT = "prerm"
POSTINST_SCRIPT = "postinst"
POSTRM_SCRIPT = "postrm"
OS_CHECK = "os_check.py"
OS_PACKAGE_DEPENDENCIES = "dependencies.properties"
OS_FAMILY_DESCRIPTION = "resources/os_family.json"
RPM_DEPENDENCIES_PROPERTY =  "rpm.dependency.list"
DEB_DEPENDENCIES_PROPERTY =  "deb.dependency.list"

FILES_TO_DOWNLOAD = [PREINST_SCRIPT, PRERM_SCRIPT, POSTINST_SCRIPT, POSTRM_SCRIPT, OS_CHECK, OS_FAMILY_DESCRIPTION, OS_PACKAGE_DEPENDENCIES]

ROOT_FOLDER_ENV_VARIABLE = "RPM_INSTALL_PREFIX"
          
class Utils:
  verbose = False
  @staticmethod
  def os_call(command, logoutput=None, env={}):
    shell = not isinstance(command, list)
    print_output = logoutput==True or (logoutput==None and Utils.verbose)
    
    if not print_output:
      stdout = subprocess32.PIPE
      stderr = subprocess32.STDOUT
    else:
      stdout = stderr = None
    
    logger.info("Running '{0}'".format(command))
    proc = subprocess32.Popen(command, shell=shell, stdout=stdout, stderr=stderr, env=env)
      
    if not print_output:
      out = proc.communicate()[0].strip('\n')
    else:
      proc.wait()
      out = None
      
    code = proc.returncode
  
    if code:
      err_msg = ("Execution of '%s'\n returned %d. %s") % (command, code, out)
      raise OsCallFailure(err_msg)
      
    return out
  
  @staticmethod
  def install_package(name):
    from os_check import OSCheck
    
    logger.info("Checking for existance of {0} dependency package".format(name))
    is_rpm = not OSCheck.is_ubuntu_family()
    
    if is_rpm:
      is_installed_cmd = ['rpm', '-q'] + [name]
      install_cmd = ['sudo', 'yum', '-y', 'install'] + [name]
    else:
      is_installed_cmd = ['dpkg', '-s'] + [name]
      install_cmd = ['sudo', 'apt-get', '-y', 'install'] + [name]
      
    try:
      Utils.os_call(is_installed_cmd, logoutput=False)
      logger.info("Package {0} is already installed. Skipping installation.".format(name))
    except OsCallFailure:
      logger.info("Package {0} is not installed. Installing it...".format(name))
      Utils.os_call(install_cmd)
    
  
class FakePropertiesHeader(object):
  """
  Hacky class to parse properties file without sections.
  see http://stackoverflow.com/questions/2819696/module-to-use-when-parsing-properties-file-in-python/2819788#2819788
  """
  FAKE_SECTION_NAME = 'section'
  def __init__(self, fp):
    self.fp = fp
    self.sechead = '[{0}]\n'.format(FakePropertiesHeader.FAKE_SECTION_NAME)
  def readline(self):
    if self.sechead:
      try: 
        return self.sechead
      finally: 
        self.sechead = None
    else: 
      return self.fp.readline()
  
class OsCallFailure(RuntimeError):
  pass

class Installer:
  def __init__(self, archive_url, root_folder, verbose, skip_dependencies):
    splited_url = archive_url.split('/')
    self.archive_name = splited_url[-1]
    self.base_url = '/'.join(splited_url[0:-1])
    self.root_folder = root_folder
    self.verbose = verbose
    self.skip_dependencies = skip_dependencies
    
  def download_files(self, files_list):
    for name in files_list:
      dirname = os.path.dirname(name)
      if dirname:
        Utils.os_call(["mkdir", "-p", dirname])
        
      url = "{0}/{1}".format(self.base_url, name)
      logger.info("Downloading {0}".format(url))
      Utils.os_call(["wget", "-O", name, url])
    
  def run(self):
    self.download_files([self.archive_name] +FILES_TO_DOWNLOAD) # [self.archive_name] +
    
    self.check_dependencies()
    self.run_script(PRERM_SCRIPT, ["remove"]) # in case we are upgrading
    self.run_script(POSTRM_SCRIPT, ["remove"]) # in case we are upgrading
    
    self.run_script(PREINST_SCRIPT, ["install"])
    self.extract_archive()
    self.run_script(POSTINST_SCRIPT, ["configure"])
    
  def check_dependencies(self):
    from os_check import OSCheck
    
    os_family = OSCheck.get_os_family()
    os_version = OSCheck.get_os_major_version()
    
    is_rpm = not OSCheck.is_ubuntu_family()
    property_prefix = RPM_DEPENDENCIES_PROPERTY if is_rpm else DEB_DEPENDENCIES_PROPERTY
    
    cp = ConfigParser.SafeConfigParser()
    with open(OS_PACKAGE_DEPENDENCIES) as fp:
      cp.readfp(FakePropertiesHeader(fp))
      
    properties = dict(cp.items(FakePropertiesHeader.FAKE_SECTION_NAME))
    
    packages_string = None
    postfixes = [os_family+str(ver) for ver in range(int(os_version),0,-1)+['']]+['']
    for postfix in postfixes:
      property_name = property_prefix + postfix
      if property_name in properties:
        packages_string = properties[property_name]
        break
      
    if packages_string is None:
      err_msg = "No os dependencies found. "
      if self.skip_dependencies:
        logger.warn(err_msg)
      else:
        raise Exception(err_msg)
    
    packages_string = re.sub('Requires\s*:','',packages_string)
    packages_string = re.sub('\\\\n','',packages_string)
    packages_string = re.sub('\s','',packages_string)
    packages_string = re.sub('[()]','',packages_string)
    
    if self.skip_dependencies:
      var = raw_input("Please confirm you have the following packages installed {0} (y/n): ".format(packages_string))
      if var.lower() != "y" and var.lower() != "yes":
        raise Exception("User canceled the installation.")
      return
    
    pacakges = packages_string.split(',')
    
    for package in pacakges:
      split_parts = re.split('[><=]', package)
      package_name = split_parts[0]
      Utils.install_package(package_name)
    
  def run_script(self, script_name, args):
    bash_args = []
    if self.verbose:
      bash_args.append("-x")
      
    Utils.os_call(["bash"] + bash_args + [script_name] + args, env={ROOT_FOLDER_ENV_VARIABLE: self.root_folder}, logoutput=True)
    

class TargzInstaller(Installer):
  def extract_archive(self):
    Utils.os_call(['tar','--no-same-owner', '-xvf', self.archive_name, '-C', self.root_folder+os.sep], logoutput=False)


class Runner:
  def parse_opts(self):
    parser = OptionParser(usage=USAGE, description=DESCRIPTION)
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                      help="sets output level to more detailed")
    parser.add_option("-r", "--root-folder", dest="root_folder", default="/",
                      help="root folder to install Ambari to. E.g.: /opt")
    parser.add_option("-d", "--dependencies-skip", dest="skip_dependencies", action="store_true",
                  help="the script won't install the package dependencies. Please make sure to install them manually.")
    
    (self.options, args) = parser.parse_args()
    
    if len(args) != 1:
      help = parser.print_help()
      sys.exit(1)
      
    self.url = args[0]
    
  @staticmethod
  def setup_logger(verbose):
    logging_level = logging.DEBUG if verbose else logging.INFO
    logger.setLevel(logging_level)

    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setLevel(logging_level)
    stdout_handler.setFormatter(formatter)
    logger.addHandler(stdout_handler)
    
  def run(self):
    self.parse_opts()
    Runner.setup_logger(self.options.verbose)
    Utils.verbose = self.options.verbose
    
    # TODO: check if ends with tar.gz?
    targz_installer = TargzInstaller(self.url, self.options.root_folder, self.options.verbose, self.options.skip_dependencies)
    targz_installer.run()
      
if __name__ == '__main__':
  Runner().run()