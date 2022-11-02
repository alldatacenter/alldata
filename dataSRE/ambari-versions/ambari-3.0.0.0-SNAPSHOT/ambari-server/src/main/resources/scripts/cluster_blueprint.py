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

#
# Main.
#
import sys
import optparse
import getpass
import logging
import urllib2
import re
import json
import base64
import os

SILENT = False
ACTION = None
PROTOCOL = "http"
HOSTNAME = "localhost"
PORT = "8080"
USERNAME = "admin"
PASSWORD = "admin"
INLINE_ARGUMENTS = None
logger = logging.getLogger()

DEFAULT_MULTINODE_JSON = "multinode-default.json"
DEFAULT_SINGLENODE_JSON = "singlenode-default.json"

SLAVES_REPLACE_EXPR = "${slavesCount}"

BLUEPRINT_CREATE_URL = "/api/v1/blueprints/{0}"
BLUEPRINT_CLUSTER_CREATE_URL = "/api/v1/clusters/{0}"
BLUEPRINT_FETCH_URL = "/api/v1/clusters/{0}?format=blueprint"

def getUrl(partial_url):
  return PROTOCOL + "://" + HOSTNAME + ":" + PORT + partial_url

def get_validated_string_input(prompt, default, pattern, description,
                               is_pass, allowEmpty=True, validatorFunction=None):
  input = ""
  while not input:
    if SILENT:
      print (prompt)
      input = default
    elif is_pass:
      input = getpass.getpass(prompt)
    else:
      input = raw_input(prompt)
    if not input.strip():
      # Empty input - if default available use default
      if not allowEmpty and not default:
        print 'Property cannot be blank.'
        input = ""
        continue
      else:
        input = default
        if validatorFunction:
          if not validatorFunction(input):
            input = ""
            continue
        break  # done here and picking up default
    else:
      if not pattern == None and not re.search(pattern, input.strip()):
        print description
        input = ""

      if validatorFunction:
        if not validatorFunction(input):
          input = ""
          continue
  return input

def get_server_info(silent=False):
  if not silent:
    host = get_validated_string_input("Server Host (localhost):", "localhost", ".*", "", True)
    port = get_validated_string_input("Server Port (8080):", "8080", ".*", "", True)
    protocol = get_validated_string_input("Protocol (http):", "http", ".*", "", True)
    user = get_validated_string_input("User (admin):", "admin", ".*", "", True)
    password = get_validated_string_input("Password (admin):", "admin", ".*", "", True)

    global HOSTNAME
    HOSTNAME = host

    global PORT
    PORT = port

    global PROTOCOL
    PROTOCOL = protocol

    global USERNAME
    USERNAME = user

    global PASSWORD
    PASSWORD = password

  pass


class PreemptiveBasicAuthHandler(urllib2.BaseHandler):

  def __init__(self):
    password_mgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
    password_mgr.add_password(None, getUrl(''), USERNAME, PASSWORD)
    self.passwd = password_mgr
    self.add_password = self.passwd.add_password

  def http_request(self, req):
    uri = req.get_full_url()
    user = USERNAME
    pw = PASSWORD
    raw = "%s:%s" % (user, pw)
    auth = 'Basic %s' % base64.b64encode(raw).strip()
    req.add_unredirected_header('Authorization', auth)
    return req

