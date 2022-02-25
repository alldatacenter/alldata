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

import Queue

import logging
import threading
import pprint
import os
import ambari_simplejson as json
import time
import signal

from AgentException import AgentException
from ambari_agent.BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle
from ambari_agent.models.commands import AgentCommand, CommandStatus
from ambari_commons.str_utils import split_on_chunks


logger = logging.getLogger()
installScriptHash = -1

MAX_SYMBOLS_PER_LOG_MESSAGE = 7900


class ActionQueue(threading.Thread):
  """ Action Queue for the agent. We pick one command at a time from the queue
  and execute it
  Note: Action and command terms in this and related classes are used interchangeably
  """

  # How many actions can be performed in parallel. Feel free to change
  MAX_CONCURRENT_ACTIONS = 5

  # How much time(in seconds) we need wait for new incoming execution command before checking status command queue
  EXECUTION_COMMAND_WAIT_TIME = 2

  def __init__(self, initializer_module):
    super(ActionQueue, self).__init__()
    self.commandQueue = Queue.Queue()
    self.backgroundCommandQueue = Queue.Queue()
    self.commandStatuses = initializer_module.commandStatuses
    self.config = initializer_module.config
    self.recovery_manager = initializer_module.recovery_manager
    self.configTags = {}
    self.stop_event = initializer_module.stop_event
    self.tmpdir = self.config.get('agent', 'prefix')
    self.customServiceOrchestrator = initializer_module.customServiceOrchestrator
    self.parallel_execution = self.config.get_parallel_exec_option()
    self.taskIdsToCancel = set()
    self.cancelEvent = threading.Event()
    self.component_status_executor = initializer_module.component_status_executor
    if self.parallel_execution == 1:
      logger.info("Parallel execution is enabled, will execute agent commands in parallel")
    self.lock = threading.Lock()

  def put(self, commands):
    for command in commands:
      if "serviceName" not in command:
        command["serviceName"] = "null"
      if "clusterId" not in command:
        command["clusterId"] = "null"

      logger.info("Adding {commandType} for role {role} for service {serviceName} of cluster_id {clusterId} to the queue".format(**command))

      if command['commandType'] == AgentCommand.background_execution:
        self.backgroundCommandQueue.put(self.create_command_handle(command))
      else:
        self.commandQueue.put(command)

  def interrupt(self):
    self.commandQueue.put(None)

  def cancel(self, commands):
    for command in commands:

      logger.info("Canceling command with taskId = {tid}".format(tid = str(command['target_task_id'])))
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug(pprint.pformat(command))

      task_id = command['target_task_id']
      reason = command['reason']

      # Remove from the command queue by task_id
      queue = self.commandQueue
      self.commandQueue = Queue.Queue()

      while not queue.empty():
        queued_command = queue.get(False)
        if queued_command['taskId'] != task_id:
          self.commandQueue.put(queued_command)
        else:
          logger.info("Canceling {commandType} for service {serviceName} and role {role} with taskId {taskId}".format(
            **queued_command
          ))

      # Kill if in progress
      self.customServiceOrchestrator.cancel_command(task_id, reason)
      self.taskIdsToCancel.add(task_id)
      self.cancelEvent.set()

  def run(self):
    while not self.stop_event.is_set():
      try:
        self.process_background_queue_safe_empty()
        self.fill_recovery_commands()
        try:
          if self.parallel_execution == 0:
            command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)

            if command is None:
              break

            self.process_command(command)
          else:
            # If parallel execution is enabled, just kick off all available
            # commands using separate threads
            while not self.stop_event.is_set():
              command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)

              if command is None:
                break
              # If command is not retry_enabled then do not start them in parallel
              # checking just one command is enough as all commands for a stage is sent
              # at the same time and retry is only enabled for initial start/install
              retry_able = False
              if 'commandParams' in command and 'command_retry_enabled' in command['commandParams']:
                retry_able = command['commandParams']['command_retry_enabled'] == "true"
              if retry_able:
                logger.info("Kicking off a thread for the command, id={} taskId={}".format(command['commandId'], command['taskId']))
                t = threading.Thread(target=self.process_command, args=(command,))
                t.daemon = True
                t.start()
              else:
                self.process_command(command)
                break
              pass
            pass
        except Queue.Empty:
          pass
      except Exception:
        logger.exception("ActionQueue thread failed with exception. Re-running it")
    logger.info("ActionQueue thread has successfully finished")

  def fill_recovery_commands(self):
    if self.recovery_manager.enabled() and not self.tasks_in_progress_or_pending():
      self.put(self.recovery_manager.get_recovery_commands())

  def process_background_queue_safe_empty(self):
    while not self.backgroundCommandQueue.empty():
      try:
        command = self.backgroundCommandQueue.get(False)
        if "__handle" in command and command["__handle"].status is None:
          self.process_command(command)
      except Queue.Empty:
        pass

  def create_command_handle(self, command):
    if "__handle" in command:
      raise AgentException("Command already has __handle")

    command['__handle'] = BackgroundCommandExecutionHandle(command, command['commandId'], None, self.on_background_command_complete_callback)
    return command

  def process_command(self, command):
    # make sure we log failures
    command_type = command['commandType']
    logger.debug("Took an element of Queue (command type = %s).", command_type)
    try:
      if command_type in AgentCommand.AUTO_EXECUTION_COMMAND_GROUP:
        try:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_start()
            self.recovery_manager.process_execution_command(command)

          self.execute_command(command)
        finally:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_finish()
      else:
        logger.error("Unrecognized command %s", pprint.pformat(command))
    except Exception:
      logger.exception("Exception while processing {0} command".format(command_type))

  def tasks_in_progress_or_pending(self):
    return not self.commandQueue.empty() or self.recovery_manager.has_active_command()

  def execute_command(self, command):
    """
    Executes commands of type EXECUTION_COMMAND
    """
    cluster_id = command['clusterId']
    command_id = command['commandId']
    command_type = command['commandType']

    num_attempts = 0
    retry_duration = 0  # even with 0 allow one attempt
    retry_able = False
    delay = 1
    log_command_output = True
    command_canceled = False
    command_result = {}

    message = "Executing command with id = {commandId}, taskId = {taskId} for role = {role} of " \
              "cluster_id {cluster}.".format(commandId=str(command_id), taskId=str(command['taskId']),
              role=command['role'], cluster=cluster_id)
    logger.info(message)

    taskId = command['taskId']
    # Preparing 'IN_PROGRESS' report
    in_progress_status = self.commandStatuses.generate_report_template(command)
    # The path of the files that contain the output log and error log use a prefix that the agent advertises to the
    # server. The prefix is defined in agent-config.ini
    if command_type != AgentCommand.auto_execution:
      in_progress_status.update({
        'tmpout': self.tmpdir + os.sep + 'output-' + str(taskId) + '.txt',
        'tmperr': self.tmpdir + os.sep + 'errors-' + str(taskId) + '.txt',
        'structuredOut': self.tmpdir + os.sep + 'structured-out-' + str(taskId) + '.json',
        'status': CommandStatus.in_progress
      })
    else:
      in_progress_status.update({
        'tmpout': self.tmpdir + os.sep + 'auto_output-' + str(taskId) + '.txt',
        'tmperr': self.tmpdir + os.sep + 'auto_errors-' + str(taskId) + '.txt',
        'structuredOut': self.tmpdir + os.sep + 'auto_structured-out-' + str(taskId) + '.json',
        'status': CommandStatus.in_progress
      })

    self.commandStatuses.put_command_status(command, in_progress_status)

    if 'commandParams' in command:
      if 'max_duration_for_retries' in command['commandParams']:
        retry_duration = int(command['commandParams']['max_duration_for_retries'])
      if 'command_retry_enabled' in command['commandParams'] and command_type != AgentCommand.auto_execution:
        #  for AgentCommand.auto_execution command retry_able should be always false
        retry_able = command['commandParams']['command_retry_enabled'] == "true"
      if 'log_output' in command['commandParams']:
        log_command_output = command['commandParams']['log_output'] != "false"

    logger.info("Command execution metadata - taskId = {taskId}, retry enabled = {retryAble}, max retry duration (sec)"
                " = {retryDuration}, log_output = {log_command_output}".format(
      taskId=taskId, retryAble=retry_able, retryDuration=retry_duration, log_command_output=log_command_output))

    self.cancelEvent.clear()
    # for case of command reschedule (e.g. command and cancel for the same taskId are send at the same time)
    self.taskIdsToCancel.discard(taskId)

    while retry_duration >= 0:
      if taskId in self.taskIdsToCancel:
        logger.info('Command with taskId = {0} canceled'.format(taskId))
        command_canceled = True

        self.taskIdsToCancel.discard(taskId)
        break

      num_attempts += 1
      start = 0
      if retry_able:
        start = int(time.time())
      # running command
      command_result = self.customServiceOrchestrator.runCommand(command,
                                                                 in_progress_status['tmpout'],
                                                                 in_progress_status['tmperr'],
                                                                 override_output_files=num_attempts == 1,
                                                                 retry=num_attempts > 1)
      end = 1
      if retry_able:
        end = int(time.time())
      retry_duration -= (end - start)

      # dumping results
      if command_type == AgentCommand.background_execution:
        logger.info("Command is background command, quit retrying. Exit code: {exitCode}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}"
                    .format(cid=taskId, exitCode=command_result['exitcode'], retryAble=retry_able, retryDuration=retry_duration, delay=delay))
        return
      else:
        if command_result['exitcode'] == 0:
          status = CommandStatus.completed
        else:
          status = CommandStatus.failed
          if (command_result['exitcode'] == -signal.SIGTERM) or (command_result['exitcode'] == -signal.SIGKILL):
            logger.info('Command with taskId = {cid} was canceled!'.format(cid=taskId))
            command_canceled = True
            self.taskIdsToCancel.discard(taskId)
            break

      if status != CommandStatus.completed and retry_able and retry_duration > 0:
        delay = self.get_retry_delay(delay)
        if delay > retry_duration:
          delay = retry_duration
        retry_duration -= delay  # allow one last attempt
        command_result['stderr'] += "\n\nCommand failed. Retrying command execution ...\n\n"
        logger.info("Retrying command with taskId = {cid} after a wait of {delay}".format(cid=taskId, delay=delay))
        if 'agentLevelParams' not in command:
          command['agentLevelParams'] = {}

        command['agentLevelParams']['commandBeingRetried'] = "true"
        self.cancelEvent.wait(delay) # wake up if something was canceled

        continue
      else:
        logger.info("Quit retrying for command with taskId = {cid}. Status: {status}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}"
                    .format(cid=taskId, status=status, retryAble=retry_able, retryDuration=retry_duration, delay=delay))
        break

    self.taskIdsToCancel.discard(taskId)

    # do not fail task which was rescheduled from server
    if command_canceled:
      with self.lock, self.commandQueue.mutex:
          for com in self.commandQueue.queue:
            if com['taskId'] == command['taskId']:
              logger.info("Command with taskId = {cid} was rescheduled by server. "
                          "Fail report on cancelled command won't be sent with heartbeat.".format(cid=taskId))
              self.commandStatuses.delete_command_data(command['taskId'])
              return

    # final result to stdout
    command_result['stdout'] += '\n\nCommand completed successfully!\n' if status == CommandStatus.completed else '\n\nCommand failed after ' + str(num_attempts) + ' tries\n'
    logger.info('Command with taskId = {cid} completed successfully!'.format(cid=taskId) if status == CommandStatus.completed else 'Command with taskId = {cid} failed after {attempts} tries'.format(cid=taskId, attempts=num_attempts))

    role_result = self.commandStatuses.generate_report_template(command)
    role_result.update({
      'stdout': unicode(command_result['stdout'], errors='replace'),
      'stderr': unicode(command_result['stderr'], errors='replace'),
      'exitCode': command_result['exitcode'],
      'status': status,
    })

    if self.config.has_option("logging", "log_command_executes") \
        and int(self.config.get("logging", "log_command_executes")) == 1 \
        and log_command_output:

      if role_result['stdout'] != '':
          logger.info("Begin command output log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])
          self.log_command_output(role_result['stdout'], str(command['taskId']))
          logger.info("End command output log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])

      if role_result['stderr'] != '':
          logger.info("Begin command stderr log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])
          self.log_command_output(role_result['stderr'], str(command['taskId']))
          logger.info("End command stderr log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])

    if role_result['stdout'] == '':
      role_result['stdout'] = 'None'
    if role_result['stderr'] == '':
      role_result['stderr'] = 'None'

    # let ambari know name of custom command

    if 'commandParams' in command and command['commandParams'].has_key('custom_command'):
      role_result['customCommand'] = command['commandParams']['custom_command']

    if 'structuredOut' in command_result:
      role_result['structuredOut'] = str(json.dumps(command_result['structuredOut']))
    else:
      role_result['structuredOut'] = ''

    self.recovery_manager.process_execution_command_result(command, status)
    self.commandStatuses.put_command_status(command, role_result)

    cluster_id = str(command['clusterId'])

    if cluster_id != '-1' and cluster_id != 'null':
      service_name = command['serviceName']
      if service_name != 'null':
        component_name = command['role']
        self.component_status_executor.check_component_status(cluster_id, service_name, component_name, "STATUS", report=True)

  def log_command_output(self, text, taskId):
    """
    Logs a message as multiple enumerated log messages every of which is not larger than MAX_SYMBOLS_PER_LOG_MESSAGE.

    If logs are redirected to syslog (syslog_enabled=1), this is very useful for logging big messages.
    As syslog usually truncates long messages.
    """
    chunks = split_on_chunks(text, MAX_SYMBOLS_PER_LOG_MESSAGE)
    if len(chunks) > 1:
      for i in range(len(chunks)):
        logger.info("Cmd log for taskId={0} and chunk {1}/{2} of log for command: \n".format(taskId, i+1, len(chunks)) + chunks[i])
    else:
      logger.info("Cmd log for taskId={0}: ".format(taskId) + text)

  def get_retry_delay(self, last_delay):
    """
    Returns exponentially growing delay. The idea being if number of retries is high then the reason to retry
    is probably a host or environment specific issue requiring longer waits
    """
    return last_delay * 2

  def on_background_command_complete_callback(self, process_condensed_result, handle):
    logger.debug('Start callback: %s', process_condensed_result)
    logger.debug('The handle is: %s', handle)
    status = CommandStatus.completed if handle.exitCode == 0 else CommandStatus.failed

    aborted_postfix = self.customServiceOrchestrator.command_canceled_reason(handle.command['taskId'])
    if aborted_postfix:
      status = CommandStatus.failed
      logger.debug('Set status to: %s , reason = %s', status, aborted_postfix)
    else:
      aborted_postfix = ''

    role_result = self.commandStatuses.generate_report_template(handle.command)

    role_result.update({
      'stdout': process_condensed_result['stdout'] + aborted_postfix,
      'stderr': process_condensed_result['stderr'] + aborted_postfix,
      'exitCode': process_condensed_result['exitcode'],
      'structuredOut': str(json.dumps(process_condensed_result['structuredOut'])) if 'structuredOut' in process_condensed_result else '',
      'status': status,
    })

    self.commandStatuses.put_command_status(handle.command, role_result)

  def reset(self):
    with self.commandQueue.mutex:
      self.commandQueue.queue.clear()
