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

from ambari_agent import main
main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
from unittest import TestCase
from mock.mock import patch, MagicMock, Mock
import unittest
import platform
import socket
from ambari_commons import subprocess32
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
from ambari_agent import hostname
from ambari_agent.Hardware import Hardware
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.Facter import Facter, FacterLinux
from ambari_commons import OSCheck


@not_for_platform(PLATFORM_WINDOWS)
@patch.object(platform, "linux_distribution", new=MagicMock(return_value=('Suse', '11', 'Final')))
@patch.object(socket, "getfqdn", new=MagicMock(return_value="ambari.apache.org"))
@patch.object(socket, "gethostbyname", new=MagicMock(return_value="192.168.1.1"))
@patch.object(FacterLinux, "setDataIfConfigShortOutput", new=MagicMock(return_value='''Iface   MTU Met    RX-OK RX-ERR RX-DRP RX-OVR    TX-OK TX-ERR TX-DRP TX-OVR Flg
eth0   1500   0     9986      0      0      0     5490      0      0      0 BMRU
eth1   1500   0        0      0      0      0        6      0      0      0 BMRU
eth2   1500   0        0      0      0      0        6      0      0      0 BMRU
lo    16436   0        2      0      0      0        2      0      0      0 LRU'''))
@patch.object(FacterLinux, "setDataIpLinkOutput", new=MagicMock(return_value='''1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT qlen 1000
    link/ether 08:00:27:d3:e8:0f brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT qlen 1000
    link/ether 08:00:27:09:92:3a brd ff:ff:ff:ff:ff:ff'''))
