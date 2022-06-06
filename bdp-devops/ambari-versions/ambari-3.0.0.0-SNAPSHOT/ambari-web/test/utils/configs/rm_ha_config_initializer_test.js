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
    initializer = App.RmHaConfigInitializer;
  });

  describe('#setup', function () {
    it('should call _updateInitializers with current params', function () {
      sinon.stub(initializer, '_updateInitializers');
      var settings = {};
      initializer.setup(settings);
      expect(initializer._updateInitializers.calledOnce).to.be.true;
      expect(initializer._updateInitializers.calledWith(settings)).to.be.true;
    });
  });

  describe('#cleanup', function () {
    it('should call _restoreInitializers', function () {
      sinon.stub(initializer, '_restoreInitializers');
      initializer.cleanup();
      expect(initializer._restoreInitializers.calledOnce).to.be.true;
    });
  });

  describe('#_initRmHaHostsWithPort', function () {
    it('should ignore not RESOURCEMANAGER components and set concated hostnames with port from initializer', function () {
      var property = Em.Object.create({});
      var localDB = {masterComponentHosts: [
        {component: 'T1', hostName: 'test1'},
        {component: 'RESOURCEMANAGER', hostName: 'test2'},
        {component: 'RESOURCEMANAGER', hostName: 'test3'},
        {component: 'T4', hostName: 'test4'}
      ]};
      var initilize = {port: 4200};
      initializer._initRmHaHostsWithPort(property, localDB, {}, initilize);
      expect(property.get('value')).to.be.equal('test2:4200,test3:4200');
    });
  });
});