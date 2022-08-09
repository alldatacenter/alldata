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

describe('App.HostsBasedInitializerMixin', function () {
  var view;
  beforeEach(function () {
    view = Em.Object.create(App.HostsBasedInitializerMixin);
  });

  describe('_initAsHostWithComponent', function () {
    var testCases = [
      {
        configProperty : Em.Object.create({}),
        localDB : {masterComponentHosts: []},
        dependencies : [],
        initializer : {},
        testDescription: 'should not change anything if no component avalilable',
        expectedResult: Em.Object.create({})
      },
      {
        configProperty : Em.Object.create({}),
        localDB : {masterComponentHosts: [{component: 'TEST', hostName: 'test'}]},
        dependencies : [],
        initializer : {component: 'TEST'},
        testDescription: 'should not change anything if no component avalilable',
        expectedResult: Em.Object.create({
          recommendedValue: 'test',
          value: 'test'
        })
      }
    ];
    testCases.forEach(function (c) {
      it(c.testDescription, function () {
        view._initAsHostWithComponent(c.configProperty, c.localDB, c.dependencies, c.initializer);
        expect(c.configProperty).to.be.eql(c.expectedResult);
      });
    });
  });

  describe('#getComponentsHostsConfig', function () {
    it('should set as array property to false if only one parameter', function () {
      expect(view.getComponentsHostsConfig([])).to.be.eql({
        type: 'hosts_with_components',
        components: [],
        asArray: false,
        isInstalled: undefined
      });
    });
    it('should set as array property to true if more than one parameter', function () {
      expect(view.getComponentsHostsConfig([], true, true)).to.be.eql({
        type: 'hosts_with_components',
        components: [],
        asArray: true,
        isInstalled: true
      });
    });
  });

  describe('#_initAsHostsWithComponents', function () {
    var testCases = [{
      configPropery: Em.Object.create({}),
      localDB: {
        masterComponentHosts: [{
          component: 'test1',
          isInstalled: true,
          hostName: 't1'
        }, {
          component: 'test2',
          isInstalled: true,
          hostName: 't2'
        }, {
          component: 'test3',
          isInstalled: true,
          hostName: 't3'
        }]
      },
      dependencies: [],
      initializer: {
        components: ['test1', 'test3'],
        isInstalled: true,
        asArray: true
      },
      testDescription: 'It should set found host names',
      expectedResult: Em.Object.create({
        value: ['t1', 't3'],
        recommendedValue: ['t1', 't3']
      })
    }, {
      configPropery: Em.Object.create({}),
      localDB: {
        masterComponentHosts: [{
          component: 'test1',
          isInstalled: true,
          hostName: 't1'
        }, {
          component: 'test2',
          isInstalled: true,
          hostName: 't2'
        }, {
          component: 'test3',
          isInstalled: true,
          hostName: 't3'
        }]
      },
      dependencies: [],
      initializer: {
        components: ['test1', 'test3'],
        isInstalled: true,
        asArray: false
      },
      testDescription: 'It should set found host names as string',
      expectedResult: Em.Object.create({
        value: 't1,t3',
        recommendedValue: 't1,t3'
      })
    }];
    testCases.forEach(function (c) {
      it(c.testDescription, function() {
        view._initAsHostsWithComponents(c.configPropery, c.localDB, c.dependencies, c.initializer);
        expect(c.configPropery).to.be.eql(c.expectedResult);
      });
    });
  });

  describe('#getSimpleComponentConfig', function () {
    var component = Em.Object.create({});
    it('should return simple config if with modifier property is false', function () {
      expect(view.getSimpleComponentConfig(component, false)).to.be.eql({
        type: 'host_with_component',
        component: component
      });
    });

    it('should return correct config if with modifier property is true', function () {
      expect(view.getSimpleComponentConfig(component)).to.be.eql({
        type: 'host_with_component',
        component: component,
        modifier: {
          type: 'regexp',
          regex: "([\\w|\\.]*)(?=:)"
        }
      });
    });
  });

  describe('#_initAsHostWithPort', function () {
    it('should set correct confgi property', function() {
      var config = Em.Object.create({});
      var localDB = {masterComponentHosts: [
        {component: 'test1', hostName: 'T1', isInstalled: true},
        {component: 'test2', hostName: 'T2', isInstalled: true}
      ]};
      var dependencies = [];
      var initializer = {
        component: 'test2',
        componentExists: true,
        modifier: {
          prefix: 'prefix',
          suffix: 'suffix'
        }
      };
      view._initAsHostWithPort(config, localDB, dependencies, initializer);
      expect(config).to.be.eql(Em.Object.create({
        value: 'prefixT2suffix',
        recommendedValue: 'prefixT2suffix'
      }));
    });
  });

  describe('#getHostsWithPortConfig', function () {
    it('should retrun proper config', function () {
      var component = {};
      var prefix = 'prefix';
      expect(view.getHostsWithPortConfig(component, prefix)).to.be.eql({
        type: 'hosts_with_port',
        component: component,
        port: undefined,
        modifier: {
          prefix: prefix,
          suffix: '',
          delimiter: ','
        }
      });

    });

    it('should retrun proper config', function () {
      var component = {};
      var prefix = 'prefix';
      var suffix = 'suffix';
      expect(view.getHostsWithPortConfig(component, prefix, suffix, ',', '4200', true)).to.be.eql({
        type: 'hosts_with_port',
        component: component,
        portKey: '4200',
        modifier: {
          prefix: prefix,
          suffix: suffix,
          delimiter: ','
        }
      });
    });
  });
});