class AmbariBlueprint:

  def __init__(self):
    handler = PreemptiveBasicAuthHandler()
    opener = urllib2.build_opener(handler)
    # Install opener for all requests
    urllib2.install_opener(opener)
    self.urlOpener = opener

  def importBlueprint(self, blueprintLocation, hostsLocation, clusterName):
    get_server_info(SILENT)
    isDefaultJson = False

    if os.path.split(blueprintLocation)[1].find(DEFAULT_MULTINODE_JSON) != -1:
      isDefaultJson = True
    pass

    with open(blueprintLocation, "r") as file:
      blueprint = file.read()

    # Verify json data
    blueprint_json = json.loads(blueprint)
    logger.debug("blueprint json: %s" % blueprint_json)

    blueprintInfo = blueprint_json.get("Blueprints")
    if not blueprintInfo:
      raise Exception("Cannot read blueprint info from blueprint at %s" % blueprintLocation)

    blueprint_name = blueprintInfo.get("blueprint_name")
    if not blueprint_name:
      raise Exception("blueprint_name required inside Blueprints %s" % blueprintInfo)

    hosts_json = None

    # Find number of slaves and set correct cardinality in default json
    if isDefaultJson and INLINE_ARGUMENTS:
      (masters, slaves, gateway) = self.parseHostData()
      expectedMasterCount = self.parseDefaultJsonData(blueprint_json, slaves.split(","))
      if expectedMasterCount != len(masters.split(",")):
        logger.info("Mismatch in cardinality. Inferring host assignment for masters...")
      pass
      hosts_json = self.buildHostAssignments(blueprint_name, blueprint_json,
                                             masters.split(","),
                                             slaves.split(","),
                                             gateway)
    pass

    # Parse user provided hostData and build host json
    if not isDefaultJson and INLINE_ARGUMENTS:
      raise Exception("Unsupported operation, please provide explict host "
                      "assignments with -o option.")
    pass

    # Read from file if available
    if not hosts_json and hostsLocation:
      with open(hostsLocation, 'r') as file:
        hosts_json = file.read()
        # Verify json data
        hostAssignments = json.loads(hosts_json)
      pass
    pass

    logger.debug("host assignments json: %s" % hosts_json)

    # Create blueprint
    blueprintCreateUrl = getUrl(BLUEPRINT_CREATE_URL.format(blueprint_name))

    retCode = self.performPostOperation(blueprintCreateUrl, blueprint)
    if retCode == "201":
      logger.info("Blueprint created successfully.")
    elif retCode == "409":
      logger.info("Blueprint %s already exists, proceeding with host "
                  "assignments." % blueprint_name)
    else:
      logger.error("Unable to create blueprint from location %s" % blueprintLocation)
      sys.exit(1)
    pass

    # Create cluster
    clusterCreateUrl = getUrl(BLUEPRINT_CLUSTER_CREATE_URL.format(clusterName))
    retCode = self.performPostOperation(clusterCreateUrl, hosts_json)

    if retCode == "202":
      logger.info("Host assignments successful.")
    else:
      logger.error("Error assigning hosts to hostgroups. Please check server logs.")
      sys.exit(1)
    pass


  def buildHostAssignments(self, blueprintName, blueprintJson, masters,
                           slaves, gateway = None):
    hostAssignments = '{{"blueprint":"{0}","host_groups":[{1}]}}'
    hostGroupHosts = '{{"name":"{0}","hosts":[{1}]}}'
    hosts = '{{"fqdn":"{0}"}},'
    logger.debug("Blueprint: {0}, Masters: {1}, Slaves: {2}".format(blueprintName, masters, slaves))
    mastersUsed = 0
    slavesUsed = 0
    hostGroupsJson = ''
    hostGroups = blueprintJson.get("host_groups")
    for hostGroup in hostGroups:
      if hostGroup.get("name").find("master") != -1:
        masterHosts = ''
        cardinality = int(hostGroup.get("cardinality"))
        hostList = self.getHostListMatchingCardinality(cardinality, masters, mastersUsed)
        mastersUsed = len(hostList)
        for host in hostList:
          masterHosts += hosts.format(host.strip())
        pass
        masterHosts = masterHosts.rstrip(",")
        masterHostsGroup = hostGroupHosts.format(hostGroup.get("name"), masterHosts)
        hostGroupsJson += masterHostsGroup + ","
      pass
      if hostGroup.get("name").find("slave") != -1:
        slaveHosts = ''
        cardinality = int(hostGroup.get("cardinality"))
        hostList = self.getHostListMatchingCardinality(cardinality, slaves, slavesUsed)
        slavesUsed = len(hostList)
        for host in hostList:
          slaveHosts += hosts.format(host.strip())
        pass
        slaveHosts = slaveHosts.rstrip(",")
        slaveHostsGroup = hostGroupHosts.format(hostGroup.get("name"), slaveHosts)
        hostGroupsJson += slaveHostsGroup + ","
      pass
      if hostGroup.get("name").find("gateway") != -1:
        gatewayHosts = ''
        cardinality = int(hostGroup.get("cardinality"))
        if gateway:
          hostList = [gateway]
        else:
          hostList = self.getHostListMatchingCardinality(cardinality, masters, mastersUsed)
          mastersUsed = len(hostList)
        pass
        gatewayHosts += hosts.format(hostList[0].strip())
        gatewayHostGroup = hostGroupHosts.format(hostGroup.get("name"), gatewayHosts)
        hostGroupsJson += gatewayHostGroup + ","
      pass
    pass

    hostGroupsJson = hostGroupsJson.rstrip(",") if hostGroupsJson.endswith(",") else hostGroupsJson

    return hostAssignments.format(blueprintName, hostGroupsJson)
  pass

  def getHostListMatchingCardinality(self, cardinality, hostList, usedCount):
    if cardinality == len(hostList):
      return hostList
    if cardinality < len(hostList):
      unUsedHosts = hostList[usedCount:len(hostList)]
      if unUsedHosts:
        if cardinality == len(unUsedHosts):
          return unUsedHosts
        elif cardinality < len(unUsedHosts):
          return unUsedHosts[0:cardinality]
        else:
          usedHosts = hostList[0:usedCount]
          for i in range(cardinality-len(unUsedHosts), cardinality):
            unUsedHosts += usedHosts[i]
          pass
          return unUsedHosts
        pass
      else:
        return hostList[0:cardinality]
    else:
      raise Exception("Not enough hosts provided.")

  # Process inline arguments and return json
  def parseHostData(self):
    hostData = INLINE_ARGUMENTS.split("&")
    masters = None
    slaves = None
    gateway = None
    if hostData:
      for item in hostData:
        data = item.split("=")
        if data and len(data) > 0:
          if data[0] == "masters":
            masters = data[1]
          elif data[0] == "slaves":
            slaves = data[1]
          elif data[0] == "gateway":
            gateway = data[1]
        pass
      pass
    if masters and slaves:
      return (masters, slaves, gateway)
    else:
      raise Exception("Master and Slave assignments required for a multi-node cluster.")

  def parseDefaultJsonData(self, json_data, slaves):
    hostGroups = json_data.get("host_groups")
    mastersCount = 0
    if hostGroups:
      for hostGroup in hostGroups:
        hostGroupName = hostGroup.get("name")
        if hostGroupName:
          if hostGroupName.find("slave") != -1:
            cardinality = hostGroup.get("cardinality")
            if cardinality and cardinality.find(SLAVES_REPLACE_EXPR) != -1:
              json_data["host_groups"]["slave"]["cardinality"] = len(slaves)
          elif hostGroupName.find("master") != -1:
            mastersCount += 1
          pass
        pass
      pass
    pass

    return mastersCount

  def exportBlueprint(self, clusterName, exportFilePath):
    get_server_info(SILENT)
    blueprintFetchUrl = getUrl(BLUEPRINT_FETCH_URL.format(clusterName))
    resp = self.performGetOperation(blueprintFetchUrl)

    if resp:
      if exportFilePath:
        with open(exportFilePath, "w") as file:
          file.write(resp)
        pass
      else:
        logger.info("Response from server:")
        logger.info(resp)
      pass
    else:
      logger.error("Unable to perform export operation on cluster, %s" % clusterName)

    pass


  def performPostOperation(self, url, data):
    req = urllib2.Request(url, data)
    req.add_header("X-Requested-By", "ambari_scripts")
    req.get_method = lambda: 'POST'

    try:
      logger.info("POST request: %s" % req.get_full_url())
      logger.debug("Payload: %s " % data)
      resp = self.urlOpener.open(req)
      if resp:
        logger.info("Create response: %s" % resp.getcode())
        retCode = str(resp.getcode()).strip()
        if retCode == "201" or retCode == "202":
          urlResp = resp.read()
          logger.info("Response data: %s" % str(urlResp))
          return retCode
        pass
      pass
    except urllib2.HTTPError, e:
      logger.error("POST request failed.")
      logger.error('HTTPError : %s' % e.read())
      if e.code == 409:
        return '409'
      pass
    except Exception, e:
      logger.error("POST request failed.")
      logger.error(e)
      if 'HTTP Error 409' in str(e):
        return '409'
      pass

    return '-1'

    pass

  def performGetOperation(self, url):
    data = None
    try:
      resp = self.urlOpener.open(url)
      if resp:
        resp = resp.read()
        data = json.loads(resp)
      else:
        logger.error("Unable to get response from server, url = %s" % url)
    except:
      logger.error("Error reading response from server, url %s" % url)

    return data


