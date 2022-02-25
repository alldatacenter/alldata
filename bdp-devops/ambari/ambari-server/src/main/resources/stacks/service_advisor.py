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

"""
The naming convention for ServiceAdvisor subclasses depends on whether they are
in common-services or are part of the stack version's services.

In common-services, the naming convention is <service_name><service_version>ServiceAdvisor.
In the stack, the naming convention is <stack_name><stack_version><service_name>ServiceAdvisor.

Unlike the StackAdvisor, the ServiceAdvisor does NOT provide any inheritance.
If you want to use inheritance to augment a previous version of a service's
advisor you can use the following code to dynamically load the previous advisor.
Some changes will be need to provide the correct path and class names.

  SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
  PARENT_DIR = os.path.join(SCRIPT_DIR, '../<old_version>')
  PARENT_FILE = os.path.join(PARENT_DIR, 'service_advisor.py')

  try:
    with open(PARENT_FILE, 'rb') as fp:
      service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
  except Exception as e:
    traceback.print_exc()
    print "Failed to load parent"

  class <NewServiceAdvisorClassName>(service_advisor.<OldServiceAdvisorClassName>)

where the NewServiceAdvisorClassName and OldServiceAdvisorClassName follow the naming
convention listed above.

For examples see: common-services/HAWQ/2.0.0/service_advisor.py
and common-services/PXF/3.0.0/service_advisor.py
"""
from stack_advisor import DefaultStackAdvisor

