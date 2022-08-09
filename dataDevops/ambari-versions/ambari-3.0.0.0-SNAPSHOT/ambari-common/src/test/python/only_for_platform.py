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

Ambari Agent

"""


import platform

PLATFORM_WINDOWS = "Windows"
PLATFORM_LINUX = "Linux"

def get_platform():
  return platform.system()

def only_for_platform(system):
  def decorator(obj):
    if platform.system() == system:
      return obj
  return decorator

def not_for_platform(system):
  def decorator(obj):
    if platform.system() != system:
      return obj
  return decorator

def for_specific_platforms(systems):
  def decorator(obj):
    if platform.system() in systems:
      return obj
  return decorator

os_distro_value_linux = ('Suse','11','Final')
os_distro_value_windows = ('win2012serverr2','6.3','WindowsServer')

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = os_distro_value_linux
else:
  os_distro_value = os_distro_value_windows
