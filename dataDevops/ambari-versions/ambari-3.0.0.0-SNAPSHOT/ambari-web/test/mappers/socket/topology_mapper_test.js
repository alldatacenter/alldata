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

require('mappers/socket/topology_mapper');

describe('App.topologyMapper', function () {
  const mapper = App.topologyMapper;


  describe('#map', function () {
    const mockCtrl = {
      updateHost: sinon.spy()
    };
    beforeEach(function () {
      sinon.stub(mapper, 'applyComponentTopologyChanges');
      sinon.stub(App.router, 'get').returns(mockCtrl);
      App.set('clusterId', 1);
    });
    afterEach(function() {
      mapper.applyComponentTopologyChanges.restore();
      App.router.get.restore();
    });
    it('applyComponentTopologyChanges should be called', function () {
      mapper.map({clusters: {1: {components: []}}, eventType: 'UPDATE'});
      expect(mapper.applyComponentTopologyChanges.calledWith([], 'UPDATE')).to.be.true;
    });
    it('updateHost should be called on UPDATE event', function () {
      App.set('allHostNames', []);
      mapper.map({clusters: {1: {hosts: [{hostName: 'host1'}]}}, eventType: 'UPDATE'});
      expect(mockCtrl.updateHost.calledWith(Em.K, null, true)).to.be.true;
      expect(JSON.stringify(App.get('allHostNames'))).to.be.equal(JSON.stringify(['host1']));
    });
    it('updateHost should be called on DELETE event', function () {
      App.set('allHostNames', ['host2']);
      mapper.map({clusters: {1: {hosts: [{hostName: 'host2'}]}}, eventType: 'DELETE'});
      expect(mockCtrl.updateHost.calledWith(Em.K, null, true)).to.be.true;
      expect(App.get('allHostNames')).to.be.empty;
    });
  });

  describe('#applyComponentTopologyChanges', function () {
    beforeEach(function () {
      sinon.stub(mapper, 'addServiceIfNew');
      sinon.stub(mapper, 'createHostComponent');
      sinon.stub(mapper, 'deleteHostComponent');
      sinon.stub(mapper, 'deleteServiceIfHasNoComponents');
      sinon.stub(App.componentsStateMapper, 'updateComponentCountOnCreate');
      sinon.stub(App.componentsStateMapper, 'updateComponentCountOnDelete');
    });
    afterEach(function() {
      mapper.addServiceIfNew.restore();
      mapper.createHostComponent.restore();
      mapper.deleteHostComponent.restore();
      mapper.deleteServiceIfHasNoComponents.restore();
      App.componentsStateMapper.updateComponentCountOnCreate.restore();
      App.componentsStateMapper.updateComponentCountOnDelete.restore();
    });
    it('CREATE component event', function () {
      const components = [
        {
          hostNames: ['host1'],
          serviceName: 'S1',
          commandParams: {},
          publicHostNames: ['public1']
        }
      ];
      mapper.applyComponentTopologyChanges(components, 'UPDATE');
      expect(mapper.addServiceIfNew.calledWith('S1')).to.be.true;
      expect(mapper.createHostComponent.calledWith(components[0], 'host1', 'public1')).to.be.true;
      expect(App.componentsStateMapper.updateComponentCountOnCreate.calledWith(components[0])).to.be.true;
    });

    it('DELETE component event', function () {
      const components = [
        {
          hostNames: ['host1'],
          serviceName: 'S1'
        }
      ];
      mapper.applyComponentTopologyChanges(components, 'DELETE');
      expect(mapper.deleteHostComponent.calledWith(components[0], 'host1')).to.be.true;
      expect(mapper.deleteServiceIfHasNoComponents.calledWith('S1')).to.be.true;
      expect(App.componentsStateMapper.updateComponentCountOnDelete.calledWith(components[0])).to.be.true;
    });
  });

  describe('#addServiceIfNew', function () {
    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({isLoaded: false}));
      sinon.stub(App.store, 'safeLoad');
    });
    afterEach(function() {
      App.Service.find.restore();
      App.store.safeLoad.restore();
    });
    it('should load service if it does not exist yet', function () {
      mapper.addServiceIfNew('S1');
      expect(App.store.safeLoad.calledOnce).to.be.true;
    });
  });

  describe('#deleteServiceIfHasNoComponents', function () {
    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({isLoaded: true, hostComponents: []}));
      sinon.stub(mapper, 'deleteRecord');
    });
    afterEach(function() {
      App.Service.find.restore();
      mapper.deleteRecord.restore();
    });
    it('should delete service record', function () {
      mapper.deleteServiceIfHasNoComponents('S1');
      expect(mapper.deleteRecord.calledOnce).to.be.true;
    });
  });

  describe('#deleteHostComponent', function () {
    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns(Em.Object.create({isLoaded: true}));
      sinon.stub(mapper, 'deleteRecord');
      sinon.stub(mapper, 'updateHostComponentsOfHost');
      sinon.stub(mapper, 'updateHostComponentsOfService');
      sinon.stub(App.Host, 'find').returns(Em.Object.create({hostComponents: [{id: 'C1_host1'}]}));
      sinon.stub(App.Service, 'find').returns(Em.Object.create({hostComponents: [{id: 'C1_host1'}]}));
      mapper.deleteHostComponent({componentName: 'C1'}, 'host1');
    });
    afterEach(function() {
      App.HostComponent.find.restore();
      mapper.deleteRecord.restore();
      mapper.updateHostComponentsOfHost.restore();
      mapper.updateHostComponentsOfService.restore();
      App.Host.find.restore();
      App.Service.find.restore();
    });
    it('deleteRecord should be called', function () {
      expect(mapper.deleteRecord.calledOnce).to.be.true;
    });
    it('updateHostComponentsOfHost should be called', function () {
      expect(mapper.updateHostComponentsOfHost.calledWith(Em.Object.create({hostComponents: [{id: 'C1_host1'}]}), [])).to.be.true;
    });
    it('updateHostComponentsOfService should be called', function () {
      expect(mapper.updateHostComponentsOfService.calledWith(Em.Object.create({hostComponents: [{id: 'C1_host1'}]}), [])).to.be.true;
    });
  });

  describe('#createHostComponent', function () {
    beforeEach(function () {
      sinon.stub(App.store, 'safeLoad');
      sinon.stub(mapper, 'updateHostComponentsOfHost');
      sinon.stub(mapper, 'updateHostComponentsOfService');
      sinon.stub(App.Host, 'find').returns(Em.Object.create({hostComponents: [], isLoaded: true}));
      sinon.stub(App.Service, 'find').returns(Em.Object.create({hostComponents: []}));
      mapper.createHostComponent({componentName: 'C1'}, 'host1');
    });
    afterEach(function() {
      App.store.safeLoad.restore();
      mapper.updateHostComponentsOfHost.restore();
      mapper.updateHostComponentsOfService.restore();
      App.Host.find.restore();
      App.Service.find.restore();
    });
    it('deleteRecord should be called', function () {
      expect(App.store.safeLoad.calledOnce).to.be.true;
    });
    it('updateHostComponentsOfHost should be called', function () {
      expect(mapper.updateHostComponentsOfHost.calledOnce).to.be.true;
    });
    it('updateHostComponentsOfService should be called', function () {
      expect(mapper.updateHostComponentsOfService.calledOnce).to.be.true;
    });
  });

  describe('#updateHostComponentsOfHost', function () {
    beforeEach(function () {
      sinon.stub(App.store, 'safeLoad');
    });
    afterEach(function() {
      App.store.safeLoad.restore();
    });
    it('App.store.safeLoad should be called', function () {
      mapper.updateHostComponentsOfHost(Em.Object.create({id: 1}), [{id: 2}]);
      expect(App.store.safeLoad.calledWith(App.Host, {id: 1, hostComponents: [{id: 2}]}));
    });
  });

  describe('#updateHostComponentsOfService', function () {
    beforeEach(function () {
      sinon.stub(App.store, 'safeLoad');
    });
    afterEach(function() {
      App.store.safeLoad.restore();
    });
    it('App.store.safeLoad should be called', function () {
      mapper.updateHostComponentsOfService(Em.Object.create({id: 1}), [{id: 2}]);
      expect(App.store.safeLoad.calledWith(App.Service, {id: 1, hostComponents: [{id: 2}]}));
    });
  });
});
