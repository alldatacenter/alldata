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

import uuid
import datetime
import time
import json
import logging
import optparse
import shutil
import sys
import subprocess
import os.path
import socket

logger = logging.getLogger('Precheck')
has_warnings=False
has_errors=False

#request types
HTTP_REQUEST_GET='GET'
HTTP_REQUEST_POST='POST'
HTTP_REQUEST_DELETE='DELETE'

#HTTP CODE
HTTP_OK=200
HTTP_CREATED=201
HTTP_BAD_REQUEST=400
HTTP_FORBIDDEN=403
HTTP_CONFLICT=409

#defaults
EXIT_MESSAGE = "Make sure to provide correct cluster information including port, admin user name and password. Default values will be used if you omit the command parameters.";
DEFAULT_HTTP_PORT=8080
DEFAULT_ADMIN_USER='admin'
DEFAULT_LOG_DIR='/tmp/preinstall_checks'
DEFAULT_LOG_FILE='preinstall_checks.log'
DEFAULT_HTTP_REQUEST_TYPE=HTTP_REQUEST_GET
DEFAULT_AMBARI_SERVER_PROPERTIES='/etc/ambari-server/conf/ambari.properties'
DEFAULT_MAX_COUNTER=4
DEFAULT_TIMER_LONG=5
DEFAULT_POLLING_TIMER_REQUEST=10
DEFAULT_MINDISKSPACE=2.0 #in GB
DEFAULT_MINDISKSPACEUSRLIB=1.0 #in GB

#ops
OPERATION_HOST_CHECK='host_check'
OPERATION_VALIDATE_BLUEPRINT='validate_blueprint'
OPERATIONS=[OPERATION_HOST_CHECK, OPERATION_VALIDATE_BLUEPRINT]

#codes
CODE_SUCCESS=0
CODE_ERROR=1
CODE_WARNING=2
CODE_CONNECTION_REFUSED=7

#labels
LABEL_OK='[   OK  ]'
LABEL_WARNING='[WARNING]'
LABEL_ERROR='[ ERROR ]'

#status
STATUS_ACCEPTED='Accepted'
STATUS_COMPLETED='COMPLETED'
STATUS_PASSED='PASSED'
STATUS_WARNING='FAILED'
STATUS_FAILED='WARNING'
STATUS_ABORTED='ABORTED'
STATUS_IN_PROGRESS='IN_PROGRESS'
STATUS_PENDING='PENDING'
#list of status indicating the operation has yet to finish
LIST_FINISHED_REQUEST_STATUS=[STATUS_FAILED, STATUS_COMPLETED, STATUS_ABORTED]

def init_parser_options(parser):
  parser.add_option('-p', '--port',
                    dest="port", default=DEFAULT_HTTP_PORT,
                    help="Ambari Server port corrsponding to the network protocol. Default port is {0} for an HTTP connection".format(DEFAULT_HTTP_PORT))
  parser.add_option('-u', '--user',
                    dest="user", default=DEFAULT_ADMIN_USER,
                    help="Ambari admin user. Default user name is {0}".format(DEFAULT_ADMIN_USER))
  parser.add_option('-a', '--password',
                    dest="password",
                    help="Ambari admin user password.")
  parser.add_option('-l', '--log',
                    dest="log",
                    default=DEFAULT_LOG_DIR,
                    help="The log file home location. Default log file home is {0}.".format(DEFAULT_LOG_DIR),
                    metavar="DIR")
  parser.add_option('--blueprint',
                    dest="blueprint",
                    default=None,
                    help="Blueprint to validate",
                    metavar="FILE")
  parser.add_option('--operation',
                    dest='operation', default=OPERATION_HOST_CHECK,
                    help='Operation can one of the following {0}'.format(', '.join(OPERATIONS)))
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Output verbosity.")

"""
Validate parameters passed in from the command line.
Exit if there are validation errors.
"""
def validate_options(options):
  errors = []

  """General parameters that must be passed in via command line or set with a default value"""
  if not options.port:
    errors.append("No Ambari server port provided.")
  if not options.user:
      errors.append("No Ambari admin user name provided.")
  if not options.password:
    errors.append("No Ambari admin user passsword provided.")
  if not options.log:
      errors.append("No log home path provided.")

  """General check for operations"""
  if not options.operation:
    errors.append('No operation provided')
  elif not options.operation in OPERATIONS:
    errors.append('Unknow operation {0}. Specify one of the following operations: {1}'.format(options.operation, ', '.join(OPERATIONS)))
  elif options.operation == OPERATION_VALIDATE_BLUEPRINT:
    if not options.blueprint:
      errors.append('No blueprint file provided')

  if not errors:
    return 'Parameters validation finished successfully', CODE_SUCCESS
  else:
    return 'Parameters validation finished with error(s). {0}'.format('. '.join(errors)), CODE_ERROR

