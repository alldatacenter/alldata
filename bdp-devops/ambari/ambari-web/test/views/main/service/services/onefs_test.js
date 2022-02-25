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
var numberUtils = require('utils/number_utils');

describe('App.MainDashboardServiceOnefsView', function () {
  var view;

  beforeEach(function () {
    view = App.MainDashboardServiceOnefsView.create();
  });

  describe('#journalNodesLive', function () {
    it('should give count of started nodes', function () {
      view.set('service', Em.Object.create({
        journalNodes: [{workStatus: 'STARTED'}, {workStatus: 'STARTED'}, {workStatus: 'STOPPED'} ]
      }));
      expect(view.get('journalNodesLive')).to.be.equal(2);
    });
  });

  describe('#nodeUptime', function () {
    it('should display notRunning text if time is equal to 0', function () {
      view.set('service', Em.Object.create({
        nameNodeStartTime: 0
      }));
      expect(view.get('nodeUptime')).to.be.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('should display notRunning text if time is nullable', function () {
      view.set('service', Em.Object.create({
        nameNodeStartTime: null
      }));
      expect(view.get('nodeUptime')).to.be.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('should display uptime text if time is more than 0', function () {
      const now = +(new Date());
      view.set('service', Em.Object.create({
        nameNodeStartTime: now
      }));
      sinon.stub(App, 'dateTime').returns(now);
      expect(view.get('nodeUptime')).to.be.equal('0s');
      App.dateTime.restore();
    });
  });

  describe('#nonDfsUsed', function () {
    it('should return null if capacityTotal or capacityRemaining or capacityUsed is null', function () {
      view.set('service', Em.Object.create({
        capacityTotal: null,
        capacityRemaining: 1,
        capacityUsed: 1
      }));
      expect(view.get('nonDfsUsed')).to.be.equal(null);

      view.set('service', Em.Object.create({
        capacityTotal: 1,
        capacityRemaining: null,
        capacityUsed: 1
      }));
      expect(view.get('nonDfsUsed')).to.be.equal(null);

      view.set('service', Em.Object.create({
        capacityTotal: 1,
        capacityRemaining: 1,
        capacityUsed: null
      }));
      expect(view.get('nonDfsUsed')).to.be.equal(null);
    });

    it('should return diff of capacityTotal and capacityRemaining and capacityUsed if they are provided', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 6,
        capacityRemaining: 2,
        capacityUsed: 1
      }));
      expect(view.get('nonDfsUsed')).to.be.equal(3);
    });
  });

  describe('#dfsUsedDisk', function () {
    it('should return correct formatted string', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 600,
        capacityUsed: 3
      }));
      expect(view.get('dfsUsedDisk')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsed').format(numberUtils.bytesToSize(3, 1, 'parseFloat'), numberUtils.bytesToSize(600, 1, 'parseFloat'))
      );
    });
  });

  describe('#nonDfsUsedDisk', function () {
    it('should return correct formatted string', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 6,
        capacityRemaining: 2,
        capacityUsed: 1
      }));
      expect(view.get('nonDfsUsedDisk')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsed').format(numberUtils.bytesToSize(3, 1, 'parseFloat'), numberUtils.bytesToSize(6, 1, 'parseFloat'))
      );
    });
  });

  describe('#remainingDisk', function () {
    it('should return correct formatted string', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 600,
        capacityRemaining: 3
      }));
      expect(view.get('remainingDisk')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsed').format(numberUtils.bytesToSize(3, 1, 'parseFloat'), numberUtils.bytesToSize(600, 1, 'parseFloat'))
      );
    });
  });

  describe('#dfsUsedDiskPercent', function () {
    it('should return formatted string with not available if used is not a number', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 600,
        capacityUsed: 'a'
      }));
      expect(view.get('dfsUsedDiskPercent')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsedPercent').format(Em.I18n.t('services.service.summary.notAvailable') + " ")
      );
    });

    it('should return correct formatted string', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 600,
        capacityUsed: 3
      }));
      expect(view.get('dfsUsedDiskPercent')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsedPercent').format('0.50')
      );
    });
  });

  describe('#nonDfsUsedDiskPercent', function () {
    it('should return formatted string with not available if used is not a number', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 6,
        capacityRemaining: 2,
        capacityUsed: 'a'
      }));
      expect(view.get('nonDfsUsedDiskPercent')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsedPercent').format(Em.I18n.t('services.service.summary.notAvailable') + " ")
      );
    });

    it('should return correct formatted string', function () {
      view.set('service', Em.Object.create({
        capacityTotal: 6,
        capacityRemaining: 2,
        capacityUsed: 1
      }));
      expect(view.get('nonDfsUsedDiskPercent')).to.be.equal(
        Em.I18n.t('dashboard.services.hdfs.capacityUsedPercent').format('50.00')
      );
    });
  });

  describe('#isNfsInStack', function () {
    it('should return true if NFS_GATEWAY is in stack', function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([{componentName: 'HDFS'}, {componentName: 'NFS_GATEWAY'}]);
      expect(view.get('isNfsInStack')).to.be.equal(true);
      App.StackServiceComponent.find.restore();
    });

    it('should return false if NFS_GATEWAY is not in stack', function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([{componentName: 'HDFS'}, {componentName: 'HDFS'}]);
      expect(view.get('isNfsInStack')).to.be.equal(false);
      App.StackServiceComponent.find.restore();
    });
  });

  describe('#safeModeStatus', function () {
    it('should return notAvailable if safeModeStatus is nullable', function () {
      view.set('service', Em.Object.create({
        safeModeStatus: null
      }));
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.notAvailable"));
    });

    it('should return notAvailable if safeModeStatus is empty array', function () {
      view.set('service', Em.Object.create({
        safeModeStatus: []
      }));
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.safeModeStatus.notInSafeMode"));
    });

    it('should return notAvailable if safeModeStatus is not empty array', function () {
      view.set('service', Em.Object.create({
        safeModeStatus: [{}]
      }));
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.safeModeStatus.inSafeMode"));
    });
  });

  describe('#upgradeStatus', function () {
    it('should return notPending if upgradeStatus is true', function () {
      view.set('service', Em.Object.create({
        upgradeStatus: 'true',
        healthStatus: 'green'
      }));
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });

    it('should return notFinalized if upgradeStatus is false and health status us green', function () {
      view.set('service', Em.Object.create({
        upgradeStatus: 'false',
        healthStatus: 'green'
      }));
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized'));
    });

    it('should return notAvailable in all othe cases', function () {
      view.set('service', Em.Object.create({
        upgradeStatus: 'false',
        healthStatus: 'red'
      }));
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });
  });
});