#!/usr/bin/python

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

import urllib2
import json
import sys
import base64
import traceback

SERVICE_DEFINITION = '''
define service {
        use                             generic-service
        host_name                       %s
        normal_check_interval           1
        servicegroups                   AMBARI,%s
        service_description             %s - %s
        check_command                   check_ambari_alert!%s!%s!%s!%s!%s!%s!%s
}

'''
HOST_DEFINITION = '''
define host{
        use                     linux-server
        hostgroups              ambari-hosts
        host_name               %s
        alias                   %s
        address                 %s
        }
'''
HOST_GROUP_DEFINITION = '''
define hostgroup{
        hostgroup_name	%s
        alias	        %s
        }
'''
ALERT_CHECK_COMMAND = '''
define command {
        command_name    check_ambari_alert
        command_line    %s $ARG1$ $ARG2$ $ARG3$ $ARG4$ $ARG5$ $ARG6$ $ARG7$
}
'''
SERVICE_GROUP_DEFINITION = '''
define servicegroup {
    servicegroup_name               %s
    alias                           %s
}
'''

try:
  host = raw_input("Enter ambari host: ")
  port = raw_input("Enter ambari port: ")
  cluster = raw_input("Enter ambari cluster: ")
  ssl = raw_input("Use SSL [true/false]: ")
  login = raw_input("Enter ambari login: ")
  password = raw_input("Enter ambari password: ")
  alerts_url = 'api/v1/clusters/{0}/alerts?fields=Alert/label,Alert/service_name,Alert/name,Alert/text,Alert/state'
  if ssl.lower() == 'true':
    protocol = 'https'
  else:
    protocol = 'http'
  url = '{0}://{1}:{2}/{3}'.format(protocol, host, port, alerts_url.format(cluster))
  admin_auth = base64.encodestring('%s:%s' % (login, password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  response = urllib2.urlopen(request)
  response_body = response.read()
except Exception as ex:
  print "Error during Ambari Alerts data fetch: %s" % ex
  sys.exit(1)
try:
  alerts = json.loads(response_body)['items']
  services = []
  service_groups = {'AMBARI': SERVICE_GROUP_DEFINITION % ('AMBARI', 'Ambari services group')}
  hosts = {host: HOST_DEFINITION % (host, 'Ambari server', host)}
  host_groups = [HOST_GROUP_DEFINITION % ('ambari-hosts', 'Ambari hosts')]
  for alert in alerts:
    service_name = alert['Alert']['service_name']
    label = alert['Alert']['label']
    name = alert['Alert']['name']
    alert_host = alert['Alert']['host_name']
    if alert_host is None:
      alert_host = host
    if alert_host not in hosts:
      hosts[alert_host] = HOST_DEFINITION % (alert_host, 'Ambari host - ' + alert_host, alert_host)
    if service_name not in service_groups:
      service_groups[service_name] = SERVICE_GROUP_DEFINITION % (service_name, service_name + ' services group')
    services.append(SERVICE_DEFINITION % (alert_host, service_name, service_name, label, host, port, cluster, protocol, login, base64.b64encode(password), name))
except Exception as ex:
  print "Error during processing Ambari Alerts data: %s" % ex
  sys.exit(1)
try:
  script_path = raw_input("Enter path to Ambari Alerts Plugin 'ambari_alerts.py': ")
  localhost_cfg = raw_input("Enter path to Nagios configuration file 'localhost.cfg' : ")
  commands_cfg = raw_input("Enter path to Nagios configuration file 'commands.cfg': ")
  with open(localhost_cfg, "a") as localhost_file:
    localhost_file.write("# Ambari Alerts HostGroups")
    for host_group in host_groups:
      localhost_file.write(host_group)
    localhost_file.write("# Ambari Alerts Hosts")
    for host_def in hosts.values():
      localhost_file.write(host_def)
    localhost_file.write("# Ambari Alerts ServiceGroups")
    for service_group in service_groups.values():
      localhost_file.write(service_group)
    localhost_file.write("# Ambari Alerts Services")
    for service in services:
      localhost_file.write(service)
  with open(commands_cfg, "a") as commands_file:
    commands_file.write(ALERT_CHECK_COMMAND % script_path)
except Exception as ex:
  print "Error during creating Nagios objects for Ambari Alerts: %s" % ex
  sys.exit(1)
