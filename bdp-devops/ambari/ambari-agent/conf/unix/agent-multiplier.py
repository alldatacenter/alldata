# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import sys
import os
import re
from ambari_commons import subprocess32
import shutil
from optparse import OptionParser


class Host:
  """
  Abstraction of the elements unique to each Ambari Agent running on this VM.
  """
  def __init__(self, host_name, ping_port, home_dir):
    self.host_name = host_name
    self.ping_port = ping_port
    self.home_dir = home_dir


class Multiplier:
  """
  In order to perform scale testing, this class bootstraps multiple Ambari Agents to run on the same host.
  Each Ambari Agent has its own home directory with subdirectories for configs, logs, etc.
  Further, each agent is given a unique port number.

  Usage: python agent-multiplier.py [command]
  [command] = start | stop | restart | status

  Optional flags:
  -v --verbose : Increase logging

  """
  CONFIG_FILE = "/etc/ambari-agent/conf/agent-multiplier.conf"

  def __init__(self, args):
    parser = OptionParser()
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False,
                      help="Verbose logging")
    (options, args) = parser.parse_args(args)
    self.verbose = options.verbose

    self.home_dir = "/home/"
    # Subdirectories to create inside the home dir for the given agent.
    self.log_dir = "/var/log/ambari-agent"
    self.config_dir = "/etc/ambari-agent/conf"
    self.pid_file = "/var/run/ambari-agent/ambari-agent.pid"
    self.prefix_dir = "/var/lib/ambari-agent/data"
    self.cache_dir = "/var/lib/ambari-agent/cache"

    # Ambari Agent config file to use as a template
    # Will change hostname and port after copying
    self.source_config_file = "/etc/ambari-agent/conf/ambari-agent.ini"
    self.source_version_file = "/var/lib/ambari-agent/data/version"
    self.base_ping_port = 5000

    self.start = 0
    self.num = 0
    self.prefix = None

    # Parse above params from config file, which must exist
    self.parse_configs()

    if len(args) != 2:
      print "Sample Usage: python agent_multiplier.py [action]\n" \
            "actions: start|stop|restart|status"
    self.command = args[1]

    # Validate configs
    self.validate()

    print "*** Params ***"
    print "Start: %d" % self.start
    print "Num: %d" % self.num
    print "Prefix: %s" % self.prefix
    print "Command: %s" % self.command

    # All hostnames that will be managed by Ambari Agents on this host
    self.hosts = []
    for i in range(self.start, self.start + self.num):
      host_name = "%s-%04d" % (self.prefix, i)
      host_home_dir = os.path.join(self.home_dir, host_name)
      host = Host(host_name, self.base_ping_port + i, host_home_dir)
      self.hosts.append(host)

    self.bootstrap()

  def parse_configs(self):
    """
    Parse the configuration file to set the config params.
    """
    if not os.path.exists(self.CONFIG_FILE):
      print "Did not find Agent Multiplier config file: %s" % str(self.CONFIG_FILE)
      sys.exit(-1)

    params = {}
    with open(self.CONFIG_FILE, "r") as f:
      for line in f.readlines():
        index = line.index("=") if "=" in line else None
        if index is not None:
          config = line[0:index].strip()
          value = line[index+1:].strip()
          params[config] = value

    # Convert some configs to ints
    if "start" in params:
      self.start = int(params["start"])

    if "num" in params:
      self.num = int(params["num"])

    if "prefix" in params:
      self.prefix = params["prefix"].strip().lower()

  def validate(self):
    """
    Validate the configs are non-empty and contain correct ranges.
    On error, will exit with code -1
    """
    errors = []
    if self.start <= 0:
      errors.append("Start must be a positive integer")
    if self.num <= 0:
      errors.append("Number of agents on this host must be a positive integer")
    if self.prefix is None or self.prefix.strip() == "":
      errors.append("Prefix is a required field")
    
    if not os.path.isfile(self.source_config_file):
      errors.append("Ambari Agent config file does not exist at %s" % self.source_config_file)

    valid_commands = set(["start", "stop", "restart", "status"])
    if self.command is None or self.command not in valid_commands:
      errors.append("Command must be one of %s" % ", ".join(valid_commands))

    if len(errors) > 0:
      print "Error:"
      print "\n".join(errors)
      sys.exit(-1)

  def bootstrap(self):
    """
    Bootstrap each Ambari Agent that will run on this host with the directories it needs and configuration file.
    """
    for host in self.hosts:
      host_name = host.host_name
      host_home_dir = host.home_dir
      host_log_dir = host_home_dir + self.log_dir
      host_config_dir = host_home_dir + self.config_dir
      host_pid_file = host_home_dir + self.pid_file
      host_pid_dir = os.path.dirname(host_pid_file)
      host_prefix = host_home_dir + self.prefix_dir
      host_cache_dir = host_home_dir + self.cache_dir

      if self.verbose:
        print "Analyzing host %s with port %d" % (host_name, host.ping_port)

      for dir in [host_home_dir, host_log_dir, host_config_dir, host_pid_dir, host_prefix, host_cache_dir]:
        if not os.path.isdir(dir):
          print "Creating dir %s" % (dir)
          os.makedirs(dir)

      # Copy config file
      host_config_file = os.path.join(host_config_dir, "ambari-agent.ini")
      if not os.path.isfile(host_config_file):
        print "Copying config file %s" % str(host_config_file)
        shutil.copyfile(self.source_config_file, host_config_file)

      # Copy version file
      version_file = os.path.join(host_prefix, "version")
      if not os.path.isfile(version_file):
        print "Copying version file %s" % str(version_file)
        shutil.copyfile(self.source_version_file, version_file)

      # Copy cache dir content
      if not os.path.isdir(os.path.join(host_cache_dir, "stacks")):
        print "Copying cache directory content %s" % str(host_cache_dir)
        self.copytree(self.cache_dir, host_cache_dir)

      # Create hostname.sh script to use custom FQDN for each agent.
      host_name_script = os.path.join(host_config_dir, "hostname.sh")
      self.create_host_name_script(host_name, host_name_script)

      # Overwrite the port and hostname
      config_dict = {"ping_port": host.ping_port,
                     "hostname_script": host_name_script,
                     "public_hostname_script": host_name_script,
                     "logdir": host_log_dir,
                     "piddir": host_pid_dir,
                     "prefix": host_prefix,
                     "cache_dir": host_cache_dir}
      self.change_config(host_config_file, config_dict)

      # Change /etc/hosts file by appending each hostname.
      self.modify_etc_hosts_file()

  def copytree(self, src, dst, symlinks=False, ignore=None):
    for item in os.listdir(src):
      s = os.path.join(src, item)
      d = os.path.join(dst, item)
      if os.path.isdir(s):
        shutil.copytree(s, d, symlinks, ignore)
      else:
        shutil.copy2(s, d)

  def create_host_name_script(self, host_name, host_name_script):
    """
    Creates a shell script that will echo the given hostname.
    :param host_name: Host name to echo
    :param host_name_script: Location to save the scrip to
    """
    template = "#!/bin/sh\n" \
               "echo HOSTNAME"
    with open(str(host_name_script), "w+") as f:
      f.writelines(template.replace("HOSTNAME", host_name))
    subprocess32.call("chmod +x %s" % host_name_script, shell=True)

  def change_config(self, config_file, config_dict):
    """
    Change existing configs. Will not append new configs.
    :param config_file: Config file to modify
    :param config_dict: Dictionary of config,value pairs to change.
    """
    # TODO, allow appending configs to [AGENT] section.

    if not os.path.exists(config_file):
      print "ERROR. Did not file config file: %s" % config_file
      return

    lines = []
    with open(config_file, "r") as f:
      lines = f.readlines()

    new_lines = []

    configs_found = set()
    configs_changed = set()
    for line in lines:
      for config, value in config_dict.iteritems():
        p = re.compile(config + "\s?=")
        if p.match(line):
          configs_found.add(config)
          new_value = config + "=" + str(value) + "\n"
          if line != new_value:
            line = new_value
            configs_changed.add(config)
          continue
      # Config didn't change value
      new_lines.append(line)

    # TODO, if can append configs, then this is not needed.
    if len(configs_found) < len(config_dict.keys()):
      missing_configs = set(config_dict.keys()) - configs_found
      print "ERROR: Did not find all required configs. Missing: %s" % ", ".join(missing_configs)
      sys.exit(-1)

    if len(configs_changed) > 0:
      print "Making changes to file %s" % config_file
      with open(config_file, "w") as f:
        f.writelines(new_lines)

  def modify_etc_hosts_file(self):
    """
    Modify this host's /etc/hosts file by changing the line for localhost with synonyms for all of the other
    fake host names that will be generated for the Ambari Agents.
    """
    etc_hosts = "/etc/hosts"
    if not os.path.isfile(etc_hosts):
      print "ERROR. Did not find file %s" % etc_hosts
      return

    lines = []
    with open(etc_hosts, "r") as f:
      lines = f.readlines()

    # Value to search for when using Vagrant VMs
    localhost_line_start = "127.0.0.1 localhost localhost.localdomain localhost4 localhost4.localdomain4 "
    new_lines = []
    line_changed = False
    for line in lines:
      if line.startswith("127.0.0.1"):
        new_change = localhost_line_start + " ".join([x.host_name for x in self.hosts]) + "\n"
        if line != new_change:
          line = new_change
          line_changed = True
      new_lines.append(line)

    if line_changed:
      print "Making changes to %s" % etc_hosts
      with open(etc_hosts, "w") as f:
        f.writelines(new_lines)

  def run(self):
    """
    Run one of the supported commands: start, stop, restart, and status
    """
    if self.command == "start":
      self.cmd_start()
    elif self.command == "stop":
      self.cmd_stop()
    elif self.command == "restart":
      self.cmd_restart()
    elif self.command == "status":
      self.cmd_status()

  def cmd_start(self):
    print "Starting %d host(s)" % len(self.hosts)
    for host in self.hosts:
      cmd = "ambari-agent start --home %s" % (host.home_dir)
      os.environ['AMBARI_AGENT_CONF_DIR'] = os.path.join(host.home_dir, "etc/ambari-agent/conf")
      subprocess32.call(cmd, shell=True, env=os.environ)

  def cmd_stop(self):
    print "Stopping %d host(s)" % len(self.hosts)
    for host in self.hosts:
      cmd = "ambari-agent stop --home %s" % (host.home_dir)
      os.environ['AMBARI_AGENT_CONF_DIR'] = os.path.join(host.home_dir, "etc/ambari-agent/conf")
      subprocess32.call(cmd, shell=True, env=os.environ)

  def cmd_restart(self):
    print "Restarting %d host(s)" % len(self.hosts)
    for host in self.hosts:
      cmd = "ambari-agent restart --home %s" % (host.home_dir)
      os.environ['AMBARI_AGENT_CONF_DIR'] = os.path.join(host.home_dir, "etc/ambari-agent/conf")
      subprocess32.call(cmd, shell=True, env=os.environ)

  def cmd_status(self):
    print "Summary of Agent Status:"
    print "Total agents: %d\n" % len(self.hosts)
    (running_hosts, unknown_hosts, stopped_hosts) = self.aggregate_status()

    print "Running agents: %d" % len(running_hosts)
    if self.verbose and len(running_hosts):
      print "(%s)\n" % (", ".join(running_hosts))

    print "Unknown agents: %d" % len(unknown_hosts)
    if self.verbose and len(unknown_hosts):
      print "(%s)\n" % (", ".join(unknown_hosts))

    print "Stopped agents: %d" % len(stopped_hosts)
    if self.verbose and len(stopped_hosts):
      print "(%s)\n" % (", ".join(stopped_hosts))

  def aggregate_status(self):
    """
    Aggregate the status of all of the hosts.
    :return: Return a 3-tuple of (list of x, list of y, list of z)
    x = hosts running with a valid pid
    y = hosts with a pid file but process is not running
    z = hosts without a pid file
    """
    running_hosts = []
    unknown_hosts = []
    stopped_hosts = []
    for host in self.hosts:
      pid_file = os.path.join(self.home_dir, host.host_name, self.pid_file.lstrip("/"))
      if os.path.isfile(pid_file):
        pid = None

        with open(pid_file, "r") as f:
          try:
            line = f.readline()
            pid = int(line.strip())
          except:
            pass

        is_running = Multiplier.check_pid(pid)
        if is_running:
          running_hosts.append(host.host_name)
        else:
          unknown_hosts.append(host.host_name)
      else:
        stopped_hosts.append(host.host_name)

    return (running_hosts, unknown_hosts, stopped_hosts)

  @classmethod
  def check_pid(cls, pid):
    """ Check For the existence of a unix pid. """
    try:
      os.kill(pid, 0)
    except OSError:
      return False
    else:
      return True

if __name__ == "__main__":
  m = Multiplier(sys.argv)
  m.run()