class TestHardware(TestCase):
 
  @patch.object(Hardware, "osdisks", new=MagicMock(return_value=[]))
  @patch.object(Hardware, "_chk_writable_mount", new=MagicMock(return_value=True))
  @patch.object(FacterLinux, "get_ip_address_by_ifname", new=MagicMock(return_value=None))
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_build(self, get_os_version_mock, get_os_type_mock):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    hardware = Hardware()
    result = hardware.get()
    osdisks = hardware.osdisks()

    for dev_item in result['mounts']:
      self.assertTrue(dev_item['available'] >= 0)
      self.assertTrue(dev_item['used'] >= 0)
      self.assertTrue(dev_item['percent'] is not None)
      self.assertTrue(dev_item['device'] is not None)
      self.assertTrue(dev_item['mountpoint'] is not None)
      self.assertTrue(dev_item['type'] is not None)
      self.assertTrue(dev_item['size'] > 0)

    for os_disk_item in osdisks:
      self.assertTrue(os_disk_item['available'] >= 0)
      self.assertTrue(os_disk_item['used'] >= 0)
      self.assertTrue(os_disk_item['percent'] is not None)
      self.assertTrue(os_disk_item['device'] is not None)
      self.assertTrue(os_disk_item['mountpoint'] is not None)
      self.assertTrue(os_disk_item['type'] is not None)
      self.assertTrue(os_disk_item['size'] > 0)

    self.assertTrue(len(result['mounts']) == len(osdisks))

  @patch.object(Hardware, "_chk_writable_mount")
  @patch("ambari_agent.Hardware.path_isfile")
  @patch("resource_management.core.shell.call")
  def test_osdisks_parsing(self, shell_call_mock, isfile_mock, chk_writable_mount_mock):
    df_output =\
                """Filesystem                                                                                        Type  1024-blocks     Used Available Capacity Mounted on
                /dev/mapper/docker-253:0-4980899-d45c264d37ab18c8ed14f890f4d59ac2b81e1c52919eb36a79419787209515f3 xfs      31447040  1282384  30164656       5% /
                tmpfs                                                                                             tmpfs    32938336        4  32938332       1% /dev
                tmpfs                                                                                             tmpfs    32938336        0  32938336       0% /sys/fs/cgroup
                /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/resolv.conf
                /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/hostname
                /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/hosts
                shm                                                                                               tmpfs       65536        0     65536       0% /dev/shm
                /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /run/secrets
                """

    def isfile_side_effect(path):
      assume_files = ["/etc/resolv.conf", "/etc/hostname", "/etc/hosts"]
      return path in assume_files

    def chk_writable_mount_side_effect(path):
      assume_read_only = ["/run/secrets"]
      return path not in assume_read_only

    isfile_mock.side_effect = isfile_side_effect
    chk_writable_mount_mock.side_effect = chk_writable_mount_side_effect
    shell_call_mock.return_value = (0, df_output, '')

    result = Hardware(cache_info=False).osdisks()

    self.assertEquals(1, len(result))

    expected_mounts_left = ["/"]
    mounts_left = [item["mountpoint"] for item in result]

    self.assertEquals(expected_mounts_left, mounts_left)

  @patch.object(Hardware, "_chk_writable_mount")
  @patch("ambari_agent.Hardware.path_isfile")
  @patch("resource_management.core.shell.call")
  def test_osdisks_no_ignore_property(self, shell_call_mock, isfile_mock, chk_writable_mount_mock):
    df_output = \
      """Filesystem                                                                                        Type  1024-blocks     Used Available Capacity Mounted on
      /dev/mapper/docker-253:0-4980899-d45c264d37ab18c8ed14f890f4d59ac2b81e1c52919eb36a79419787209515f3 xfs      31447040  1282384  30164656       5% /
      """

    isfile_mock.return_value = False
    chk_writable_mount_mock.return_value = True
    shell_call_mock.return_value = (0, df_output, '')
    config = AmbariConfig()

    # check, that config do not define ignore_mount_points property
    self.assertEquals("test", config.get('agent', 'ignore_mount_points', default="test"))

    result = Hardware(config=config, cache_info=False).osdisks()

    self.assertEquals(1, len(result))

    expected_mounts_left = ["/"]
    mounts_left = [item["mountpoint"] for item in result]

    self.assertEquals(expected_mounts_left, mounts_left)

  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  @patch("resource_management.core.shell.call")
  def test_osdisks_remote(self, shell_call_mock, get_os_version_mock, get_os_type_mock):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    Hardware(cache_info=False).osdisks()
    timeout = 10
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    config = AmbariConfig()
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    config.add_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY, "true")
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY, "false")
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, "0")
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    timeout = 1
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, str(timeout))
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

    timeout = 2
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, str(timeout))
    Hardware(config=config, cache_info=False).osdisks()
    shell_call_mock.assert_called_with(['timeout', str(timeout), "df", "-kPT", "-l"], stdout = subprocess32.PIPE, stderr = subprocess32.PIPE, timeout = timeout, quiet = True)

  def test_parse_df_line(self):
    df_line_sample = "device type size used available percent mountpoint"

    samples = [
      {
        "sample": df_line_sample,
        "expected": dict(zip(df_line_sample.split(), df_line_sample.split()))
      },
      {
        "sample": "device type size used available percent",
        "expected": None,
      },
      {
        "sample": "device type size used available percent mountpoint info",
        "expected": None,
      },
      {
        "sample": "",
        "expected": None
      }
    ]

    for sample in samples:
      try:
        result = Hardware(cache_info=False)._parse_df([sample["sample"]]).next()
      except StopIteration:
        result = None

      self.assertEquals(result, sample["expected"], "Failed with sample: '{0}', expected: {1}, got: {2}".format(
        sample["sample"],
        sample["expected"],
        result
      ))

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new=MagicMock(return_value=None))
  @patch.object(hostname, "hostname")
  @patch.object(FacterLinux, "getFqdn")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_fqdnDomainHostname(self, get_os_version_mock, get_os_type_mock, facter_getFqdn_mock, hostname_mock):
    facter_getFqdn_mock.return_value = "ambari.apache.org"
    hostname_mock.return_value = 'ambari'
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertEquals(result['hostname'], "ambari")
    self.assertEquals(result['domain'], "apache.org")
    self.assertEquals(result['fqdn'], (result['hostname'] + '.' + result['domain']))

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new=MagicMock(return_value=None))
  @patch.object(FacterLinux, "setDataUpTimeOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_uptimeSecondsHoursDays(self, get_os_version_mock, get_os_type_mock, facter_setDataUpTimeOutput_mock):
    # 3 days + 1 hour + 13 sec
    facter_setDataUpTimeOutput_mock.return_value = "262813.00 123.45"
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertEquals(result['uptime_seconds'], '262813')
    self.assertEquals(result['uptime_hours'], '73')
    self.assertEquals(result['uptime_days'], '3')

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new=MagicMock(return_value=None))
  @patch.object(FacterLinux, "setMemInfoOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  @patch.object(FacterLinux, "getSystemResourceOverrides")
  def test_facterMemInfoOutput(self, getSystemResourceOverridesMock, get_os_version_mock, get_os_type_mock, facter_setMemInfoOutput_mock):

    getSystemResourceOverridesMock.return_value = {}
    facter_setMemInfoOutput_mock.return_value = '''
MemTotal:        1832392 kB
MemFree:          868648 kB
HighTotal:             0 kB
HighFree:              0 kB
LowTotal:        1832392 kB
LowFree:          868648 kB
SwapTotal:       2139592 kB
SwapFree:        1598676 kB
    '''

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertEquals(result['memorysize'], 1832392)
    self.assertEquals(result['memorytotal'], 1832392)
    self.assertEquals(result['memoryfree'], 868648)
    self.assertEquals(result['swapsize'], '2.04 GB')
    self.assertEquals(result['swapfree'], '1.52 GB')

  @patch("fcntl.ioctl")
  @patch("socket.socket")
  @patch("struct.pack")
  @patch("socket.inet_ntoa")
  @patch.object(FacterLinux, "get_ip_address_by_ifname")
  @patch.object(Facter, "getIpAddress")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIfConfigOutput(self, get_os_version_mock, get_os_type_mock,
                                    getIpAddress_mock, get_ip_address_by_ifname_mock, inet_ntoa_mock, struct_pack_mock,
                                    socket_socket_mock, fcntl_ioctl_mock):
    getIpAddress_mock.return_value = "10.0.2.15"
    get_ip_address_by_ifname_mock.return_value = "10.0.2.15"
    inet_ntoa_mock.return_value = "255.255.255.0"

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertTrue(inet_ntoa_mock.called)
    self.assertTrue(get_ip_address_by_ifname_mock.called)
    self.assertTrue(getIpAddress_mock.called)
    self.assertEquals(result['ipaddress'], '10.0.2.15')
    self.assertEquals(result['netmask'], '255.255.255.0')
    self.assertEquals(result['interfaces'], 'eth0,eth1,eth2,lo')

  @patch("fcntl.ioctl")
  @patch("socket.socket")
  @patch("struct.pack")
  @patch("socket.inet_ntoa")
  @patch.object(FacterLinux, "get_ip_address_by_ifname")
  @patch.object(Facter, "getIpAddress")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIfConfigOutputNone(self, get_os_version_mock, get_os_type_mock,
                                    getIpAddress_mock, get_ip_address_by_ifname_mock, inet_ntoa_mock, struct_pack_mock,
                                    socket_socket_mock, fcntl_ioctl_mock):
    getIpAddress_mock.return_value = "10.0.2.15"
    get_ip_address_by_ifname_mock.return_value = ""
    inet_ntoa_mock.return_value = "255.255.255.0"

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertTrue(get_ip_address_by_ifname_mock.called)
    self.assertEquals(result['netmask'], None)

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new=MagicMock(return_value=None))
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataOperatingsystemVsFamily(self, get_os_version_mock, get_os_family_mock, get_os_type_mock):
    get_os_type_mock.return_value = "some_type_of_os"
    get_os_version_mock.return_value = "11"
    get_os_family_mock.return_value = "redhat"

    config = None
    result = Facter(config).facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'redhat')

    get_os_family_mock.return_value = "ubuntu"
    result = Facter(config).facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'ubuntu')

    get_os_family_mock.return_value = "suse"
    result = Facter(config).facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'suse')

    get_os_family_mock.return_value = "My_new_family"
    result = Facter(config).facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'My_new_family')

  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("json.loads")
  @patch("glob.glob")
  @patch("__builtin__.open")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  @patch.object(FacterLinux, "resolve_ambari_config")
  def test_system_resource_overrides(self, resolve_ambari_config, get_os_version_mock, get_os_type_mock,
                                     open_mock, glob_mock, json_mock, isdir, exists):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = MagicMock()
    config.get.return_value = '/etc/custom_resource_overrides'
    config.has_option.return_value = True
    resolve_ambari_config.return_value = config
    isdir.return_value = True
    exists.return_value = True
    open_mock.return_value.read = "1"
    file_handle = open_mock.return_value.__enter__.return_value
    file_handle.read.return_value = '1'
    glob_mock.side_effect = \
      [
        [
          "/etc/custom_resource_overrides/1.json",
          "/etc/custom_resource_overrides/2.json"
          ]
      ]
    json_data = json_mock.return_value
    json_data.items.return_value = [('key', 'value')]
    json_data.__getitem__.return_value = 'value'

    facter = Facter(config)
    result = facter.getSystemResourceOverrides()

    isdir.assert_called_with('/etc/custom_resource_overrides')
    exists.assert_called_with('/etc/custom_resource_overrides')
    glob_mock.assert_called_with('/etc/custom_resource_overrides/*.json')
    self.assertTrue(config.has_option.called)
    self.assertTrue(config.get.called)
    self.assertTrue(glob_mock.called)
    self.assertEquals(2, file_handle.read.call_count)
    self.assertEquals(2, open_mock.call_count)
    self.assertEquals(2, json_mock.call_count)
    self.assertEquals('value', result['key'])

  @patch.object(Hardware, "_chk_writable_mount")
  @patch("ambari_agent.Hardware.path_isfile")
  @patch("resource_management.core.shell.call")
  def test_osdisks_blacklist(self, shell_call_mock, isfile_mock, chk_writable_mount_mock):
    df_output = \
      """Filesystem                                                                                        Type  1024-blocks     Used Available Capacity Mounted on
      /dev/mapper/docker-253:0-4980899-d45c264d37ab18c8ed14f890f4d59ac2b81e1c52919eb36a79419787209515f3 xfs      31447040  1282384  30164656       5% /
      tmpfs                                                                                             tmpfs    32938336        4  32938332       1% /dev
      tmpfs                                                                                             tmpfs    32938336        0  32938336       0% /sys/fs/cgroup
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/resolv.conf
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/hostname
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /etc/hosts
      shm                                                                                               tmpfs       65536        0     65536       0% /dev/shm
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /run/secrets
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /mnt/blacklisted_mount
      /dev/mapper/fedora-root                                                                           ext4    224161316 12849696 199901804       7% /mnt/blacklisted_mount/sub-dir
      """

    def isfile_side_effect(path):
      assume_files = ["/etc/resolv.conf", "/etc/hostname", "/etc/hosts"]
      return path in assume_files

    def chk_writable_mount_side_effect(path):
      assume_read_only = ["/run/secrets"]
      return path not in assume_read_only

    isfile_mock.side_effect = isfile_side_effect
    chk_writable_mount_mock.side_effect = chk_writable_mount_side_effect

    config_dict = {
      "agent": {
        "ignore_mount_points": "/mnt/blacklisted_mount"
      }
    }

    shell_call_mock.return_value = (0, df_output, '')

    def conf_get(section, key, default=""):
      if section in config_dict and key in config_dict[section]:
        return config_dict[section][key]

      return default

    def has_option(section, key):
      return section in config_dict and key in config_dict[section]

    conf = Mock()
    attr = {
      'get.side_effect': conf_get,
      'has_option.side_effect': has_option
    }
    conf.configure_mock(**attr)

    result = Hardware(config=conf, cache_info=False).osdisks()

    self.assertEquals(1, len(result))

    expected_mounts_left = ["/"]
    mounts_left = [item["mountpoint"] for item in result]

    self.assertEquals(expected_mounts_left, mounts_left)

