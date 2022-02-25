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

import argparse
import os
import subprocess
import sys
import pprint
import time
import traceback
import re
import socket

cluster_prefix = "perf"
ambari_repo_file_url = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos7/2.x/updates/2.7.1.0/ambaribn.repo"

public_hostname_script = "foo"
hostname_script = "foo"

NUMBER_OF_AGENTS_ON_HOST = 50


class SSH:
  """
  Ssh implementation of this
  """

  def __init__(self, user, sshkey_file, host, command, custom_option='', errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.command = command
    self.errorMessage = errorMessage
    self.custom_option = custom_option

  def run(self):
    sshcommand = ["ssh",
                  "-o", "ConnectTimeOut=180",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "BatchMode=yes",
                  self.custom_option,
                  "-i", self.sshkey_file,
                  self.user + "@" + self.host, self.command]

    if not self.custom_option:
      del sshcommand[7]

    i = 1
    while True:
      try:
        sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
        log = sshstat.communicate()
        if sshstat.returncode != 0:
          print "Executing SSH command on {0} failed: {1}".format(self.host, log)
          print "\nRetrying SSH command one more time!"
          if i >= 3:
            break
          i += 1
          time.sleep(10)
          continue
        break
      except:
        print "Could not SSH to {0}, waiting for it to start".format(self.host)
        i += 1
        time.sleep(10)

    if i >= 3:
      print "Could not execute remote ssh command: " + ' '.join(sshcommand)
      raise Exception("Could not connect to {0}. Giving up with erros: {1}".format(self.host, log))

    errorMsg = log[1]
    if self.errorMessage and sshstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg

    print "SSH command execution finished"

    return {"exitstatus": sshstat.returncode, "log": log, "errormsg": errorMsg}


class SCP:
  """
  SCP implementation that is thread based. The status can be returned using
  status val
  """

  def __init__(self, user, sshkey_file, host, inputFile, remote, errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.errorMessage = errorMessage

  def run(self):
    scpcommand = ["scp",
                  "-r",
                  "-o", "ConnectTimeout=60",
                  "-o", "BatchMode=yes",
                  "-o", "StrictHostKeyChecking=no",
                  "-i", self.sshkey_file, self.inputFile, self.user + "@" +
                                                          self.host + ":" + self.remote]
    i = 1
    while True:
      try:
        scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        log = scpstat.communicate()
        if scpstat.returncode != 0:
          print "Executing SCP command on {0} failed: {1}".format(self.host, log)
          print "\nRetrying SCP command one more time!"
          if i >= 3:
            break
          i += 1
          time.sleep(10)
          continue
        break
      except:
        print "Could not SCP to {0}, waiting for it to start".format(self.host)
        i += 1
        time.sleep(10)

      if i >= 3:
        print "Could not execute remote scp command: " + ' '.join(scpcommand)
        raise Exception("Could not connect to {0}. Giving up with erros: {1}".format(self.host, log))

    errorMsg = log[1]
    if self.errorMessage and scpstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg

    print "SCP command execution finished"

    return {"exitstatus": scpstat.returncode, "log": log, "errormsg": errorMsg}


# main method to parse arguments from user and start work
def main():
  parser = argparse.ArgumentParser(
    description='This script brings up a cluster with ambari installed, configured and started',
    epilog='Only GCE is supported as of now!'
  )

  # options
  parser.add_argument('--controller', type=str,
                      action='store', help='GCE controller ip address.')

  parser.add_argument('--key', type=str,
                      action='store', help='Path to GCE ssh key.')

  parser.add_argument('--cluster-suffix', type=str,
                      action='store', help='Cluster name suffix.')

  parser.add_argument('--agent-prefix', type=str,
                      action='store', help='Agent name prefix.')

  parser.add_argument('--agents-count', type=int,
                      action='store', help='Agents count for whole cluster (multiples of 50).')

  if len(sys.argv) <= 1:
    parser.print_help()
    sys.exit(-1)

  args = parser.parse_args()
  do_work(args)


def do_work(args):
  """
  Check that all required args are passed in. If so, deploy the cluster.
  :param args: Command line args
  """
  if not args.controller:
    raise Exception("GCE controller ip address is not defined!")

  if not args.key:
    raise Exception("Path to gce ssh key is not defined!")

  if not args.cluster_suffix:
    raise Exception("Cluster name suffix is not defined!")

  if not args.agent_prefix:
    raise Exception("Agent name prefix is not defined!")

  if not args.agents_count:
    raise Exception("Agents count for whole cluster is not defined (will put 50 Agents per VM)!")

  deploy_cluster(args)


def deploy_cluster(args):
  """
  Process cluster deployment
  :param args: Command line args.
  """
  # When dividing, need to get the ceil.
  number_of_nodes = ((args.agents_count - 1) / NUMBER_OF_AGENTS_ON_HOST) + 1

  # In case of an error after creating VMs, can simply comment out this function to run again without creating VMs.
  create_vms(args, number_of_nodes)

  # getting list of vms information like hostname and ip address
  print "Getting list of virtual machines from cluster..."
  # Dictionary from host name to IP
  (server_dict, agents_dict) = get_vms_list(args)

  # check number of nodes in cluster to be the same as user asked
  print "Checking count of created nodes in cluster..."
  if not agents_dict or len(agents_dict) < number_of_nodes:
    raise Exception("Cannot bring up enough nodes. Requested {0}, but got {1}. Probably not enough resources!".format(number_of_nodes, len(agents_dict)))

  print "GCE cluster was successfully created!\n"

  # installing/starting ambari-server and ambari-agents on each host
  server_item = server_dict.items()[0]
  server_host_name = server_item[0]
  server_ip = server_item[1]
  print "=========================="
  print "Server Hostname: %s" % server_host_name
  print "Server IP: %s" % server_ip
  print "==========================\n"

  # Sort the agents by hostname into a list.
  sorted_agents = sort_hosts(agents_dict)
  pretty_print_vms(sorted_agents)

  print "Creating server.sh script (which will be executed on server to install/configure/start ambari-server)..."
  create_server_script(server_host_name)

  print "Creating agent.sh script (which will be executed on agent hosts to install/configure/start ambari-agent..."
  create_agent_script(server_host_name)

  time.sleep(10)

  prepare_server(args, server_host_name, server_ip)

  # If the user asks for a number of agents that is not a multiple of 50, then only create how many are needed instead
  # of 50 on every VM.
  num_agents_left_to_create = args.agents_count
  start_num = 1

  for (hostname, ip) in sorted_agents:
    num_agents_on_this_host = min(num_agents_left_to_create, NUMBER_OF_AGENTS_ON_HOST)

    print "=========================="
    print "Working on VM {0} that will contain hosts {1} - {2}".format(hostname, start_num, start_num + num_agents_on_this_host - 1)

    # The agent multiplier config will be different on each VM.

    cmd_generate_multiplier_conf = "mkdir -p /etc/ambari-agent/conf/ ; printf \"start={0}\\nnum={1}\\nprefix={2}\" > /etc/ambari-agent/conf/agent-multiplier.conf".format(start_num, num_agents_on_this_host, args.agent_prefix)
    start_num += num_agents_on_this_host
    num_agents_left_to_create -= num_agents_on_this_host

    prepare_agent(args, hostname, ip, cmd_generate_multiplier_conf)

  pass
  print "All scripts where successfully copied and started on all hosts. " \
        "\nPay attention that server.sh script need 5 minutes to finish and agent.sh need 3 minutes!"


def create_vms(args, number_of_nodes):
  """
  Request the server and VMs for the agents from GCE.
  :param args: Command line args
  :param number_of_nodes: Number of VMs to request.
  """
  print "Creating server VM {0}-server-{1} with xxlarge nodes on centos7...".format(cluster_prefix, args.cluster_suffix)
  execute_command(args, args.controller, "/opt/gce-utils/gce up {0}-server-{1} 1 --centos7 --xxlarge --ex --disk-xxlarge --ssd".format(cluster_prefix, args.cluster_suffix),
                  "Failed to create server, probably not enough resources!", "-tt")
  time.sleep(10)

  # trying to create cluster with needed params
  print "Creating agent VMs {0}-agent-{1} with {2} xlarge nodes on centos7...".format(cluster_prefix, args.cluster_suffix, str(number_of_nodes))
  execute_command(args, args.controller, "/opt/gce-utils/gce up {0}-agent-{1} {2} --centos7 --xlarge --ex --disk-xlarge".format(cluster_prefix, args.cluster_suffix, str(number_of_nodes)),
                  "Failed to create cluster VMs, probably not enough resources!", "-tt")

  # VMs are not accessible immediately
  time.sleep(10)


def prepare_server(args, hostname, ip):
  remote_path = "/server.sh"
  local_path = "server.sh"
  print "Copying server.sh to {0}...".format(hostname)
  put_file(args, ip, local_path, remote_path, "Failed to copy file!")

  print "Executing remote ssh command (set correct permissions and start executing server.sh in separate process) on {0}...".format(hostname)
  execute_command(args, ip, "cd /; chmod 777 server.sh; nohup ./server.sh >/server.log 2>&1 &",
                  "Install/configure/start server script failed!")


def prepare_agent(args, hostname, ip, cmd_generate_multiplier_conf):
  remote_path = "/agent.sh"
  local_path = "agent.sh"
  print "Copying agent.sh to {0}...".format(hostname)
  put_file(args, ip, local_path, remote_path, "Failed to copy file!")

  print "Generating agent-multiplier.conf"
  execute_command(args, ip, cmd_generate_multiplier_conf, "Failed to generate agent-multiplier.conf on host {0}".format(hostname))

  print "Executing remote ssh command (set correct permissions and start executing agent.sh in separate process) on {0}...".format(hostname)
  execute_command(args, ip, "cd /; chmod 777 agent.sh; nohup ./agent.sh >/agent.log 2>&1 &",
                  "Install/configure start agent script failed!")


def create_server_script(server_host_name):
  """
  Creating server.sh script in the same dir where current script is located
  server.sh script will install, configure and start ambari-server and ambari-agent on host
  :param server_host_name: Server host name
  """

  # ambari-server setup <options> may not work property, so doing several calls like
  # echo "arg=value" >> .../ambari.properties

  contents = "#!/bin/bash\n" + \
  "yum install wget -y\n" + \
  "wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url) + \
  "yum clean all; yum install git ambari-server -y\n" + \
  "mkdir /home ; cd /home ; git clone https://github.com/apache/ambari.git ; cd ambari ; git checkout branch-2.5\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-server/resources/stacks/PERF\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n" + \
  "sed -i -f /home/ambari/ambari-server/src/main/resources/stacks/PERF/install_packages.sed /var/lib/ambari-server/resources/custom_actions/scripts/install_packages.py\n" + \
  "sed -i -f /home/ambari/ambari-server/src/main/resources/stacks/PERF/install_packages.sed /var/lib/ambari-agent/cache/custom_actions/scripts/install_packages.py\n" + \
  "\n" + \
  "\n" + \
  "cd /; wget http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.40/mysql-connector-java-5.1.40.jar;\n" + \
  "mkdir /usr/share/java; chmod 777 /usr/share/java;" + \
  "cp mysql-connector-java-5.1.40.jar /usr/share/java/; chmod 777 /usr/share/java/mysql-connector-java-5.1.40.jar;\n" + \
  "ln -s /usr/share/java/mysql-connector-java-5.1.40.jar /usr/share/java/mysql-connector-java.jar;\n" + \
  "cd /etc/yum.repos.d/; wget http://repo.mysql.com/mysql-community-release-el6-5.noarch.rpm; rpm -ivh mysql-community-release-el6-5.noarch.rpm;" + \
  "yum clean all; yum install mysql-server -y\n" + \
  "sed -i -e 's/mysqld]/mysqld]\\nmax_allowed_packet=1024M\\njoin_buffer_size=512M\\nsort_buffer_size=128M\\nread_rnd_buffer_size=128M\\ninnodb_buffer_pool_size=16G" \
  "\\ninnodb_file_io_threads=16\\ninnodb_thread_concurrency=32\\nkey_buffer_size=16G\\nquery_cache_limit=16M\\nquery_cache_size=512M\\nthread_cache_size=128\\ninnodb_log_buffer_size=512M/1' /etc/my.cnf\n" + \
  "service mysqld start\n" + \
  "mysql -uroot -e \"CREATE DATABASE ambari;\"\n" + \
  "mysql -uroot -e \"SOURCE /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql;\" ambari\n" + \
  "mysql -uroot -e \"CREATE USER 'ambari'@'%' IDENTIFIED BY 'bigdata';\"\n" + \
  "mysql -uroot -e \"GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'%%';\"\n" + \
  "mysql -uroot -e \"CREATE USER 'ambari'@'localhost' IDENTIFIED BY 'bigdata';\"\n" + \
  "mysql -uroot -e \"GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'localhost';\"\n" + \
  "mysql -uroot -e \"CREATE USER 'ambari'@'{0}' IDENTIFIED BY 'bigdata';\"\n".format(server_host_name) + \
  "mysql -uroot -e \"GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'{0}';\"\n".format(server_host_name) + \
  "mysql -uroot -e \"FLUSH PRIVILEGES;\"\n" + \
  "\n" + \
  "\n" + \
  "ambari-server setup -s\n" + \
  "ambari-server setup --database mysql --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar --databasehost=localhost --databaseport=3306 --databasename=ambari --databaseusername=ambari --databasepassword=bigdata\n" + \
  "sed -i -e 's/=postgres/=mysql/g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/server.persistence.type=local/server.persistence.type=remote/g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/local.database.user=postgres//g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/server.jdbc.postgres.schema=ambari//g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/agent.threadpool.size.max=25/agent.threadpool.size.max=100/g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/client.threadpool.size.max=25/client.threadpool.size.max=65/g' /etc/ambari-server/conf/ambari.properties\n" + \
  "sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/1.0/metainfo.xml\n" + \
  "sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/2.0/metainfo.xml\n" + \
  "sed -i -e 's/-Xmx2048m/-Xmx16384m/g' /var/lib/ambari-server/ambari-env.sh\n" + \
  "\n" + \
  "echo 'server.jdbc.driver=com.mysql.jdbc.Driver' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'server.jdbc.rca.url=jdbc:mysql://{0}:3306/ambari' >> /etc/ambari-server/conf/ambari.properties\n".format(server_host_name) + \
  "echo 'server.jdbc.rca.driver=com.mysql.jdbc.Driver' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'server.jdbc.url=jdbc:mysql://{0}:3306/ambari' >> /etc/ambari-server/conf/ambari.properties\n".format(server_host_name) + \
  "echo 'server.jdbc.port=3306' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'server.jdbc.hostname=localhost' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'server.jdbc.driver.path=/usr/share/java/mysql-connector-java.jar' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'alerts.cache.enabled=true' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'alerts.cache.size=100000' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'alerts.execution.scheduler.maxThreads=4' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'security.temporary.keystore.retention.minutes=180' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "echo 'stack.hooks.folder=stacks/PERF/1.0/hooks' >> /etc/ambari-server/conf/ambari.properties\n" + \
  "\n" + \
  "ambari-server start --skip-database-check\n" + \
  "exit 0"

  with open("server.sh", "w") as f:
    f.write(contents)


def create_agent_script(server_host_name):
  """
  Creating agent.sh script in the same dir where current script is located
  agent.sh script will install, configure and start ambari-agent on host
  :param server_host_name: Server host name
  """

  # TODO, instead of cloning Ambari repo on each VM, do it on the server once and distribute to all of the agents.
  contents = "#!/bin/bash\n" + \
  "yum install wget -y\n" + \
  "wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url) + \
  "yum clean all; yum install krb5-workstation git ambari-agent -y\n" + \
  "mkdir /home ; cd /home; git clone https://github.com/apache/ambari.git ; cd ambari ; git checkout branch-2.5\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n" + \
  "sed -i -f /var/lib/ambari-agent/cache/stacks/PERF/PythonExecutor.sed /usr/lib/ambari-agent/lib/ambari_agent/PythonExecutor.py\n" + \
  "sed -i -f /var/lib/ambari-agent/cache/stacks/PERF/check_host.sed /var/lib/ambari-agent/cache/custom_actions/scripts/check_host.py\n" + \
  "sed -i -e 's/hostname=localhost/hostname={0}/g' /etc/ambari-agent/conf/ambari-agent.ini\n".format(server_host_name) + \
  "sed -i -e 's/agent]/agent]\\nhostname_script={0}\\npublic_hostname_script={1}\\n/1' /etc/ambari-agent/conf/ambari-agent.ini\n".format(hostname_script, public_hostname_script) + \
  "python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start\n" + \
  "exit 0"

  with open("agent.sh", "w") as f:
    f.write(contents)


def execute_command(args, ip, cmd, fail_message, custom_option='', login='root'):
  """
  Method to execute ssh commands via SSH class
  :param args: Command line args
  :param ip: IP to ssh to
  :param cmd: Command to execute
  :param fail_message: In case of an error, what to report
  :param custom_option: Custom flags
  :param login: Login user
  :return: Return execute log message
  """
  ssh = SSH(login, args.key, ip, cmd, custom_option, fail_message)
  ssh_result = ssh.run()
  status_code = ssh_result["exitstatus"]
  if status_code != 0:
    raise Exception(ssh_result["errormsg"])

  return ssh_result["log"][0]


def put_file(args, ip, local_file, remote_file, fail_message, login='root'):
  """
  Method to copy file from local to remote host via SCP class
  :param args: Command line args
  :param ip: IP to ssh to
  :param local_file: Path to local file
  :param remote_file: Path to remote file
  :param fail_message: In case of an error, what to report
  :param login: Login user.
  :return: Return copy log message
  """
  scp = SCP(login, args.key, ip, local_file,
            remote_file, fail_message)
  scp_result = scp.run()
  status_code = scp_result["exitstatus"]
  if status_code != 0:
    raise Exception(scp_result["errormsg"])

  return scp_result["log"][0]


def get_vms_list(args):
  """
  Get tuple of (x, y) where 
  x = dictionary from single server host name to ip
  y = dictionary from multiple agent host names to ip
  :param args: Command line arguments
  :return: Tuple of dictionaries of hostnames and ip for server and agents.
  """
  # Get the server.
  server = __get_vms_list_from_name(args, "{0}-server-{1}".format(cluster_prefix, args.cluster_suffix))

  # Get the agents
  agents = __get_vms_list_from_name(args, "{0}-agent-{1}".format(cluster_prefix, args.cluster_suffix))

  return (server, agents)

def __get_vms_list_from_name(args, cluster_name):
  """
  Method to parse "gce info {cluster-name}" command output and get hosts and ips pairs for every host in cluster
  :param args: Command line args
  :return: Mapping of VM host name to ip.
  """
  gce_fqdb_cmd = '/opt/gce-utils/gce info {0}'.format(cluster_name)
  out = execute_command(args, args.controller, gce_fqdb_cmd, "Failed to get VMs list!", "-tt")
  lines = out.split('\n')
  #print "LINES=" + str(lines)
  if lines[0].startswith("Using profile") and not lines[1].strip():
    result = {}
    for s in lines[4:]:  # Ignore non-meaningful lines
      if not s:
        continue

      match = re.match(r'^ [^ ]+ ([\w\.-]*)\s+([\d\.]*).*$', s, re.M)
      if match:
        result[match.group(1)] = match.group(2)
      else:
        raise Exception('Cannot parse "{0}"'.format(s))
    return result
  else:
    raise Exception('Cannot parse "{0}"'.format(lines))


def sort_hosts(hosts):
  """
  Sort the hosts by name and take into account the numbers.
  :param hosts: Dictionary from host name (e.g., perf-9-test, perf-62-test), to the IP
  :return: Sorted list of tuples
  """
  host_names = hosts.keys()
  sorted_host_tuples = [(None, None),] * len(hosts)

  pattern = re.compile(".*?-agent-.*?(\d+)")
  for host_name in host_names:
    m = pattern.match(host_name)
    if m and len(m.groups()) == 1:
      number = int(m.group(1))
      ip = hosts[host_name]
      sorted_host_tuples[number - 1] = (host_name, ip)

  return sorted_host_tuples


def pretty_print_vms(vms):
  """
  Pretty print the VMs hostnames
  :param vms: List of tuples (hostname, ip)
  """
  print "=========================="
  print "Hostnames of nodes in cluster:"
  for (hostname, ip) in vms:
    print hostname
  print "==========================\n"


if __name__ == "__main__":
  main()


