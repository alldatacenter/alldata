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

import socket
from ambari_commons import subprocess32
import urllib2
import logging
import traceback
import sys

logger = logging.getLogger(__name__)

cached_hostname = None
cached_public_hostname = None
cached_server_hostnames = []

def arrayFromCsvString(str):
  CSV_DELIMITER = ','

  result_array = []
  items = str.lower().split(CSV_DELIMITER)

  for item in items:
    result_array.append(item.strip())
  return result_array

def hostname(config):
  global cached_hostname
  if cached_hostname is not None:
    return cached_hostname

  try:
    scriptname = config.get('agent', 'hostname_script')
    try:
      osStat = subprocess32.Popen([scriptname], stdout=subprocess32.PIPE, stderr=subprocess32.PIPE)
      out, err = osStat.communicate()
      if (0 == osStat.returncode and 0 != len(out.strip())):
        cached_hostname = out.strip()
        logger.info("Read hostname '{0}' using agent:hostname_script '{1}'".format(cached_hostname, scriptname))
      else:
        logger.warn("Execution of '{0}' failed with exit code {1}. err='{2}'\nout='{3}'".format(scriptname, osStat.returncode, err.strip(), out.strip()))
        cached_hostname = socket.getfqdn()
        logger.info("Read hostname '{0}' using socket.getfqdn() as '{1}' failed".format(cached_hostname, scriptname))
    except:
      cached_hostname = socket.getfqdn()
      logger.warn("Unexpected error while retrieving hostname: '{0}', defaulting to socket.getfqdn()".format(sys.exc_info()))
      logger.info("Read hostname '{0}' using socket.getfqdn().".format(cached_hostname))
  except:
    cached_hostname = socket.getfqdn()
    logger.info("agent:hostname_script configuration not defined thus read hostname '{0}' using socket.getfqdn().".format(cached_hostname))

  cached_hostname = cached_hostname.lower()
  return cached_hostname


def public_hostname(config):
  global cached_public_hostname
  if cached_public_hostname is not None:
    return cached_public_hostname

  out = ''
  err = ''
  try:
    if config.has_option('agent', 'public_hostname_script'):
      scriptname = config.get('agent', 'public_hostname_script')
      output = subprocess32.Popen(scriptname, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE, shell=True)
      out, err = output.communicate()
      if (0 == output.returncode and 0 != len(out.strip())):
        cached_public_hostname = out.strip().lower()
        logger.info("Read public hostname '" + cached_public_hostname + "' using agent:public_hostname_script")
        return cached_public_hostname
      else:
        logger.warn("Execution of '{0}' returned {1}. {2}\n{3}".format(scriptname, output.returncode, err.strip(), out.strip()))
  except:
    #ignore for now.
    trace_info = traceback.format_exc()
    logger.info("Error using the scriptname:" +  trace_info
                + " :out " + out + " :err " + err)
    logger.info("Defaulting to fqdn.")

  try:
    handle = urllib2.urlopen('http://169.254.169.254/latest/meta-data/public-hostname', '', 2)
    str = handle.read()
    handle.close()
    cached_public_hostname = str.lower()
    logger.info("Read public hostname '" + cached_public_hostname + "' from http://169.254.169.254/latest/meta-data/public-hostname")
  except:
    cached_public_hostname = socket.getfqdn().lower()
    logger.info("Read public hostname '" + cached_public_hostname + "' using socket.getfqdn()")
  return cached_public_hostname

def server_hostnames(config):
  """
  Reads the ambari server name from the config or using the supplied script
  """
  global cached_server_hostnames
  if cached_server_hostnames != []:
    return cached_server_hostnames

  if config.has_option('server', 'hostname_script'):
    scriptname = config.get('server', 'hostname_script')
    try:
      osStat = subprocess32.Popen([scriptname], stdout=subprocess32.PIPE, stderr=subprocess32.PIPE)
      out, err = osStat.communicate()
      if (0 == osStat.returncode and 0 != len(out.strip())):
        cached_server_hostnames = arrayFromCsvString(out)
        logger.info("Read server hostname '" + cached_server_hostnames + "' using server:hostname_script")
    except Exception, err:
      logger.info("Unable to execute hostname_script for server hostname. " + str(err))

  if not cached_server_hostnames:
    cached_server_hostnames  = arrayFromCsvString(config.get('server', 'hostname'))
  return cached_server_hostnames


def main(argv=None):
  print hostname()
  print public_hostname()
  print server_hostname()

if __name__ == '__main__':
  main()
