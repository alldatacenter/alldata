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

describe('App.MoveNameNodeConfigInitializer', function () {
  var initializer;
  beforeEach(function () {
    initializer = Em.Object.create(App.MoveNameNodeConfigInitializer);
  });

  describe('#_initInstanceVolumes', function () {
    var configProperty = Em.Object.create({value: 'http://test.com/testurl'});
    var dependencies = {targetHostName: 'testhost'};
    it('should no change anything if HA is enabled', function () {
      sinon.stub(App, 'get').returns(true);
      var result = initializer._initInstanceVolumes(configProperty, {installedServices: ['ACCUMULO', 'test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
      App.get.restore();
    });

    it('should no change anything if intalled services does not contains ACCUMULO', function () {
      var result = initializer._initInstanceVolumes(configProperty, {installedServices: ['test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should change config property value if ACCUMULO is installed and HA is disabled', function () {
      var result = initializer._initInstanceVolumes(configProperty, {installedServices: ['test', 'ACCUMULO']}, dependencies);
      expect(result.get('value')).to.be.equal('http://testhost:8020/testurl');
    });
  });

  describe('#_initInstanceVolumesReplacements', function () {
    var configProperty = Em.Object.create({value: 'http://test.com/testurl'});
    var dependencies = {targetHostName: 'target-host', sourceHostName: 'source-host'};
    it('should no change anything if HA is enabled', function () {
      sinon.stub(App, 'get').returns(true);
      var result = initializer._initInstanceVolumesReplacements(configProperty, {installedServices: ['ACCUMULO', 'test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
      App.get.restore();
    });

    it('should no change anything if intalled services does not contains ACCUMULO', function () {
      var result = initializer._initInstanceVolumesReplacements(configProperty, {installedServices: ['test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should change config property value if ACCUMULO is installed and HA is disabled', function () {
      var result = initializer._initInstanceVolumesReplacements(configProperty, {installedServices: ['test', 'ACCUMULO']}, dependencies);
      var resStr = 'hdfs://source-host:8020/apps/accumulo/data hdfs://target-host:8020/apps/accumulo/data';
      expect(result.get('value')).to.be.equal(resStr);
    });
  });

  describe('#_initHbaseRootDir', function () {
    var dependencies = {targetHostName: 'testhost'};
    it('should no change anything if HA is enabled', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hbase-site'});
      sinon.stub(App, 'get').returns(true);
      var result = initializer._initHbaseRootDir(configProperty, {installedServices: ['HBASE', 'test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
      App.get.restore();
    });

    it('should no change anything if intalled services does not contains HBASE', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hbase-site'});
      var result = initializer._initHbaseRootDir(configProperty, {installedServices: ['test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should no change anything if filename is not equal hbase-site', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl'});
      var result = initializer._initHbaseRootDir(configProperty, {installedServices: ['test', 'HBASE']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should change config property to correct one', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hbase-site'});
      var result = initializer._initHbaseRootDir(configProperty, {installedServices: ['test', 'HBASE']}, dependencies);
      expect(result.get('value')).to.be.equal('http://testhost:8020/testurl');
    });
  });

  describe('#_initHawqDfsUrl', function () {
    var dependencies = {targetHostName: 'testhost'};
    it('should no change anything if HA is enabled', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hawq-site'});
      sinon.stub(App, 'get').returns(true);
      var result = initializer._initHawqDfsUrl(configProperty, {installedServices: ['HAWQ', 'test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
      App.get.restore();
    });

    it('should no change anything if intalled services does not contains HBASE', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hawq-site'});
      var result = initializer._initHawqDfsUrl(configProperty, {installedServices: ['test']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should no change anything if filename is not equal hbase-site', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl'});
      var result = initializer._initHawqDfsUrl(configProperty, {installedServices: ['test', 'HAWQ']}, dependencies);
      expect(result.get('value')).to.be.equal('http://test.com/testurl');
    });

    it('should change config property to correct one', function () {
      var configProperty = Em.Object.create({value: 'http://test.com/testurl', filename: 'hawq-site'});
      var result = initializer._initHawqDfsUrl(configProperty, {installedServices: ['test', 'HAWQ']}, dependencies);
      expect(result.get('value')).to.be.equal('testhost://test.com/testurl');
    });
  });
});