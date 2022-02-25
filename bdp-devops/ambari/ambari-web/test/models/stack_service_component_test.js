/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var modelSetup = require('test/init_model_test');
require('models/stack_service_component');

/**

  Component properties template:

  {
    componentName: 'SUPERVISOR',
    expected: {
      displayName: 'Supervisor',
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: true,
      isMaster: false,
      isClient: false,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: false,
      isShownOnInstallerSlaveClientPage: true,
      isShownOnAddServiceAssignMasterPage: false,
      isMasterWithMultipleInstances: false,
      isMasterAddableInstallerWizard: false,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      defaultNoOfMasterHosts: 1,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false,
      selectionSchemeForMasterComponent: {"else": 0}
    }
  }

**/
var componentPropertiesValidationTests = [
  {
    componentName: 'SUPERVISOR',
    expected: {
      displayName: 'Supervisor',
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: true,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: true,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerSlaveClientPage: true,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'ZOOKEEPER_SERVER',
    expected: {
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isMaster: true,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: false,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: true,
      isShownOnInstallerSlaveClientPage: false,
      isShownOnAddServiceAssignMasterPage: true,
      isMasterWithMultipleInstances: true,
      isMasterAddableInstallerWizard: true,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      defaultNoOfMasterHosts: 3,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'APP_TIMELINE_SERVER',
    expected: {
      displayName: 'App Timeline Server',
      minToInstall: 0,
      maxToInstall: 1,
      isRequired: false,
      isMultipleAllowed: false,
      isSlave: false,
      isMaster: true,
      isRestartable: true,
      isReassignable: true,
      isDeletable: true,
      isRollinRestartAllowed: false,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: true,
      isShownOnInstallerSlaveClientPage: false,
      isShownOnAddServiceAssignMasterPage: true,
      isMasterWithMultipleInstances: false,
      isMasterAddableInstallerWizard: false,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'GANGLIA_MONITOR',
    expected: {
      displayName: 'Ganglia Monitor',
      minToInstall: Infinity,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: true,
      isMaster: false,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: false,
      isShownOnInstallerSlaveClientPage: false,
      isShownOnAddServiceAssignMasterPage: false,
      isMasterWithMultipleInstances: false,
      isMasterAddableInstallerWizard: false,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: true,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'FLUME_HANDLER',
    expected: {
      displayName: 'Flume',
      minToInstall: 0,
      maxToInstall: Infinity,
      isRequired: false,
      isMultipleAllowed: true,
      isSlave: true,
      isMaster: false,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: true,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: false,
      isShownOnInstallerSlaveClientPage: true,
      isShownOnAddServiceAssignMasterPage: false,
      isMasterWithMultipleInstances: false,
      isMasterAddableInstallerWizard: false,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'HIVE_METASTORE',
    expected: {
      displayName: 'Hive Metastore',
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: false,
      isMaster: true,
      isRestartable: true,
      isReassignable: true,
      isDeletable: true,
      isRollinRestartAllowed: false,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: true,
      isShownOnInstallerSlaveClientPage: false,
      isShownOnAddServiceAssignMasterPage: true,
      isMasterWithMultipleInstances: true,
      isMasterAddableInstallerWizard: true,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'HIVE_SERVER',
    expected: {
      displayName: 'HiveServer2',
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: false,
      isMaster: true,
      isRestartable: true,
      isReassignable: true,
      isDeletable: true,
      isRollinRestartAllowed: false,
      isDecommissionAllowed: false,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: true,
      isShownOnInstallerSlaveClientPage: false,
      isShownOnAddServiceAssignMasterPage: true,
      isMasterWithMultipleInstances: true,
      isMasterAddableInstallerWizard: true,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'DATANODE',
    expected: {
      displayName: 'DataNode',
      minToInstall: 1,
      maxToInstall: Infinity,
      isRequired: true,
      isMultipleAllowed: true,
      isSlave: true,
      isMaster: false,
      isRestartable: true,
      isReassignable: false,
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: true,
      isRefreshConfigsAllowed: false,
      isAddableToHost: true,
      isShownOnInstallerAssignMasterPage: false,
      isShownOnInstallerSlaveClientPage: true,
      isShownOnAddServiceAssignMasterPage: false,
      isMasterWithMultipleInstances: false,
      isMasterAddableInstallerWizard: false,
      isHAComponentOnly: false,
      isRequiredOnAllHosts: false,
      coHostedComponents: [],
      isOtherComponentCoHosted: false,
      isCoHostedComponent: false
    }
  },
  {
    componentName: 'POSTGRESQL_SERVER',
    expected: {
      isShownOnInstallerAssignMasterPage: false
    }
  },
  {
    componentName: 'MYSQL_SERVER',
    expected: {
      isShownOnInstallerAssignMasterPage: false,
      isDeletable: true
    }
  }
];

describe('App.StackServiceComponent', function() {
  before(function() {
    modelSetup.setupStackServiceComponent();
  });

  App.TestAliases.testAsComputedAnd(App.StackServiceComponent.createRecord(), 'isMasterAddableInstallerWizard', ['isMaster', 'isMultipleAllowed', '!isMasterAddableOnlyOnHA']);

  describe('component properties validation', function() {
    componentPropertiesValidationTests.forEach(function(test) {
      describe('properties validation for ' + test.componentName + ' component', function() {
        var component = App.StackServiceComponent.find(test.componentName);
        var properties = Em.keys(test.expected);
        properties.forEach(function(property) {
          it('#{0} should be {1}'.format(property, JSON.stringify(test.expected[property])), function() {
            expect(component.get(property)).to.be.eql(test.expected[property]);
          })
        });
      });
    });
  });

  describe('#isMasterAddableOnlyOnHA', function () {

    var cases = [
      {
        componentName: 'NAMENODE',
        isMasterAddableOnlyOnHA: true
      },
      {
        componentName: 'RESOURCEMANAGER',
        isMasterAddableOnlyOnHA: true
      },
      {
        componentName: 'RANGER_ADMIN',
        isMasterAddableOnlyOnHA: true
      },
      {
        componentName: 'OOZIE_SERVER',
        isMasterAddableOnlyOnHA: false
      }
    ];

    cases.forEach(function (item) {
      it(item.componentName, function () {
        expect(App.StackServiceComponent.find().findProperty('componentName', item.componentName).get('isMasterAddableOnlyOnHA')).to.equal(item.isMasterAddableOnlyOnHA);
      });
    });

  });

  after(function() {
    modelSetup.cleanStackServiceComponent();
  });
});
