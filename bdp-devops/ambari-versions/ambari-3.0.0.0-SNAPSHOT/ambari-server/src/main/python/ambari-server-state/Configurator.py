from Entities import *
from os.path import exists
import xml.dom.minidom

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#* Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


class Configurator:

  servicesPath = {"Core": "core-site.xml",
                  "Configuration": "initProperties.xml"
                  #                  "NameNode": "resources/hdfs-site.xml",
                  #                  "JobTracker": "resources/mapred-site.xml",
                  #                  "OOZIE": "resources/oozie-site.xml",
                  #                  "HBASE": "resources/hbase-site.xml",

                  #                 "ZooKeeper": "zk.properties",
  }

  CONFIG_INIT_TYPE = ("CURRENT_DIR", "USER_PATH_INPUT");
  RESULT_FILE_PATH = "ambari-server-state.xml"
  configurationType = 0;

  configurations = Configurations()#root xml element for the resulting file

  def __init__(self):
    "Init class with all required information about resources"
    self.configurationType = self.chooseConfInitType()
    self.initPaths()##TODO uncomment
    self.isFilesExist()
    self.getServicesFromUserInputConfigFile()
    self.createResultFile()


  def getServicesFromUserInputConfigFile(self):
    "Get all configeration data from resources(files)"
    self.getConfiguration()
    self.getStackInformation()
    self.getHosts()

  def getStackInformation(self):
    "Init data for Stack block of the result configuration file"
    DOMTree = xml.dom.minidom.parse(self.servicesPath["Configuration"])
    rootElement = DOMTree.documentElement
    stack = rootElement.getElementsByTagName("stack")[0]
    self.configurations.stack.comment = stack.getElementsByTagName('comment')[0].childNodes[0].data
    services = stack.getElementsByTagName("service")
    servicesResultStructure = self.configurations.stack.services
    for service in services:
      serviceStructure = Service()
      serviceStructure.name = service.getElementsByTagName('name')[0].childNodes[0].data
      serviceStructure.version = service.getElementsByTagName('version')[0].childNodes[0].data
      serviceStructure.comment = service.getElementsByTagName('comment')[0].childNodes[0].data
      serviceStructure.user = service.getElementsByTagName('user')[0].childNodes[0].data
      serviceStructure.enabled = service.getElementsByTagName('enabled')[0].childNodes[0].data
      servicesResultStructure.addService(serviceStructure)

    repositories = rootElement.getElementsByTagName("repository")
    for repositiry in repositories:
      self.configurations.stack.repository.comment = repositiry.getElementsByTagName('comment')[0].childNodes[0].data
      infos = repositiry.getElementsByTagName("info")
      for info in infos:
        for key in info.childNodes:
          if key.nodeType == 1:
            repositoryStructure = self.configurations.stack.repository
            repositoryStructure.info.addKey(key.nodeName, key.childNodes[0].data)

  def getHosts(self):
    "Init data for Hosts block of the result configuration file"
  ###########################Get Hosts information##########################
    DOMTree = xml.dom.minidom.parse(self.servicesPath["Configuration"])
    rootElement = DOMTree.documentElement
    hostsElement = rootElement.getElementsByTagName("hosts")[0]
    hosts = hostsElement.getElementsByTagName("host")
    for host in hosts:
      if host.nodeType == 1:
        hostsStructure = self.configurations.hosts
        hostElement = Host()
        hostElement.name = host.childNodes[0].data
        hostsStructure.addHost(hostElement)


  def getConfiguration(self):
    "Init Configuration block of result configuration file"
    DOMTree = xml.dom.minidom.parse(self.servicesPath["Configuration"])
    rootElement = DOMTree.documentElement

    hadoopEnvResultStructure = self.configurations.configuration.hadoopEnv
    coreSiteResultStructure = self.configurations.configuration.coreSite

    ###########################Get Configuration information##########################
    env = rootElement.getElementsByTagName("hadoop-env")[0]
    confDir = env.getElementsByTagName('conf-dir')[0].childNodes[0].data
    hadoopEnvResultStructure.confDir = confDir
    namenodeJvmOpts = env.getElementsByTagName('namenode-jvm-opts')[0].childNodes[0].data
    hadoopEnvResultStructure.namenodeJvmOpts = namenodeJvmOpts
    clientOpts = env.getElementsByTagName('client-opts')[0].childNodes[0].data
    hadoopEnvResultStructure.clientOpts = clientOpts

    coreSite = rootElement.getElementsByTagName("core-site")
    fsDefaultName = coreSite[0].getElementsByTagName('fs-default-name')[0].childNodes[0].data
    coreSiteResultStructure.fsDefaultName = fsDefaultName

    hadoopSecurityAuthentication = coreSite[0].getElementsByTagName('hadoop-security-authentication')[0].childNodes[0].data
    coreSiteResultStructure.hadoopSecurityAuthentication = hadoopSecurityAuthentication

    #########################Get configuration.coreSite#########################
    DOMTree = xml.dom.minidom.parse(self.servicesPath["Core"])
    rootElement = DOMTree.documentElement
    properties = rootElement.getElementsByTagName("property")
    for property in properties:
      name = property.getElementsByTagName('name')[0].childNodes[0].data
      if name == "fs.default.name":
        self.configurations.configuration.coreSite.fsDefaultName = property.getElementsByTagName('value')[0].childNodes[0].data
      if name == "hadoop.security.authentication":
        self.configurations.configuration.coreSite.hadoopSecurityAuthentication = property.getElementsByTagName('value')[0].childNodes[0].data


  def chooseConfInitType(self):
    "Type of how to get paths to configuration files"
    "Configuration types are base on Configuration.CONFIG_INIT_TYPE tuple"
    return int(raw_input("\tInput configuration type:\n" +
                         "0)Current path contains all required configuration files.\n" +
                         "1)Enter path for each conf file manually.\n" +
                         "Choose:"
    )
    ).numerator


  def initPaths(self):
    "Input alternative file paths for resources"
    if self.configurationType != 0:
      for service in self.servicesPath.keys():
        path = raw_input("Please enter path for " + service + "(if there is no such service type \"no\") :")
        if len(path) > 0 and not path == "no":
          self.servicesPath[service] = path
        elif path == "no":
          self.servicesPath.__delitem__(service)
          print(self.servicesPath)
        else:
          raise ValueError(
            "Path to the configuration file can't be empty.") #Catch it layter and start input mode automatically


  def isFilesExist(self):
    "Checking for resources file existing"
    for service in self.servicesPath.keys():
      path = self.servicesPath[service]
      isExit = exists(path)
      errorMessage = "File " + path + " doesn't exist! ("+ service+ " service)"
      if not isExit:
        raise  IOError(errorMessage)
#      else:
#        print("File " + path + " exist!")

  def createResultFile(self):
    resultFile = open(self.RESULT_FILE_PATH, "w")
    resultFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    resultFile.write( str(self.configurations) )
    resultFile.flush()
    resultFile.close()
    print("\t\t Result configuration file( "+self.RESULT_FILE_PATH+") was generate successfully.")