def get_log_file(log_home):
  return '{0}/{1}'.format(log_home, DEFAULT_LOG_FILE)

def init_logger(options):
  log_dir = options.log
  if not os.path.exists(log_dir):
    os.makedirs(log_dir)

  logging_level = logging.DEBUG if options.verbose else logging.INFO
  logger.setLevel(logging_level)
  logger.handlers = []

  formatter = logging.Formatter('%(asctime)s [%(levelname)s] %(message)s')
  file_handler = logging.FileHandler(get_log_file(log_dir), mode='w')
  file_handler.setFormatter(formatter)
  logger.addHandler(file_handler)

  stdout_handler = logging.StreamHandler(sys.stdout)
  stdout_handler.setFormatter(formatter)
  logger.addHandler(stdout_handler)

"""
Back up log directory if it already exists.
"""
def backup_log(filePath):
  if filePath is not None and os.path.exists(filePath):
    timestamp = datetime.datetime.now()
    simpleformat = '%Y%m%d%H%M%S'
    backup_file = filePath + "." + timestamp.strftime(simpleformat)
    try:
      shutil.move(filePath, backup_file)
    except Exception, err:
      print('Failed to backup "{0}": {1}'.format(str(filePath), str(err)))
      return '', CODE_WARNING
    return backup_file, CODE_SUCCESS
  else:
    return '', CODE_SUCCESS

def get_current_time():
  total_seconds = time.time()
  current_time = datetime.datetime.fromtimestamp(total_seconds).strftime('%Y-%m-%d %H:%M:%S')
  return current_time

def step(msg):
  logger.info('')
  if len(msg) >= 43:
    logger.info('********   Check: {0}   ********'.format(msg))
  else:
    spaces = ' '.ljust((50 - len(msg))/2)
    logger.info('{0}{2}Check: {1}{2}{0}'.format('********',msg,spaces))

def print_check_result(check, msgs, code):
  if len(check)>=43:
    spaces = ' '.ljust(20)
  else:
    spaces = ' '.ljust(63 - len(check))
  if code == CODE_SUCCESS:
    logger.info('{0}{1}{2}'.format(check, spaces, LABEL_OK))
  elif code == CODE_WARNING:
    logger.info('{0}{1}{2}'.format(check, spaces, LABEL_WARNING))
    if msgs:
      for msg in msgs:
        if msg.strip():
          logger.warning('\t{0}'.format(msg.strip()))
  else:
    logger.info('{0}{1}{2}'.format(check, spaces, LABEL_ERROR))
    if msgs:
      for msg in msgs:
          logger.error('\t{0}'.format(msg.strip()))

def print_check_results(results):
  global has_warnings
  global has_errors
  for result in results:
    status = result['status']
    if STATUS_PASSED == status:
      code = CODE_SUCCESS
      print_check_result(result['key'], None, code)
    elif STATUS_WARNING == status:
      if not has_warnings:
        has_warnings = True
      code = CODE_WARNING
      print_check_result(result['key'], result['warning'], code)
    else:
      if not has_errors:
        has_errors = True
      code = CODE_ERROR
      print_check_result(result['key'], result['error'] if result['error'] else None, code)

def dump_parameters_to_log(options):
  server_url = get_server_url(options.port)

  logger.info('/******************************************************************************/')
  logger.info('                  Parameters used for script run                                ')
  logger.info('Cluster parameters')
  logger.info("Server URL:     {0}".format(server_url))
  logger.info("Port:           {0}".format(options.port))
  logger.info("User:           {0}".format(options.user))
  logger.info('')
  logger.info('Operation info')
  logger.info("Operation:      {0}".format(options.operation))
  logger.info("Log Home Dir:   {0}".format(options.log))
  logger.info("Log File:       {0}".format(get_log_file(options.log)))
  logger.info('/******************************************************************************/')

"""
Retrieve property value from Ambari Server properties file.
"""
def get_ambari_server_property(key):
  try:
    with open(DEFAULT_AMBARI_SERVER_PROPERTIES, 'r') as property_file:
      file_content = property_file.read()
    lines = file_content.splitlines()
    lines.reverse()
    for line in lines:
      tokens = line.split('=')
      if len(tokens) == 2:
        if tokens[0] == key:
          return tokens[1]
  except Exception, err:
    logger.error(str(err))
    return None
  return None

def get_server_protocol():
  sslActive = get_ambari_server_property('api.ssl')
  if sslActive == "true":
    return "https"
  else:
    return "http"

def get_admin_server_fqdn():
  return socket.getfqdn()

def get_server_url(port):
  protocol = get_server_protocol()
  url = "{0}://{1}:{2}".format(protocol, get_admin_server_fqdn(), str(port))
  return url

