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

import re
import os
import sys
import platform

def _get_windows_version():
  """
  Get's the OS major and minor versions.  Returns a tuple of
  (OS_MAJOR, OS_MINOR).
  """
  import ctypes

  class _OSVERSIONINFOEXW(ctypes.Structure):
    _fields_ = [('dwOSVersionInfoSize', ctypes.c_ulong),
                ('dwMajorVersion', ctypes.c_ulong),
                ('dwMinorVersion', ctypes.c_ulong),
                ('dwBuildNumber', ctypes.c_ulong),
                ('dwPlatformId', ctypes.c_ulong),
                ('szCSDVersion', ctypes.c_wchar*128),
                ('wServicePackMajor', ctypes.c_ushort),
                ('wServicePackMinor', ctypes.c_ushort),
                ('wSuiteMask', ctypes.c_ushort),
                ('wProductType', ctypes.c_byte),
                ('wReserved', ctypes.c_byte)]

  os_version = _OSVERSIONINFOEXW()
  os_version.dwOSVersionInfoSize = ctypes.sizeof(os_version)
  retcode = ctypes.windll.Ntdll.RtlGetVersion(ctypes.byref(os_version))
  if retcode != 0:
    raise Exception("Failed to get OS version")

  return os_version.dwMajorVersion, os_version.dwMinorVersion, os_version.dwBuildNumber, os_version.wProductType

# path to resources dir
RESOURCES_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "resources")

# family JSON data
OSFAMILY_JSON_RESOURCE = "os_family.json"
JSON_OS_MAPPING = "mapping"
JSON_OS_ALIASES = "aliases"
JSON_OS_TYPE = "distro"
JSON_OS_VERSION = "versions"
JSON_EXTENDS = "extends"

#windows family constants
SYSTEM_WINDOWS = "Windows"
REL_2008 = "win2008server"
REL_2008R2 = "win2008serverr2"
REL_2012 = "win2012server"
REL_2012R2 = "win2012serverr2"

# windows machine types
VER_NT_WORKSTATION = 1
VER_NT_DOMAIN_CONTROLLER = 2
VER_NT_SERVER = 3

# Linux specific releases, caching them since they are execution invariants
_IS_ORACLE_LINUX = os.path.exists('/etc/oracle-release')
_IS_REDHAT_LINUX = os.path.exists('/etc/redhat-release')

OS_RELEASE_FILE = "/etc/os-release"

def _is_oracle_linux():
  return _IS_ORACLE_LINUX

def _is_redhat_linux():
  return _IS_REDHAT_LINUX

def _is_powerpc():
  return platform.processor() == 'powerpc' or platform.machine().startswith('ppc')

def advanced_check(distribution):
  distribution = list(distribution)
  if os.path.exists(OS_RELEASE_FILE):
    with open(OS_RELEASE_FILE, "rb") as fp:
      file_content = fp.read()
  
    search_groups = re.search('NAME="(.+)"', file_content)
    name = search_groups.group(1) if search_groups else ''

    if "amazon" in name.lower():
      distribution[0] = "amazonlinux"
      search_groups = re.search('VERSION_ID="(\d+)"', file_content)
      
      if search_groups:
        distribution[1] = search_groups.group(1)
      
  return tuple(distribution)
    