@not_for_platform(PLATFORM_WINDOWS)
@patch.object(platform, "linux_distribution", new=MagicMock(return_value=('Suse', '11', 'Final')))
@patch.object(socket, "getfqdn", new=MagicMock(return_value="ambari.apache.org"))
@patch.object(socket, "gethostbyname", new=MagicMock(return_value="192.168.1.1"))
@patch.object(FacterLinux, "setDataIfConfigShortOutput", new=MagicMock(return_value=''))
@patch.object(FacterLinux, "setDataIpLinkOutput", new=MagicMock(return_value='''1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT qlen 1000
    link/ether 08:00:27:d3:e8:0f brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT qlen 1000
    link/ether 08:00:27:09:92:3a brd ff:ff:ff:ff:ff:ff'''))
class TestHardwareWithoutIfConfig(TestCase):
  @patch("fcntl.ioctl")
  @patch("socket.socket")
  @patch("struct.pack")
  @patch("socket.inet_ntoa")
  @patch.object(FacterLinux, "get_ip_address_by_ifname")
  @patch.object(Facter, "getIpAddress")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIpLinkOutput(self, get_os_version_mock,
                                  get_os_type_mock, getIpAddress_mock, get_ip_address_by_ifname_mock,
                                  inet_ntoa_mock, struct_pack_mock,
                                  socket_socket_mock, fcntl_ioctl_mock):

    getIpAddress_mock.return_value = "10.0.2.15"
    get_ip_address_by_ifname_mock.return_value = "10.0.2.15"
    inet_ntoa_mock.return_value = "255.255.255.0"

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = None
    result = Facter(config).facterInfo()

    self.assertTrue(inet_ntoa_mock.called)
    self.assertTrue(get_ip_address_by_ifname_mock.called)
    self.assertTrue(getIpAddress_mock.called)
    self.assertEquals(result['ipaddress'], '10.0.2.15')
    self.assertEquals(result['netmask'], '255.255.255.0')
    self.assertEquals(result['interfaces'], 'lo,enp0s3,enp0s8')


if __name__ == "__main__":
  unittest.main()