"""
Submit REST API to Ambari Server
"""
def execute_curl_command(url, headers=[], request_type=DEFAULT_HTTP_REQUEST_TYPE, request_body=None, user=DEFAULT_ADMIN_USER, password=None):
  """
  @param url: REST URL
  @param headers: Optional. Headers to be included in the REST API call
  @param request_type: HTTP request type (GET/POST/PUT/DELETE). Use HTTP GET as the default.
  @param request_body: Data to be submitted for HTTP POST and PUT requests
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  """
  curl_cmd_array = ["curl", "-v", "-u", "{0}:{1}".format(user,password), "-k", "-H", "X-Requested-By: ambari"]

  for header in headers:
    curl_cmd_array.append('-H')
    curl_cmd_array.append(header)
  curl_cmd_array.append('-s')
  curl_cmd_array.append('-X')
  curl_cmd_array.append(request_type)
  if request_type == 'PUT' or request_type == 'POST':
    if request_body:
      curl_cmd_array.append("-d")
      curl_cmd_array.append(request_body)
  curl_cmd_array.append(url)
  logger.debug('Curl command: {0}'.format(' '.join(curl_cmd_array)))
  exeProcess = subprocess.Popen(curl_cmd_array, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  out, err = exeProcess.communicate()
  exit_code = exeProcess.returncode
  return out, err, exit_code

def get_http_response_code(out):
  for a_line in out.split('\n'):
    a_line = a_line.strip()
    if a_line.endswith('HTTP/1.1 200 OK'):
      return HTTP_OK
    elif a_line.endswith('HTTP/1.1 201 Created'):
      return HTTP_CREATED
    elif a_line.endswith('HTTP/1.1 400 Bad Request'):
      return HTTP_BAD_REQUEST
    elif a_line.endswith('HTTP/1.1 409 Conflict'):
      return HTTP_CONFLICT
    elif a_line.endswith('HTTP/1.1 400 Forbidden'):
      return HTTP_FORBIDDEN
  return -1

"""
Determine if Ambari Server responded with an error message for the REST API call
"""
def is_erroneous_response_by_server(json_str):
  if not json_str:
    return False, 0, ''
  else:
    response = json.loads(json_str)
    status_code = response.get('status', -1)
    message =  response.get('message', None)
    if -1 == status_code and not message:
      return False, 0, ''
    else:
      return True, int(status_code), message

"""
Determine if Ambari Server has accepted the REST API call
"""
def is_request_accepted(json_str):
  logger.debug("Checking request in {0}".format(json_str))
  if not json_str:
    return False
  response = json.loads(json_str)
  summary = response.get('Requests', {})
  if summary:
    status = summary.get('status', None)
    return (STATUS_ACCEPTED == status)
  else:
    return False

def get_request_url(json_str, summary_only=True):
  if not json_str:
    return None
  response = json.loads(json_str)
  href = response.get('href', None)
  if href:
    url_str = str(href)
    if summary_only:
      return '{0}?fields=Requests'.format(url_str)
    else:
      return url_str
  else:
    logger.error("Failed to obtain request url. {0} does not contain 'href' section".format(json_str))
    return None

"""
Determine if the request is finished
"""
def is_request_finished(json_str):
  request_status = get_request_status(json_str)
  is_finished = (request_status in LIST_FINISHED_REQUEST_STATUS)
  is_finished_successfully = (STATUS_COMPLETED == request_status)
  response = json.loads(json_str)
  summary = response.get('Requests', {})
  progress_percent = summary.get('progress_percent', '-1')
  return is_finished, is_finished_successfully, int(progress_percent)

def is_request_finished_successfully(json_str):
  request_status = get_request_status(json_str)
  return STATUS_COMPLETED == request_status

def get_request_status(json_str):
  if not json_str:
    return None
  response = json.loads(json_str)
  summary = response.get('Requests', {})
  request_status = summary.get('request_status', None)
  if request_status:
    return request_status
  else:
    logger.error("Failed to determin request state. {0} does not contain 'Requests' section".format(json_str))
    return None

"""
Check request status based on the time interval
"""
def polling_request(url, user=None, password=None, timer=DEFAULT_POLLING_TIMER_REQUEST):
  """
  @param url: Request URL returned by the Ambari Server
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  @param timer: Time interval between two check status REST API call. Default is 10 seconds.
  """
  out=None
  err=None
  ec=0
  request_in_progress = True

  logger.debug("Polling status for {0} every {1} seconds".format(url, timer))
  logger.debug("Polling started at {0}".format(str(get_current_time())))

  previous_percentage = 0
  while request_in_progress:
    out, err, ec = execute_curl_command(url, user=user, password=password)
    if CODE_SUCCESS != ec:
      logger.debug('Server became offline')
      request_in_progress = False
    else:
      is_finished, is_finished_successfully, percentage = is_request_finished(out)
      if percentage >= 0:
        if percentage != previous_percentage:
          previous_percentage = percentage
          logger.debug(' {0}%'.format(percentage))
        else:
          logger.debug('.')
      if is_finished:
        request_in_progress = False
      else:
        time.sleep(timer)
  logger.debug("Stopped polling {0} at {1}. Request finished.".format(url, str(get_current_time())))
  return out, err, ec

def get_host(json_str):
  if not json_str:
    return None
  response = json.loads(json_str)
  task_result = response.get('Tasks', {})
  host_name = task_result.get('host_name', None)
  return host_name

"""
Summarize results of all the tasks executed in the request
"""
def summarize_tasks_by_hosts(request_url, user, password):
  """
  @param request_url: Request URL returned by the Ambari Server
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  """
  task_results_by_host = {}
  results_to_print = []
  out, err, ec = execute_curl_command(request_url, user=user, password=password)
  if CODE_SUCCESS == ec:
    if out:
      is_erroneous_response, http_ec, http_err = is_erroneous_response_by_server(out)
      if is_erroneous_response:
        results_to_print=[{'key':'Error response from server', 'status':http_ec, 'error':[http_err]}]
      else:
        urls = get_tasks_urls(out)
        if urls:
          for task_url in urls:
            task_out, err, ec = execute_curl_command(task_url, user=user, password=password)
            logger.debug(task_out)
            if CODE_SUCCESS == ec:
              host = get_host(task_out)
              if host:
                task_results_by_host[host] = task_out
            else:
              results_to_print=[{'key':'Connection refused', 'status':STATUS_FAILED, 'error':[err]}]
              break
        else:
          results_to_print=[{'key':'Empty task list', 'status':STATUS_FAILED}]
    else:
      results_to_print=[{'key':'Empty response from server', 'status':STATUS_FAILED}]
  else:
    results_to_print=[{'key':'Connection refused', 'status':STATUS_FAILED, 'error':[err]}]
  return task_results_by_host, results_to_print

def get_tasks_urls(json_str):
  response = json.loads(json_str)
  tasks = response.get('tasks', [])
  urls = set()
  for task in tasks:
    url = task.get('href',None)
    if url:
      urls.add(url)
  return urls

"""
Check if the script can log in Ambari Server REST API via user and password provided
"""
def server_reachable_by_credentials_with_retry(server_url, user, password):
  """
  @param server_url: Basic server url to connect and log in
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  """
  retry_counter = 0
  out = None
  ec = CODE_SUCCESS
  while retry_counter < DEFAULT_MAX_COUNTER:
    out, ec = server_reachable_by_credentials(server_url, user, password)
    if CODE_CONNECTION_REFUSED == ec:
      retry_counter = retry_counter + 1
      logger.debug('Server may have not become fully online yet, try to reconnect in {0} seconds'.format(DEFAULT_TIMER_LONG))
      time.sleep(DEFAULT_TIMER_LONG)
    else:
      logger.debug('Connected to server.')
      break
  if CODE_CONNECTION_REFUSED == ec:
    message = 'Server did not become fully online in {0} seconds.'.format(str(DEFAULT_MAX_COUNTER * DEFAULT_TIMER_LONG))
    logger.debug(message)
  return out, ec

"""
Check if the script can log in Ambari Server REST API via user and password provided
"""
def server_reachable_by_credentials(server_url, user, password):
  """
  @param server_url: Basic server url to connect and log in
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  """
  url = '{0}/api/v1/requests'.format(server_url)
  out, err, ec =  execute_curl_command(url, user=user, password=password)
  if ec != CODE_SUCCESS:
    return err, ec
  else:
    is_erroneous_response, http_ec, http_err = is_erroneous_response_by_server(out)
    if is_erroneous_response:
      return http_err, http_ec
    else:
      return '', CODE_SUCCESS

"""
Obtain a list of Ambari Agents registered to the host via a REST API call
"""
def get_ambari_agent_nodes(server_url, user, password):
  """
  @param server_url: Basic server url to connect and log in
  @param user: User for Ambari REST API authentication
  @param password: Password for the user used to authenticate the Ambari REST API call
  """
  url = "{0}/api/v1/services/AMBARI/components/AMBARI_AGENT".format(server_url)
  hosts = set()
  out, err, ec = execute_curl_command(url, user=user, password=password)
  is_erroneous_response, ec, err = is_erroneous_response_by_server(out)
  if is_erroneous_response:
    logger.error("HTTP {0}:{1}".format(ec, err))
    return hosts

  response = json.loads(out)
  host_list = response.get('hostComponents', [])
  for item in host_list:
    host_summary = item.get('RootServiceHostComponents', {})
    host_name = host_summary.get('host_name', None)
    if host_name:
      hosts.add(host_name)
  return hosts

"""
Run host checks
"""
def run_check(options, url, label_check, data):
  """
  @param options: Parameters passed in from the command line
  @param url: Ambari Server URL
  @param label_check: Text to display for the check result section
  @param data: Data to be submitted to the Ambari Server via a REST API call
  """
  out, err, ec = execute_curl_command(url, request_type=HTTP_REQUEST_POST, request_body=data, user=options.user, password=options.password)
  if CODE_SUCCESS != ec or not out:
    logger.debug(out)
    logger.debug(ec)
    logger.debug(err)
    print_check_result(label_check, ['Failed to connect to Ambari server'], ec)
    return ec
  else:
    is_erroneous_response, http_ec, http_err = is_erroneous_response_by_server(out)
    if is_erroneous_response:
      print_check_result(label_check, [http_err], http_ec)
      return http_ec
    elif is_request_accepted(out):
      request_url = get_request_url(out)
      finalresult, err, ec = polling_request(request_url, options.user, options.password)
      logger.debug(finalresult)
      if is_request_finished_successfully(finalresult):
        request_url = get_request_url(out, summary_only=False)
        return summarize_tasks_by_hosts(request_url, options.user, options.password)
      else:
        print_check_result(label_check, [err], CODE_ERROR)
    else:
      print_check_result(label_check, [out], CODE_ERROR)

def basic_task_result_parser(json_str, results):
  response = json.loads(json_str)
  task_result = response.get('Tasks', {})
  host_name = task_result.get('host_name', None)
  status = task_result.get('status', None)
  if STATUS_COMPLETED != status:
    stderr = task_result.get('stderr', None)
    results.append({'key':host_name, 'status':status, 'error':stderr})
    return {}
  else:
    return task_result.get('structured_out', {})

def host_check_parser(task_results_by_hosts, results_to_print):
  if not task_results_by_hosts:
    return
  for key in task_results_by_hosts:
    json_str = task_results_by_hosts[key]
    structured_out = basic_task_result_parser(json_str, results_to_print)
    if structured_out:
      check_result = structured_out.get('host_resolution_check', {})
      ec = check_result.get('exit_code', -1)
      if CODE_SUCCESS == ec:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
      else:
        results_to_print.append({'key':key, 'status':STATUS_FAILED, 'error':[check_result.get('message', None)]})

"""
Host name resolution check
"""
def run_host_checks(options, agents, server_url):
  label_check = 'Host name resolution'
  step(label_check)
  url = '{0}/api/v1/requests'.format(server_url)
  data = '{{"RequestInfo":{{"action":"check_host","context":"Check host","parameters":{{"check_execute_list":"host_resolution_check","jdk_location":"{0}/resources","threshold":"20","hosts":"{1}"}}}},"Requests/resource_filters":[{{"hosts":"{1}"}}]}}'.format(server_url, ','.join(agents))
  logger.debug('Host resolution check data {0}'.format(data))
  task_results_by_hosts, results_to_print = run_check(options, url, label_check, data)
  host_check_parser(task_results_by_hosts, results_to_print)
  print_check_results(results_to_print)

def java_home_check_parser(task_results_by_hosts, results_to_print):
  if not task_results_by_hosts:
    return
  for key in task_results_by_hosts:
    json_str = task_results_by_hosts[key]
    structured_out = basic_task_result_parser(json_str, results_to_print)
    if structured_out:
      check_result = structured_out.get('java_home_check', {})
      ec = check_result.get('exit_code', -1)
      if CODE_SUCCESS == ec:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
      else:
        results_to_print.append({'key':key, 'status':STATUS_FAILED, 'error':[check_result.get('message', None)]})

"""
Java home path check
"""
def run_java_home_checks(options, agents, server_url):
  label_check = 'Java Home location'
  step(label_check)
  url = '{0}/api/v1/requests'.format(server_url)
  java_home = get_ambari_server_property('java.home')
  logger.info('Ambari server java home: {0}'.format(java_home))
  data = '{{"RequestInfo":{{"context":"Check hosts","action":"check_host","parameters":{{"threshold":"60","java_home":"{0}","jdk_location":"{1}/resources","check_execute_list":"java_home_check"}}}},"Requests/resource_filters":[{{"hosts":"{2}"}}]}}'.format(java_home, server_url, ','.join(agents))
  logger.debug('Java home check data {0}'.format(data))
  task_results_by_hosts, results_to_print = run_check(options, url, label_check, data)
  java_home_check_parser(task_results_by_hosts, results_to_print)
  print_check_results(results_to_print)

def thp_checks_parser(task_results_by_hosts, results_to_print):
  if not task_results_by_hosts:
    return
  for key in task_results_by_hosts:
    json_str = task_results_by_hosts[key]
    structured_out = basic_task_result_parser(json_str, results_to_print)
    if structured_out:
      check_result = structured_out.get('transparentHugePage', {})
      thp_message = check_result.get('message', None)
      if thp_message == 'always':
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':['Transparent Huge Pages (THP) is enabled', 'THP should be disabled to avoid potential Hadoop performance issues.']})
      else:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})

