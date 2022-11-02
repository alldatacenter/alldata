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

import os
import logging

from models.commands import AgentCommand
from models.hooks import HookPrefix

__all__ = ["ResolvedHooks", "HooksOrchestrator"]


class ResolvedHooks(object):
  """
  Hooks sequence holder
  """

  def __init__(self, pre_hooks=set(), post_hooks=set()):
    """
    Creates response instance with generated hooks sequence

    :arg pre_hooks hook sequence, typically generator is passed
    :arg post_hooks hook sequence, typically generator is passed

    :type pre_hooks Collections.Iterable|types.GeneratorType
    :type post_hooks Collections.Iterable|types.GeneratorType
    """
    self._pre_hooks = pre_hooks
    self._post_hooks = post_hooks

  @property
  def pre_hooks(self):
    """
    :rtype list
    """
    # Converting generator to real sequence on first user request
    if not isinstance(self._pre_hooks, (list, set)):
      self._pre_hooks = list(self._pre_hooks)

    return self._pre_hooks

  @property
  def post_hooks(self):
    """
    :rtype list
    """
    # Converting generator to real sequence on first user request
    if not isinstance(self._post_hooks, (list, set)):
      self._post_hooks = list(self._post_hooks)

    return self._post_hooks


class HookSequenceBuilder(object):
  """
  Sequence builder according to passed definition
  """

  # ToDo: move hooks sequence definition to configuration or text file definition?
  _hooks_sequences = {
    HookPrefix.pre: [
      "{prefix}-{command}",
      "{prefix}-{command}-{service}",
      "{prefix}-{command}-{service}-{role}"
    ],
    HookPrefix.post: [
      "{prefix}-{command}-{service}-{role}",
      "{prefix}-{command}-{service}",
      "{prefix}-{command}"
    ]
  }

  def build(self, prefix, command, service, role):
    """
    Building hooks sequence depends on incoming data

    :type prefix str
    :type command str
    :type service str
    :type role str
    :rtype types.GeneratorType
    """
    if prefix not in self._hooks_sequences:
      raise TypeError("Unable to locate hooks sequence definition for '{}' prefix".format(prefix))

    for hook_definition in self._hooks_sequences[prefix]:
      if "service" in hook_definition and service is None:
        continue

      if "role" is hook_definition and role is None:
        continue

      yield hook_definition.format(prefix=prefix, command=command, service=service, role=role)


class HooksOrchestrator(object):
  """
   Resolving hooks according to HookSequenceBuilder definitions
  """

  def __init__(self, injector):
    """
    :type injector InitializerModule
    """
    self._file_cache = injector.file_cache
    self._logger = logging.getLogger()
    self._hook_builder = HookSequenceBuilder()

  def resolve_hooks(self, command, command_name):
    """
    Resolving available hooks sequences which should be appended or prepended to script execution chain

    :type command dict
    :type command_name str
    :rtype ResolvedHooks
    """
    command_type = command["commandType"]
    if command_type == AgentCommand.status or not command_name:
      return None

    hook_dir = self._file_cache.get_hook_base_dir(command)

    if not hook_dir:
      return ResolvedHooks()

    service = command["serviceName"] if "serviceName" in command else None
    component = command["role"] if "role" in command else None

    pre_hooks_seq = self._hook_builder.build(HookPrefix.pre, command_name, service, component)
    post_hooks_seq = self._hook_builder.build(HookPrefix.post, command_name, service, component)

    return ResolvedHooks(
      self._resolve_hooks_path(hook_dir, pre_hooks_seq),
      self._resolve_hooks_path(hook_dir, post_hooks_seq)
    )

  def _resolve_hooks_path(self, stack_hooks_dir, hooks_sequence):
    """
    Returns a tuple(path to hook script, hook base dir) according to passed hooks_sequence

    :type stack_hooks_dir str
    :type hooks_sequence collections.Iterable|types.GeneratorType
    """

    for hook in hooks_sequence:
      hook_base_dir = os.path.join(stack_hooks_dir, hook)
      hook_script_path = os.path.join(hook_base_dir, "scripts", "hook.py")

      if not os.path.isfile(hook_script_path):
        self._logger.debug("Hook script {0} not found, skipping".format(hook_script_path))
        continue

      yield hook_script_path, hook_base_dir
