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
require('utils/batch_scheduled_requests');
require('controllers/main/host');
require('mappers/server_data_mapper');

describe('MainHostController', function () {

  var hostController, db;

  beforeEach(function () {
    hostController = App.MainHostController.create({});
  });

  afterEach(function () {
    hostController.destroy();
  });

  describe('#getRegExp()', function () {
    var message = '`{0}` should convert to `{1}`',
      tests = [
        {value: '.*', expected: '.*'},
        {value: '.', expected: '.*'},
        {value: '.*.*', expected: '.*'},
        {value: '*', expected: '^$'},
        {value: '........', expected: '.*'},
        {value: '........*', expected: '.*'},
        {value: 'a1', expected: '.*a1.*'},
        {value: 'a1.', expected: '.*a1.*'},
        {value: 'a1...', expected: '.*a1.*'},
        {value: 'a1.*', expected: '.*a1.*'},
        {value: 'a1.*.a2.a3', expected: '.*a1.*.a2.a3.*'},
        {value: 'a1.*.a2...a3', expected: '.*a1.*.a2...a3.*'}
      ];

    tests.forEach(function (test) {
      it(message.format(test.value, test.expected), function () {
        expect(hostController.getRegExp(test.value)).to.be.equal(test.expected);
      });
    });
  });

  describe('#getQueryParameters', function () {
    beforeEach(function () {
      sinon.spy(hostController, 'getRegExp');
      sinon.stub(App.db, 'getFilterConditions', function () {
        return [{
          iColumn: 1,
          skipFilter: false,
          type: "string",
          value: "someval"
        }];
      });
    });

    afterEach(function () {
      App.db.getFilterConditions.restore();
      hostController.getRegExp.restore();
    });

    it('should call #getRegExp with value `someval` on host name filter', function () {
      hostController.getQueryParameters();
      expect(hostController.getRegExp.calledWith('someval')).to.ok;
    });

    it('result should include host name filter converted value', function () {
      expect(hostController.getQueryParameters().findProperty('key', 'Hosts/host_name').value).to.equal('.*someval.*');
    });
  });

  describe('#getSortProps', function () {

    beforeEach(function () {
      db = {
        mainHostController: [
          {name: 'hostName', status: 'sorting'}
        ]
      };
      sinon.stub(App.db, 'getSortingStatuses', function (k) {
        return db[k];
      });
      sinon.stub(App.db, 'setSortingStatuses', function (k, v) {
        db[k] = Em.makeArray(v);
      });
    });

    afterEach(function () {
      App.db.getSortingStatuses.restore();
      App.db.setSortingStatuses.restore();
    });

    it('should set default sorting condition', function () {
      hostController.getSortProps();
      expect(db.mainHostController).to.eql([{name: 'hostName', status: 'sorting_asc'}]);
    });

  });

  describe("#getProperValue()", function() {

    var testCases = [
      {
        input: '>1',
        expected: '1'
      },
      {
        input: '<1',
        expected: '1'
      },
      {
        input: '=1',
        expected: '1'
      },
      {
        input: '1',
        expected: '1'
      }
    ];

    testCases.forEach(function(test) {
      it("value =" + test.input, function() {
        expect(hostController.getProperValue(test.input)).to.be.equal(test.expected);
      });
    });

  });

  describe("#convertMemory()", function() {

    beforeEach(function() {
      sinon.stub(hostController, 'getProperValue', function(input) {
        return input;
      })
    });
    afterEach(function() {
      hostController.getProperValue.restore();
    });

    var testCases = [
      {
        input: 'm',
        expected: 'm'
      },
      {
        input: '1',
        expected: 1048576
      },
      {
        input: '1g',
        expected: 1048576
      },
      {
        input: '1m',
        expected: 1024
      },
      {
        input: '1k',
        expected: 1
      }
    ];

    testCases.forEach(function(test) {
      it("value =" + test.input, function() {
        expect(hostController.convertMemory(test.input)).to.be.equal(test.expected);
      });
    });
  });

  describe("#convertMemoryToRange()", function() {

    beforeEach(function() {
      sinon.stub(hostController, 'rangeConvertNumber', function(arg1) {
        return [arg1, arg1];
      })
    });
    afterEach(function() {
      hostController.rangeConvertNumber.restore();
    });

    var testCases = [
      {
        input: 'm',
        expected: [0, 0]
      },
      {
        input: '1',
        expected: [1048576, 1048576]
      },
      {
        input: '1g',
        expected: [1048576, 1048576]
      },
      {
        input: '1m',
        expected: [1024, 1024]
      },
      {
        input: '1k',
        expected: [1, 1]
      }
    ];

    testCases.forEach(function(test) {
      it("value =" + test.input, function() {
        expect(hostController.convertMemoryToRange(test.input)).to.be.eql(test.expected);
      });
    });
  });

  describe("#rangeConvertNumber()", function() {

    var testCases = [
      {
        value: 'm',
        scale: '',
        expected: [0, 0]
      },
      {
        value: 1,
        scale: '',
        expected: [0.995, 1.004999999]
      },
      {
        value: 1,
        scale: 'g',
        expected: [0.995, 1.004999999]
      },
      {
        value: 1,
        scale: 'm',
        expected: [0.95, 1.04999]
      },
      {
        value: 1,
        scale: 'k',
        expected: [0.95, 1.04999]
      }
    ];

    testCases.forEach(function(test) {
      it("value = " + test.value + 'scale = ' + test.scale, function() {
        expect(hostController.rangeConvertNumber(test.value, test.scale)).to.be.eql(test.expected);
      });
    });
  });

  describe("#getComparisonType()", function() {

    var testCases = [
      {
        value: '1',
        expected: 'EQUAL'
      },
      {
        value: '>',
        expected: 'MORE'
      },
      {
        value: '<',
        expected: 'LESS'
      },
      {
        value: '=',
        expected: 'EQUAL'
      }
    ];

    testCases.forEach(function(test) {
      it("value = " + test.value, function() {
        expect(hostController.getComparisonType(test.value)).to.be.equal(test.expected);
      });
    });
  });

  describe("#filterByComponent()", function() {

    beforeEach(function() {
      sinon.stub(App.db, 'setFilterConditions');
    });
    afterEach(function() {
      App.db.setFilterConditions.restore();
    });

    it("component is null", function() {
      hostController.filterByComponent();
      expect(App.db.setFilterConditions.called).to.be.false;
    });

    it("component exist", function() {
      hostController.set('name', 'ctrl1');
      hostController.filterByComponent(Em.Object.create({
        componentName: 'C1'
      }));
      expect(App.db.setFilterConditions.calledWith('ctrl1', [{
        iColumn: 15,
        value: 'C1:ALL',
        type: 'string'
      }])).to.be.true;
    });
  });

  describe("#filterByStack()", function() {

    beforeEach(function() {
      sinon.stub(App.db, 'setFilterConditions');
    });
    afterEach(function() {
      App.db.setFilterConditions.restore();
    });

    it("displayName is null", function() {
      hostController.filterByStack(null, ['INSTALLED']);
      expect(App.db.setFilterConditions.called).to.be.false;
    });

    it("state is null", function() {
      hostController.filterByStack('stack1', null);
      expect(App.db.setFilterConditions.called).to.be.false;
    });

    it("stack and displayName exist", function() {
      hostController.set('name', 'ctrl1');
      hostController.filterByStack('stack1', ['INSTALLED']);
      expect(App.db.setFilterConditions.calledWith('ctrl1', [
      {
        iColumn: 16,
        value: 'stack1',
        type: 'string'
      },
      {
        iColumn: 17,
        value: ['INSTALLED'],
        type: 'string'
      }])).to.be.true;
    });
  });

  describe("#goToHostAlerts()", function() {

    beforeEach(function() {
      sinon.stub(App.router, 'transitionTo');
    });
    afterEach(function() {
      App.router.transitionTo.restore();
    });

    it("event is null", function() {
      hostController.goToHostAlerts(null);
      expect(App.router.transitionTo.called).to.be.false;
    });

    it("event.context is null", function() {
      hostController.goToHostAlerts({context: null});
      expect(App.router.transitionTo.called).to.be.false;
    });

    it("event.context is exist", function() {
      hostController.goToHostAlerts({context: {}});
      expect(App.router.transitionTo.calledWith('main.hosts.hostDetails.alerts', {})).to.be.true;
    });
  });

  describe("#removeHosts()", function() {

    it("host should be removed", function() {
      var host1 = Em.Object.create({id: 'host1', isChecked: true});
      hostController.set('content', [host1]);
      hostController.set('fullContent', [host1]);
      hostController.removeHosts();
      expect(hostController.get('fullContent')).to.be.empty;
    });
  });

  describe("#checkRemoved()", function() {

    it("host should be removed", function() {
      var host1 = Em.Object.create({id: 'host1', isChecked: true});
      hostController.set('content', [host1]);
      hostController.set('fullContent', [host1]);
      hostController.checkRemoved('host1');
      expect(hostController.get('fullContent')).to.be.empty;
    });
  });

});
