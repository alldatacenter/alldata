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
var testHelpers = require('test/helpers');
require('mappers/stack_version_mapper');

describe('App.stackVersionMapper', function () {
  var stackVersionRecords = App.StackVersion.find(),
    repoVersionRecords = App.RepositoryVersion.find(),
    clearModels = function () {
      stackVersionRecords.clear();
      repoVersionRecords.clear();
    };

  before(clearModels);

  afterEach(clearModels);

  describe('#map', function () {
    beforeEach(function () {
      App.stackVersionMapper.map({
        items: [
          {
            ClusterStackVersions: {
              id: 1,
              cluster_name: 'c',
              stack: 'HDP',
              version: '3.0',
              repository_version: 11,
              state: 'OUT_OF_SYNC',
              supports_revert: false,
              host_states: {
                CURRENT: ['h0, h1'],
                INSTALLED: ['h2', 'h3'],
                INSTALLING: ['h4', 'h5'],
                INSTALL_FAILED: ['h6', 'h7'],
                NOT_REQUIRED: ['h8', 'h9'],
                OUT_OF_SYNC: ['h10', 'h11'],
                UPGRADING: ['h12', 'h13'],
                UPGRADED: ['h14', 'h15'],
                UPGRADE_FAILED: ['h16', 'h17']
              },
              repository_summary: {
                services: {
                  ZOOKEEPER: {
                    version: '4.0.0',
                    release_version: '3.0.1.1',
                    upgrade: true
                  }
                }
              }
            },
            repository_versions: [
              {
                RepositoryVersions: {
                  id: 11,
                  repository_version: '3.0.1.1'
                }
              }
            ]
          },
          {
            ClusterStackVersions: {
              id: 2,
              cluster_name: 'c',
              stack: 'HDP',
              version: '3.0',
              repository_version: 12,
              state: 'INSTALL_FAILED',
              supports_revert: false,
              host_states: {
                CURRENT: [],
                INSTALLED: [],
                INSTALLING: [],
                INSTALL_FAILED: [],
                NOT_REQUIRED: [],
                OUT_OF_SYNC: [],
                UPGRADING: [],
                UPGRADED: [],
                UPGRADE_FAILED: []
              },
              repository_summary: {
                services: {
                  ZOOKEEPER: {
                    version: '3.8.0',
                    release_version: '3.0.0.0',
                    upgrade: true
                  }
                }
              }
            },
            repository_versions: [
              {
                RepositoryVersions: {
                  id: 12,
                  repository_version: '3.0.0.0'
                }
              }
            ]
          },
          {
            ClusterStackVersions: {
              id: 3,
              cluster_name: 'c',
              stack: 'HDP',
              version: '3.0',
              repository_version: 13,
              state: 'COMPLETED',
              supports_revert: false,
              host_states: {
                CURRENT: [],
                INSTALLED: [],
                INSTALLING: [],
                INSTALL_FAILED: [],
                NOT_REQUIRED: [],
                OUT_OF_SYNC: [],
                UPGRADING: [],
                UPGRADED: [],
                UPGRADE_FAILED: []
              },
              repository_summary: {
                services: {
                  ZOOKEEPER: {
                    version: '3.9.0',
                    release_version: '3.0.1.0',
                    upgrade: true
                  }
                }
              }
            },
            repository_versions: [
              {
                RepositoryVersions: {
                  id: 13,
                  repository_version: '3.0.1.0'
                }
              }
            ]
          },
          {
            ClusterStackVersions: {
              id: 4,
              cluster_name: 'c',
              stack: 'HDP',
              version: '3.0',
              repository_version: 14,
              state: 'INSTALLING',
              supports_revert: true,
              revert_upgrade_id: 1,
              host_states: {
                CURRENT: [],
                INSTALLED: [],
                INSTALLING: [],
                INSTALL_FAILED: [],
                NOT_REQUIRED: [],
                OUT_OF_SYNC: [],
                UPGRADING: [],
                UPGRADED: [],
                UPGRADE_FAILED: []
              },
              repository_summary: {
                services: {
                  ZOOKEEPER: {
                    version: '4.1.0',
                    release_version: '3.1.1.0',
                    upgrade: true
                  }
                }
              }
            },
            repository_versions: [
              {
                RepositoryVersions: {
                  id: 14,
                  repository_version: '3.1.1.0'
                }
              }
            ]
          },
          {
            ClusterStackVersions: {
              id: 5,
              cluster_name: 'c',
              stack: 'HDP',
              version: '3.0',
              repository_version: 15,
              state: 'NOT_REQUIRED',
              supports_revert: false,
              host_states: {
                CURRENT: [],
                INSTALLED: [],
                INSTALLING: [],
                INSTALL_FAILED: [],
                NOT_REQUIRED: [],
                OUT_OF_SYNC: [],
                UPGRADING: [],
                UPGRADED: [],
                UPGRADE_FAILED: []
              },
              repository_summary: {
                services: {
                  ZOOKEEPER: {
                    version: '3.7.0',
                    release_version: '2.99.99.0',
                    upgrade: true
                  }
                }
              }
            },
            repository_versions: [
              {
                RepositoryVersions: {
                  id: 15,
                  repository_version: '2.99.99.0'
                }
              }
            ]
          }
        ]
      });
    });

    it('should load sorted and mapped data to App.StackVersion', function () {
      testHelpers.nestedExpect([
        {
          id: 5,
          clusterName: 'c',
          stack: 'HDP',
          version: '3.0',
          state: 'NOT_REQUIRED',
          notInstalledHosts: [],
          installingHosts: [],
          installedHosts: [],
          installFailedHosts: [],
          outOfSyncHosts: [],
          upgradingHosts: [],
          upgradedHosts: [],
          upgradeFailedHosts: [],
          currentHosts: [],
          supportsRevert: false
        },
        {
          id: 2,
          clusterName: 'c',
          stack: 'HDP',
          version: '3.0',
          state: 'INSTALL_FAILED',
          notInstalledHosts: [],
          installingHosts: [],
          installedHosts: [],
          installFailedHosts: [],
          outOfSyncHosts: [],
          upgradingHosts: [],
          upgradedHosts: [],
          upgradeFailedHosts: [],
          currentHosts: [],
          supportsRevert: false
        },
        {
          id: 3,
          clusterName: 'c',
          stack: 'HDP',
          version: '3.0',
          state: 'COMPLETED',
          notInstalledHosts: [],
          installingHosts: [],
          installedHosts: [],
          installFailedHosts: [],
          outOfSyncHosts: [],
          upgradingHosts: [],
          upgradedHosts: [],
          upgradeFailedHosts: [],
          currentHosts: [],
          supportsRevert: false
        },
        {
          id: 1,
          clusterName: 'c',
          stack: 'HDP',
          version: '3.0',
          state: 'OUT_OF_SYNC',
          notInstalledHosts: ['h4', 'h5', 'h6', 'h7', 'h10', 'h11'],
          installingHosts: ['h4', 'h5'],
          installedHosts: ['h2', 'h3', 'h12', 'h13', 'h14', 'h15', 'h16', 'h17'],
          installFailedHosts: ['h6', 'h7'],
          outOfSyncHosts: ['h10', 'h11'],
          upgradingHosts: ['h12', 'h13'],
          upgradedHosts: ['h14', 'h15'],
          upgradeFailedHosts: ['h16', 'h17'],
          currentHosts: ['h0, h1'],
          supportsRevert: false
        },
        {
          id: 4,
          clusterName: 'c',
          stack: 'HDP',
          version: '3.0',
          state: 'INSTALLING',
          notInstalledHosts: [],
          installingHosts: [],
          installedHosts: [],
          installFailedHosts: [],
          outOfSyncHosts: [],
          upgradingHosts: [],
          upgradedHosts: [],
          upgradeFailedHosts: [],
          currentHosts: [],
          supportsRevert: true
        },
      ], stackVersionRecords.toArray());
    });

    it('should set relations to App.RepositoryVersion', function () {
      testHelpers.nestedExpect(stackVersionRecords.mapProperty('repositoryVersion'), repoVersionRecords.toArray());
    })
  });
});