class OS_CONST_TYPE(type):

  # Declare here os type mapping
  OS_FAMILY_COLLECTION = []
  # Would be generated from Family collection definition
  OS_COLLECTION = []
  FAMILY_COLLECTION = []

  def initialize_data(cls):
    """
      Initialize internal data structures from file
    """
    try:
      f = open(os.path.join(RESOURCES_DIR, OSFAMILY_JSON_RESOURCE))
      json_data = eval(f.read())
      f.close()
      
      if JSON_OS_MAPPING not in json_data:
        raise Exception("Invalid {0}".format(OSFAMILY_JSON_RESOURCE))
      
      json_mapping_data = json_data[JSON_OS_MAPPING]
      
      for family in json_mapping_data:
        cls.FAMILY_COLLECTION += [family]
        cls.OS_COLLECTION += json_mapping_data[family][JSON_OS_TYPE]
        cls.OS_FAMILY_COLLECTION += [{
          'name': family,
          'os_list': json_mapping_data[family][JSON_OS_TYPE]
        }]
        
        if JSON_EXTENDS in json_mapping_data[family]:
          cls.OS_FAMILY_COLLECTION[-1][JSON_EXTENDS] = json_mapping_data[family][JSON_EXTENDS]
          
        cls.OS_TYPE_ALIASES = json_data[JSON_OS_ALIASES] if JSON_OS_ALIASES in json_data else {}
    except:
      raise Exception("Couldn't load '%s' file" % OSFAMILY_JSON_RESOURCE)

  def __init__(cls, name, bases, dct):
    cls.initialize_data()

  def __getattr__(cls, name):
    """
      Added support of class.OS_<os_type> properties defined in OS_COLLECTION
      Example:
              OSConst.OS_CENTOS would return centos
              OSConst.OS_OTHEROS would triger an error, coz
               that os is not present in OS_FAMILY_COLLECTION map
    """
    name = name.lower()
    if "os_" in name and name[3:] in cls.OS_COLLECTION:
      return name[3:]
    if "_family" in name and name[:-7] in cls.FAMILY_COLLECTION:
      return name[:-7]
    raise Exception("Unknown class property '%s'" % name)


class OSConst:
  __metaclass__ = OS_CONST_TYPE

  systemd_redhat_os_major_versions = ["7"]


