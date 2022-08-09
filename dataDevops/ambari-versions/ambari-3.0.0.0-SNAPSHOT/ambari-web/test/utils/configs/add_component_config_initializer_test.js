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

require('models/configs/objects/service_config_property');
require('utils/configs/add_component_config_initializer');


describe('App.AddComponentConfigInitializer', function () {
  var serviceConfigProperty;
  var addComponentConfigInitializer = App.AddComponentConfigInitializer.create({
    initializeForProperties: [
      'zookeeper.connect',
      'ha.zookeeper.quorum',
      'hbase.zookeeper.quorum',
      'instance.zookeeper.host',
      'templeton.zookeeper.hosts',
      'hive.cluster.delegation.token.store.zookeeper.connectString',
      'yarn.resourcemanager.zk-address',
      'hive.zookeeper.quorum',
      'storm.zookeeper.servers',
      'hadoop.registry.zk.quorum',
      'atlas.audit.hbase.zookeeper.quorum',
      'atlas.graph.index.search.solr.zookeeper-url',
      'atlas.graph.storage.hostname',
      'atlas.kafka.zookeeper.connect'
    ]
  });

  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create();
  });

  describe('#getJSONStringifiedValueConfig', function () {
    it('should return correct type', function () {
      expect(addComponentConfigInitializer.getJSONStringifiedValueConfig().type).to.equal('json_stringified_value');
    })
  });

  describe('#_initAsJSONStrigifiedValueConfig', function () {
    it('should split by comma and change value to array of strings', function () {
      serviceConfigProperty.set('value', 'value0,value1,value2');
      addComponentConfigInitializer._initAsJSONStrigifiedValueConfig(serviceConfigProperty);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal("['value0','value1','value2']");
    });

    it('should split by slash and change value to array of strings', function () {
      serviceConfigProperty.set('value', 'value0/value1/value2');
      addComponentConfigInitializer._initAsJSONStrigifiedValueConfig(serviceConfigProperty, null, null, Em.Object.create({
        modifier: {
          delimiter: '/'
        }
      }));
      expect(serviceConfigProperty.get('recommendedValue')).to.equal("['value0','value1','value2']");
    });
  });

  describe('#updateSiteObj', function () {
    beforeEach(function () {
      sinon.stub(App.config, 'updateHostsListValue');
    });

    afterEach(function () {
      App.config.updateHostsListValue.restore();
    });

    it('should return false if no params and not update anything', function () {
      expect(addComponentConfigInitializer.updateSiteObj()).to.equal(false);
      expect(App.config.updateHostsListValue.called).to.equal(false);
    });

    it('should call updateHostsListValue if params are present', function () {
      addComponentConfigInitializer.updateSiteObj({}, {});
      expect(App.config.updateHostsListValue.called).to.equal(true);
    });

    it('should call updateHostsListValue with correct params', function () {
      addComponentConfigInitializer.set('initializers', {name: {type: 'json_stringified_value'}});
      var siteConfigs = {};
      var configProperty = {
        name: 'name',
        fileName: 'fileName',
        value: 'value'
      };
      addComponentConfigInitializer.updateSiteObj(siteConfigs, configProperty);
      expect(App.config.updateHostsListValue.calledWith(siteConfigs, configProperty.fileName, configProperty.name, configProperty.value, false)).to.equal(true);
    })
  });

  describe('#_initTempletonHiveProperties', function () {
    var configProperty = {
      value: 'thrift123,'
    };
    var localDB = {
      masterComponentHosts: [{component: 'HIVE_METASTORE', hostName: '1', isInstalled: true}]
    };
    var dependecies = {hiveMetastorePort: 3333};
    it('should return correct configProperty', function () {
      expect(addComponentConfigInitializer._initTempletonHiveProperties(configProperty, localDB, dependecies).value).to.equal('thrift://1:3333,');
    });

    it('should not change nothing if config property value has not separator', function () {
      configProperty = {
        value: 'thrift123'
      };

      expect(addComponentConfigInitializer._initTempletonHiveProperties(configProperty, localDB, dependecies).value).to.equal('thrift123');
    });

    it('should not change nothing if master component hosts have not HIVE_METASTORE component', function () {
      localDB = {
        masterComponentHosts: [{component: 'ATLAS', hostName: '1', isInstalled: false}]
      };
      expect(addComponentConfigInitializer._initTempletonHiveProperties(configProperty, localDB, dependecies).value).to.equal('thrift123');
    });

    it('should not change nothing if master component hosts have not installed HIVE_METASTORE component', function () {
      localDB = {
        masterComponentHosts: [{component: 'HIVE_METASTORE', hostName: '1', isInstalled: false}]
      };
      expect(addComponentConfigInitializer._initTempletonHiveProperties(configProperty, localDB, dependecies).value).to.equal('thrift123');
    });
  });
});
