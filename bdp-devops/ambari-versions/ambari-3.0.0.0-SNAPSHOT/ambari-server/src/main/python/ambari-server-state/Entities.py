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

from ambari_commons.xml_utils import ConvertToXml


class Configurations(ConvertToXml):
  "Root element for resulting file, incluge all exported data"
  stack = "" #Stack entuty
  configuration = "" #Configuration entity
  hosts = ""  #Hosts entity

  def __init__(self):
    self.stack = Stack()
    self.hosts = Hosts()
    self.configuration = ConfigurationEntity()

  def __str__(self):
    result = "<configurations>"
    result += str(self.stack)
    result += str(self.hosts)
    result += str(self.configuration)
    result += "</configurations>"
    return result


###Stack structure {
class Stack(ConvertToXml):
  comment = ""
  services = "" #Services object
  repository = "" #Repository object

  def __init__(self):
    self.services = Services()
    self.repository = Repository()

  def __str__(self):
    result = "<stack>"
    result += "<comment>" + self.comment + "</comment>"
    result += str(self.services)
    result += str(self.repository)
    result += "</stack>"
    return result


class Services(ConvertToXml):
  service = [] #Service objects

  def __str__(self):
    result = "<services>"
    for serv in self.service:
      result += str(serv)
    result += "</services>"
    return result

  def addService(self, service):
    self.service.append(service)


class Service(ConvertToXml):
  name = ""
  version = ""
  comment = ""
  user = ""
  enabled = ""

#  def __init__(self, name, ver, comment, user, enabled):
#    super(self)
#
#  def __init__(self, name, ver, comment, user, enabled):
#    self.name = name
#    self.version = ver
#    self.comment = comment
#    self.user = user
#    self.enabled = enabled

  def __str__(self):
    result = "<service>"
    result += self.attributesToXml()
    result += "</service>"
    return result


class Repository(ConvertToXml):
  comment = ""
  info = "" #Info object

  def __init__(self):
    self.info = Info()

  def __str__(self):
    result = "<repository>"
    result += "<comment>" + self.comment + "</comment>"
    result += str(self.info)
    result += "</repository>"
    return result


class Info(ConvertToXml):
  keys = {}

  def __str__(self):
    result = "<info>"
    for key in self.keys.keys():
      result += "<" + key + ">"
      result += self.keys.get(key)
      result += "</" + key + ">"
    result += "</info>"
    return result

  def addKey(self, key, value):
    self.keys[key] = value

###Stack structure }


###Configuration structure {
class ConfigurationEntity(ConvertToXml):
  hadoopEnv = "" #HadoopEnv object
  coreSite = ""   #CoreSite

  def __init__(self):
    self.hadoopEnv = HadoopEnv()
    self.coreSite = CoreSite()

  def __str__(self):
    result = "<configuration>"
    result += str(self.hadoopEnv)
    result += str(self.coreSite)
    result += "</configuration>"
    return result


class Hosts:
  hosts = []  #Host collection
  comment = ""

  def addHost(self, host):
    self.hosts.append(host)

  def __str__(self):
    result = "<hosts>"
    for host in self.hosts:
      result += str(host)
    result += "</hosts>"
    return result

class Host(ConvertToXml):
  name = ""

  def __str__(self):
    result = "<host>"
    result += self.name
    result += "</host>"
    return result


class HadoopEnv(ConvertToXml):
  confDir = ""
  namenodeJvmOpts = ""
  clientOpts = ""

  def __str__(self):
    result = "<hadoop-env>"
    result += self.attributesToXml()
    result += "</hadoop-env>"
    return result


class CoreSite(ConvertToXml):
  fsDefaultName = ""
  #hadoopTmpDir = ""
  hadoopSecurityAuthentication = ""

  def __str__(self):
    result = "<core-site>"
    result += self.attributesToXml()
    result += "</core-site>"
    return result

###Configuration structure }
