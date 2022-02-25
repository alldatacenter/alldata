#!/usr/bin/env ambari-python-wrap

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

import logging
import os
import getpass
import platform
import hostname
import re
import shlex
import socket
import multiprocessing
from ambari_commons import subprocess32
from ambari_commons.shell import shellRunner
import time
import uuid
import json
import glob
from AmbariConfig import AmbariConfig
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

log = logging.getLogger()


def run_os_command(cmd):
  shell = (type(cmd) == str)
  process = subprocess32.Popen(cmd,
                             shell=shell,
                             stdout=subprocess32.PIPE,
                             stdin=subprocess32.PIPE,
                             stderr=subprocess32.PIPE
  )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata


class Facter(object):
  def __init__(self, config):
    """
    Initialize the configs, which can be provided if using multiple Agents per host.
    :param config: Agent configs. None if will use the default location.
    """
    self.config = config if config is not None else self.resolve_ambari_config()

  def resolve_ambari_config(self):
    """
    Resolve the default Ambari Agent configs.
    :return: The default configs.
    """
    try:
      config = AmbariConfig()
      if os.path.exists(AmbariConfig.getConfigFile()):
        config.read(AmbariConfig.getConfigFile())
      else:
        raise Exception("No config found, use default")

    except Exception, err:
      log.warn(err)
    return config

  # Return first ip adress
  def getIpAddress(self):
    return socket.gethostbyname(self.getFqdn().lower())

  # Returns the currently running user id
  def getId(self):
    return getpass.getuser()

  # Returns the OS name
  def getKernel(self):
    return platform.system()

  # Returns the host's primary DNS domain name
  def getDomain(self):
    fqdn = self.getFqdn()
    hostname = self.getHostname()
    domain = fqdn.replace(hostname, "", 1)
    domain = domain.replace(".", "", 1)
    return domain

  # Returns the short hostname
  def getHostname(self):
    return self.getFqdn().split('.', 1)[0]

  # Returns the CPU hardware architecture
  def getArchitecture(self):
    result = platform.processor()
    if not result:
      retcode, out, err = run_os_command("lscpu | grep Architecture: | awk '{ print $2 }'")
      out = out.strip()
      if out:
        return out
      return 'unknown cpu arch'
    else:
      return result

  # Returns the full name of the OS
  def getOperatingSystem(self):
    return OSCheck.get_os_type()

  # Returns the OS version
  def getOperatingSystemRelease(self):
    return OSCheck.get_os_version()

  # Returns the OS TimeZone
  def getTimeZone(self):
    return time.tzname[time.daylight - 1]


  # Returns the CPU count
  def getProcessorcount(self):
    return multiprocessing.cpu_count()

  # Returns the Kernel release
  def getKernelRelease(self):
    return platform.release()


  # Returns the Kernel release version
  def getKernelVersion(self):
    kernel_release = platform.release()
    return kernel_release.split('-', 1)[0]

  # Returns the major kernel release version
  def getKernelMajVersion(self):
    return '.'.join(self.getKernelVersion().split('.', 2)[0:2])

  def getMacAddress(self):
    mac = uuid.getnode()
    if uuid.getnode() == mac:
      mac = ':'.join('%02X' % ((mac >> 8 * i) & 0xff) for i in reversed(xrange(6)))
    else:
      mac = 'UNKNOWN'
    return mac

  # Returns the operating system family

  def getOsFamily(self):
    return OSCheck.get_os_family()

  # Return uptime hours
  def getUptimeHours(self):
    return self.getUptimeSeconds() / (60 * 60)

  # Return uptime days
  def getUptimeDays(self):
    return self.getUptimeSeconds() / (60 * 60 * 24)

  def getSystemResourceIfExists(self, systemResources, key, default):
    if key in systemResources:
      return systemResources[key]
    else:
      return default

  def replaceFacterInfoWithSystemResources(self, systemResources, facterInfo):
    """
    Replace facter info with fake system resource data (if there are any).
    """
    for key in facterInfo:
      facterInfo[key] = self.getSystemResourceIfExists(systemResources, key, facterInfo[key])
    return facterInfo

  def getSystemResourceOverrides(self):
    """
    Read all json files from 'system_resource_overrides' directory, and later these values are used as
    fake system data for hosts. In case of the key-value pairs cannot be loaded use default behaviour.
    """
    systemResources = {}
    if self.config.has_option('agent', 'system_resource_overrides'):
      systemResourceDir = self.config.get('agent', 'system_resource_overrides', '').strip()
      if systemResourceDir:
        if os.path.isdir(systemResourceDir) and os.path.exists(systemResourceDir):
          try:
            for filename in glob.glob('%s/*.json' % systemResourceDir):
              with open(filename) as fp:
                data = json.loads(fp.read())
                for (key, value) in data.items():
                  systemResources[key] = data[key]
          except:
            log.warn(
              "Cannot read values from json files in %s. it won't be used for gathering system resources." % systemResourceDir)
        else:
          log.info(
            "Directory: '%s' does not exist - it won't be used for gathering system resources." % systemResourceDir)
      else:
        log.info("'system_resource_dir' is not set - it won't be used for gathering system resources.")
    return systemResources

  def getFqdn(self):
    raise NotImplementedError()

  def getNetmask(self):
    raise NotImplementedError()

  def getInterfaces(self):
    raise NotImplementedError()

  def getUptimeSeconds(self):
    raise NotImplementedError()

  def getMemorySize(self):
    raise NotImplementedError()

  def getMemoryFree(self):
    raise NotImplementedError()

  def getMemoryTotal(self):
    raise NotImplementedError()

  def facterInfo(self):
    return {
      'id': self.getId(),
      'kernel': self.getKernel(),
      'domain': self.getDomain(),
      'fqdn': self.getFqdn(),
      'hostname': self.getHostname(),
      'macaddress': self.getMacAddress(),
      'architecture': self.getArchitecture(),
      'operatingsystem': self.getOperatingSystem(),
      'operatingsystemrelease': self.getOperatingSystemRelease(),
      'physicalprocessorcount': self.getProcessorcount(),
      'processorcount': self.getProcessorcount(),
      'timezone': self.getTimeZone(),
      'hardwareisa': self.getArchitecture(),
      'hardwaremodel': self.getArchitecture(),
      'kernelrelease': self.getKernelRelease(),
      'kernelversion': self.getKernelVersion(),
      'osfamily': self.getOsFamily(),
      'kernelmajversion': self.getKernelMajVersion(),
      'ipaddress': self.getIpAddress(),
      'netmask': self.getNetmask(),
      'interfaces': self.getInterfaces(),
      'uptime_seconds': str(self.getUptimeSeconds()),
      'uptime_hours': str(self.getUptimeHours()),
      'uptime_days': str(self.getUptimeDays()),
      'memorysize': self.getMemorySize(),
      'memoryfree': self.getMemoryFree(),
      'memorytotal': self.getMemoryTotal()
    }

  #Convert kB to GB
  @staticmethod
  def convertSizeKbToGb(size):
    return "%0.2f GB" % round(float(size) / (1024.0 * 1024.0), 2)

  #Convert MB to GB
  @staticmethod
  def convertSizeMbToGb(size):
    return "%0.2f GB" % round(float(size) / (1024.0), 2)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class FacterWindows(Facter):
  GET_SYSTEM_INFO_CMD = "systeminfo"
  GET_MEMORY_CMD = '$mem =(Get-WMIObject Win32_OperatingSystem -ComputerName "LocalHost" ); echo "$($mem.FreePhysicalMemory) $($mem.TotalVisibleMemorySize)"'
  GET_PAGE_FILE_INFO = '$pgo=(Get-WmiObject Win32_PageFileUsage); echo "$($pgo.AllocatedBaseSize) $($pgo.AllocatedBaseSize-$pgo.CurrentUsage)"'
  GET_UPTIME_CMD = 'echo $([int]((get-date)-[system.management.managementdatetimeconverter]::todatetime((get-wmiobject -class win32_operatingsystem).Lastbootuptime)).TotalSeconds)'


  # Returns the FQDN of the host
  def getFqdn(self):
    return socket.getfqdn().lower()

  # Return  netmask
  def getNetmask(self):
    #TODO return correct netmask
    return 'OS NOT SUPPORTED'

  # Return interfaces
  def getInterfaces(self):
    #TODO return correct interfaces
    return 'OS NOT SUPPORTED'

  # Return uptime seconds
  def getUptimeSeconds(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_UPTIME_CMD).output.replace('\n', '').replace('\r',
                                                                                                                '')
      return int(result)
    except:
      log.warn("Can not get SwapFree")
    return 0

  # Return memoryfree
  def getMemoryFree(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_MEMORY_CMD).output.split(" ")[0].replace('\n',
                                                                                                            '').replace(
        '\r', '')
      return result
    except:
      log.warn("Can not get MemoryFree")
    return 0

  # Return memorytotal
  def getMemoryTotal(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_MEMORY_CMD).output.split(" ")[-1].replace('\n',
                                                                                                             '').replace(
        '\r', '')
      return result
    except:
      log.warn("Can not get MemoryTotal")
    return 0

  # Return swapfree
  def getSwapFree(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_PAGE_FILE_INFO).output.split(" ")[-1].replace('\n',
                                                                                                                 '').replace(
        '\r', '')
      return result
    except:
      log.warn("Can not get SwapFree")
    return 0

  # Return swapsize
  def getSwapSize(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_PAGE_FILE_INFO).output.split(" ")[0].replace('\n',
                                                                                                                '').replace(
        '\r', '')
      return result
    except:
      log.warn("Can not get SwapFree")
    return 0

  # Return memorysize
  def getMemorySize(self):
    try:
      runner = shellRunner()
      result = runner.runPowershell(script_block=FacterWindows.GET_MEMORY_CMD).output.split(" ")[-1].replace('\n',
                                                                                                             '').replace(
        '\r', '')
      return result
    except:
      log.warn("Can not get MemorySize")
    return 0

  def facterInfo(self):
    facterInfo = super(FacterWindows, self).facterInfo()
    systemResourceOverrides = self.getSystemResourceOverrides()
    facterInfo = self.replaceFacterInfoWithSystemResources(systemResourceOverrides, facterInfo)
    facterInfo['swapsize'] = Facter.convertSizeMbToGb(
      self.getSystemResourceIfExists(systemResourceOverrides, 'swapsize', self.getSwapSize()))
    facterInfo['swapfree'] = Facter.convertSizeMbToGb(
      self.getSystemResourceIfExists(systemResourceOverrides, 'swapfree', self.getSwapFree()))
    return facterInfo


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class FacterLinux(Facter):
  FIRST_WORDS_REGEXP = re.compile(r',$')
  IFNAMES_REGEXP = re.compile("^\d")
  SE_STATUS_REGEXP = re.compile('(enforcing|permissive|enabled)')
  DIGITS_REGEXP = re.compile("\d+")
  FREEMEM_REGEXP = re.compile("MemFree:.*?(\d+) .*")
  TOTALMEM_REGEXP = re.compile("MemTotal:.*?(\d+) .*")
  SWAPFREE_REGEXP = re.compile("SwapFree:.*?(\d+) .*")
  SWAPTOTAL_REGEXP = re.compile("SwapTotal:.*?(\d+) .*")

  # selinux command
  GET_SE_LINUX_ST_CMD = "/usr/sbin/sestatus"
  GET_IFCONFIG_SHORT_CMD = "ifconfig -s"
  GET_IP_LINK_CMD = "ip link"
  GET_UPTIME_CMD = "cat /proc/uptime"
  GET_MEMINFO_CMD = "cat /proc/meminfo"

  def __init__(self, config):
    super(FacterLinux,self).__init__(config)
    self.DATA_IFCONFIG_SHORT_OUTPUT = FacterLinux.setDataIfConfigShortOutput()
    self.DATA_IP_LINK_OUTPUT = FacterLinux.setDataIpLinkOutput()
    self.DATA_UPTIME_OUTPUT = FacterLinux.setDataUpTimeOutput()
    self.DATA_MEMINFO_OUTPUT = FacterLinux.setMemInfoOutput()

  # Returns the output of `ifconfig -s` command
  @staticmethod
  def setDataIfConfigShortOutput():

    try:
      return_code, stdout, stderr = run_os_command(FacterLinux.GET_IFCONFIG_SHORT_CMD)
      return stdout
    except OSError:
      log.warn("Can't execute {0}".format(FacterLinux.GET_IFCONFIG_SHORT_CMD))
    return ""

  # Returns the output of `ip link` command
  @staticmethod
  def setDataIpLinkOutput():

    try:
      return_code, stdout, stderr = run_os_command(FacterLinux.GET_IP_LINK_CMD)
      return stdout
    except OSError:
      log.warn("Can't execute {0}".format(FacterLinux.GET_IP_LINK_CMD))
    return ""

  @staticmethod
  def setDataUpTimeOutput():

    try:
      return_code, stdout, stderr = run_os_command(FacterLinux.GET_UPTIME_CMD)
      return stdout
    except OSError:
      log.warn("Can't execute {0}".format(FacterLinux.GET_UPTIME_CMD))
    return ""

  @staticmethod
  def setMemInfoOutput():

    try:
      return_code, stdout, stderr = run_os_command(FacterLinux.GET_MEMINFO_CMD)
      return stdout
    except OSError:
      log.warn("Can't execute {0}".format(FacterLinux.GET_MEMINFO_CMD))
    return ""

  # Returns the FQDN of the host
  def getFqdn(self):
    return hostname.hostname(self.config)

  def isSeLinux(self):

    try:
      retcode, out, err = run_os_command(FacterLinux.GET_SE_LINUX_ST_CMD)
      se_status = FacterLinux.SE_STATUS_REGEXP.search(out)
      if se_status:
        return True
    except OSError:
      log.warn("Could not run {0}: OK".format(FacterLinux.GET_SE_LINUX_ST_CMD))
    return False

  def return_first_words_from_list(self, list):
    result = ""
    for i in list:
      if i.strip():
        result = result + i.split()[0].strip() + ","

    result = FacterLinux.FIRST_WORDS_REGEXP.sub("", result)
    return result

  def return_ifnames_from_ip_link(self, ip_link_output):
    list = []
    for line in ip_link_output.splitlines():
      if FacterLinux.IFNAMES_REGEXP.match(line):
        list.append(line.split()[1].rstrip(":"))
    return ",".join(list)

  def data_return_first(self, patern, data):
    full_list = patern.findall(data)
    result = ""
    if full_list:
      result = full_list[0]

    return result

  # Return  netmask
  def getNetmask(self):
    import fcntl
    import struct
    primary_ip = self.getIpAddress().strip()

    for ifname in self.getInterfaces().split(","):
      if ifname.strip():
        ip_address_by_ifname = self.get_ip_address_by_ifname(ifname)
        if ip_address_by_ifname is not None:
          if primary_ip == ip_address_by_ifname.strip():
            return socket.inet_ntoa(fcntl.ioctl(socket.socket(socket.AF_INET, socket.SOCK_DGRAM), 35099, struct.pack('256s', ifname))[20:24])

    return None
      
  # Return IP by interface name
  def get_ip_address_by_ifname(self, ifname):
    import fcntl
    import struct
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    ip_address_by_ifname = None
    try:
      ip_address_by_ifname = socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
        )[20:24])
    except Exception, err:
      log.warn("Can't get the IP address for {0}".format(ifname))
    
    return ip_address_by_ifname
      

  # Return interfaces
  def getInterfaces(self):
    result = self.return_first_words_from_list(self.DATA_IFCONFIG_SHORT_OUTPUT.splitlines()[1:])
    # If the host has `ifconfig` command, then return that result.
    if result != '':
      return result
    # If the host has `ip` command, then return that result.
    result = self.return_ifnames_from_ip_link(self.DATA_IP_LINK_OUTPUT)
    if result != '':
      return result
    # If the host has neither `ifocnfig` command nor `ip` command, then return "OS NOT SUPPORTED"
    log.warn("Can't get a network interfaces list from {0}".format(self.DATA_IFCONFIG_SHORT_OUTPUT))
    return 'OS NOT SUPPORTED'

  # Return uptime seconds
  def getUptimeSeconds(self):
    try:
      return int(self.data_return_first(FacterLinux.DIGITS_REGEXP, self.DATA_UPTIME_OUTPUT))
    except ValueError:
      log.warn("Can't get an uptime value from {0}".format(self.DATA_UPTIME_OUTPUT))
      return 0

  # Return memoryfree
  def getMemoryFree(self):
    #:memoryfree_mb => "MemFree",
    try:
      return int(self.data_return_first(FacterLinux.FREEMEM_REGEXP, self.DATA_MEMINFO_OUTPUT))
    except ValueError:
      log.warn("Can't get free memory size from {0}".format(self.DATA_MEMINFO_OUTPUT))
      return 0

  # Return memorytotal
  def getMemoryTotal(self):
    try:
      return int(self.data_return_first(FacterLinux.TOTALMEM_REGEXP, self.DATA_MEMINFO_OUTPUT))
    except ValueError:
      log.warn("Can't get total memory size from {0}".format(self.DATA_MEMINFO_OUTPUT))
      return 0

  # Return swapfree
  def getSwapFree(self):
    #:swapfree_mb   => "SwapFree"
    try:
      return int(self.data_return_first(FacterLinux.SWAPFREE_REGEXP, self.DATA_MEMINFO_OUTPUT))
    except ValueError:
      log.warn("Can't get free swap memory size from {0}".format(self.DATA_MEMINFO_OUTPUT))
      return 0

  # Return swapsize
  def getSwapSize(self):
    #:swapsize_mb   => "SwapTotal",
    try:
      return int(self.data_return_first(FacterLinux.SWAPTOTAL_REGEXP, self.DATA_MEMINFO_OUTPUT))
    except ValueError:
      log.warn("Can't get total swap memory size from {0}".format(self.DATA_MEMINFO_OUTPUT))
      return 0

  # Return memorysize
  def getMemorySize(self):
    #:memorysize_mb => "MemTotal"
    try:
      return int(self.data_return_first(FacterLinux.TOTALMEM_REGEXP, self.DATA_MEMINFO_OUTPUT))
    except ValueError:
      log.warn("Can't get memory size from {0}".format(self.DATA_MEMINFO_OUTPUT))
      return 0

  def facterInfo(self):
    facterInfo = super(FacterLinux, self).facterInfo()
    systemResourceOverrides = self.getSystemResourceOverrides()
    facterInfo = self.replaceFacterInfoWithSystemResources(systemResourceOverrides, facterInfo)

    facterInfo['selinux'] = self.isSeLinux()
    facterInfo['swapsize'] = Facter.convertSizeKbToGb(
      self.getSystemResourceIfExists(systemResourceOverrides, 'swapsize', self.getSwapSize()))
    facterInfo['swapfree'] = Facter.convertSizeKbToGb(
      self.getSystemResourceIfExists(systemResourceOverrides, 'swapfree', self.getSwapFree()))
    return facterInfo


def main(argv=None):
  config = None
  print Facter(config).facterInfo()


if __name__ == '__main__':
  main()
