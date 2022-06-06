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

from collections import namedtuple
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.constants import Direction

UpgradeSummary = namedtuple("UpgradeSummary", "type direction orchestration is_revert services is_downgrade_allowed is_switch_bits associated_stack associated_version")
UpgradeServiceSummary = namedtuple("UpgradeServiceSummary", "service_name source_stack source_version target_stack target_version")


def get_source_stack(service_name):
  """
  Gets the source stack (from) version of a service participating in an upgrade. If there is no
  upgrade or the specific service is not participating, this will return None.
  :param service_name:  the service name to check for, or None to extract it from the command
  :return:  the stack that the service is upgrading from or None if there is no upgrade or
  the service is not included in the upgrade.
  """
  service_summary = _get_service_summary(service_name)
  if service_summary is None:
    return None

  return service_summary.source_stack


def get_source_version(service_name = None, default_version=None):
  """
  Gets the source (from) version of a service participating in an upgrade. If there is no
  upgrade or the specific service is not participating, this will return None.
  :param service_name:  the service name to check for, or None to extract it from the command
  :param default_version: if the version of the service can't be calculated, this optional
  default value is returned
  :return:  the version that the service is upgrading from or None if there is no upgrade or
  the service is not included in the upgrade.
  """
  service_summary = _get_service_summary(service_name)
  if service_summary is None:
    return default_version

  return service_summary.source_version


def get_target_version(service_name = None, default_version=None):
  """
  Gets the target (to) version of a service participating in an upgrade. If there is no
  upgrade or the specific service is not participating, this will return None.
  :param service_name:  the service name to check for, or None to extract it from the command
  :param default_version: if the version of the service can't be calculated, this optional
  default value is returned
  :return:  the version that the service is upgrading to or None if there is no upgrade or
  the service is not included in the upgrade.
  """
  service_summary = _get_service_summary(service_name)
  if service_summary is None:
    return default_version

  return service_summary.target_version



def get_upgrade_summary():
  """
  Gets a summary of an upgrade in progress, including type, direction, orchestration and from/to
  repository versions.
  """
  config = Script.get_config()
  if "upgradeSummary" not in config or not config["upgradeSummary"]:
    return None

  upgrade_summary = config["upgradeSummary"]
  service_summary_dict = {}

  service_summary = upgrade_summary["services"]
  for service_name, service_summary_json in service_summary.iteritems():
    service_summary =  UpgradeServiceSummary(service_name = service_name,
      source_stack = service_summary_json["sourceStackId"],
      source_version = service_summary_json["sourceVersion"],
      target_stack = service_summary_json["targetStackId"],
      target_version = service_summary_json["targetVersion"])

    service_summary_dict[service_name] = service_summary

  return UpgradeSummary(type=upgrade_summary["type"], direction=upgrade_summary["direction"],
    orchestration=upgrade_summary["orchestration"], is_revert = upgrade_summary["isRevert"],
    services = service_summary_dict,
    is_downgrade_allowed=upgrade_summary["isDowngradeAllowed"],
    is_switch_bits=upgrade_summary["isSwitchBits"],
    associated_stack=upgrade_summary["associatedStackId"],
    associated_version = upgrade_summary["associatedVersion"])


def get_downgrade_from_version(service_name = None):
  """
  Gets the downgrade-from-version for the specificed service. If there is no downgrade or
  the service isn't participating in the downgrade, then this will return None
  :param service_name:  the service, or optionally onmitted to infer it from the command.
  :return: the downgrade-from-version or None
  """
  upgrade_summary = get_upgrade_summary()
  if upgrade_summary is None:
    return None

  if Direction.DOWNGRADE.lower() != upgrade_summary.direction.lower():
    return None

  service_summary = _get_service_summary(service_name)
  if service_summary is None:
    return None

  return service_summary.source_version


def _get_service_summary(service_name):
  """
  Gets the service summary for the upgrade/downgrade for the given service, or None if
  the service isn't participating.
  :param service_name:  the service name
  :return:  the service summary or None
  """
  upgrade_summary = get_upgrade_summary()
  if upgrade_summary is None:
    return None

  if service_name is None:
    config = Script.get_config()
    service_name = config['serviceName']

  service_summary = upgrade_summary.services
  if service_name not in service_summary:
    return None

  return service_summary[service_name]