def disk_space_checks_parser(host_info_by_host, results_to_print):
  min_disk_space = DEFAULT_MINDISKSPACE * 1024 * 1024
  min_disk_space_usrlib = DEFAULT_MINDISKSPACEUSRLIB * 1024 * 1024
  for key in host_info_by_host:
    host_summary = host_info_by_host[key]
    info = host_summary.get('Hosts', {})
    disk_info = info.get('disk_info', [])
    for disk in disk_info:
      errors = []
      passes = 0
      mountpoint = disk.get('mountpoint', None)
      if '/' == mountpoint:
        free_space = disk.get('available', -1)
        if free_space == -1:
          errors.append('Failed to obtain free space for mountpoint /')
        elif free_space < min_disk_space:
          errors.append('A miminum of {} GB free space for mountpoint /'.format(DEFAULT_MINDISKSPACE))
        else:
          passes += 1
      elif '/usr' == mountpoint or '/usr/lib' == mountpoint:
        free_space = disk.get('available', -1)
        if free_space == -1:
          errors.append('Failed to obtain free space for mountpoint /usr or /usr/lib')
        elif free_space < min_disk_space_usrlib:
          errors.append('A miminum of {} GB free space for mountpoint /usr or /usr/lib'.format(DEFAULT_MINDISKSPACEUSRLIB))
        else:
          passes += 1
      if passes > 0:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
      elif errors:
        results_to_print.append({'key':key, 'status':STATUS_FAILED, 'error':errors})

