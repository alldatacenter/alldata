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

describe('App.NnHaConfigInitializer', function () {
  var initializer;
  beforeEach(function () {
    initializer = App.NnHaConfigInitializer;
  });

  describe('#_initWithRename', function () {
    it('should init display name and name properties if they are not provided', function () {
      var configProperty = Em.Object.create({});
      initializer._initWithRename(configProperty, {}, {namespaceId: 'test'}, {});
      expect(configProperty.get('name')).to.be.eql('');
      expect(configProperty.get('displayName')).to.be.eql('');
    });

    it('should init display name and name properties if they are not provided', function () {
      var configProperty = Em.Object.create({
        name: 'test123',
        displayName: 'test:test'
      });
      initializer._initWithRename(configProperty, {}, {namespaceId: 'TEST'}, {toReplace: 'test'});
      expect(configProperty.get('name')).to.be.equal('TEST123');
      expect(configProperty.get('displayName')).to.be.equal('TEST:test');
    });
  });

  describe('#_initAsNamespace', function () {
    it('should set namespaceId as value if prefix and suffix are empty strings', function () {
      var configProperty = Em.Object.create({recommendedValue: '', value: ''});
      initializer._initAsNamespace(configProperty, {}, {namespaceId: 'test'}, {modifier: {prefix: '', suffix: ''}});
      expect(configProperty.get('value')).to.be.equal('test');
      expect(configProperty.get('recommendedValue')).to.be.equal('test');
    });

    it('should set namespaceId as value with prefix and suffix', function () {
      var configProperty = Em.Object.create({recommendedValue: '', value: ''});
      initializer._initAsNamespace(configProperty, {}, {namespaceId: 'test'}, {modifier: {prefix: 'pre-', suffix: '-suff'}});
      expect(configProperty.get('value')).to.be.equal('pre-test-suff');
      expect(configProperty.get('recommendedValue')).to.be.equal('pre-test-suff');
    });
  });

  describe('#_initWithNamespace', function () {
    it('should change display name and name properties to correct one', function () {
      var configProperty = Em.Object.create({
        value: 'test123',
        recommendedValue: 'test:test'
      });
      initializer._initWithNamespace(configProperty, {}, {namespaceId: 'TEST'}, {toReplace: 'test'});
      expect(configProperty.get('value')).to.be.equal('TEST123');
      expect(configProperty.get('recommendedValue')).to.be.equal('TEST:test');
    });
  });

  describe('#_initHbaseRootDir', function () {
    beforeEach(function () {
      sinon.stub(initializer, '_initHbaseRootDirForHbase');
      sinon.stub(initializer, '_initHbaseRootDirForAMS');
    });
    afterEach(function () {
      initializer._initHbaseRootDirForHbase.restore();
      initializer._initHbaseRootDirForAMS.restore();
    });
    it('should not call anything if filename is not hbase-site, ams-hbase-site', function() {
      var configProperty = Em.Object.create({filename: 'test'});
      initializer._initHbaseRootDir(configProperty, {}, {}, {});
      expect(initializer._initHbaseRootDirForHbase.called).to.be.false;
      expect(initializer._initHbaseRootDirForAMS.called).to.be.false;
    });

    it('should call _initHbaseRootDirForHbase if filename is hbase-site', function() {
      var configProperty = Em.Object.create({filename: 'hbase-site'});
      initializer._initHbaseRootDir(configProperty, {}, {}, {});
      expect(initializer._initHbaseRootDirForHbase.called).to.be.true;
      expect(initializer._initHbaseRootDirForAMS.called).to.be.false;
    });

    it('should call _initHbaseRootDirForHbase if filename is ams-hbase-site', function() {
      var configProperty = Em.Object.create({filename: 'ams-hbase-site'});
      initializer._initHbaseRootDir(configProperty, {}, {}, {});
      expect(initializer._initHbaseRootDirForHbase.called).to.be.false;
      expect(initializer._initHbaseRootDirForAMS.called).to.be.true;
    });
  });

  describe('#_initHbaseRootDirForHbase', function () {
    it('should not change anything if HBASE is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initHbaseRootDirForHbase(configProperty, {installedServices: ['HIVE']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should change hbase root dir to correct one', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var serverConfigs = [{
        type: 'hbase-site',
        properties: {
          'hbase.rootdir': 'http://hbase:4200',
          'test.rootdir': 'http://test:4200',
        }
      }, {
        type: 'test-site',
        properties: {
          'hbase.rootdir': 'http://hbase:4201',
          'test.rootdir': 'http://test:4200',
        }
      }];
      initializer._initHbaseRootDirForHbase(configProperty, {installedServices: ['HBASE']}, {serverConfigs: serverConfigs, namespaceId: 'test123'}, {});
      expect(configProperty.get('value')).to.be.equal('http://test123');
      expect(configProperty.get('recommendedValue')).to.be.equal('http://test123');
    });
  });

  describe('#_initHbaseRootDirForAMS', function () {
    it('should not change anything if AMBARI_METRICS is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initHbaseRootDirForAMS(configProperty, {installedServices: ['HIVE', 'HDFS']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should not change anything if hbase.rootdir not contains hdfs link', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var localDB =  {
        installedServices: ['AMBARI_METRICS', 'HDFS'],
        masterComponentHosts: [
          {component: 'NAMENODE', isInstalled: true, hostName: 'test1'},
          {component: 'TEST', isInstalled: true, hostName: 'test2'},
          {component: 'NAMENODE', isInstalled: false, hostName: 'test3'}
        ]
      };
      var serverConfigs = [{type: 'ams-hbase-site', properties: {'hbase.rootdir': 'hdfs://test'}}, {type: 'ams-hive-site'}];
      initializer._initHbaseRootDirForAMS(configProperty, localDB, {serverConfigs: serverConfigs}, {});
      expect(configProperty.get('value')).to.be.equal('hdfs://test');
      expect(configProperty.get('recommendedValue')).to.be.equal('hdfs://test');
    });

    it('should change property if hbase.rootdir contains hdfs link', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var localDB =  {
        installedServices: ['AMBARI_METRICS', 'HDFS'],
        masterComponentHosts: [
          {component: 'NAMENODE', isInstalled: true, hostName: 'test'},
          {component: 'TEST', isInstalled: true, hostName: 'test2'},
          {component: 'NAMENODE', isInstalled: false, hostName: 'test3'}
        ]
      };
      var serverConfigs = [{type: 'ams-hbase-site', properties: {'hbase.rootdir': 'hdfs://test'}}, {type: 'ams-hive-site'}];
      initializer._initHbaseRootDirForAMS(configProperty, localDB, {serverConfigs: serverConfigs, namespaceId: 'test123'}, {});
      expect(configProperty.get('value')).to.be.equal('hdfs://test123');
      expect(configProperty.get('recommendedValue')).to.be.equal('hdfs://test123');
    });
  });

  describe('#_initHawqDfsUrl', function () {
    it('should not change anything if HAWQ is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initHawqDfsUrl(configProperty, {installedServices: ['HIVE', 'HDFS']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should change anything if HAWQ is installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var localDB =  {
        installedServices: ['AMBARI_METRICS', 'HAWQ']
      };
      var serverConfigs = [{type: 'hawq-site', properties: {'hawq_dfs_url': 'http://test:4200/t'}}, {type: 'ams-hive-site'}];
      initializer._initHawqDfsUrl(configProperty, localDB, {serverConfigs: serverConfigs, namespaceId: 'https://test123:8080'}, {});
      expect(configProperty.get('value')).to.be.equal('https://test123:8080/t');
      expect(configProperty.get('recommendedValue')).to.be.equal('https://test123:8080/t');
    });
  });

  describe('#_initInstanceVolumes', function () {
    it('should not change anything if ACCUMULO is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initInstanceVolumes(configProperty, {installedServices: ['HIVE', 'HDFS']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should change anything if HAWQ is installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var localDB =  {
        installedServices: ['AMBARI_METRICS', 'ACCUMULO']
      };
      var serverConfigs = [{type: 'accumulo-site', properties: {'instance.volumes': 'http://test:4200'}}, {type: 'ams-hive-site'}];
      initializer._initInstanceVolumes(configProperty, localDB, {serverConfigs: serverConfigs, namespaceId: 'test123:8080'}, {});
      expect(configProperty.get('value')).to.be.equal('http://test123:8080');
      expect(configProperty.get('recommendedValue')).to.be.equal('http://test123:8080');
    });
  });

  describe('#_initInstanceVolumesReplacements', function () {
    it('should not change anything if ACCUMULO is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initInstanceVolumesReplacements(configProperty, {installedServices: ['HIVE', 'HDFS']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should change anything if HAWQ is installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      var localDB =  {
        installedServices: ['AMBARI_METRICS', 'ACCUMULO']
      };
      var serverConfigs = [{type: 'accumulo-site', properties: {'instance.volumes': 'http://test:4200'}}, {type: 'ams-hive-site'}];
      initializer._initInstanceVolumesReplacements(configProperty, localDB, {serverConfigs: serverConfigs, namespaceId: 'test123:8080'}, {});
      expect(configProperty.get('value')).to.be.equal('http://test:4200 http://test123:8080');
      expect(configProperty.get('recommendedValue')).to.be.equal('http://test:4200 http://test123:8080');
    });
  });

  describe('#_initDfsJnEditsDir', function () {
    it('should not change anything if HDFS is not installed', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      initializer._initDfsJnEditsDir(configProperty, {installedServices: ['HIVE']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
    });

    it('should not change anything if isHadoopWindowsStack is false', function () {
      var configProperty = Em.Object.create({
        value: '',
        recommendedValue: ''
      });
      sinon.stub(App, 'get').returns(false);
      initializer._initDfsJnEditsDir(configProperty, {installedServices: ['HIVE', 'HDFS']}, {}, {});
      expect(configProperty.get('value')).to.be.equal('');
      expect(configProperty.get('recommendedValue')).to.be.equal('');
      App.get.restore();
    });
  });
});