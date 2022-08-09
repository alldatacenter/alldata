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
# MUST be run on ambari-server host
import json
import time
from resource_management.core.shell import checked_call, call

# Change this to hostname of your ambari-server
HOSTNAME = checked_call("hostname -f")[1].strip()

############# Configurations (feel free to change) #############

SERVICE_NAME = "STORM"

COMPONENTS = [
  "NIMBUS",
  "SUPERVISOR"
]

COMPONENTS_TO_HOSTS = [
  {"NIMBUS": HOSTNAME},
  {"SUPERVISOR": HOSTNAME},
  #{"SUPERVISOR": "c6402.ambari.apache.org"},
  #{"SUPERVISOR": "c6403.ambari.apache.org"}
]

PROTOCOL = "http"
PORT = "8080"

CLUSTER_NAME = "c1"
STACK_VERSION = "2.0.8"

CONFIGS_TO_CHANGE = {
  "storm-site":{
    #"storm.zookeeper.servers":"['c6401.amabri.apache.org','c6402.amabri.apache.org','c6403.amabri.apache.org']",
    #"nimbus.host": "c6401.ambari.apache.org"
  },
  #"global":{
  #  "clientPort":"2182"
  #}
}

#################################################################
SERVER_URL = "{protocol}://{hostname}:{port}".format(protocol=PROTOCOL, hostname=HOSTNAME, port=PORT)    

def main():
  # add service
  checked_call('curl -H \'X-Requested-By:anything\' -i -X POST -d \'[{{"ServiceInfo":{{"service_name":"{service_name}"}}}}]\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}/services'.
               format(service_name=SERVICE_NAME, server_url=SERVER_URL, cluster_name=CLUSTER_NAME))
  
  # add components
  for component in COMPONENTS:
    checked_call('curl -H \'X-Requested-By:anything\' -i -X POST -d \'{{"components":[{{"ServiceComponentInfo":{{"component_name":"{component}"}}}}]}}\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}/services?ServiceInfo/service_name={service_name}'.
               format(service_name=SERVICE_NAME, component=component, server_url=SERVER_URL, cluster_name=CLUSTER_NAME))
    
  # assign components to hosts
  for x in COMPONENTS_TO_HOSTS: 
    for component, host in x.iteritems():
      checked_call('curl -H \'X-Requested-By:anything\' -i -X POST -d \'{{"host_components":[{{"HostRoles":{{"component_name":"{component}"}}}}]}}\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}/hosts?Hosts/host_name={host}'.
               format(host=host, component=component, server_url=SERVER_URL, cluster_name=CLUSTER_NAME))
    
  # update and create all the service-specific configurations
  checked_call('curl -H \'X-Requested-By:anything\'-X GET -u admin:admin {server_url}/api/v1/stacks2/HDP/versions/{stack_version}/stackServices/{service_name}/configurations?fields=* > /tmp/config.json'.
               format(server_url=SERVER_URL, stack_version=STACK_VERSION, service_name=SERVICE_NAME))
  with open('/tmp/config.json', "r") as f:
    d = json.load(f)
  
  configs = {}
  for x in d['items']:
    site_name = x['StackConfigurations']['type'][:-4]
    if not site_name in configs:
      configs[site_name] = {}
    config = configs[site_name]
    config[x['StackConfigurations']['property_name']] = x['StackConfigurations']['property_value']

  for site_name, site_content in configs.iteritems():
    code = call('/var/lib/ambari-server/resources/scripts/configs.sh get {hostname} {cluster_name} {site_name}'.format(hostname=HOSTNAME, cluster_name=CLUSTER_NAME, site_name=site_name))[0]

    if code:
      print "Adding new site: "+site_name
      checked_call('curl -i -H \'X-Requested-By:anything\' -X PUT -d \'{{"Clusters":{{"desired_configs":{{"type":"{site_name}","tag":"version1","properties":{site_content}}}}}}}\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}'.format(site_name=site_name, site_content=json.dumps(site_content), server_url=SERVER_URL, cluster_name=CLUSTER_NAME))
    else:
      timestamp = int(time.time())
      print "Modifiying site: "+site_name+" version"+str(timestamp)
      checked_call('/var/lib/ambari-server/resources/scripts/configs.sh get {hostname} {cluster_name} {site_name} /tmp/current_site.json'.format(hostname=HOSTNAME, cluster_name=CLUSTER_NAME, site_name=site_name))
      
      with open('/tmp/current_site.json', "r") as f:
        fcontent = f.read()
        d = json.loads("{"+fcontent+"}")
      
      for k,v in site_content.iteritems():
        d['properties'][k] = v
        
      checked_call('curl -i -H \'X-Requested-By:anything\' -X PUT -d \'{{"Clusters":{{"desired_configs":{{"type":"{site_name}","tag":"version{timestamp}","properties":{site_content}}}}}}}\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}'.format(site_name=site_name, timestamp=timestamp, site_content=json.dumps(d['properties']), server_url=SERVER_URL, cluster_name=CLUSTER_NAME))

  for site_name, site_configs in CONFIGS_TO_CHANGE.iteritems():
    for config_name, config_value in site_configs.iteritems():
      print "Adding config "+config_name+"="+config_value+" to "+site_name
      checked_call('/var/lib/ambari-server/resources/scripts/configs.sh set {hostname} {cluster_name} {site_name} {config_name} {config_value}'.format(config_name=config_name, config_value=config_value, hostname=HOSTNAME, cluster_name=CLUSTER_NAME, site_name=site_name))
      
        
  # install all new components
  checked_call('curl -H \'X-Requested-By:anything\' -i -X PUT -d  \'{{"RequestInfo": {{"context" :"Installing Services"}}, "Body": {{"ServiceInfo": {{"state": "INSTALLED"}}}}}}\' -u admin:admin {server_url}/api/v1/clusters/{cluster_name}/services?ServiceInfo/state=INIT'.
             format(server_url=SERVER_URL, cluster_name=CLUSTER_NAME))

if __name__ == '__main__':
  main()
