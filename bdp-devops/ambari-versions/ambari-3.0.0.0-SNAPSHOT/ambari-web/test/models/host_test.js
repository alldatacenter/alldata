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
var misc = require('utils/misc');

require('models/host');

describe('App.Host', function () {

  var data = [
    {
      id: 'host1',
      host_name: 'host1',
      memory: 200000,
      disk_total: 100.555,
      disk_free: 90.555,
      health_status: 'HEALTHY',
      state: 'HEALTHY',
      last_heart_beat_time: (new Date()).getTime() - 18100000
    },
    {
      id: 'host2',
      host_name: 'host2',
      memory: 99999,
      disk_total: 90,
      disk_free: 90,
      health_status: 'HEALTHY',
      state: 'HEALTHY',
      last_heart_beat_time: (new Date()).getTime() - 170000
    },
    {
      id: 'host3',
      host_name: 'host3',
      memory: 99999,
      disk_total: 99.999,
      disk_free: 0,
      health_status: 'UNKNOWN',
      state: 'HEARTBEAT_LOST',
      last_heart_beat_time: (new Date()).getTime()
    }
  ];

  App.Host.reopen({
    hostComponents: []
  });

  App.store.loadMany(App.Host, data);

  var host1 = App.Host.find('host1');

  App.TestAliases.testAsComputedAlias(host1, 'componentsInPassiveStateCount', 'componentsInPassiveState.length', 'number');

  App.TestAliases.testAsComputedAlias(host1, 'componentsWithStaleConfigsCount', 'componentsWithStaleConfigs.length', 'number');

  App.TestAliases.testAsComputedAlias(host1, 'disksMounted', 'diskInfo.length', 'number');

  describe('#diskUsedFormatted', function () {

    it('host1 - 10GB ', function () {
      expect(host1.get('diskUsedFormatted')).to.equal('10GB');
    });
    it('host2 - 0GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskUsedFormatted')).to.equal('0GB');
    });
    it('host3 - 100GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskUsedFormatted')).to.equal('100GB');
    });
  });

  describe('#diskTotalFormatted', function () {

    it('host1 - 100.56GB ', function () {
      expect(host1.get('diskTotalFormatted')).to.equal('100.56GB');
    });
    it('host2 - 90GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskTotalFormatted')).to.equal('90GB');
    });
    it('host3 - 100GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskTotalFormatted')).to.equal('100GB');
    });
  });

  describe('#diskUsageFormatted', function () {

    it('host1 - 9.94% ', function () {
      expect(host1.get('diskUsageFormatted')).to.equal('9.94%');
    });
    it('host2 - 0%', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskUsageFormatted')).to.equal('0%');
    });
    it('host3 - 100%', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskUsageFormatted')).to.equal('100%');
    });
  });

  describe('#isNotHeartBeating', function () {
    it('host2 - false', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('isNotHeartBeating')).to.equal(false);
    });
    it('host3 - false', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('isNotHeartBeating')).to.equal(true);
    });
  });

  describe('#cpuUsage', function () {
    var testCases = [
      {
        params: {
          cpuSystem: undefined,
          cpuUser: undefined
        },
        result: 0
      },
      {
        params: {
          cpuSystem: 0,
          cpuUser: 0
        },
        result: 0
      },
      {
        params: {
          cpuSystem: 1,
          cpuUser: 0
        },
        result: 0
      },
      {
        params: {
          cpuSystem: 0,
          cpuUser: 1
        },
        result: 0
      },
      {
        params: {
          cpuSystem: 1,
          cpuUser: 1
        },
        result: 2
      }
    ];
    testCases.forEach(function (test) {
      it('cpuSystem - ' + test.params.cpuSystem + ', cpuUser - ' + test.params.cpuUser, function () {
        host1.set('cpuSystem', test.params.cpuSystem);
        host1.set('cpuUser', test.params.cpuUser);
        host1.propertyDidChange('cpuUsage');

        expect(host1.get('cpuUsage')).to.equal(test.result);
      });
    });
  });

  describe('#memoryUsage', function () {
    var testCases = [
      {
        params: {
          memFree: undefined,
          memTotal: undefined
        },
        result: 0
      },
      {
        params: {
          memFree: 0,
          memTotal: 0
        },
        result: 0
      },
      {
        params: {
          memFree: 1,
          memTotal: 0
        },
        result: 0
      },
      {
        params: {
          memFree: 0,
          memTotal: 1
        },
        result: 0
      },
      {
        params: {
          memFree: 1,
          memTotal: 2
        },
        result: 50
      }
    ];
    testCases.forEach(function (test) {
      it('memFree - ' + test.params.memFree + ', memTotal - ' + test.params.memTotal, function () {
        host1.set('memFree', test.params.memFree);
        host1.set('memTotal', test.params.memTotal);
        host1.propertyDidChange('memoryUsage');

        expect(host1.get('memoryUsage')).to.equal(test.result);
      });
    });
  });

  describe.skip('#componentsWithStaleConfigs', function () {
    it('One component with stale configs', function () {
      host1.set('hostComponents', [Em.Object.create({
        staleConfigs: true
      })]);
      host1.propertyDidChange('componentsWithStaleConfigs');
      expect(host1.get('componentsWithStaleConfigs')).to.eql([Em.Object.create({
        staleConfigs: true
      })]);
    });
    it('No components with stale configs', function () {
      host1.set('hostComponents', [Em.Object.create({
        staleConfigs: false
      })]);
      host1.propertyDidChange('componentsWithStaleConfigs');
      expect(host1.get('componentsWithStaleConfigs')).to.be.empty;
    });
  });

  describe.skip('#componentsInPassiveStateCount', function () {
    it('No component in passive state', function () {
      host1.set('hostComponents', [Em.Object.create({
        passiveState: 'OFF'
      })]);
      host1.propertyDidChange('componentsInPassiveStateCount');

      expect(host1.get('componentsInPassiveStateCount')).to.equal(0);
    });
    it('One component in passive state', function () {
      host1.set('hostComponents', [Em.Object.create({
        passiveState: 'ON'
      })]);
      host1.propertyDidChange('componentsInPassiveStateCount');

      expect(host1.get('componentsInPassiveStateCount')).to.equal(1);
    });
  });

  describe('#disksMounted', function () {
    it('depends on diskInfo count', function () {
      host1.set('diskInfo', [
        {}
      ]);
      host1.propertyDidChange('disksMounted');
      expect(host1.get('disksMounted')).to.equal(1);
    });
  });

  describe('#coresFormatted', function () {
    it('depends on cpu, cpuPhysical', function () {
      host1.set('cpu', 1);
      host1.set('cpuPhysical', 2);
      host1.propertyDidChange('coresFormatted');
      expect(host1.get('coresFormatted')).to.equal('1 (2)');
    });
  });

  describe('#diskUsed', function () {
    it('diskFree and diskTotal are 0', function () {
      host1.set('diskFree', 0);
      host1.set('diskTotal', 0);
      host1.propertyDidChange('diskUsed');
      expect(host1.get('diskUsed')).to.equal(0);
    });
    it('diskFree is 0 and diskTotal is 10', function () {
      host1.set('diskFree', 0);
      host1.set('diskTotal', 10);
      host1.propertyDidChange('diskUsed');
      expect(host1.get('diskUsed')).to.equal(10);
    });
  });

  describe('#diskUsage', function () {
    it('depends on diskTotal, diskUsed', function () {
      host1.reopen({
        diskUsed: 10
      });
      host1.set('diskTotal', 100);
      host1.propertyDidChange('diskUsage');
      expect(host1.get('diskUsage')).to.equal(10);
    });
  });

  describe('#memoryFormatted', function () {

    beforeEach(function () {
      sinon.stub(misc, 'formatBandwidth', Em.K);
    });

    afterEach(function () {
      misc.formatBandwidth.restore();
    });

    it('depends on memory', function () {
      host1.set('memory', 1024);
      host1.propertyDidChange('memoryFormatted');
      host1.get('memoryFormatted');
      expect(misc.formatBandwidth.calledWith(1048576)).to.be.true;
    });
  });

  describe('#loadAvg', function () {
    var testCases = [
      {
        params: {
          loadOne: null,
          loadFive: null,
          loadFifteen: null
        },
        result: null
      },
      {
        params: {
          loadOne: 1.111,
          loadFive: 5.555,
          loadFifteen: 15.555
        },
        result: '1.11'
      },
      {
        params: {
          loadOne: null,
          loadFive: 5.555,
          loadFifteen: 15.555
        },
        result: '5.55'
      },
      {
        params: {
          loadOne: null,
          loadFive: null,
          loadFifteen: 15.555
        },
        result: '15.55'
      }
    ];

    testCases.forEach(function (test) {
      it('loadOne - ' + test.params.loadOne + ', loadFive - ' + test.params.loadFive + ', loadFifteen - ' + test.params.loadFifteen, function () {
        host1.set('loadOne', test.params.loadOne);
        host1.set('loadFive', test.params.loadFive);
        host1.set('loadFifteen', test.params.loadFifteen);
        host1.propertyDidChange('loadAvg');
        expect(host1.get('loadAvg')).to.equal(test.result);
      });
    });
  });

  describe('#healthClass', function () {
    var testCases = [
      {
        params: {
          passiveState: 'ON',
          healthStatus: null
        },
        result: 'icon-medkit'
      },
      {
        params: {
          passiveState: 'OFF',
          healthStatus: 'UNKNOWN'
        },
        result: 'health-status-DEAD-YELLOW'
      },
      {
        params: {
          passiveState: 'OFF',
          healthStatus: 'HEALTHY'
        },
        result: 'health-status-LIVE'
      },
      {
        params: {
          passiveState: 'OFF',
          healthStatus: 'UNHEALTHY'
        },
        result: 'health-status-DEAD-RED'
      },
      {
        params: {
          passiveState: 'OFF',
          healthStatus: 'ALERT'
        },
        result: 'health-status-DEAD-ORANGE'
      },
      {
        params: {
          passiveState: 'OFF',
          healthStatus: null
        },
        result: 'health-status-DEAD-YELLOW'
      }
    ];

    testCases.forEach(function (test) {
      it('passiveState - ' + test.params.passiveState + ', healthStatus - ' + test.params.healthStatus, function () {
        host1.set('passiveState', test.params.passiveState);
        host1.set('healthStatus', test.params.healthStatus);
        host1.propertyDidChange('healthClass');
        expect(host1.get('healthClass')).to.equal(test.result);
      });
    });
  });

  describe('#criticalWarningAlertsCount', function () {
    it('should return sum of critical and warning alerts', function () {
      host1.set('alertsSummary', {
        CRITICAL: 1,
        WARNING: 2,
        OK: 0
      });
      expect(host1.get('criticalWarningAlertsCount')).to.equal(3);
    });
  });

  App.TestAliases.testAsComputedGetByKey(host1, 'healthIconClass', 'healthIconClassMap', 'healthClass', {defaultValue: '', map: {
    'health-status-LIVE': App.healthIconClassGreen,
    'health-status-DEAD-RED': App.healthIconClassRed,
    'health-status-DEAD-YELLOW': App.healthIconClassYellow,
    'health-status-DEAD-ORANGE': App.healthIconClassOrange
  }});

});
