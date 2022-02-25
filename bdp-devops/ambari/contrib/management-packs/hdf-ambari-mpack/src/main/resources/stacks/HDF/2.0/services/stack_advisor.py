#!/usr/bin/env ambari-python-wrap
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
import math
import re
import os
import sys
from math import ceil, floor

from stack_advisor import DefaultStackAdvisor

DB_TYPE_DEFAULT_PORT_MAP = {"MYSQL":"3306", "ORACLE":"1521", "POSTGRES":"5432", "MSSQL":"1433", "SQLA":"2638"}

class HDF20StackAdvisor(DefaultStackAdvisor):

  def getComponentLayoutValidations(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    items = super(HDF20StackAdvisor, self).getComponentLayoutValidations(services, hosts)

    # Use a set for fast lookup
    hostsSet =  set(super(HDF20StackAdvisor, self).getActiveHosts([host["Hosts"] for host in hosts["items"]]))  #[host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsSet)

    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]

    # Validating cardinality
    for component in componentsList:
      if component["StackServiceComponents"]["cardinality"] is not None:
         componentName = component["StackServiceComponents"]["component_name"]
         componentDisplayName = component["StackServiceComponents"]["display_name"]
         componentHosts = []
         if component["StackServiceComponents"]["hostnames"] is not None:
           componentHosts = [componentHost for componentHost in component["StackServiceComponents"]["hostnames"] if componentHost in hostsSet]
         componentHostsCount = len(componentHosts)
         cardinality = str(component["StackServiceComponents"]["cardinality"])
         # cardinality types: null, 1+, 1-2, 1, ALL
         message = None
         if "+" in cardinality:
           hostsMin = int(cardinality[:-1])
           if componentHostsCount < hostsMin:
             message = "At least {0} {1} components should be installed in cluster.".format(hostsMin, componentDisplayName)
         elif "-" in cardinality:
           nums = cardinality.split("-")
           hostsMin = int(nums[0])
           hostsMax = int(nums[1])
           if componentHostsCount > hostsMax or componentHostsCount < hostsMin:
             message = "Between {0} and {1} {2} components should be installed in cluster.".format(hostsMin, hostsMax, componentDisplayName)
         elif "ALL" == cardinality:
           if componentHostsCount != hostsCount:
             message = "{0} component should be installed on all hosts in cluster.".format(componentDisplayName)
         else:
           if componentHostsCount != int(cardinality):
             message = "Exactly {0} {1} components should be installed in cluster.".format(int(cardinality), componentDisplayName)

         if message is not None:
           items.append({"type": 'host-component', "level": 'ERROR', "message": message, "component-name": componentName})

    # Validating host-usage
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    usedHostsList = [item for sublist in usedHostsListList for item in sublist]
    nonUsedHostsList = [item for item in hostsSet if item not in usedHostsList]
    for host in nonUsedHostsList:
      items.append( { "type": 'host-component', "level": 'ERROR', "message": 'Host is not used', "host": str(host) } )

    return items

  def getServiceConfigurationRecommenderDict(self):
    return {
      "KAFKA": self.recommendKAFKAConfigurations,
      "NIFI":  self.recommendNIFIConfigurations,
      "STORM": self.recommendStormConfigurations,
      "AMBARI_METRICS": self.recommendAmsConfigurations,
      "RANGER": self.recommendRangerConfigurations,
      "LOGSEARCH" : self.recommendLogsearchConfigurations
    }

  def putProperty(self, config, configType, services=None):
    userConfigs = {}
    changedConfigs = []
    # if services parameter, prefer values, set by user
    if services:
      if 'configurations' in services.keys():
        userConfigs = services['configurations']
      if 'changed-configurations' in services.keys():
        changedConfigs = services["changed-configurations"]

    if configType not in config:
      config[configType] = {}
    if"properties" not in config[configType]:
      config[configType]["properties"] = {}
    def appendProperty(key, value):
      # If property exists in changedConfigs, do not override, use user defined property
      if self.__isPropertyInChangedConfigs(configType, key, changedConfigs):
        config[configType]["properties"][key] = userConfigs[configType]['properties'][key]
      else:
        config[configType]["properties"][key] = str(value)
    return appendProperty

  def __isPropertyInChangedConfigs(self, configType, propertyName, changedConfigs):
    for changedConfig in changedConfigs:
      if changedConfig['type']==configType and changedConfig['name']==propertyName:
        return True
    return False

  def putPropertyAttribute(self, config, configType):
    if configType not in config:
      config[configType] = {}
    def appendPropertyAttribute(key, attribute, attributeValue):
      if "property_attributes" not in config[configType]:
        config[configType]["property_attributes"] = {}
      if key not in config[configType]["property_attributes"]:
        config[configType]["property_attributes"][key] = {}
      config[configType]["property_attributes"][key][attribute] = attributeValue if isinstance(attributeValue, list) else str(attributeValue)
    return appendPropertyAttribute

  def recommendKAFKAConfigurations(self, configurations, clusterData, services, hosts):
    kafka_broker = getServicesSiteProperties(services, "kafka-broker")

    # kerberos security for kafka is decided from `security.inter.broker.protocol` property value
    security_enabled = (kafka_broker is not None and 'security.inter.broker.protocol' in  kafka_broker
                        and 'SASL' in kafka_broker['security.inter.broker.protocol'])
    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    putKafkaLog4jProperty = self.putProperty(configurations, "kafka-log4j", services)
    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")

    #If AMS is part of Services, use the KafkaTimelineMetricsReporter for metric reporting. Default is ''.
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "AMBARI_METRICS" in servicesList:
      putKafkaBrokerProperty('kafka.metrics.reporters', 'org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter')

    if "ranger-env" in services["configurations"] and "ranger-kafka-plugin-properties" in services["configurations"] and \
            "ranger-kafka-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putKafkaRangerPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      rangerEnvKafkaPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-kafka-plugin-enabled"]
      putKafkaRangerPluginProperty("ranger-kafka-plugin-enabled", rangerEnvKafkaPluginProperty)

    if 'ranger-kafka-plugin-properties' in services['configurations'] and ('ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']):
      kafkaLog4jRangerLines = [{
        "name": "log4j.appender.rangerAppender",
        "value": "org.apache.log4j.DailyRollingFileAppender"
      },
        {
          "name": "log4j.appender.rangerAppender.DatePattern",
          "value": "'.'yyyy-MM-dd-HH"
        },
        {
          "name": "log4j.appender.rangerAppender.File",
          "value": "${kafka.logs.dir}/ranger_kafka.log"
        },
        {
          "name": "log4j.appender.rangerAppender.layout",
          "value": "org.apache.log4j.PatternLayout"
        },
        {
          "name": "log4j.appender.rangerAppender.layout.ConversionPattern",
          "value": "%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n"
        },
        {
          "name": "log4j.logger.org.apache.ranger",
          "value": "INFO, rangerAppender"
        }]

      rangerPluginEnabled=''
      if 'ranger-kafka-plugin-properties' in configurations and 'ranger-kafka-plugin-enabled' in  configurations['ranger-kafka-plugin-properties']['properties']:
        rangerPluginEnabled = configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled']
      elif 'ranger-kafka-plugin-properties' in services['configurations'] and 'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
        rangerPluginEnabled = services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled']

      if  rangerPluginEnabled and rangerPluginEnabled.lower() == "Yes".lower():
        # recommend authorizer.class.name
        putKafkaBrokerProperty("authorizer.class.name", 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer')
        # change kafka-log4j when ranger plugin is installed

        if 'kafka-log4j' in services['configurations'] and 'content' in services['configurations']['kafka-log4j']['properties']:
          kafkaLog4jContent = services['configurations']['kafka-log4j']['properties']['content']
          for item in range(len(kafkaLog4jRangerLines)):
            if kafkaLog4jRangerLines[item]["name"] not in kafkaLog4jContent:
              kafkaLog4jContent+= '\n' + kafkaLog4jRangerLines[item]["name"] + '=' + kafkaLog4jRangerLines[item]["value"]
          putKafkaLog4jProperty("content",kafkaLog4jContent)


      else:
        # Kerberized Cluster with Ranger plugin disabled
        if security_enabled and 'kafka-broker' in services['configurations'] and 'authorizer.class.name' in services['configurations']['kafka-broker']['properties'] and \
                services['configurations']['kafka-broker']['properties']['authorizer.class.name'] == 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer':
          putKafkaBrokerProperty("authorizer.class.name", 'kafka.security.auth.SimpleAclAuthorizer')
        # Non-kerberos Cluster with Ranger plugin disabled
        else:
          putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

    # Non-Kerberos Cluster without Ranger
    elif not security_enabled:
      putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

  def getOracleDBConnectionHostPort(self, db_type, db_host, rangerDbName):
    connection_string = self.getDBConnectionHostPort(db_type, db_host)
    colon_count = db_host.count(':')
    if colon_count == 1 and '/' in db_host:
      connection_string = "//" + connection_string
    elif colon_count == 0 or colon_count == 1:
      connection_string = "//" + connection_string + "/" + rangerDbName if rangerDbName else "//" + connection_string

    return connection_string

  def getDBConnectionHostPort(self, db_type, db_host):
    connection_string = ""
    if db_type is None or db_type == "":
      return connection_string
    else:
      colon_count = db_host.count(':')
      if colon_count == 0:
        if DB_TYPE_DEFAULT_PORT_MAP.has_key(db_type):
          connection_string = db_host + ":" + DB_TYPE_DEFAULT_PORT_MAP[db_type]
        else:
          connection_string = db_host
      elif colon_count == 1:
        connection_string = db_host
      elif colon_count == 2:
        connection_string = db_host

    return connection_string

  def recommendNIFIConfigurations(self, configurations, clusterData, services, hosts):
    nifi = getServicesSiteProperties(services, "nifi")

    if "ranger-env" in services["configurations"] and "ranger-nifi-plugin-properties" in services["configurations"] and \
                    "ranger-nifi-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putNiFiRangerPluginProperty = self.putProperty(configurations, "ranger-nifi-plugin-properties", services)
      rangerEnvNiFiPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-nifi-plugin-enabled"]
      putNiFiRangerPluginProperty("ranger-nifi-plugin-enabled", rangerEnvNiFiPluginProperty)

      if rangerEnvNiFiPluginProperty == 'Yes' and \
                      "nifi.authentication" in services["configurations"]["ranger-nifi-plugin-properties"]["properties"] and \
                      "nifi.node.ssl.isenabled" in services["configurations"]["nifi-ambari-ssl-config"]["properties"]:
        nifiAmbariSSLConfig = 'SSL' if services["configurations"]["nifi-ambari-ssl-config"]["properties"]["nifi.node.ssl.isenabled"] == 'true' else 'NONE'
        putNiFiRangerPluginProperty("nifi.authentication",nifiAmbariSSLConfig)

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)
    putRangerAdminProperty = self.putProperty(configurations, "admin-properties", services)
    putRangerAdminSiteProperty = self.putProperty(configurations, "ranger-admin-site", services)
    putRangerUgsyncSite = self.putProperty(configurations, "ranger-ugsync-site", services)
    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)
    putTagsyncSiteProperty = self.putProperty(configurations, "ranger-tagsync-site", services)

    # get zookeeper hosts
    zookeeper_host_port = self.getZKHostPortString(services)

    # Build policymgr_external_url
    protocol = 'http'
    ranger_admin_host = 'localhost'
    port = '6080'

    # Check if http is disabled. For HDF this can be checked in ranger-admin-site/ranger.service.http.enabled
    if ('ranger-admin-site' in services['configurations'] and 'ranger.service.http.enabled' in services['configurations']['ranger-admin-site']['properties'] \
      and services['configurations']['ranger-admin-site']['properties']['ranger.service.http.enabled'].lower() == 'false'):
      # HTTPS protocol is used
      protocol = 'https'
      if 'ranger-admin-site' in services['configurations'] and \
          'ranger.service.https.port' in services['configurations']['ranger-admin-site']['properties']:
        port = services['configurations']['ranger-admin-site']['properties']['ranger.service.https.port']
    else:
      # HTTP protocol is used
      if 'ranger-admin-site' in services['configurations'] and \
          'ranger.service.http.port' in services['configurations']['ranger-admin-site']['properties']:
        port = services['configurations']['ranger-admin-site']['properties']['ranger.service.http.port']

    ranger_admin_hosts = self.getComponentHostNames(services, "RANGER", "RANGER_ADMIN")
    if ranger_admin_hosts:
      if len(ranger_admin_hosts) > 1 \
        and services['configurations'] \
        and 'admin-properties' in services['configurations'] and 'policymgr_external_url' in services['configurations']['admin-properties']['properties'] \
        and services['configurations']['admin-properties']['properties']['policymgr_external_url'] \
        and services['configurations']['admin-properties']['properties']['policymgr_external_url'].strip():

        # in case of HA deployment keep the policymgr_external_url specified in the config
        policymgr_external_url = services['configurations']['admin-properties']['properties']['policymgr_external_url']
      else:
        ranger_admin_host = ranger_admin_hosts[0]
        policymgr_external_url = "{0}://{1}:{2}".format(protocol, ranger_admin_host, port)

      putRangerAdminProperty('policymgr_external_url', policymgr_external_url)

    cluster_env = getServicesSiteProperties(services, "cluster-env")
    security_enabled = cluster_env is not None and "security_enabled" in cluster_env and \
                       cluster_env["security_enabled"].lower() == "true"
    if "ranger-env" in configurations and not security_enabled:
      putRangerEnvProperty("ranger-storm-plugin-enabled", "No")

    if 'admin-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['admin-properties']['properties']) \
        and ('db_host' in services['configurations']['admin-properties']['properties']) and ('db_name' in services['configurations']['admin-properties']['properties']):

      rangerDbFlavor = services['configurations']["admin-properties"]["properties"]["DB_FLAVOR"]
      rangerDbHost =   services['configurations']["admin-properties"]["properties"]["db_host"]
      rangerDbName =   services['configurations']["admin-properties"]["properties"]["db_name"]
      ranger_db_url_dict = {
        'MYSQL': {'ranger.jpa.jdbc.driver': 'com.mysql.jdbc.Driver',
                  'ranger.jpa.jdbc.url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/' + rangerDbName},
        'ORACLE': {'ranger.jpa.jdbc.driver': 'oracle.jdbc.driver.OracleDriver',
                   'ranger.jpa.jdbc.url': 'jdbc:oracle:thin:@//' + self.getOracleDBConnectionHostPort(rangerDbFlavor, rangerDbHost, rangerDbName)},
        'POSTGRES': {'ranger.jpa.jdbc.driver': 'org.postgresql.Driver',
                     'ranger.jpa.jdbc.url': 'jdbc:postgresql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/' + rangerDbName},
        'MSSQL': {'ranger.jpa.jdbc.driver': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
                  'ranger.jpa.jdbc.url': 'jdbc:sqlserver://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';databaseName=' + rangerDbName},
        'SQLA': {'ranger.jpa.jdbc.driver': 'sap.jdbc4.sqlanywhere.IDriver',
                 'ranger.jpa.jdbc.url': 'jdbc:sqlanywhere:host=' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';database=' + rangerDbName}
      }
      rangerDbProperties = ranger_db_url_dict.get(rangerDbFlavor, ranger_db_url_dict['MYSQL'])
      for key in rangerDbProperties:
        putRangerAdminSiteProperty(key, rangerDbProperties.get(key))

      if 'admin-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['admin-properties']['properties']) \
          and ('db_host' in services['configurations']['admin-properties']['properties']):

        rangerDbFlavor = services['configurations']["admin-properties"]["properties"]["DB_FLAVOR"]
        rangerDbHost =   services['configurations']["admin-properties"]["properties"]["db_host"]
        ranger_db_privelege_url_dict = {
          'MYSQL': {'ranger_privelege_user_jdbc_url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost)},
          'ORACLE': {'ranger_privelege_user_jdbc_url': 'jdbc:oracle:thin:@//' + self.getOracleDBConnectionHostPort(rangerDbFlavor, rangerDbHost, None)},
          'POSTGRES': {'ranger_privelege_user_jdbc_url': 'jdbc:postgresql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/postgres'},
          'MSSQL': {'ranger_privelege_user_jdbc_url': 'jdbc:sqlserver://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';'},
          'SQLA': {'ranger_privelege_user_jdbc_url': 'jdbc:sqlanywhere:host=' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';'}
        }
        rangerPrivelegeDbProperties = ranger_db_privelege_url_dict.get(rangerDbFlavor, ranger_db_privelege_url_dict['MYSQL'])
        for key in rangerPrivelegeDbProperties:
          putRangerEnvProperty(key, rangerPrivelegeDbProperties.get(key))

    # Recommend ldap settings based on ambari.properties configuration
    if 'ambari-server-properties' in services and \
            'ambari.ldap.isConfigured' in services['ambari-server-properties'] and \
            services['ambari-server-properties']['ambari.ldap.isConfigured'].lower() == "true":
      serverProperties = services['ambari-server-properties']
      if 'authentication.ldap.baseDn' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.searchBase', serverProperties['authentication.ldap.baseDn'])
      if 'authentication.ldap.groupMembershipAttr' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.memberattributename', serverProperties['authentication.ldap.groupMembershipAttr'])
      if 'authentication.ldap.groupNamingAttr' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.nameattribute', serverProperties['authentication.ldap.groupNamingAttr'])
      if 'authentication.ldap.groupObjectClass' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.objectclass', serverProperties['authentication.ldap.groupObjectClass'])
      if 'authentication.ldap.managerDn' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.binddn', serverProperties['authentication.ldap.managerDn'])
      if 'authentication.ldap.primaryUrl' in serverProperties:
        ldap_protocol =  'ldap://'
        if 'authentication.ldap.useSSL' in serverProperties and serverProperties['authentication.ldap.useSSL'] == 'true':
          ldap_protocol =  'ldaps://'
        ldapUrl = ldap_protocol + serverProperties['authentication.ldap.primaryUrl'] if serverProperties['authentication.ldap.primaryUrl'] else serverProperties['authentication.ldap.primaryUrl']
        putRangerUgsyncSite('ranger.usersync.ldap.url', ldapUrl)
      if 'authentication.ldap.userObjectClass' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.user.objectclass', serverProperties['authentication.ldap.userObjectClass'])
      if 'authentication.ldap.usernameAttribute' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.user.nameattribute', serverProperties['authentication.ldap.usernameAttribute'])


    # Recommend Ranger Authentication method
    authMap = {
      'org.apache.ranger.unixusersync.process.UnixUserGroupBuilder': 'UNIX',
      'org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder': 'LDAP'
    }

    if 'ranger-ugsync-site' in services['configurations'] and 'ranger.usersync.source.impl.class' in services['configurations']["ranger-ugsync-site"]["properties"]:
      rangerUserSyncClass = services['configurations']["ranger-ugsync-site"]["properties"]["ranger.usersync.source.impl.class"]
      if rangerUserSyncClass in authMap:
        rangerAuthMethod = authMap.get(rangerUserSyncClass)
        putRangerAdminSiteProperty('ranger.authentication.method', rangerAuthMethod)

    # Recommend Ambari Infra Solr properties
    is_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_solrCloud_enabled' in services['configurations']["ranger-env"]["properties"]:
      is_solr_cloud_enabled = services['configurations']["ranger-env"]["properties"]["is_solrCloud_enabled"]  == "true"

    is_external_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_external_solrCloud_enabled' in services['configurations']['ranger-env']['properties']:
      is_external_solr_cloud_enabled = services['configurations']['ranger-env']['properties']['is_external_solrCloud_enabled']  == 'true'

    ranger_audit_zk_port = ''


    if 'AMBARI_INFRA' in servicesList and zookeeper_host_port and is_solr_cloud_enabled and not is_external_solr_cloud_enabled:
      zookeeper_host_port = zookeeper_host_port.split(',')
      zookeeper_host_port.sort()
      zookeeper_host_port = ",".join(zookeeper_host_port)
      infra_solr_znode = '/infra-solr'

      if 'infra-solr-env' in services['configurations'] and \
        ('infra_solr_znode' in services['configurations']['infra-solr-env']['properties']):
        infra_solr_znode = services['configurations']['infra-solr-env']['properties']['infra_solr_znode']
        ranger_audit_zk_port = '{0}{1}'.format(zookeeper_host_port, infra_solr_znode)
      putRangerAdminSiteProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    elif zookeeper_host_port and is_solr_cloud_enabled and is_external_solr_cloud_enabled:
      ranger_audit_zk_port = '{0}/{1}'.format(zookeeper_host_port, 'ranger_audits')
      putRangerAdminSiteProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    else:
      putRangerAdminSiteProperty('ranger.audit.solr.zookeepers', 'NONE')

    # Recommend Ranger supported service's audit properties
    ranger_services = [
      {'service_name': 'KAFKA', 'audit_file': 'ranger-kafka-audit'},
      {'service_name': 'STORM', 'audit_file': 'ranger-storm-audit'},
      {'service_name': 'NIFI', 'audit_file': 'ranger-nifi-audit'}
    ]

    for item in range(len(ranger_services)):
      if ranger_services[item]['service_name'] in servicesList:
        component_audit_file =  ranger_services[item]['audit_file']
        if component_audit_file in services["configurations"]:
          ranger_audit_dict = [
            {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.solr', 'target_configname': 'xasecure.audit.destination.solr'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.urls', 'target_configname': 'xasecure.audit.destination.solr.urls'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.zookeepers', 'target_configname': 'xasecure.audit.destination.solr.zookeepers'}
          ]
          putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

          for item in ranger_audit_dict:
            if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
              if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
              else:
                rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
              putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

    ranger_plugins_serviceuser = [
      {'service_name': 'STORM', 'file_name': 'storm-env', 'config_name': 'storm_user', 'target_configname': 'ranger.plugins.storm.serviceuser'},
      {'service_name': 'KAFKA', 'file_name': 'kafka-env', 'config_name': 'kafka_user', 'target_configname': 'ranger.plugins.kafka.serviceuser'},
      {'service_name': 'NIFI', 'file_name': 'nifi-env', 'config_name': 'nifi_user', 'target_configname': 'ranger.plugins.nifi.serviceuser'}
    ]

    for item in range(len(ranger_plugins_serviceuser)):
      if ranger_plugins_serviceuser[item]['service_name'] in servicesList:
        file_name = ranger_plugins_serviceuser[item]['file_name']
        config_name = ranger_plugins_serviceuser[item]['config_name']
        target_configname = ranger_plugins_serviceuser[item]['target_configname']
        if file_name in services["configurations"] and config_name in services["configurations"][file_name]["properties"]:
          service_user = services["configurations"][file_name]["properties"][config_name]
          putRangerAdminSiteProperty(target_configname, service_user)


    has_ranger_tagsync = False
    if 'RANGER' in servicesList:
      ranger_tagsync_host = self.getComponentHostNames(services, "RANGER", "RANGER_TAGSYNC")
      has_ranger_tagsync = len(ranger_tagsync_host) > 0

    if 'ATLAS' in servicesList and has_ranger_tagsync:
      putTagsyncSiteProperty('ranger.tagsync.source.atlas', 'true')

    if zookeeper_host_port and has_ranger_tagsync:
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_port)

    if 'KAFKA' in servicesList and has_ranger_tagsync:
      kafka_hosts = self.getHostNamesWithComponent("KAFKA", "KAFKA_BROKER", services)
      kafka_port = '6667'
      if 'kafka-broker' in services['configurations'] and (
            'port' in services['configurations']['kafka-broker']['properties']):
        kafka_port = services['configurations']['kafka-broker']['properties']['port']
      kafka_host_port = []
      for i in range(len(kafka_hosts)):
        kafka_host_port.append(kafka_hosts[i] + ':' + kafka_port)

      final_kafka_host = ",".join(kafka_host_port)
      putTagsyncAppProperty('atlas.kafka.bootstrap.servers', final_kafka_host)

  def getAmsMemoryRecommendation(self, services, hosts):
    # MB per sink in hbase heapsize
    HEAP_PER_MASTER_COMPONENT = 50
    HEAP_PER_SLAVE_COMPONENT = 10

    schMemoryMap = {
      "KAFKA": {
        "KAFKA_BROKER": HEAP_PER_MASTER_COMPONENT
      },
      "STORM": {
        "NIMBUS": HEAP_PER_MASTER_COMPONENT,
      },
      "AMBARI_METRICS": {
        "METRICS_COLLECTOR": HEAP_PER_MASTER_COMPONENT,
        "METRICS_MONITOR": HEAP_PER_SLAVE_COMPONENT
      }
    }
    total_sinks_count = 0
    # minimum heap size
    hbase_heapsize = 500
    for serviceName, componentsDict in schMemoryMap.items():
      for componentName, multiplier in componentsDict.items():
        schCount = len(
          self.getHostsWithComponent(serviceName, componentName, services,
                                     hosts))
        hbase_heapsize += int((schCount * multiplier) ** 0.9)
        total_sinks_count += schCount
    collector_heapsize = int(hbase_heapsize/4 if hbase_heapsize > 2048 else 512)

    return round_to_n(collector_heapsize), round_to_n(hbase_heapsize), total_sinks_count

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    putStormStartupProperty = self.putProperty(configurations, "storm-site", services)
    putStormSiteAttributes = self.putPropertyAttribute(configurations, "storm-site")

    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      putStormSiteProperty('metrics.reporter.register', 'org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter')

    storm_site = getServicesSiteProperties(services, "storm-site")
    security_enabled = (storm_site is not None and "storm.zookeeper.superACL" in storm_site)
    if "ranger-env" in services["configurations"] and "ranger-storm-plugin-properties" in services["configurations"] and \
            "ranger-storm-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putStormRangerPluginProperty = self.putProperty(configurations, "ranger-storm-plugin-properties", services)
      rangerEnvStormPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-storm-plugin-enabled"]
      putStormRangerPluginProperty("ranger-storm-plugin-enabled", rangerEnvStormPluginProperty)

    rangerPluginEnabled = ''
    if 'ranger-storm-plugin-properties' in configurations and 'ranger-storm-plugin-enabled' in  configurations['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']
    elif 'ranger-storm-plugin-properties' in services['configurations'] and 'ranger-storm-plugin-enabled' in services['configurations']['ranger-storm-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled']

    nonRangerClass = 'backtype.storm.security.auth.authorizer.SimpleACLAuthorizer'
    rangerServiceVersion=''
    if 'RANGER' in servicesList:
      rangerServiceVersion = [service['StackServices']['service_version'] for service in services["services"] if service['StackServices']['service_name'] == 'RANGER'][0]

    if rangerServiceVersion and rangerServiceVersion == '0.4.0':
      rangerClass = 'com.xasecure.authorization.storm.authorizer.XaSecureStormAuthorizer'
    else:
      rangerClass = 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer'
    # Cluster is kerberized
    if security_enabled:
      if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putStormSiteProperty('nimbus.authorizer',rangerClass)
      elif (services["configurations"]["storm-site"]["properties"]["nimbus.authorizer"] == rangerClass):
        putStormSiteProperty('nimbus.authorizer', nonRangerClass)
    else:
      putStormSiteAttributes('nimbus.authorizer', 'delete', 'true')

    if "storm-site" in services["configurations"]:
      # atlas
      notifier_plugin_property = "storm.topology.submission.notifier.plugin.class"
      if notifier_plugin_property in services["configurations"]["storm-site"]["properties"]:
        notifier_plugin_value = services["configurations"]["storm-site"]["properties"][notifier_plugin_property]
        if notifier_plugin_value is None:
          notifier_plugin_value = " "
      else:
        notifier_plugin_value = " "

      include_atlas = "ATLAS" in servicesList
      atlas_hook_class = "org.apache.atlas.storm.hook.StormAtlasHook"
      if include_atlas and atlas_hook_class not in notifier_plugin_value:
        if notifier_plugin_value == " ":
          notifier_plugin_value = atlas_hook_class
        else:
          notifier_plugin_value = notifier_plugin_value + "," + atlas_hook_class
      if not include_atlas and atlas_hook_class in notifier_plugin_value:
        application_classes = []
        for application_class in notifier_plugin_value.split(","):
          if application_class != atlas_hook_class and application_class != " ":
            application_classes.append(application_class)
        if application_classes:
          notifier_plugin_value = ",".join(application_classes)
        else:
          notifier_plugin_value = " "
      if notifier_plugin_value != " ":
        putStormStartupProperty(notifier_plugin_property, notifier_plugin_value)

  def recommendAmsConfigurations(self, configurations, clusterData, services, hosts):
    putAmsEnvProperty = self.putProperty(configurations, "ams-env", services)
    putAmsHbaseSiteProperty = self.putProperty(configurations, "ams-hbase-site", services)
    putAmsSiteProperty = self.putProperty(configurations, "ams-site", services)
    putHbaseEnvProperty = self.putProperty(configurations, "ams-hbase-env", services)
    putGrafanaProperty = self.putProperty(configurations, "ams-grafana-env", services)
    putGrafanaPropertyAttribute = self.putPropertyAttribute(configurations, "ams-grafana-env")

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")

    if 'cluster-env' in services['configurations'] and \
        'metrics_collector_external_hosts' in services['configurations']['cluster-env']['properties']:
      metric_collector_host = services['configurations']['cluster-env']['properties']['metrics_collector_external_hosts']
    else:
      metric_collector_host = 'localhost' if len(amsCollectorHosts) == 0 else amsCollectorHosts[0]

    putAmsSiteProperty("timeline.metrics.service.webapp.address", str(metric_collector_host) + ":6188")

    log_dir = "/var/log/ambari-metrics-collector"
    if "ams-env" in services["configurations"]:
      if "metrics_collector_log_dir" in services["configurations"]["ams-env"]["properties"]:
        log_dir = services["configurations"]["ams-env"]["properties"]["metrics_collector_log_dir"]
      putHbaseEnvProperty("hbase_log_dir", log_dir)

    defaultFs = 'file:///'
    if "core-site" in services["configurations"] and \
      "fs.defaultFS" in services["configurations"]["core-site"]["properties"]:
      defaultFs = services["configurations"]["core-site"]["properties"]["fs.defaultFS"]

    operatingMode = "embedded"
    if "ams-site" in services["configurations"]:
      if "timeline.metrics.service.operation.mode" in services["configurations"]["ams-site"]["properties"]:
        operatingMode = services["configurations"]["ams-site"]["properties"]["timeline.metrics.service.operation.mode"]

    if operatingMode == "distributed":
      putAmsSiteProperty("timeline.metrics.service.watcher.disabled", 'true')
      putAmsSiteProperty("timeline.metrics.host.aggregator.ttl", 259200)
      putAmsHbaseSiteProperty("hbase.cluster.distributed", 'true')
      putAmsHbaseSiteProperty("hbase.unsafe.stream.capability.enforce", 'true')
    else:
      putAmsSiteProperty("timeline.metrics.service.watcher.disabled", 'false')
      putAmsSiteProperty("timeline.metrics.host.aggregator.ttl", 86400)
      putAmsHbaseSiteProperty("hbase.cluster.distributed", 'false')

    rootDir = "file:///var/lib/ambari-metrics-collector/hbase"
    tmpDir = "/var/lib/ambari-metrics-collector/hbase-tmp"
    zk_port_default = []
    if "ams-hbase-site" in services["configurations"]:
      if "hbase.rootdir" in services["configurations"]["ams-hbase-site"]["properties"]:
        rootDir = services["configurations"]["ams-hbase-site"]["properties"]["hbase.rootdir"]
      if "hbase.tmp.dir" in services["configurations"]["ams-hbase-site"]["properties"]:
        tmpDir = services["configurations"]["ams-hbase-site"]["properties"]["hbase.tmp.dir"]
      if "hbase.zookeeper.property.clientPort" in services["configurations"]["ams-hbase-site"]["properties"]:
        zk_port_default = services["configurations"]["ams-hbase-site"]["properties"]["hbase.zookeeper.property.clientPort"]

      # Skip recommendation item if default value is present
    if operatingMode == "distributed" and not "{{zookeeper_clientPort}}" in zk_port_default:
      zkPort = self.getZKPort(services)
      putAmsHbaseSiteProperty("hbase.zookeeper.property.clientPort", zkPort)
    elif operatingMode == "embedded" and not "{{zookeeper_clientPort}}" in zk_port_default:
      putAmsHbaseSiteProperty("hbase.zookeeper.property.clientPort", "61181")

    mountpoints = ["/"]
    for collectorHostName in amsCollectorHosts:
      for host in hosts["items"]:
        if host["Hosts"]["host_name"] == collectorHostName:
          mountpoints = self.getPreferredMountPoints(host["Hosts"])
          break
    isLocalRootDir = rootDir.startswith("file://") or (defaultFs.startswith("file://") and rootDir.startswith("/"))
    if isLocalRootDir:
      rootDir = re.sub("^file:///|/", "", rootDir, count=1)
      rootDir = "file://" + os.path.join(mountpoints[0], rootDir)
    tmpDir = re.sub("^file:///|/", "", tmpDir, count=1)
    if len(mountpoints) > 1 and isLocalRootDir:
      tmpDir = os.path.join(mountpoints[1], tmpDir)
    else:
      tmpDir = os.path.join(mountpoints[0], tmpDir)
    putAmsHbaseSiteProperty("hbase.tmp.dir", tmpDir)

    if operatingMode == "distributed":
      putAmsHbaseSiteProperty("hbase.rootdir", defaultFs + "/user/ams/hbase")

    if operatingMode == "embedded":
      if isLocalRootDir:
        putAmsHbaseSiteProperty("hbase.rootdir", rootDir)
      else:
        putAmsHbaseSiteProperty("hbase.rootdir", "file:///var/lib/ambari-metrics-collector/hbase")

    collector_heapsize, hbase_heapsize, total_sinks_count = self.getAmsMemoryRecommendation(services, hosts)

    putAmsEnvProperty("metrics_collector_heapsize", collector_heapsize)

    # blockCache = 0.3, memstore = 0.35, phoenix-server = 0.15, phoenix-client = 0.25
    putAmsHbaseSiteProperty("hfile.block.cache.size", 0.3)
    putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 134217728)
    putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.upperLimit", 0.35)
    putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.lowerLimit", 0.3)

    if len(amsCollectorHosts) > 1:
      pass
    else:
      # blockCache = 0.3, memstore = 0.3, phoenix-server = 0.2, phoenix-client = 0.3
      if total_sinks_count >= 2000:
        putAmsHbaseSiteProperty("hbase.regionserver.handler.count", 60)
        putAmsHbaseSiteProperty("hbase.regionserver.hlog.blocksize", 134217728)
        putAmsHbaseSiteProperty("hbase.regionserver.maxlogs", 64)
        putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 268435456)
        putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.upperLimit", 0.3)
        putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.lowerLimit", 0.25)
        putAmsHbaseSiteProperty("phoenix.query.maxGlobalMemoryPercentage", 20)
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 81920000)
        putAmsSiteProperty("phoenix.query.maxGlobalMemoryPercentage", 30)
        putAmsSiteProperty("timeline.metrics.service.resultset.fetchSize", 10000)
      elif total_sinks_count >= 500:
        putAmsHbaseSiteProperty("hbase.regionserver.handler.count", 60)
        putAmsHbaseSiteProperty("hbase.regionserver.hlog.blocksize", 134217728)
        putAmsHbaseSiteProperty("hbase.regionserver.maxlogs", 64)
        putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 268435456)
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 40960000)
        putAmsSiteProperty("timeline.metrics.service.resultset.fetchSize", 5000)
      else:
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 20480000)
      pass

    metrics_api_handlers = min(50, max(20, int(total_sinks_count / 100)))
    putAmsSiteProperty("timeline.metrics.service.handler.thread.count", metrics_api_handlers)

    # Distributed mode heap size
    if operatingMode == "distributed":
      hbase_heapsize = max(hbase_heapsize, 768)
      putHbaseEnvProperty("hbase_master_heapsize", "512")
      putHbaseEnvProperty("hbase_master_xmn_size", "102") #20% of 512 heap size
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_heapsize)
      putHbaseEnvProperty("regionserver_xmn_size", round_to_n(0.15*hbase_heapsize,64))
    else:
      # Embedded mode heap size : master + regionserver
      hbase_rs_heapsize = 768
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_rs_heapsize)
      putHbaseEnvProperty("hbase_master_heapsize", hbase_heapsize)
      putHbaseEnvProperty("hbase_master_xmn_size", round_to_n(0.15*(hbase_heapsize+hbase_rs_heapsize),64))

    # If no local DN in distributed mode
    if operatingMode == "distributed":
      dn_hosts = self.getComponentHostNames(services, "HDFS", "DATANODE")
      # call by Kerberos wizard sends only the service being affected
      # so it is possible for dn_hosts to be None but not amsCollectorHosts
      if dn_hosts and len(dn_hosts) > 0:
        if set(amsCollectorHosts).intersection(dn_hosts):
          collector_cohosted_with_dn = "true"
        else:
          collector_cohosted_with_dn = "false"
        putAmsHbaseSiteProperty("dfs.client.read.shortcircuit", collector_cohosted_with_dn)

    #split points
    scriptDir = os.path.dirname(os.path.abspath(__file__))
    metricsDir = os.path.join(scriptDir, '../../../../common-services/AMBARI_METRICS/0.1.0/package')
    serviceMetricsDir = os.path.join(metricsDir, 'files', 'service-metrics')
    sys.path.append(os.path.join(metricsDir, 'scripts'))
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    from split_points import FindSplitPointsForAMSRegions

    ams_hbase_site = None
    ams_hbase_env = None

    # Overriden properties form the UI
    if "ams-hbase-site" in services["configurations"]:
      ams_hbase_site = services["configurations"]["ams-hbase-site"]["properties"]
    if "ams-hbase-env" in services["configurations"]:
       ams_hbase_env = services["configurations"]["ams-hbase-env"]["properties"]

    # Recommendations
    if not ams_hbase_site:
      ams_hbase_site = configurations["ams-hbase-site"]["properties"]
    if not ams_hbase_env:
      ams_hbase_env = configurations["ams-hbase-env"]["properties"]

    split_point_finder = FindSplitPointsForAMSRegions(
      ams_hbase_site, ams_hbase_env, serviceMetricsDir, operatingMode, servicesList)

    result = split_point_finder.get_split_points()
    precision_splits = ' '
    aggregate_splits = ' '
    if result.precision:
      precision_splits = result.precision
    if result.aggregate:
      aggregate_splits = result.aggregate
    putAmsSiteProperty("timeline.metrics.host.aggregate.splitpoints", ','.join(precision_splits))
    putAmsSiteProperty("timeline.metrics.cluster.aggregate.splitpoints", ','.join(aggregate_splits))

    component_grafana_exists = False
    for service in services['services']:
      if 'components' in service:
        for component in service['components']:
          if 'StackServiceComponents' in component:
            # If Grafana is installed the hostnames would indicate its location
            if 'METRICS_GRAFANA' in component['StackServiceComponents']['component_name'] and\
              len(component['StackServiceComponents']['hostnames']) != 0:
              component_grafana_exists = True
              break
    pass

    if not component_grafana_exists:
      putGrafanaPropertyAttribute("metrics_grafana_password", "visible", "false")

    pass

  def getHostNamesWithComponent(self, serviceName, componentName, services):
    """
    Returns the list of hostnames on which service component is installed
    """
    if services is not None and serviceName in [service["StackServices"]["service_name"] for service in services["services"]]:
      service = [serviceEntry for serviceEntry in services["services"] if serviceEntry["StackServices"]["service_name"] == serviceName][0]
      components = [componentEntry for componentEntry in service["components"] if componentEntry["StackServiceComponents"]["component_name"] == componentName]
      if (len(components) > 0 and len(components[0]["StackServiceComponents"]["hostnames"]) > 0):
        componentHostnames = components[0]["StackServiceComponents"]["hostnames"]
        return componentHostnames
    return []

  def getHostsWithComponent(self, serviceName, componentName, services, hosts):
    if services is not None and hosts is not None and serviceName in [service["StackServices"]["service_name"] for service in services["services"]]:
      service = [serviceEntry for serviceEntry in services["services"] if serviceEntry["StackServices"]["service_name"] == serviceName][0]
      components = [componentEntry for componentEntry in service["components"] if componentEntry["StackServiceComponents"]["component_name"] == componentName]
      if (len(components) > 0 and len(components[0]["StackServiceComponents"]["hostnames"]) > 0):
        componentHostnames = components[0]["StackServiceComponents"]["hostnames"]
        componentHosts = [host for host in hosts["items"] if host["Hosts"]["host_name"] in componentHostnames]
        return componentHosts
    return []

  def getHostWithComponent(self, serviceName, componentName, services, hosts):
    componentHosts = self.getHostsWithComponent(serviceName, componentName, services, hosts)
    if (len(componentHosts) > 0):
      return componentHosts[0]
    return None

  def getHostComponentsByCategories(self, hostname, categories, services, hosts):
    components = []
    if services is not None and hosts is not None:
      for service in services["services"]:
          components.extend([componentEntry for componentEntry in service["components"]
                              if componentEntry["StackServiceComponents"]["component_category"] in categories
                              and hostname in componentEntry["StackServiceComponents"]["hostnames"]])
    return components

  def getZKHostPortString(self, services, include_port=True):
    """
    Returns the comma delimited string of zookeeper server host with the configure port installed in a cluster
    Example: zk.host1.org:2181,zk.host2.org:2181,zk.host3.org:2181
    include_port boolean param -> If port is also needed.
    """
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    include_zookeeper = "ZOOKEEPER" in servicesList
    zookeeper_host_port = ''

    if include_zookeeper:
      zookeeper_hosts = self.getHostNamesWithComponent("ZOOKEEPER", "ZOOKEEPER_SERVER", services)
      zookeeper_host_port_arr = []

      if include_port:
        zookeeper_port = self.getZKPort(services)
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i] + ':' + zookeeper_port)
      else:
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i])

      zookeeper_host_port = ",".join(zookeeper_host_port_arr)
    return zookeeper_host_port

  def getZKPort(self, services):
    zookeeper_port = '2181'     #default port
    if 'zoo.cfg' in services['configurations'] and ('clientPort' in services['configurations']['zoo.cfg']['properties']):
      zookeeper_port = services['configurations']['zoo.cfg']['properties']['clientPort']
    return zookeeper_port

  def getConfigurationClusterSummary(self, servicesList, hosts, components, services):

    hBaseInstalled = False
    cluster = {
      "cpu": 0,
      "disk": 0,
      "ram": 0,
      "hBaseInstalled": hBaseInstalled,
      "components": components
    }

    if len(hosts["items"]) > 0:
      nodeManagerHosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      # NodeManager host with least memory is generally used in calculations as it will work in larger hosts.
      if nodeManagerHosts is not None and len(nodeManagerHosts) > 0:
        nodeManagerHost = nodeManagerHosts[0];
        for nmHost in nodeManagerHosts:
          if nmHost["Hosts"]["total_mem"] < nodeManagerHost["Hosts"]["total_mem"]:
            nodeManagerHost = nmHost
        host = nodeManagerHost["Hosts"]
        cluster["referenceNodeManagerHost"] = host
      else:
        host = hosts["items"][0]["Hosts"]
      cluster["referenceHost"] = host
      cluster["cpu"] = host["cpu_count"]
      cluster["disk"] = len(host["disk_info"])
      cluster["ram"] = int(host["total_mem"] / (1024 * 1024))

    ramRecommendations = [
      {"os":1, "hbase":1},
      {"os":2, "hbase":1},
      {"os":2, "hbase":2},
      {"os":4, "hbase":4},
      {"os":6, "hbase":8},
      {"os":8, "hbase":8},
      {"os":8, "hbase":8},
      {"os":12, "hbase":16},
      {"os":24, "hbase":24},
      {"os":32, "hbase":32},
      {"os":64, "hbase":32}
    ]
    index = {
      cluster["ram"] <= 4: 0,
      4 < cluster["ram"] <= 8: 1,
      8 < cluster["ram"] <= 16: 2,
      16 < cluster["ram"] <= 24: 3,
      24 < cluster["ram"] <= 48: 4,
      48 < cluster["ram"] <= 64: 5,
      64 < cluster["ram"] <= 72: 6,
      72 < cluster["ram"] <= 96: 7,
      96 < cluster["ram"] <= 128: 8,
      128 < cluster["ram"] <= 256: 9,
      256 < cluster["ram"]: 10
    }[1]


    cluster["reservedRam"] = ramRecommendations[index]["os"]
    cluster["hbaseRam"] = ramRecommendations[index]["hbase"]


    cluster["minContainerSize"] = {
      cluster["ram"] <= 4: 256,
      4 < cluster["ram"] <= 8: 512,
      8 < cluster["ram"] <= 24: 1024,
      24 < cluster["ram"]: 2048
    }[1]

    totalAvailableRam = cluster["ram"] - cluster["reservedRam"]
    if cluster["hBaseInstalled"]:
      totalAvailableRam -= cluster["hbaseRam"]
    cluster["totalAvailableRam"] = max(512, totalAvailableRam * 1024)
    '''containers = max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))'''
    cluster["containers"] = round(max(3,
                                min(2 * cluster["cpu"],
                                    min(ceil(1.8 * cluster["disk"]),
                                            cluster["totalAvailableRam"] / cluster["minContainerSize"]))))

    '''ramPerContainers = max(2GB, RAM - reservedRam - hBaseRam) / containers'''
    cluster["ramPerContainer"] = abs(cluster["totalAvailableRam"] / cluster["containers"])
    '''If greater than 1GB, value will be in multiples of 512.'''
    if cluster["ramPerContainer"] > 1024:
      cluster["ramPerContainer"] = int(cluster["ramPerContainer"] / 512) * 512

    cluster["mapMemory"] = int(cluster["ramPerContainer"])
    cluster["reduceMemory"] = cluster["ramPerContainer"]
    cluster["amMemory"] = max(cluster["mapMemory"], cluster["reduceMemory"])

    return cluster

  def getServiceConfigurationValidators(self):
    return {
      "KAFKA": {"ranger-kafka-plugin-properties": self.validateKafkaRangerPluginConfigurations,
                "kafka-broker": self.validateKAFKAConfigurations},
      "STORM": {"storm-site": self.validateStormConfigurations,
                "ranger-storm-plugin-properties": self.validateStormRangerPluginConfigurations},
      "AMBARI_METRICS": {"ams-hbase-site": self.validateAmsHbaseSiteConfigurations,
              "ams-hbase-env": self.validateAmsHbaseEnvConfigurations,
              "ams-site": self.validateAmsSiteConfigurations},
      "RANGER": {"ranger-env": self.validateRangerConfigurationsEnv,
                 "admin-properties": self.validateRangerAdminConfigurations,
                 "ranger-tagsync-site": self.validateRangerTagsyncConfigurations},
      "NIFI": {"ranger-nifi-plugin-properties": self.validateNiFiRangerPluginConfigurations,
               "nifi-ambari-ssl-config": self.validateNiFiSslProperties }
    }

  def recommendLogsearchConfigurations(self, configurations, clusterData, services, hosts):
    putLogsearchProperty = self.putProperty(configurations, "logsearch-properties", services)
    infraSolrHosts = self.getComponentHostNames(services, "AMBARI_INFRA", "INFRA_SOLR")

    if infraSolrHosts is not None and len(infraSolrHosts) > 0 \
      and "logsearch-properties" in services["configurations"]:
      recommendedMinShards = len(infraSolrHosts)
      recommendedShards = 2 * len(infraSolrHosts)
      recommendedMaxShards = 3 * len(infraSolrHosts)
      # recommend number of shard
      putLogsearchAttribute = self.putPropertyAttribute(configurations, "logsearch-properties")
      putLogsearchAttribute('logsearch.collection.service.logs.numshards', 'minimum', recommendedMinShards)
      putLogsearchAttribute('logsearch.collection.service.logs.numshards', 'maximum', recommendedMaxShards)
      putLogsearchProperty("logsearch.collection.service.logs.numshards", recommendedShards)

      putLogsearchAttribute('logsearch.collection.audit.logs.numshards', 'minimum', recommendedMinShards)
      putLogsearchAttribute('logsearch.collection.audit.logs.numshards', 'maximum', recommendedMaxShards)
      putLogsearchProperty("logsearch.collection.audit.logs.numshards", recommendedShards)
      # recommend replication factor
      replicationReccomendFloat = math.log(len(infraSolrHosts), 5)
      recommendedReplicationFactor = int(1 + math.floor(replicationReccomendFloat))
      putLogsearchProperty("logsearch.collection.service.logs.replication.factor", recommendedReplicationFactor)
      putLogsearchProperty("logsearch.collection.audit.logs.replication.factor", recommendedReplicationFactor)

  def validateMinMax(self, items, recommendedDefaults, configurations):

    # required for casting to the proper numeric type before comparison
    def convertToNumber(number):
      try:
        return int(number)
      except ValueError:
        return float(number)

    for configName in configurations:
      validationItems = []
      if configName in recommendedDefaults and "property_attributes" in recommendedDefaults[configName]:
        for propertyName in recommendedDefaults[configName]["property_attributes"]:
          if propertyName in configurations[configName]["properties"]:
            if "maximum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              maxValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["maximum"])
              if userValue > maxValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is greater than the recommended maximum of {0} ".format(maxValue))}])
            if "minimum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                    propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              minValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["minimum"])
              if userValue < minValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is less than the recommended minimum of {0} ".format(minValue))}])
      items.extend(self.toConfigurationValidationProblems(validationItems, configName))
    pass

  def validateAmsSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []

    op_mode = properties.get("timeline.metrics.service.operation.mode")
    correct_op_mode_item = None
    if op_mode not in ("embedded", "distributed"):
      correct_op_mode_item = self.getErrorItem("Correct value should be set.")
      pass

    validationItems.extend([{"config-name":'timeline.metrics.service.operation.mode', "item": correct_op_mode_item }])
    return self.toConfigurationValidationProblems(validationItems, "ams-site")

  def validateAmsHbaseSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
    ams_site = getSiteProperties(configurations, "ams-site")
    core_site = getSiteProperties(configurations, "core-site")

    collector_heapsize, hbase_heapsize, total_sinks_count = self.getAmsMemoryRecommendation(services, hosts)
    recommendedDiskSpace = 10485760
    # TODO validate configuration for multiple AMBARI_METRICS collectors
    if len(amsCollectorHosts) > 1:
      pass
    else:
      if total_sinks_count > 2000:
        recommendedDiskSpace  = 104857600  # * 1k == 100 Gb
      elif total_sinks_count > 500:
        recommendedDiskSpace  = 52428800  # * 1k == 50 Gb
      elif total_sinks_count > 250:
        recommendedDiskSpace  = 20971520  # * 1k == 20 Gb

    validationItems = []

    rootdir_item = None
    op_mode = ams_site.get("timeline.metrics.service.operation.mode")
    default_fs = core_site.get("fs.defaultFS") if core_site else "file:///"
    hbase_rootdir = properties.get("hbase.rootdir")
    hbase_tmpdir = properties.get("hbase.tmp.dir")
    distributed = properties.get("hbase.cluster.distributed")
    is_local_root_dir = hbase_rootdir.startswith("file://") or (default_fs.startswith("file://") and hbase_rootdir.startswith("/"))

    if op_mode == "distributed" and is_local_root_dir:
      rootdir_item = self.getWarnItem("In distributed mode hbase.rootdir should point to HDFS.")
    elif op_mode == "embedded":
      if distributed.lower() == "false" and hbase_rootdir.startswith('/') or hbase_rootdir.startswith("hdfs://"):
        rootdir_item = self.getWarnItem("In embedded mode hbase.rootdir cannot point to schemaless values or HDFS, "
                                        "Example - file:// for localFS")
      pass

    distributed_item = None
    if op_mode == "distributed" and not distributed.lower() == "true":
      distributed_item = self.getErrorItem("hbase.cluster.distributed property should be set to true for "
                                           "distributed mode")
    if op_mode == "embedded" and distributed.lower() == "true":
      distributed_item = self.getErrorItem("hbase.cluster.distributed property should be set to false for embedded mode")

    hbase_zk_client_port = properties.get("hbase.zookeeper.property.clientPort")
    zkPort = self.getZKPort(services)
    hbase_zk_client_port_item = None
    if distributed.lower() == "true" and op_mode == "distributed" and \
        hbase_zk_client_port != zkPort and hbase_zk_client_port != "{{zookeeper_clientPort}}":
      hbase_zk_client_port_item = self.getErrorItem("In AMS distributed mode, hbase.zookeeper.property.clientPort "
                                                    "should be the cluster zookeeper server port : {0}".format(zkPort))

    if distributed.lower() == "false" and op_mode == "embedded" and \
        hbase_zk_client_port == zkPort and hbase_zk_client_port != "{{zookeeper_clientPort}}":
      hbase_zk_client_port_item = self.getErrorItem("In AMS embedded mode, hbase.zookeeper.property.clientPort "
                                                    "should be a different port than cluster zookeeper port."
                                                    "(default:61181)")

    validationItems.extend([{"config-name":'hbase.rootdir', "item": rootdir_item },
                            {"config-name":'hbase.cluster.distributed', "item": distributed_item },
                            {"config-name":'hbase.zookeeper.property.clientPort', "item": hbase_zk_client_port_item }])

    for collectorHostName in amsCollectorHosts:
      for host in hosts["items"]:
        if host["Hosts"]["host_name"] == collectorHostName:
          if op_mode == 'embedded' or is_local_root_dir:
            validationItems.extend([{"config-name": 'hbase.rootdir', "item": self.validatorEnoughDiskSpace(properties, 'hbase.rootdir', host["Hosts"], recommendedDiskSpace)}])
            validationItems.extend([{"config-name": 'hbase.rootdir', "item": self.validatorNotRootFs(properties, recommendedDefaults, 'hbase.rootdir', host["Hosts"])}])
            validationItems.extend([{"config-name": 'hbase.tmp.dir', "item": self.validatorNotRootFs(properties, recommendedDefaults, 'hbase.tmp.dir', host["Hosts"])}])

          dn_hosts = self.getComponentHostNames(services, "HDFS", "DATANODE")
          if is_local_root_dir:
            mountPoints = []
            for mountPoint in host["Hosts"]["disk_info"]:
              mountPoints.append(mountPoint["mountpoint"])
            hbase_rootdir_mountpoint = getMountPointForDir(hbase_rootdir, mountPoints)
            hbase_tmpdir_mountpoint = getMountPointForDir(hbase_tmpdir, mountPoints)
            preferred_mountpoints = self.getPreferredMountPoints(host['Hosts'])
            # hbase.rootdir and hbase.tmp.dir shouldn't point to the same partition
            # if multiple preferred_mountpoints exist
            if hbase_rootdir_mountpoint == hbase_tmpdir_mountpoint and \
              len(preferred_mountpoints) > 1:
              item = self.getWarnItem("Consider not using {0} partition for storing metrics temporary data. "
                                      "{0} partition is already used as hbase.rootdir to store metrics data".format(hbase_tmpdir_mountpoint))
              validationItems.extend([{"config-name":'hbase.tmp.dir', "item": item}])

            # if METRICS_COLLECTOR is co-hosted with DATANODE
            # cross-check dfs.datanode.data.dir and hbase.rootdir
            # they shouldn't share same disk partition IO
            hdfs_site = getSiteProperties(configurations, "hdfs-site")
            dfs_datadirs = hdfs_site.get("dfs.datanode.data.dir").split(",") if hdfs_site and "dfs.datanode.data.dir" in hdfs_site else []
            if dn_hosts and collectorHostName in dn_hosts and ams_site and \
              dfs_datadirs and len(preferred_mountpoints) > len(dfs_datadirs):
              for dfs_datadir in dfs_datadirs:
                dfs_datadir_mountpoint = getMountPointForDir(dfs_datadir, mountPoints)
                if dfs_datadir_mountpoint == hbase_rootdir_mountpoint:
                  item = self.getWarnItem("Consider not using {0} partition for storing metrics data. "
                                          "{0} is already used by datanode to store HDFS data".format(hbase_rootdir_mountpoint))
                  validationItems.extend([{"config-name": 'hbase.rootdir', "item": item}])
                  break
          # If no local DN in distributed mode
          elif collectorHostName not in dn_hosts and distributed.lower() == "true":
            item = self.getWarnItem("It's recommended to install Datanode component on {0} "
                                    "to speed up IO operations between HDFS and Metrics "
                                    "Collector in distributed mode ".format(collectorHostName))
            validationItems.extend([{"config-name": "hbase.cluster.distributed", "item": item}])
          # Short circuit read should be enabled in distibuted mode
          # if local DN installed
          else:
            validationItems.extend([{"config-name": "dfs.client.read.shortcircuit", "item": self.validatorEqualsToRecommendedItem(properties, recommendedDefaults, "dfs.client.read.shortcircuit")}])

    return self.toConfigurationValidationProblems(validationItems, "ams-hbase-site")

  def validateStormConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList and "metrics.reporter.register" in properties and \
      "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter" not in properties.get("metrics.reporter.register"):

      validationItems.append({"config-name": 'metrics.reporter.register',
                              "item": self.getWarnItem(
                                "Should be set to org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter to report the metrics to Ambari Metrics service.")})

    return self.toConfigurationValidationProblems(validationItems, "storm-site")

  def validateStormRangerPluginConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-storm-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-storm-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ranger_plugin_enabled.lower() == 'yes':
      # ranger-storm-plugin must be enabled in ranger-env
      ranger_env = getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-storm-plugin-enabled' in ranger_env or \
              ranger_env['ranger-storm-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-storm-plugin-enabled',
                                "item": self.getWarnItem(
                                    "ranger-storm-plugin-properties/ranger-storm-plugin-enabled must correspond ranger-env/ranger-storm-plugin-enabled")})
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()) and not 'KERBEROS' in servicesList:
      validationItems.append({"config-name": "ranger-storm-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Storm plugin should not be enabled in non-kerberos environment.")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-storm-plugin-properties")

  def validateKafkaRangerPluginConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ranger_plugin_enabled.lower() == 'yes':
      # ranger-kafka-plugin must be enabled in ranger-env
      ranger_env = getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-kafka-plugin-enabled' in ranger_env or \
              ranger_env['ranger-kafka-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-kafka-plugin-enabled',
                                "item": self.getWarnItem(
                                    "ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled must correspond ranger-env/ranger-kafka-plugin-enabled")})

    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'yes') and not 'KERBEROS' in servicesList:
      validationItems.append({"config-name": "ranger-kafka-plugin-enabled",
                              "item": self.getWarnItem(
                              "Ranger Kafka plugin should not be enabled in non-kerberos environment.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-kafka-plugin-properties")

  def validateRangerAdminConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_site = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'RANGER' in servicesList and 'policymgr_external_url' in ranger_site:
      policymgr_mgr_url = ranger_site['policymgr_external_url']
      if policymgr_mgr_url.endswith('/'):
        validationItems.append({'config-name':'policymgr_external_url',
                               'item':self.getWarnItem('Ranger External URL should not contain trailing slash "/"')})
    return self.toConfigurationValidationProblems(validationItems,'admin-properties')

  def validateRangerConfigurationsEnv(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_env_properties = properties
    validationItems = []
    security_enabled = False

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'KERBEROS' in servicesList:
      security_enabled = True

    if "ranger-storm-plugin-enabled" in ranger_env_properties and ranger_env_properties['ranger-storm-plugin-enabled'].lower() == 'yes' and not security_enabled:
      validationItems.append({"config-name": "ranger-storm-plugin-enabled",
                              "item": self.getWarnItem("Ranger Storm plugin should not be enabled in non-kerberos environment.")})
    if "ranger-kafka-plugin-enabled" in ranger_env_properties and ranger_env_properties["ranger-kafka-plugin-enabled"].lower() == 'yes' and not security_enabled:
      validationItems.append({"config-name": "ranger-kafka-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Kafka plugin should not be enabled in non-kerberos environment.")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-env")

  def validateRangerTagsyncConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_tagsync_properties = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    has_atlas = False
    if "RANGER" in servicesList:
      has_atlas = not "ATLAS" in servicesList

      if has_atlas and 'ranger.tagsync.source.atlas' in ranger_tagsync_properties and \
              ranger_tagsync_properties['ranger.tagsync.source.atlas'].lower() == 'true':
        validationItems.append({"config-name": "ranger.tagsync.source.atlas",
                                "item": self.getWarnItem(
                                    "Need to Install ATLAS service to set ranger.tagsync.source.atlas as true.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-tagsync-site")

  def validateAmsHbaseEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    ams_env = getSiteProperties(configurations, "ams-env")
    amsHbaseSite = getSiteProperties(configurations, "ams-hbase-site")
    validationItems = []
    mb = 1024 * 1024
    gb = 1024 * mb

    regionServerItem = self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hbase_regionserver_heapsize") ## FIXME if new service added
    if regionServerItem:
      validationItems.extend([{"config-name": "hbase_regionserver_heapsize", "item": regionServerItem}])

    hbaseMasterHeapsizeItem = self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hbase_master_heapsize")
    if hbaseMasterHeapsizeItem:
      validationItems.extend([{"config-name": "hbase_master_heapsize", "item": hbaseMasterHeapsizeItem}])

    logDirItem = self.validatorEqualsPropertyItem(properties, "hbase_log_dir", ams_env, "metrics_collector_log_dir")
    if logDirItem:
      validationItems.extend([{"config-name": "hbase_log_dir", "item": logDirItem}])

    collector_heapsize = to_number(ams_env.get("metrics_collector_heapsize"))
    hbase_master_heapsize = to_number(properties["hbase_master_heapsize"])
    hbase_master_xmn_size = to_number(properties["hbase_master_xmn_size"])
    hbase_regionserver_heapsize = to_number(properties["hbase_regionserver_heapsize"])
    hbase_regionserver_xmn_size = to_number(properties["regionserver_xmn_size"])

    # Validate Xmn settings.
    masterXmnItem = None
    regionServerXmnItem = None
    is_hbase_distributed = amsHbaseSite.get("hbase.cluster.distributed").lower() == 'true'

    if is_hbase_distributed:
      minMasterXmn = 0.12 * hbase_master_heapsize
      maxMasterXmn = 0.2 * hbase_master_heapsize
      if hbase_master_xmn_size < minMasterXmn:
        masterXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                         "(12% of hbase_master_heapsize)".format(int(ceil(minMasterXmn))))

      if hbase_master_xmn_size > maxMasterXmn:
        masterXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                         "(20% of hbase_master_heapsize)".format(int(floor(maxMasterXmn))))

      minRegionServerXmn = 0.12 * hbase_regionserver_heapsize
      maxRegionServerXmn = 0.2 * hbase_regionserver_heapsize
      if hbase_regionserver_xmn_size < minRegionServerXmn:
        regionServerXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                               "(12% of hbase_regionserver_heapsize)"
                                               .format(int(ceil(minRegionServerXmn))))

      if hbase_regionserver_xmn_size > maxRegionServerXmn:
        regionServerXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                               "(20% of hbase_regionserver_heapsize)"
                                               .format(int(floor(maxRegionServerXmn))))
    else:
      minMasterXmn = 0.12 * (hbase_master_heapsize + hbase_regionserver_heapsize)
      maxMasterXmn = 0.2 *  (hbase_master_heapsize + hbase_regionserver_heapsize)
      if hbase_master_xmn_size < minMasterXmn:
        masterXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                         "(12% of hbase_master_heapsize + hbase_regionserver_heapsize)"
                                         .format(int(ceil(minMasterXmn))))

      if hbase_master_xmn_size > maxMasterXmn:
        masterXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                         "(20% of hbase_master_heapsize + hbase_regionserver_heapsize)"
                                         .format(int(floor(maxMasterXmn))))
    if masterXmnItem:
      validationItems.extend([{"config-name": "hbase_master_xmn_size", "item": masterXmnItem}])

    if regionServerXmnItem:
      validationItems.extend([{"config-name": "regionserver_xmn_size", "item": regionServerXmnItem}])

    if hbaseMasterHeapsizeItem is None:
      hostMasterComponents = {}

      for service in services["services"]:
        for component in service["components"]:
          if component["StackServiceComponents"]["hostnames"] is not None:
            for hostName in component["StackServiceComponents"]["hostnames"]:
              if self.isMasterComponent(component):
                if hostName not in hostMasterComponents.keys():
                  hostMasterComponents[hostName] = []
                hostMasterComponents[hostName].append(component["StackServiceComponents"]["component_name"])

      amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
      for collectorHostName in amsCollectorHosts:
        for host in hosts["items"]:
          if host["Hosts"]["host_name"] == collectorHostName:
            # AMS Collector co-hosted with other master components in bigger clusters
            if len(hosts['items']) > 31 and \
                            len(hostMasterComponents[collectorHostName]) > 2 and \
                            host["Hosts"]["total_mem"] < 32*mb: # < 32Gb(total_mem in k)
              masterHostMessage = "Host {0} is used by multiple master components ({1}). " \
                                  "It is recommended to use a separate host for the " \
                                  "Ambari Metrics Collector component and ensure " \
                                  "the host has sufficient memory available."

              hbaseMasterHeapsizeItem = self.getWarnItem(masterHostMessage.format(
                  collectorHostName, str(", ".join(hostMasterComponents[collectorHostName]))))
              if hbaseMasterHeapsizeItem:
                validationItems.extend([{"config-name": "hbase_master_heapsize", "item": hbaseMasterHeapsizeItem}])

            # Check for unused RAM on AMS Collector node
            hostComponents = []
            for service in services["services"]:
              for component in service["components"]:
                if component["StackServiceComponents"]["hostnames"] is not None:
                  if collectorHostName in component["StackServiceComponents"]["hostnames"]:
                    hostComponents.append(component["StackServiceComponents"]["component_name"])

            requiredMemory = getMemorySizeRequired(hostComponents, configurations)
            unusedMemory = host["Hosts"]["total_mem"] * 1024 - requiredMemory # in bytes
            if unusedMemory > 4*gb:  # warn user, if more than 4GB RAM is unused
              heapPropertyToIncrease = "hbase_regionserver_heapsize" if is_hbase_distributed else "hbase_master_heapsize"
              xmnPropertyToIncrease = "regionserver_xmn_size" if is_hbase_distributed else "hbase_master_xmn_size"
              recommended_collector_heapsize = int((unusedMemory - 4*gb)/5) + collector_heapsize*mb
              recommended_hbase_heapsize = int((unusedMemory - 4*gb)*4/5) + to_number(properties.get(heapPropertyToIncrease))*mb
              recommended_hbase_heapsize = min(32*gb, recommended_hbase_heapsize) #Make sure heapsize <= 32GB
              recommended_xmn_size = round_to_n(0.12*recommended_hbase_heapsize/mb,128)

              if collector_heapsize < recommended_collector_heapsize or \
                  to_number(properties[heapPropertyToIncrease]) < recommended_hbase_heapsize:
                collectorHeapsizeItem = self.getWarnItem("{0} MB RAM is unused on the host {1} based on components " \
                                                         "assigned. Consider allocating  {2} MB to " \
                                                         "metrics_collector_heapsize in ams-env, " \
                                                         "{3} MB to {4} in ams-hbase-env"
                                                         .format(unusedMemory/mb, collectorHostName,
                                                                 recommended_collector_heapsize/mb,
                                                                 recommended_hbase_heapsize/mb,
                                                                 heapPropertyToIncrease))
                validationItems.extend([{"config-name": heapPropertyToIncrease, "item": collectorHeapsizeItem}])

              if to_number(properties[xmnPropertyToIncrease]) < recommended_hbase_heapsize:
                xmnPropertyToIncreaseItem = self.getWarnItem("Consider allocating {0} MB to use up some unused memory "
                                                             "on host".format(recommended_xmn_size))
                validationItems.extend([{"config-name": xmnPropertyToIncrease, "item": xmnPropertyToIncreaseItem}])
      pass

    return self.toConfigurationValidationProblems(validationItems, "ams-hbase-env")

  def validateKAFKAConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    kafka_broker = properties
    validationItems = []

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'authorizer.class.name'
    prop_val = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if kafka_broker[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                    "If Ranger Kafka Plugin is enabled." \
                                    "{0} needs to be set to {1}".format(prop_name,prop_val))})

    return self.toConfigurationValidationProblems(validationItems, "kafka-broker")

  def __find_ca(self, services):
    for service in services['services']:
      if 'components' in service:
        for component in service['components']:
          stackServiceComponent = component['StackServiceComponents']
          if 'NIFI_CA' == stackServiceComponent['component_name'] and stackServiceComponent['hostnames']:
            return True
    return False
    
  def validateConfigurationsForSite(self, configurations, recommendedDefaults, services, hosts, siteName, method):
    if siteName == 'nifi-ambari-ssl-config':
      return method(self.getSiteProperties(configurations, siteName), None, configurations, services, hosts)
    else:
      return DefaultStackAdvisor.validateConfigurationsForSite(self, configurations, recommendedDefaults, services, hosts, siteName, method)

  def validateNiFiSslProperties(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ssl_enabled = properties['nifi.node.ssl.isenabled'] and str(properties['nifi.node.ssl.isenabled']).lower() != 'false'
    if (self.__find_ca(services)):
      if not properties['nifi.toolkit.tls.token']:
        validationItems.append({"config-name": 'nifi.toolkit.tls.token', 'item': self.getErrorItem('If NiFi Certificate Authority is used, nifi.toolkit.tls.token must be set')})
      if not ssl_enabled:
        validationItems.append({"config-name": 'nifi.node.ssl.isenabled', 'item': self.getWarnItem('For NiFi Certificate Authority to be useful, ssl should be enabled')})
    else:
      if properties['nifi.toolkit.tls.token']:
        validationItems.append({"config-name": 'nifi.toolkit.tls.token', 'item': self.getWarnItem("If NiFi Certificate Authority is not used, nifi.toolkit.tls.token doesn't do anything.")})
      if ssl_enabled:
        if not properties['nifi.security.keystorePasswd']:
          validationItems.append({"config-name": 'nifi.security.keystorePasswd', 'item': self.getErrorItem('If NiFi Certificate Authority is not used and SSL is enabled, must specify nifi.security.keystorePasswd')})
        if not properties['nifi.security.keyPasswd']:
          validationItems.append({"config-name": 'nifi.security.keyPasswd', 'item': self.getErrorItem('If NiFi Certificate Authority is not used and SSL is enabled, must specify nifi.security.keyPasswd')})
        if not properties['nifi.security.truststorePasswd']:
          validationItems.append({"config-name": 'nifi.security.truststorePasswd', 'item': self.getErrorItem('If NiFi Certificate Authority is not used and SSL is enabled, must specify nifi.security.truststorePasswd')})
        if not properties['nifi.security.keystoreType']:
          validationItems.append({"config-name": 'nifi.security.keystoreType', 'item': self.getErrorItem('If NiFi Certificate Authority is not used and SSL is enabled, must specify nifi.security.keystoreType')})
        if not properties['nifi.security.truststoreType']:
          validationItems.append({"config-name": 'nifi.security.truststoreType', 'item': self.getErrorItem('If NiFi Certificate Authority is not used and SSL is enabled, must specify nifi.security.truststoreType')})
    return self.toConfigurationValidationProblems(validationItems, "nifi-ambari-ssl-config")

  def validateNiFiRangerPluginConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-nifi-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-nifi-plugin-enabled'] if ranger_plugin_properties else 'No'

    if ranger_plugin_enabled.lower() == 'yes':
      ranger_env = getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-nifi-plugin-enabled' in ranger_env or \
                      ranger_env['ranger-nifi-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-nifi-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-nifi-plugin-properties/ranger-nifi-plugin-enabled must correspond ranger-env/ranger-nifi-plugin-enabled")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-nifi-plugin-properties")

  def validateServiceConfigurations(self, serviceName):
    return self.getServiceConfigurationValidators().get(serviceName, None)

  def getWarnItem(self, message):
    return {"level": "WARN", "message": message}

  def getErrorItem(self, message):
    return {"level": "ERROR", "message": message}

  def getPreferredMountPoints(self, hostInfo):

    # '/etc/resolv.conf', '/etc/hostname', '/etc/hosts' are docker specific mount points
    undesirableMountPoints = ["/", "/home", "/etc/resolv.conf", "/etc/hosts",
                              "/etc/hostname", "/tmp"]
    undesirableFsTypes = ["devtmpfs", "tmpfs", "vboxsf", "CDFS"]
    mountPoints = []
    if hostInfo and "disk_info" in hostInfo:
      mountPointsDict = {}
      for mountpoint in hostInfo["disk_info"]:
        if not (mountpoint["mountpoint"] in undesirableMountPoints or
                mountpoint["mountpoint"].startswith(("/boot", "/mnt")) or
                mountpoint["type"] in undesirableFsTypes or
                mountpoint["available"] == str(0)):
          mountPointsDict[mountpoint["mountpoint"]] = to_number(mountpoint["available"])
      if mountPointsDict:
        mountPoints = sorted(mountPointsDict, key=mountPointsDict.get, reverse=True)
    mountPoints.append("/")
    return mountPoints

  def validatorNotRootFs(self, properties, recommendedDefaults, propertyName, hostInfo):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    dir = properties[propertyName]
    if not dir.startswith("file://") or dir == recommendedDefaults.get(propertyName):
      return None

    dir = re.sub("^file://", "", dir, count=1)
    mountPoints = []
    for mountPoint in hostInfo["disk_info"]:
      mountPoints.append(mountPoint["mountpoint"])
    mountPoint = getMountPointForDir(dir, mountPoints)

    if "/" == mountPoint and self.getPreferredMountPoints(hostInfo)[0] != mountPoint:
      return self.getWarnItem("It is not recommended to use root partition for {0}".format(propertyName))

    return None

  def validatorEnoughDiskSpace(self, properties, propertyName, hostInfo, reqiuredDiskSpace):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    dir = properties[propertyName]
    if not dir.startswith("file://"):
      return None

    dir = re.sub("^file://", "", dir, count=1)
    mountPoints = {}
    for mountPoint in hostInfo["disk_info"]:
      mountPoints[mountPoint["mountpoint"]] = to_number(mountPoint["available"])
    mountPoint = getMountPointForDir(dir, mountPoints.keys())

    if not mountPoints:
      return self.getErrorItem("No disk info found on host %s" % hostInfo["host_name"])

    if mountPoint is None:
      return self.getErrorItem("No mount point in directory %s. Mount points: %s" % (dir, ', '.join(mountPoints.keys())))

    if mountPoints[mountPoint] < reqiuredDiskSpace:
      msg = "Ambari Metrics disk space requirements not met. \n" \
            "Recommended disk space for partition {0} is {1}G"
      return self.getWarnItem(msg.format(mountPoint, reqiuredDiskSpace/1048576)) # in Gb
    return None

  def validatorLessThenDefaultValue(self, properties, recommendedDefaults, propertyName):
    if propertyName not in recommendedDefaults:
      # If a property name exists in say hbase-env and hbase-site (which is allowed), then it will exist in the
      # "properties" dictionary, but not necessarily in the "recommendedDefaults" dictionary". In this case, ignore it.
      return None

    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = to_number(properties[propertyName])
    if value is None:
      return self.getErrorItem("Value should be integer")
    defaultValue = to_number(recommendedDefaults[propertyName])
    if defaultValue is None:
      return None
    if value < defaultValue:
      return self.getWarnItem("Value is less than the recommended default of {0}".format(defaultValue))
    return None

  def validatorEqualsPropertyItem(self, properties1, propertyName1,
                                  properties2, propertyName2,
                                  emptyAllowed=False):
    if not propertyName1 in properties1:
      return self.getErrorItem("Value should be set for %s" % propertyName1)
    if not propertyName2 in properties2:
      return self.getErrorItem("Value should be set for %s" % propertyName2)
    value1 = properties1.get(propertyName1)
    if value1 is None and not emptyAllowed:
      return self.getErrorItem("Empty value for %s" % propertyName1)
    value2 = properties2.get(propertyName2)
    if value2 is None and not emptyAllowed:
      return self.getErrorItem("Empty value for %s" % propertyName2)
    if value1 != value2:
      return self.getWarnItem("It is recommended to set equal values "
             "for properties {0} and {1}".format(propertyName1, propertyName2))

    return None

  def validatorEqualsToRecommendedItem(self, properties, recommendedDefaults,
                                       propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set for %s" % propertyName)
    value = properties.get(propertyName)
    if not propertyName in recommendedDefaults:
      return self.getErrorItem("Value should be recommended for %s" % propertyName)
    recommendedValue = recommendedDefaults.get(propertyName)
    if value != recommendedValue:
      return self.getWarnItem("It is recommended to set value {0} "
             "for property {1}".format(recommendedValue, propertyName))
    return None

  def validateMinMemorySetting(self, properties, defaultValue, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")

    value = properties[propertyName]
    if value is None:
      return self.getErrorItem("Value can't be null or undefined")
    try:
      valueInt = to_number(value)
      # TODO: generify for other use cases
      defaultValueInt = int(str(defaultValue).strip())
      if valueInt < defaultValueInt:
        return self.getWarnItem("Value is less than the minimum recommended default of -Xmx" + str(defaultValue))
    except:
      return None

    return None

  def validateXmxValue(self, properties, recommendedDefaults, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = properties[propertyName]
    defaultValue = recommendedDefaults[propertyName]
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")
    if not checkXmxValueFormat(value) and checkXmxValueFormat(defaultValue):
      # Xmx is in the default-value but not the value, should be an error
      return self.getErrorItem('Invalid value format')
    if not checkXmxValueFormat(defaultValue):
      # if default value does not contain Xmx, then there is no point in validating existing value
      return None
    valueInt = formatXmxSizeToBytes(getXmxSize(value))
    defaultValueXmx = getXmxSize(defaultValue)
    defaultValueInt = formatXmxSizeToBytes(defaultValueXmx)
    if valueInt < defaultValueInt:
      return self.getWarnItem("Value is less than the recommended default of -Xmx" + defaultValueXmx)
    return None

  def getMastersWithMultipleInstances(self):
    return ['ZOOKEEPER_SERVER', 'METRICS_COLLECTOR']

  def getNotValuableComponents(self):
    return ['METRICS_MONITOR']

  def getNotPreferableOnServerComponents(self):
    return ['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'METRICS_COLLECTOR']

  def getCardinalitiesDict(self, hosts):
    return {
      'ZOOKEEPER_SERVER': {"min": 3},
      'METRICS_COLLECTOR': {"min": 1}
    }

  def getComponentLayoutSchemes(self):
    return {
      'METRICS_COLLECTOR': {3: 2, 6: 2, 31: 3, "else": 5},
    }

  def get_system_min_uid(self):
    login_defs = '/etc/login.defs'
    uid_min_tag = 'UID_MIN'
    comment_tag = '#'
    uid_min = uid_default = '1000'
    uid = None

    if os.path.exists(login_defs):
      with open(login_defs, 'r') as f:
        data = f.read().split('\n')
        # look for uid_min_tag in file
        uid = filter(lambda x: uid_min_tag in x, data)
        # filter all lines, where uid_min_tag was found in comments
        uid = filter(lambda x: x.find(comment_tag) > x.find(uid_min_tag) or x.find(comment_tag) == -1, uid)

      if uid is not None and len(uid) > 0:
        uid = uid[0]
        comment = uid.find(comment_tag)
        tag = uid.find(uid_min_tag)
        if comment == -1:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:].strip()
        elif comment > tag:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:comment].strip()

    # check result for value
    try:
      int(uid_min)
    except ValueError:
      return uid_default

    return uid_min

  def mergeValidators(self, parentValidators, childValidators):
    for service, configsDict in childValidators.iteritems():
      if service not in parentValidators:
        parentValidators[service] = {}
      parentValidators[service].update(configsDict)

  def checkSiteProperties(self, siteProperties, *propertyNames):
    """
    Check if properties defined in site properties.
    :param siteProperties: config properties dict
    :param *propertyNames: property names to validate
    :returns: True if all properties defined, in other cases returns False
    """
    if siteProperties is None:
      return False
    for name in propertyNames:
      if not (name in siteProperties):
        return False
    return True

def getOldValue(self, services, configType, propertyName):
  if services:
    if 'changed-configurations' in services.keys():
      changedConfigs = services["changed-configurations"]
      for changedConfig in changedConfigs:
        if changedConfig["type"] == configType and changedConfig["name"]== propertyName and "old_value" in changedConfig:
          return changedConfig["old_value"]
  return None

# Validation helper methods
def getSiteProperties(configurations, siteName):
  siteConfig = configurations.get(siteName)
  if siteConfig is None:
    return None
  return siteConfig.get("properties")

def getServicesSiteProperties(services, siteName):
  configurations = services.get("configurations")
  if not configurations:
    return None
  siteConfig = configurations.get(siteName)
  if siteConfig is None:
    return None
  return siteConfig.get("properties")

def to_number(s):
  try:
    return int(re.sub("\D", "", s))
  except ValueError:
    return None

def checkXmxValueFormat(value):
  p = re.compile('-Xmx(\d+)(b|k|m|g|p|t|B|K|M|G|P|T)?')
  matches = p.findall(value)
  return len(matches) == 1

def getXmxSize(value):
  p = re.compile("-Xmx(\d+)(.?)")
  result = p.findall(value)[0]
  if len(result) > 1:
    # result[1] - is a space or size formatter (b|k|m|g etc)
    return result[0] + result[1].lower()
  return result[0]

def formatXmxSizeToBytes(value):
  value = value.lower()
  if len(value) == 0:
    return 0
  modifier = value[-1]

  if modifier == ' ' or modifier in "0123456789":
    modifier = 'b'
  m = {
    modifier == 'b': 1,
    modifier == 'k': 1024,
    modifier == 'm': 1024 * 1024,
    modifier == 'g': 1024 * 1024 * 1024,
    modifier == 't': 1024 * 1024 * 1024 * 1024,
    modifier == 'p': 1024 * 1024 * 1024 * 1024 * 1024
    }[1]
  return to_number(value) * m

def getPort(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None:
    return int(m.group(2))
  else:
    return None

def isSecurePort(port):
  """
  Returns True if port is root-owned at *nix systems
  """
  if port is not None:
    return port < 1024
  else:
    return False

def getMountPointForDir(dir, mountPoints):
  """
  :param dir: Directory to check, even if it doesn't exist.
  :return: Returns the closest mount point as a string for the directory.
  if the "dir" variable is None, will return None.
  If the directory does not exist, will return "/".
  """
  bestMountFound = None
  if dir:
    dir = re.sub("^file://", "", dir, count=1).strip().lower()

    # If the path is "/hadoop/hdfs/data", then possible matches for mounts could be
    # "/", "/hadoop/hdfs", and "/hadoop/hdfs/data".
    # So take the one with the greatest number of segments.
    for mountPoint in mountPoints:
      # Ensure that the mount path and the dir path ends with "/"
      # The mount point "/hadoop" should not match with the path "/hadoop1"
      if os.path.join(dir, "").startswith(os.path.join(mountPoint, "")):
        if bestMountFound is None:
          bestMountFound = mountPoint
        elif os.path.join(bestMountFound, "").count(os.path.sep) < os.path.join(mountPoint, "").count(os.path.sep):
          bestMountFound = mountPoint

  return bestMountFound

def getHeapsizeProperties():
  return { "ZOOKEEPER_SERVER": [{"config-name": "zookeeper-env",
                                 "property": "zookeeper_heapsize",
                                 "default": "1024m"}],
           "METRICS_COLLECTOR": [{"config-name": "ams-hbase-env",
                                   "property": "hbase_master_heapsize",
                                   "default": "1024"},
                                 {"config-name": "ams-hbase-env",
                                  "property": "hbase_regionserver_heapsize",
                                  "default": "1024"},
                                 {"config-name": "ams-env",
                                   "property": "metrics_collector_heapsize",
                                   "default": "512"}],
           }

def getMemorySizeRequired(components, configurations):
  totalMemoryRequired = 512*1024*1024 # 512Mb for OS needs
  for component in components:
    if component in getHeapsizeProperties().keys():
      heapSizeProperties = getHeapsizeProperties()[component]
      for heapSizeProperty in heapSizeProperties:
        try:
          properties = configurations[heapSizeProperty["config-name"]]["properties"]
          heapsize = properties[heapSizeProperty["property"]]
        except KeyError:
          heapsize = heapSizeProperty["default"]

        # Assume Mb if no modifier
        if len(heapsize) > 1 and heapsize[-1] in '0123456789':
          heapsize = str(heapsize) + "m"

        totalMemoryRequired += formatXmxSizeToBytes(heapsize)

  return totalMemoryRequired

def round_to_n(mem_size, n=128):
  return int(round(mem_size / float(n))) * int(n)

