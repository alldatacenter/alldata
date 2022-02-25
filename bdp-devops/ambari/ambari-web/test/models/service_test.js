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
require('models/service');

var service,
  serviceData = {
    id: 'service'
  },
  statusPropertiesCases = [
    {
      status: 'INSTALLED',
      property: 'isStopped'
    },
    {
      status: 'STARTED',
      property: 'isStarted'
    }
  ],
  restartData = {
    host0: ['service0', 'service1']
};

function getModel() {
  return App.Service.createRecord(serviceData);
}

describe('App.Service', function () {

  beforeEach(function () {
    service = App.Service.createRecord(serviceData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(service);
  });

  describe('#isInPassive', function () {
    it('should be true', function () {
      service.set('passiveState', 'ON');
      expect(service.get('isInPassive')).to.be.true;
    });
    it('should be false', function () {
      service.set('passiveState', 'OFF');
      expect(service.get('isInPassive')).to.be.false;
    });
  });

  App.TestAliases.testAsComputedGetByKey(getModel(), 'healthStatus', 'healthStatusMap', 'workStatus', {defaultValue: 'yellow', map: {
    STARTED: 'green',
    STARTING: 'green-blinking',
    INSTALLED: 'red',
    STOPPING: 'red-blinking',
    UNKNOWN: 'yellow'
  }});

  statusPropertiesCases.forEach(function (item) {
    var status = item.status,
      property = item.property;
    describe('#' + property, function () {
      it('status ' + status + ' is for ' + property, function () {
        service.set('workStatus', status);
        expect(service.get(property)).to.be.true;
        var falseStates = statusPropertiesCases.mapProperty('property').without(property);
        var falseStatuses = [];
        falseStates.forEach(function (state) {
          falseStatuses.push(service.get(state));
        });
        expect(falseStatuses).to.eql([false]);
      });
    });
  });

  describe('#isRestartRequired', function () {

    beforeEach(function () {
      service.reopen({
        serviceName: 'HDFS',
        clientComponents: [],
        slaveComponents: [],
        masterComponents: []
      });
    });
    it('should be false when no component has stale configs', function () {
      expect(service.get('isRestartRequired')).to.be.false;
    });
    it('should be true when clientComponents has stale configs', function () {
      service.set('clientComponents', [Em.Object.create({staleConfigHosts: ['host1']})]);
      expect(service.get('isRestartRequired')).to.be.true;
    });
    it('should be true when slaveComponents has stale configs', function () {
      service.set('slaveComponents', [Em.Object.create({staleConfigHosts: ['host1']})]);
      expect(service.get('isRestartRequired')).to.be.true;
    });
    it('should be true when masterComponents has stale configs', function () {
      service.set('masterComponents', [Em.Object.create({staleConfigHosts: ['host1']})]);
      expect(service.get('isRestartRequired')).to.be.true;
    });
  });

  describe('#restartRequiredMessage', function () {
    it('should form message for 2 services on 1 host', function () {
      service.set('restartRequiredHostsAndComponents', restartData);
      expect(service.get('restartRequiredMessage')).to.contain('host0');
      expect(service.get('restartRequiredMessage')).to.contain('service0');
      expect(service.get('restartRequiredMessage')).to.contain('service1');
    });
  });

  describe('#serviceTypes', function () {
    var testCases = [
      {
        serviceName: 'PIG',
        result: []
      },
      {
        serviceName: 'GANGLIA',
        result: ['MONITORING']
      },
      {
        serviceName: 'HDFS',
        result: ['HA_MODE', 'FEDERATION']
      },
      {
        serviceName: 'YARN',
        result: ['HA_MODE']
      }
    ];
    testCases.forEach(function (test) {
      it('service name - ' + test.serviceName, function () {
        service.set('serviceName', test.serviceName);
        service.propertyDidChange('serviceTypes');
        expect(service.get('serviceTypes')).to.eql(test.result);
      });
    });
  });

  describe('#allowToDelete', function () {

    beforeEach(function () {
      this.stub = sinon.stub(service, 'get');
    });

    afterEach(function () {
      this.stub.restore();
    });

    Em.A([
      {
        m: 'may be deleted (1)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'INIT',
        e: true
      },
      {
        m: 'may be deleted (2)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'INSTALL_FAILED',
        e: true
      },
      {
        m: 'may be deleted (3)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'INSTALLED',
        e: true
      },
      {
        m: 'may be deleted (4)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'UNKNOWN',
        e: true
      },
      {
        m: 'deleting is not allowed (1)',
        slaveComponents: [{allowToDelete: false}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'UNKNOWN',
        e: false
      },
      {
        m: 'deleting is not allowed (2)',
        slaveComponents: [{allowToDelete: false}],
        masterComponents: [{allowToDelete: false}],
        workStatus: 'UNKNOWN',
        e: false
      },
      {
        m: 'deleting is not allowed (3)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: false}],
        workStatus: 'UNKNOWN',
        e: false
      },
      {
        m: 'deleting is not allowed (4)',
        slaveComponents: [{allowToDelete: true}],
        masterComponents: [{allowToDelete: true}],
        workStatus: 'STARTED',
        e: false
      }
    ]).forEach(function (test) {
      it(test.m, function () {
        this.stub.withArgs('workStatus').returns(test.workStatus);
        this.stub.withArgs('slaveComponents').returns(test.slaveComponents);
        this.stub.withArgs('masterComponents').returns(test.masterComponents);
        expect(Em.get(service, 'allowToDelete')).to.be.equal(test.e);
      });
    });

  });

});