def get_last_agent_env(host_info):
    info = host_info.get('Hosts', {})
    last_agent_env = info.get('last_agent_env', {})
    return last_agent_env

def firewall_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    if structured_out:
      last_agent_env = structured_out.get('last_agent_env_check', {})
      if 'firewallRunning' in last_agent_env:
        firewall_running = last_agent_env['firewallRunning']
        if firewall_running:
          results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':['Firewall is running on the host', 'Please configure the firewall to allow communications on the ports documented in the Configuring Ports section of the Ambari documentation.']})
        else:
          results_to_print.append({'key':key, 'status':STATUS_PASSED})
      else:
        results_to_print.append({'key':key, 'status':STATUS_FAILED, 'error':['Failed to determine if firewall is running on the host']})

def java_process_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    if structured_out:
      last_agent_env = structured_out.get('last_agent_env_check', {})
      host_health = last_agent_env.get('hostHealth', {})
      active_java_processes = host_health.get('activeJavaProcs', [])
      if active_java_processes:
        warnings = []
        for process in active_java_processes:
          warnings.append('Process {0} under user {1} should not be running'.format(process['pid'], process['user']))
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':warnings})
      else:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})

def install_packages_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    installed_packages = structured_out.get('installed_packages', [])
    if installed_packages:
      warnings = []
      for package in installed_packages:
        warnings.append('{0} (version {1}) is installed from repo {2}. It should be removed before deploying the cluster.'.format(package['name'], package['version'], package['repoName']))
      results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':warnings})
    else:
      results_to_print.append({'key':key, 'status':STATUS_PASSED})

