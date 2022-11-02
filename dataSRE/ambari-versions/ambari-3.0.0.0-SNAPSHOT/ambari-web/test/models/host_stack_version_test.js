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

require('models/host_stack_version');

describe('App.HostStackVersion', function () {

  describe("#displayStatus", function () {
    var testCases = [
      {
        status: 'OUT_OF_SYNC',
        result: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')
      },
      {
        status: 'INSTALLED',
        result: Em.I18n.t('hosts.host.stackVersions.status.installed')
      },
      {
        status: 'INSTALLING',
        result: Em.I18n.t('hosts.host.stackVersions.status.installing')
      },
      {
        status: 'INSTALL_FAILED',
        result: Em.I18n.t('hosts.host.stackVersions.status.install_failed')
      },
      {
        status: 'UPGRADE_FAILED',
        result: Em.I18n.t('hosts.host.stackVersions.status.upgrade_failed')
      },
      {
        status: 'UPGRADING',
        result: Em.I18n.t('hosts.host.stackVersions.status.upgrading')
      },
      {
        status: 'CURRENT',
        result: Em.I18n.t('hosts.host.stackVersions.status.current')
      },
      {
        status: 'ANY',
        result: 'Any'
      }
    ];
    afterEach(function () {
      App.HostStackVersion.find().clear();
    });
    testCases.forEach(function (test) {
      it('status is ' + test.status, function () {
        App.store.safeLoad(App.HostStackVersion, {
          id: 1,
          status: test.status
        });
        expect(App.HostStackVersion.find().objectAt(0).get('displayStatus')).to.equal(test.result);
      });
    }, this);
  });

  describe("#installEnabled", function () {
    var testCases = [
      {
        status: 'OUT_OF_SYNC',
        result: true
      },
      {
        status: 'INSTALLED',
        result: false
      },
      {
        status: 'INSTALLING',
        result: false
      },
      {
        status: 'INSTALL_FAILED',
        result: true
      },
      {
        status: '',
        result: false
      }
    ];
    afterEach(function () {
      App.HostStackVersion.find().clear();
    });
    testCases.forEach(function (test) {
      it('status is ' + test.status, function () {
        App.store.safeLoad(App.HostStackVersion, {
          id: 1,
          status: test.status
        });
        expect(App.HostStackVersion.find().objectAt(0).get('installEnabled')).to.equal(test.result);
      });
    }, this);
  });

  describe("#isCurrent", function () {
    afterEach(function () {
      App.HostStackVersion.find().clear();
    });
    it("status is CURRENT", function () {
      App.store.safeLoad(App.HostStackVersion, {
        id: 1,
        status: 'CURRENT'
      });
      expect(App.HostStackVersion.find(1).get('isCurrent')).to.be.true;
    });
    it("status is not CURRENT", function () {
      App.store.safeLoad(App.HostStackVersion, {
        id: 1,
        status: 'INSTALLED'
      });
      expect(App.HostStackVersion.find(1).get('isCurrent')).to.be.false;
    });
  });

  describe("#isInstalling", function () {
    afterEach(function () {
      App.HostStackVersion.find().clear();
    });
    it("status is INSTALLING", function () {
      App.store.safeLoad(App.HostStackVersion, {
        id: 1,
        status: 'INSTALLING'
      });
      expect(App.HostStackVersion.find(1).get('isInstalling')).to.be.true;
    });
    it("status is not INSTALLING", function () {
      App.store.safeLoad(App.HostStackVersion, {
        id: 1,
        status: 'INSTALLED'
      });
      expect(App.HostStackVersion.find(1).get('isInstalling')).to.be.false;
    });
  });
});