class ServiceAdvisor(DefaultStackAdvisor):
  """
  Abstract class implemented by all service advisors.
  """

  def colocateServiceWithServicesInfo(self, hostsComponentsMap, serviceComponents, services):
    """
    Populate hostsComponentsMap with key = hostname and value = [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    of services that must be co-hosted and on which host they should be present.
    :param hostsComponentsMap: Map from hostname to list of [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    present on on that host.
    :param serviceComponents: Mapping of components
    :param services: The full list of servies

    If any components of the service should be colocated with other services,
    and the decision should be based on information that is only available in the services list,
    such as what are the master components, etc,
    this is where you should set up that layout.

    Each service should only implement either this method or the colocateService method
    """
    pass

  def colocateService(self, hostsComponentsMap, serviceComponents):
    """
    Populate hostsComponentsMap with key = hostname and value = [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    of services that must be co-hosted and on which host they should be present.
    :param hostsComponentsMap: Map from hostname to list of [{"name": "COMP_NAME_1"}, {"name": "COMP_NAME_2"}, ...]
    present on on that host.
    :param serviceComponents: Mapping of components

    If any components of the service should be colocated with other services, this is where you should set up that layout.

    Each service should only implement either this method or the colocateServiceWithServicesInfo method
    """
    pass

  def getServiceConfigurationRecommendations(self, configurations, clusterSummary, services, hosts):
    """
    Any configuration recommendations for the service should be defined in this function.
    This should be similar to any of the recommendXXXXConfigurations functions in the stack_advisor.py
    such as recommendYARNConfigurations().
    """
    pass

  def getServiceConfigurationRecommendationsForSSO(self, configurations, clusterSummary, services, hosts):
    """
    Any SSO-related configuration recommendations for the service should be defined in this function.
    """
    pass

  def getServiceConfigurationRecommendationsForLDAP(self, configurations, clusterSummary, services, hosts):
    """
    Any LDAP related configuration recommendations for the service should be defined in this function.
    """
    pass

  def getServiceConfigurationRecommendationsForKerberos(self, configurations, clusterSummary, services, hosts):
    """
    Any Kerberos-related configuration recommendations for the service should be defined in this function.

    Redirect to getServiceConfigurationRecommendations for backward compatibility
    """
    return self.getServiceConfigurationRecommendations(configurations, clusterSummary, services, hosts)

  def getActiveHosts(self, hosts):
    """ Filters the list of specified hosts object and returns
        a list of hosts which are not in maintenance mode. """
    hostsList = []
    if hosts is not None:
      hostsList = [host['host_name'] for host in hosts
                   if host.get('maintenance_state') is None or host.get('maintenance_state') == "OFF"]
    return hostsList

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Returns an array of Validation objects about issues with the hostnames to which components are assigned.
    This should detect validation issues which are different than those the stack_advisor.py detects.
    The default validations are in stack_advisor.py getComponentLayoutValidations function.
    """
    return []

  def getServiceComponentCardinalityValidations(self, services, hosts, service_name):
    """
    Returns an array of Validation objects about issues with the hostnames to which components are assigned.
    This should detect cardinality Validation issues.
    """
    items = []
    hostsSet = set(self.getActiveHosts(
      [host["Hosts"] for host in hosts["items"]]))  # [host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsSet)

    target_service = None
    for service in services["services"]:
      if service["StackServices"]["service_name"] == service_name:
        target_service = service
    if not target_service:
      return []
    componentsList = target_service["components"]

    # Validating cardinality
    for component in componentsList:
      if component["StackServiceComponents"]["cardinality"] is not None:
        componentName = component["StackServiceComponents"]["component_name"]
        componentDisplayName = component["StackServiceComponents"]["display_name"]
        componentHosts = []
        if component["StackServiceComponents"]["hostnames"] is not None:
          componentHosts = [componentHost for componentHost in component["StackServiceComponents"]["hostnames"] if
                            componentHost in hostsSet]
        componentHostsCount = len(componentHosts)
        cardinality = str(component["StackServiceComponents"]["cardinality"])
        # cardinality types: null, 1+, 1-2, 1, ALL
        message = None
        if "+" in cardinality:
          hostsMin = int(cardinality[:-1])
          if componentHostsCount < hostsMin:
            message = "at least {0} {1} components should be installed in cluster.".format(hostsMin,
                                                                                           componentDisplayName)
        elif "-" in cardinality:
          nums = cardinality.split("-")
          hostsMin = int(nums[0])
          hostsMax = int(nums[1])
          if componentHostsCount > hostsMax or componentHostsCount < hostsMin:
            message = "between {0} and {1} {2} components should be installed in cluster.".format(hostsMin, hostsMax,
                                                                                                  componentDisplayName)
        elif "ALL" == cardinality:
          if componentHostsCount != hostsCount:
            message = "{0} component should be installed on all hosts in cluster.".format(componentDisplayName)
        else:
          if componentHostsCount != int(cardinality):
            message = "exactly {0} {1} components should be installed in cluster.".format(int(cardinality),
                                                                                          componentDisplayName)

        if message is not None:
          message = "You have selected {0} {1} components. Please consider that {2}".format(componentHostsCount,
                                                                                            componentDisplayName,
                                                                                            message)
          items.append(
            {"type": 'host-component', "level": 'ERROR', "message": message, "component-name": componentName})
    return items

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Any configuration validations for the service should be defined in this function.
    This should be similar to any of the validateXXXXConfigurations functions in the stack_advisor.py
    such as validateHDFSConfigurations.
    """
    return []

  def getDBDriver(self, databaseType):
    driverDict = {
      "NEW MYSQL DATABASE": "com.mysql.jdbc.Driver",
      "NEW DERBY DATABASE": "org.apache.derby.jdbc.EmbeddedDriver",
      "EXISTING MYSQL DATABASE": "com.mysql.jdbc.Driver",
      "EXISTING MYSQL / MARIADB DATABASE": "com.mysql.jdbc.Driver",
      "EXISTING POSTGRESQL DATABASE": "org.postgresql.Driver",
      "EXISTING ORACLE DATABASE": "oracle.jdbc.driver.OracleDriver",
      "EXISTING SQL ANYWHERE DATABASE": "sap.jdbc4.sqlanywhere.IDriver"
    }
    return driverDict.get(databaseType.upper())

  def getDBConnectionString(self, databaseType):
    driverDict = {
      "NEW MYSQL DATABASE": "jdbc:mysql://{0}/{1}?createDatabaseIfNotExist=true",
      "NEW DERBY DATABASE": "jdbc:derby:${{oozie.data.dir}}/${{oozie.db.schema.name}}-db;create=true",
      "EXISTING MYSQL DATABASE": "jdbc:mysql://{0}/{1}",
      "EXISTING MYSQL / MARIADB DATABASE": "jdbc:mysql://{0}/{1}",
      "EXISTING POSTGRESQL DATABASE": "jdbc:postgresql://{0}:5432/{1}",
      "EXISTING ORACLE DATABASE": "jdbc:oracle:thin:@//{0}:1521/{1}",
      "EXISTING SQL ANYWHERE DATABASE": "jdbc:sqlanywhere:host={0};database={1}"
    }
    return driverDict.get(databaseType.upper())

  def getProtocol(self, databaseType):
    first_parts_of_connection_string = {
      "NEW MYSQL DATABASE": "jdbc:mysql",
      "NEW DERBY DATABASE": "jdbc:derby",
      "EXISTING MYSQL DATABASE": "jdbc:mysql",
      "EXISTING MYSQL / MARIADB DATABASE": "jdbc:mysql",
      "EXISTING POSTGRESQL DATABASE": "jdbc:postgresql",
      "EXISTING ORACLE DATABASE": "jdbc:oracle",
      "EXISTING SQL ANYWHERE DATABASE": "jdbc:sqlanywhere"
    }
    return first_parts_of_connection_string.get(databaseType.upper())

  def getDBTypeAlias(self, databaseType):
    driverDict = {
      "NEW MYSQL DATABASE": "mysql",
      "NEW DERBY DATABASE": "derby",
      "EXISTING MYSQL / MARIADB DATABASE": "mysql",
      "EXISTING MYSQL DATABASE": "mysql",
      "EXISTING POSTGRESQL DATABASE": "postgres",
      "EXISTING ORACLE DATABASE": "oracle",
      "EXISTING SQL ANYWHERE DATABASE": "sqla"
    }
    return driverDict.get(databaseType.upper())