def file_and_folder_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    stack_files_and_folders = last_agent_env.get('stackFoldersAndFiles',[])
    if stack_files_and_folders:
      warnings = []
      for item in stack_files_and_folders:
        warnings.append('{0} {1} should not exist.'.format(item['type'].title(), item['name']))
      results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':warnings})
    else:
      results_to_print.append({'key':key, 'status':STATUS_PASSED})

def live_services_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    host_health = last_agent_env.get('hostHealth', {})
    live_services = host_health.get('liveServices', [])
    if live_services:
      warnings = []
      for service in live_services:
        if 'Unhealthy' == service['status']:
          warnings.append('Service {0} shoud be up.'.format(service['name']))
      if warnings:
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':warnings})
      else:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
    else:
      results_to_print.append({'key':key, 'status':STATUS_PASSED})

def default_user_ids_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    existing_users = last_agent_env.get('existingUsers', [])
    if existing_users:
      messages = []
      for user in existing_users:
        messages.append('User {0} with home directory {1} exists.'.format(user['name'], user['homeDir']))
      if messages:
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':messages})
    else:
      results_to_print.append({'key':key, 'status':STATUS_PASSED})

def umask_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    if 'umask' in last_agent_env:
      umask = int(last_agent_env['umask'])
      if umask > 23:
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':['Umask is {0}. Consider update it.'.format(umask)]})
      else:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
    else:
      results_to_print.append({'key':key, 'status':STATUS_FAILED, 'errors':['Failed to obtain umask value on the host.']})

