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
require('views/main/admin/stack_upgrade/upgrade_version_column_view');

describe('App.UpgradeVersionColumnView', function () {
  var view =  App.UpgradeVersionColumnView.create({});
  var services = [
    Em.Object.create({
      id: 'zk',
      desiredRepositoryVersionId: 1
    }),
    Em.Object.create({
      id: 'storm',
      desiredRepositoryVersionId: 2
    }),
    Em.Object.create({
      id: 'hdfs',
      desiredRepositoryVersionId: 1
    })
  ];
  var versions = [
    Em.Object.create({
      id: 1,
      status: "CURRENT",
      repositoryVersion: "2.3.1.1",
      stackVersionType: 'HDP',
      hidden: false,
      isCurrent: true,
      stackServices:[
        Em.Object.create({
          name: 'zk',
          isAvailable: true
        }),
        Em.Object.create({
          name: 'storm',
          isAvailable: false
        }),
        Em.Object.create({
          name: 'hdfs',
          isAvailable: true
        })
      ]
    }),
    Em.Object.create({
      id: 2,
      status: "CURRENT",
      repositoryVersion: "2.2.0.1",
      stackVersionType: 'HDP',
      hidden: false,
      isCurrent: true,
      stackServices: [
        Em.Object.create({
          name: 'zk',
          isAvailable: true
        }),
        Em.Object.create({
          name: 'storm',
          isAvailable: true
        }),
        Em.Object.create({
          name: 'hdfs',
          isAvailable: true
        })
      ]
    }),
    Em.Object.create({
      id: 3,
      status: "INSTALLED",
      repositoryVersion: "2.0.2.1",
      stackVersionType: 'HCP',
      isCompatible: true,
      hidden: false,
      isMaint: false,
      stackServices: [
        Em.Object.create({
          name: 'zk',
          isAvailable: true
        }),
        Em.Object.create({
          name: 'storm',
          isAvailable: false
        }),
        Em.Object.create({
          name: 'hdfs',
          isAvailable: true
        })
      ]
    }),
    Em.Object.create({
      id: 4,
      status: "INSTALLED",
      repositoryVersion: "2.0.2.1",
      stackVersionType: 'HCP',
      isCompatible: true,
      hidden: false,
      isMaint: true,
      stackServices: [
        Em.Object.create({
          name: 'zk',
          isAvailable: true
        }),
        Em.Object.create({
          name: 'storm',
          isAvailable: false
        }),
        Em.Object.create({
          name: 'hdfs',
          isAvailable: true
        })
      ]
    })
  ];

  describe("#isStackServiceAvailable", function () {
    beforeEach(function() {
      sinon.stub(App.Service, 'find', function (id) {
        return services.find(function (service) {
          return id === service.get('id');
        })
      })
    });
    afterEach(function() {
      App.Service.find.restore();
    });
    it('Current upgrade with invalid service', function () {
      view.set('content', versions[0]);
      expect(view.isStackServiceAvailable()).to.be.false;
    });
    it('Current upgrade with new service', function () {
      view.set('content', versions[0]);
      expect(view.isStackServiceAvailable(versions[0].get('stackServices')[0])).to.be.true;
    });
    it('Current upgrade with old service', function () {
      view.set('content', versions[1]);
      expect(view.isStackServiceAvailable(versions[1].get('stackServices')[0])).to.be.false;
    });
    it('Install upgrade with available service', function () {
      view.set('content', versions[2]);
      expect(view.isStackServiceAvailable(versions[2].get('stackServices')[0])).to.be.true;
    });
    it('Install upgrade with unavailable service', function () {
      view.set('content', versions[2]);
      expect(view.isStackServiceAvailable(versions[2].get('stackServices')[1])).to.be.false;
    });
    it('Current upgrade with unavailable service', function () {
      view.set('content', versions[1]);
      expect(view.isStackServiceAvailable(versions[1].get('stackServices')[1])).to.be.true;
    });
  });

  describe("#getNotUpgradable", function () {
    it ('Should return false for not maint', function () {
      view.set('content', versions[2]);
      expect(view.getNotUpgradable(true, false)).to.be.false;
    });
    it ('Should return true for maint, when service is available and not upgradable', function () {
      view.set('content', versions[3]);
      expect(view.getNotUpgradable(true, false)).to.be.true;
    });
    it ('Should return false for maint, when service is available and upgradable', function () {
      view.set('content', versions[3]);
      expect(view.getNotUpgradable(true, true)).to.be.false;
    });
    it ('Should return false for maint, when service is not available and upgradable', function () {
      view.set('content', versions[3]);
      expect(view.getNotUpgradable(false, true)).to.be.false;
    })

    it ('Should return false for maint, when service is available and not upgradable while is upgrading', function () {
      view.set('content', versions[3]);
      view.set('isUpgrading', true);
      expect(view.getNotUpgradable(false, true)).to.be.false;
    })
  })
});