def main():
  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is a Ambari thin client and '
                         'supports import/export of Ambari managed clusters '
                         'using a cluster blueprint.')

  parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                  default=False, help="output verbosity.")
  parser.add_option("-a", "--action", dest="action", default = "import",
                  help="Script action. (import/export) [default: import]")
  parser.add_option("-f", "--blueprint", dest="blueprint", metavar="FILE",
                  help="File Path. (import/export) file path.")
  parser.add_option("-o", "--hosts", dest="hosts", metavar="FILE",
                  help="Host Assignments. Import only.")
  parser.add_option("-c", "--cluster", dest="cluster", help="Target cluster.")
  parser.add_option("-d", "--arguments", dest="arguments",
                    help="Inline arguments for masters and slaves. "
                         "master=X,Y&slaves=A,B&gateway=G")
  parser.add_option("-s", "--silent", dest="silent", default=False,
                    action="store_true", help="Run silently. Appropriate accompanying arguments required.")
  parser.add_option("-r", "--port", dest="port", default="8080",
                    help="Ambari server port, when running silently. [default: 8080]")
  parser.add_option("-u", "--user", dest="user", default="admin",
                    help="Ambari server username, when running silently. [default: admin]")
  parser.add_option("-p", "--password", dest="password", default="admin",
                    help="Ambari server password, when running silently. [default: admin]")
  parser.add_option("-i", "--host", dest="hostname", default="localhost",
                    help="Ambari server host, when running silently. [default: localhost]")

  (options, args) = parser.parse_args()

  global ACTION
  ACTION = options.action

  global SILENT
  SILENT = options.silent

  global INLINE_ARGUMENTS
  INLINE_ARGUMENTS = options.arguments

  if options.cluster is None:
    raise Exception("Cluster name is required. '-c' option not provided.")

  if options.silent:
    if options.blueprint is None:
      raise Exception("Destination file path required. '-f' option not "
                      "provided.")
    elif options.action == "import" and options.hosts is None and options.arguments is None:
      raise Exception("Host assignment file path required. '-o' option not "
                      "provided.")
  pass

  # set verbose
  global logger
  if options.verbose:
    logger.setLevel(level=logging.DEBUG)
  else:
    logger.setLevel(level=logging.INFO)
  pass
  ch = logging.StreamHandler(sys.stdout)
  formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
  ch.setFormatter(formatter)
  logger.addHandler(ch)

  global PORT
  PORT = options.port

  global USERNAME
  USERNAME = options.user

  global PASSWORD
  PASSWORD = options.password

  global HOSTNAME
  HOSTNAME = options.hostname

  ambariBlueprint = AmbariBlueprint()

  if options.action == "import":
    ambariBlueprint.importBlueprint(options.blueprint, options.hosts, options.cluster)
  elif options.action == "export":
    ambariBlueprint.exportBlueprint(options.cluster, options.blueprint)
  else:
    raise Exception("Unsupported action %s" % options.action)
  pass


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
