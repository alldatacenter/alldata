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

describe('App.MoveHmConfigInitializer', function () {
  var initializer;
  beforeEach(function () {
    initializer = Em.Object.create(App.MoveHmConfigInitializer);
  });

  describe('#__getHmHostsConsideringMoved', function () {
    it('should map masterComponentsHosts to hostNames and filter out no HIVE_METASTORE components', function () {
      var result = initializer.__getHmHostsConsideringMoved({masterComponentHosts: [
          {component: 'HIVE_METASTORE', hostName: 'test1'},
          {component: 'HIVE_METASTORE', hostName: 'test2'},
          {component: 'TEST', hostName: 'test3'},
        ]}, {});
      expect(result).to.be.eql(['test1', 'test2', undefined]);
    });

    it('should change host from dependencies and change it with dependenices target', function () {
      var result = initializer.__getHmHostsConsideringMoved({masterComponentHosts: [
        {component: 'HIVE_METASTORE', hostName: 'test1'},
        {component: 'HIVE_METASTORE', hostName: 'test2'},
      ]}, {sourceHostName: 'test1', targetHostName: 'test4'});
      expect(result).to.be.eql(['test2', 'test4']);
    });

    it('should add host from dependencies target if there is no target host', function () {
      var result = initializer.__getHmHostsConsideringMoved({masterComponentHosts: [
          {component: 'HIVE_METASTORE', hostName: 'test1'},
          {component: 'HIVE_METASTORE', hostName: 'test2'},
        ]}, {sourceHostName: 'test3', targetHostName: 'test4'});
      expect(result).to.be.eql(['test1', 'test2', 'test4']);
    });

    it('should always return unique host names', function () {
      var result = initializer.__getHmHostsConsideringMoved({masterComponentHosts: [
          {component: 'HIVE_METASTORE', hostName: 'test1'},
          {component: 'HIVE_METASTORE', hostName: 'test2'},
          {component: 'HIVE_METASTORE', hostName: 'test2'},
        ]}, {sourceHostName: 'test3', targetHostName: 'test4'});
      expect(result).to.be.eql(['test1', 'test2', 'test4']);
    });
  });

  describe('#_initTempletonHiveProperties', function () {
    it('should replace value thrift with correct value', function () {
      var result = initializer._initTempletonHiveProperties(
        Em.Object.create({value: 'thrift-01,'}),
        {masterComponentHosts: [
            {component: 'HIVE_METASTORE', hostName: 'test1'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
          ]},
        {sourceHostName: 'test3', targetHostName: 'test4'}
      );
      expect(result.get('value')).to.be.equal('test1\\,test2\\,test4,');
    });

    it('should not replace anything if value is not found', function () {
      var result = initializer._initTempletonHiveProperties(
        Em.Object.create({value: 'test value'}),
        {masterComponentHosts: [
            {component: 'HIVE_METASTORE', hostName: 'test1'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
          ]},
        {sourceHostName: 'test3', targetHostName: 'test4'}
      );
      expect(result.get('value')).to.be.equal('test value');
    });
  });

  describe('#_initHiveMetastoreUris', function () {
    it('should not change property value if config tag is not equal hive-site', function () {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('test');
      var result = initializer._initHiveMetastoreUris(Em.Object.create({value: 'test'}), {}, {});
      expect(result.get('value')).to.be.equal('test');
      App.config.getConfigTagFromFileName.restore();
    });

    it('should set correct value if config tag is equal to hive-site and set 9083 port by default', function () {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('hive-site');
      var result = initializer._initHiveMetastoreUris(
        Em.Object.create({value: 'test'}),
        {masterComponentHosts: [
            {component: 'HIVE_METASTORE', hostName: 'test1'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
            {component: 'HIVE_METASTORE', hostName: 'test2'}
          ]},
        {sourceHostName: 'test3', targetHostName: 'test4'}
      );
      expect(result.get('value')).to.be.equal('thrift://test1:9083,thrift://test2:9083,thrift://test4:9083');
      App.config.getConfigTagFromFileName.restore();
    });

    it('should set correct value if config tag is equal to hive-site and set correct port', function () {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('hive-site');
      var result = initializer._initHiveMetastoreUris(
        Em.Object.create({value: 'test:4200'}),
        {masterComponentHosts: [
            {component: 'HIVE_METASTORE', hostName: 'test1'},
            {component: 'HIVE_METASTORE', hostName: 'test2'},
            {component: 'HIVE_METASTORE', hostName: 'test2'}
          ]},
        {sourceHostName: 'test3', targetHostName: 'test4'}
      );
      expect(result.get('value')).to.be.equal('thrift://test1:4200,thrift://test2:4200,thrift://test4:4200');
      App.config.getConfigTagFromFileName.restore();
    });
  });
});