class OSCheck:

  @staticmethod
  def os_distribution():
    if platform.system() == SYSTEM_WINDOWS:
      # windows distribution
      major, minor, build, code = _get_windows_version()
      if code in (VER_NT_DOMAIN_CONTROLLER, VER_NT_SERVER):
        # we are on server os
        release = None
        if major == 6:
          if minor == 0:
            release = REL_2008
          elif minor == 1:
            release = REL_2008R2
          elif minor == 2:
            release = REL_2012
          elif minor == 3:
            release = REL_2012R2
        distribution = (release, "{0}.{1}".format(major,minor),"WindowsServer")
      else:
        # we are on unsupported desktop os
        distribution = ("", "", "")
    else:
      # linux distribution
      PYTHON_VER = sys.version_info[0] * 10 + sys.version_info[1]

      if PYTHON_VER <= 26:
        raise RuntimeError("Python 2.6 or less not supported")
      elif _is_redhat_linux():
        distribution = platform.dist()
      else:
        distribution = platform.linux_distribution()

    if distribution[0] == '':
      distribution = advanced_check(distribution)
    
      if platform.system().lower() == 'darwin':
        # mac - used for unit tests
        distribution = ("Darwin", "TestOnly", "1.1.1", "1.1.1", "1.1")
    
    return distribution
  
  @staticmethod
  def get_alias(os_type, os_version):
    version_parts = os_version.split('.')
    full_os_and_major_version = os_type + version_parts[0]

    if full_os_and_major_version in OSConst.OS_TYPE_ALIASES:
      alias = OSConst.OS_TYPE_ALIASES[full_os_and_major_version]
      re_groups = re.search('(\D+)(\d+)$', alias).groups()
      os_type = re_groups[0]
      os_major_version = re_groups[1]
      
      version_parts[0] = os_major_version
      os_version = '.'.join(version_parts)
      
    return os_type, os_version
      
    
  @staticmethod
  def get_os_type():
    """
    Return values:
    redhat, fedora, centos, oraclelinux, ascendos,
    amazon, xenserver, oel, ovs, cloudlinux, slc, scientific, psbm,
    ubuntu, debian, sles, sled, opensuse, suse ... and others

    In case cannot detect - exit.
    """
    return OSCheck.get_alias(OSCheck._get_os_type(), OSCheck._get_os_version())[0]

  @staticmethod
  def _get_os_type():
    # Read content from /etc/*-release file
    # Full release name
    dist = OSCheck.os_distribution()
    operatingSystem = dist[0].lower()

    # special cases
    if _is_oracle_linux():
      operatingSystem = 'oraclelinux'
    elif operatingSystem.startswith('suse linux enterprise server'):
      operatingSystem = 'sles'
    elif operatingSystem.startswith('red hat enterprise linux'):
      operatingSystem = 'redhat'
    elif operatingSystem.startswith('darwin'):
      operatingSystem = 'mac'

    if operatingSystem == '':
      raise Exception("Cannot detect os type. Exiting...")

    if _is_powerpc():
      operatingSystem += '-ppc'
    
    return operatingSystem

  @staticmethod
  def get_os_family():
    """
    Return values:
    redhat, debian, suse ... and others

    In case cannot detect raises exception( from self.get_operating_system_type() ).
    """
    os_family = OSCheck.get_os_type()
    for os_family_item in OSConst.OS_FAMILY_COLLECTION:
      if os_family in os_family_item['os_list']:
        os_family = os_family_item['name']
        break

    return os_family.lower()

  @staticmethod
  def get_os_family_parent(os_family):
    for os_family_item in OSConst.OS_FAMILY_COLLECTION:
      if os_family_item['name'] == os_family:
        if JSON_EXTENDS in os_family_item:
          return os_family_item[JSON_EXTENDS]
        else:
          return None

  @staticmethod
  def get_os_version():
    """
    Returns the OS version

    In case cannot detect raises exception.
    """
    return OSCheck.get_alias(OSCheck._get_os_type(), OSCheck._get_os_version())[1]
    
  @staticmethod
  def _get_os_version():
    # Read content from /etc/*-release file
    # Full release name
    dist = OSCheck.os_distribution()
    dist = dist[1]
    
    if dist:
      return dist
    else:
      raise Exception("Cannot detect os version. Exiting...")

  @staticmethod
  def get_os_major_version():
    """
    Returns the main OS version like
    Centos 6.5 --> 6
    RedHat 1.2.3 --> 1
    """
    return OSCheck.get_os_version().split('.')[0]

  @staticmethod
  def get_os_release_name():
    """
    Returns the OS release name

    In case cannot detect raises exception.
    """
    dist = OSCheck.os_distribution()
    dist = dist[2].lower()

    if dist:
      return dist
    else:
      raise Exception("Cannot detect os release name. Exiting...")

  #  Exception safe family check functions

  @staticmethod
  def is_ubuntu_family():
    """
     Return true if it is so or false if not

     This is safe check for ubuntu/debian families, doesn't generate exception
    """
    return OSCheck.is_in_family(OSCheck.get_os_family(), OSConst.UBUNTU_FAMILY)

  @staticmethod
  def is_suse_family():
    """
     Return true if it is so or false if not

     This is safe check for suse family, doesn't generate exception
    """
    return OSCheck.is_in_family(OSCheck.get_os_family(), OSConst.SUSE_FAMILY)

  @staticmethod
  def is_redhat_family():
    """
     Return true if it is so or false if not

     This is safe check for redhat family, doesn't generate exception
    """
    return OSCheck.is_in_family(OSCheck.get_os_family(), OSConst.REDHAT_FAMILY)
  
  @staticmethod
  def is_in_family(current_family, family):
    try:
      if current_family == family or OSCheck.get_os_family_parent(current_family) and OSCheck.is_in_family(OSCheck.get_os_family_parent(current_family), family):
        return True
    except Exception:
      pass
    return False    

  @staticmethod
  def is_windows_family():
    """
     Return true if it is so or false if not

     This is safe check for winsrv , doesn't generate exception
    """
    try:
      return OSCheck.get_os_family() == OSConst.WINSRV_FAMILY
    except Exception:
      pass
    return False