def alternatives_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    alternatives = last_agent_env.get('alternatives', [])
    if alternatives:
      warnings = []
      for alternative in alternatives:
        warnings.append('Existing /etc/alternativies entry: {0} points to {1}'.format(alternative['name'], alternative['target']))
      results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':warnings})
    else:
      results_to_print.append({'key':key, 'status':STATUS_PASSED})

def reverse_lookup_checks_parser(task_results_by_host, results_to_print):
  for key in task_results_by_host:
    structured_out = basic_task_result_parser(task_results_by_host[key], results_to_print)
    last_agent_env = structured_out.get('last_agent_env_check', [])
    if 'reverseLookup' in last_agent_env:
      reverse_lookup = last_agent_env['reverseLookup']
      if reverse_lookup:
        results_to_print.append({'key':key, 'status':STATUS_PASSED})
      else:
        results_to_print.append({'key':key, 'status':STATUS_WARNING, 'warning':['The hostname was not found in the reverse DNS lookup', 'This may result in incorrect behavior. Please check the DNS setup and fix the issue.']})
    else:
      results_to_print.append({'key':key, 'status':STATUS_FAILED, 'error':['Failed to determine if DNS reverse lookup is configured on the host']})

"""
Agent last enviornment check
"""
def run_agent_checks(options, agents, server_url):
  logger.info('')
  logger.info('Prepare for Ambari Agent host check')
  label_check = 'Ambari Agent host check'
  url = '{0}/api/v1/requests'.format(server_url)
  data = '{{"RequestInfo":{{"action":"check_host","context":"Check host","parameters":{{"check_execute_list":"last_agent_env_check,installed_packages,existing_repos,transparentHugePage","jdk_location":"{0}/resources","threshold":"20"}}}},"Requests/resource_filters":[{{"hosts":"{1}"}}]}}'.format(server_url, ','.join(agents))
  logger.debug('Agent enviornment check data to submit {0}'.format(data))
  task_results_by_host, results_to_print = run_check(options, url, label_check, data)

  step('Transparent Huge Pages')
  thp_checks_parser(task_results_by_host, results_to_print)
  print_check_results(results_to_print)

  host_info_url = '{0}/api/v1/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env,Hosts/host_name,Hosts/os_type,Hosts/os_arch,Hosts/os_family,Hosts/ip'.format(server_url)
  out, err, ec = execute_curl_command(host_info_url, user=options.user, password=options.password)
  logger.debug('Agent host information {0}'.format(out))
  host_info_by_host = {}
  if out:
    response = json.loads(out)
    items = response.get('items', {})
    for item in items:
      info = item.get('Hosts', {})
      host_name = info.get('host_name', None)
      if host_name:
        host_info_by_host[host_name]=item
  if host_info_by_host:
    step('Disk space')
    results_to_print = []
    disk_space_checks_parser(host_info_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Firewall enabled')
    results_to_print = []
    firewall_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Java processes')
    results_to_print = []
    java_process_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Installed packages')
    results_to_print = []
    install_packages_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Stack files and directories')
    results_to_print = []
    file_and_folder_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Live services')
    results_to_print = []
    live_services_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Default user names')
    results_to_print = []
    default_user_ids_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Umask')
    results_to_print = []
    umask_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Alternatives')
    results_to_print = []
    alternatives_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

    step('Reverse lookup')
    results_to_print = []
    reverse_lookup_checks_parser(task_results_by_host, results_to_print)
    print_check_results(results_to_print)

def run_validate_blueprint(options, server_url):
  results_to_print = []
  blueprint_file = options.blueprint
  label_check = 'Blueprint validation'
  step(label_check)
  logger.debug('Blueprint file to check {0}'.format(blueprint_file))
  if os.path.isfile(blueprint_file):
    """Validate blueprint file is a valid json file"""
    valid_json_file = False
    try:
      with open(blueprint_file) as data_file:
        data = json.load(data_file)
        valid_json_file = True
    except ValueError as value_error:
      results_to_print.append({'key':label_check, 'status':STATUS_FAILED, 'error':[str(value_error)]})

    if valid_json_file:
      """Either a timestamp based name or the name defined in the blueprint"""
      blueprint_metadata = data.get('Blueprints', {})
      blueprint_name = blueprint_metadata.get('blueprint_name', None)
      if not blueprint_name:
        blueprint_name = 'blueprint_validation_{0}'.format(str(uuid.uuid4()))
      logger.debug('Blueprint name used for server side validation: {0}'.format(blueprint_name))
      url = '{0}/api/v1/blueprints/{1}'.format(server_url, blueprint_name)
      out, err, ec = execute_curl_command(url, request_type=HTTP_REQUEST_POST, request_body="@{0}".format(blueprint_file), user=options.user, password=options.password)
      logger.debug(out)
      logger.debug(err)
      if CODE_ERROR == ec:
        results_to_print.append({'key':label_check, 'status':STATUS_FAILED, 'error':[err]})
      else:
        http_response_code = get_http_response_code(err)
        logger.debug('HTTP response from the Ambari server: {0}'.format(http_response_code))
        if http_response_code == HTTP_CREATED and not out :
          results_to_print.append({'key':label_check, 'status':STATUS_PASSED})
        else:
          is_erroneous_response, http_ec, http_err = is_erroneous_response_by_server(out)
          if is_erroneous_response:
            results_to_print.append({'key':label_check, 'status':STATUS_FAILED, 'error':[http_err]})
          else:
            results_to_print.append({'key':label_check, 'status':STATUS_FAILED, 'error':[err]})
  else:
    results_to_print.append({'key':label_check, 'status':STATUS_FAILED, 'error':['{0} does not exist'.format(blueprint_file)]})
  print_check_results(results_to_print)
  deregister_temporary_blueprint(options, server_url, blueprint_name)

def deregister_temporary_blueprint(options, server_url, blueprint_name):
  url = '{0}/api/v1/blueprints/{1}'.format(server_url, blueprint_name)
  out, err, ec = execute_curl_command(url, request_type=HTTP_REQUEST_DELETE, user=options.user, password=options.password)
  if CODE_ERROR == ec:
    logger.error(out)
    logger.error(err)
  else:
    logger.debug(out)
    logger.debug(err)
    http_response_code = get_http_response_code(err)
    logger.debug('HTTP response from the Ambari server: {0}'.format(http_response_code))
    if http_response_code == HTTP_OK and not out :
      logger.debug("{0} deregistered".format(blueprint_name))
    else:
      is_erroneous_response, http_ec, http_err = is_erroneous_response_by_server(out)
      if is_erroneous_response:
        logger.error(http_err)
      else:
        logger.info(out)
        if err:
          logger.error(err)

"""
Execute the operation passed in from the command line
"""
def run(options):
  global has_warnings
  global has_errors

  server_url = get_server_url(options.port)
  label_check = 'Ambari server reachable by user credentials'
  step(label_check)
  out, ec = server_reachable_by_credentials_with_retry(server_url, options.user, options.password)
  if CODE_SUCCESS == ec:
    print_check_result(label_check, ['Ambari server reachable via {0}'.format(server_url)], ec)
  elif CODE_ERROR == ec:
    print_check_result(label_check, ['Failed to establish connection to {0}.'.format(server_url)], ec)
    return ec
  elif HTTP_FORBIDDEN == ec:
    print_check_result(label_check, ['Wrong credentials provided.'], ec)
    return ec
  agents = get_ambari_agent_nodes(server_url, options.user, options.password)
  logger.info('Total number of agents {0}'.format(len(agents)))
  if not agents:
    logger.error('No Ambari Agent registered to the Ambari Server. Install Ambari Agent first.')
    return CODE_ERROR

  if OPERATION_HOST_CHECK == options.operation:
    run_host_checks(options, agents, server_url)
    run_java_home_checks(options, agents, server_url)
    run_agent_checks(options, agents, server_url)
  elif OPERATION_VALIDATE_BLUEPRINT == options.operation:
    run_validate_blueprint(options, server_url)

  if has_errors:
    logger.info('')
    logger.error('Checks finished with errors')
    return CODE_ERROR
  elif has_warnings:
    logger.info('')
    logger.warning('Checks finished with warnings')
    return CODE_WARNING
  else:
    logger.info('')
    logger.info('Checks finished')
    return CODE_SUCCESS

def main():
  parser = optparse.OptionParser(usage="usage: %prog [option] arg ... [option] arg",)
  init_parser_options(parser)
  (options, args) = parser.parse_args()

  backup_file, ec = backup_log(options.log)

  init_logger(options)

  if backup_file:
    logger.info('Previous logs backed up as {0}'.format(backup_file))

  out, ec = validate_options(options)
  if CODE_ERROR == ec:
    logger.error(out)
    sys.exit(ec)
  else:
    dump_parameters_to_log(options)
    try:
      ec = run(options)
      sys.exit(ec)
    except Exception, e:
      logger.exception(e)
      sys.exit(CODE_ERROR)

if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("Aborting ... Keyboard Interrupt.")
    sys.exit(1)
