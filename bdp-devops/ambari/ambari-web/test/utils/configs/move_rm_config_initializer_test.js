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

describe('App.MoveRmConfigInitializer', function () {
  var initializer;

  describe('#_initAsRmHaDepended', function () {
    var configProperty = Em.Object.create({value: 'test1:test2:test3'});
    var localDb = {};
    var dependencies = {targetHostName: 'test'};
    var initializer = {rmHaShouldBeEnabled: true};

    it('should not change property if isRMHaEnabled is not equal to initializer property', function () {
      sinon.stub(App, 'get').returns(false);
      var result = App.MoveRmConfigInitializer._initAsRmHaDepended(configProperty, localDb, dependencies, initializer);
      expect(result.get('value')).to.be.equal('test1:test2:test3');
      App.get.restore();
    });

    it('should change property if isRMHaEnabled is equal to initializer property', function () {
      sinon.stub(App, 'get').returns(true);
      var result = App.MoveRmConfigInitializer._initAsRmHaDepended(configProperty, localDb, dependencies, initializer);
      expect(result.get('value')).to.be.equal('test:test2:test3');
      App.get.restore();
    });
  });

  describe('#_initAsRmHaHawq', function () {
    var configProperty = Em.Object.create({value: 'test1:test2:test3'});
    var localDb = {};
    var dependencies = {
      targetHostName: 'target-test',
      sourceHostName: 'source-test',
      rm1: 'source-test',
      rm2: 'target-test'
    };
    var initializer = {rmHaShouldBeEnabled: true};

    it('should not change property if isRMHaEnabled is not equal to initializer property', function () {
      sinon.stub(App, 'get').returns(false);
      var result = App.MoveRmConfigInitializer._initAsRmHaHawq(configProperty, localDb, dependencies, initializer);
      expect(result.get('value')).to.be.equal('test1:test2:test3');
      App.get.restore();
    });

    it('should change property if isRMHaEnabled is equal to initializer property and use 8030 port', function () {
      sinon.stub(App, 'get').returns(true);
      var result = App.MoveRmConfigInitializer._initAsRmHaHawq(configProperty, localDb, dependencies, initializer);
      expect(result.get('value')).to.be.equal('target-test:8030,target-test:8030');
      App.get.restore();
    });

    it('should change property if isRMHaEnabled is equal to initializer property and use 8032 port', function () {
      var dependencies = {
        targetHostName: 'target-test',
        sourceHostName: 'target-test',
        rm1: 'source-test',
        rm2: 'test'
      };
      var configProperty = Em.Object.create({
        name: 'yarn.resourcemanager.ha',
        value: 'test1:test2:test3'
      });
      sinon.stub(App, 'get').returns(true);
      var result = App.MoveRmConfigInitializer._initAsRmHaHawq(configProperty, localDb, dependencies, initializer);
      expect(result.get('value')).to.be.equal('source-test:8032,target-test:8032');
      App.get.restore();
    });
  });
});