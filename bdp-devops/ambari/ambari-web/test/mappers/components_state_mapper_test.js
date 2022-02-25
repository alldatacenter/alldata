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
require('mappers/components_state_mapper');

describe('App.componentsStateMapper', function () {

  describe('#getComponentConfig', function() {
    
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        isClient: true
      }));
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });
    it('should paths to component properties', function() {
      expect(App.componentsStateMapper.getComponentConfig('DATANODE')).to.be.eql({
        "data_nodes_installed": "ServiceComponentInfo.installed_count",
        "data_nodes_started": "ServiceComponentInfo.started_count",
        "data_nodes_total": "ServiceComponentInfo.total_count"
      });
    });
    it('Client paths', function() {
      expect(App.componentsStateMapper.getComponentConfig('HDFS_CLIENT')).to.be.eql({
        "installed_clients": "ServiceComponentInfo.total_count"
      });
    });
  });

  describe('#getExtendedModel', function() {

    it('should return HDFS extended model', function() {
      expect(App.componentsStateMapper.getExtendedModel('HDFS')).to.be.eql(App.HDFSService.find('HDFS'));
    });

    it('should return null when service does not have extended model', function() {
      expect(App.componentsStateMapper.getExtendedModel('S1')).to.be.null;
    });
  });

  describe('#mapExtendedModelComponents', function() {
    var serviceModel = Em.Object.create({isLoaded: true});
    var serviceExtendedModel = Em.Object.create({isLoaded: true});
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(serviceModel);
      sinon.stub(App.componentsStateMapper, 'getExtendedModel').returns(serviceExtendedModel);
      sinon.stub(App.componentsStateMapper, 'parseIt').returns({
        prop1: 'val1'
      });
      App.cache['services'] = [
        {
          ServiceInfo: {
            service_name: 'S1'
          }
        }
      ];
      App.componentsStateMapper.mapExtendedModelComponents({
        ServiceComponentInfo: {
          service_name: 'S1',
          component_name: 'C1'
        }
      });
    });
    afterEach(function() {
      App.Service.find.restore();
      App.componentsStateMapper.getExtendedModel.restore();
      App.componentsStateMapper.parseIt.restore();
      App.cache['services'] = [];
    });

    it('should set properties to cacheService', function() {
      expect(App.cache['services'][0].prop1).to.be.equal('val1');
    });

    it('should set properties to service model', function() {
      expect(serviceModel.get('prop1')).to.be.equal('val1');
    });

    it('should set properties to service extended model', function() {
      expect(serviceExtendedModel.get('prop1')).to.be.equal('val1');
    });
  });

  describe('#updateStaleConfigsHosts', function() {
    var model = Em.Object.create();
    beforeEach(function() {
      sinon.stub(App.ClientComponent, 'getModelByComponentName').returns(model);
      App.componentsStateMapper.updateStaleConfigsHosts('C1', ['host1']);
    });
    afterEach(function() {
      App.ClientComponent.getModelByComponentName.restore();
      App.cache.staleConfigsComponentHosts = {}
    });

    it('should set hosts to cache map', function() {
      expect(App.cache.staleConfigsComponentHosts).to.be.eql({'C1': ['host1']});
    });

    it('should set hosts to model', function() {
      expect(model.get('staleConfigHosts')).to.be.eql(['host1']);
    });
  });

  describe('#componentStateToJSON', function() {

    it('should return raw componentState', function() {
      var model = Em.Object.create({
        componentName: 'C1',
        service: {
          serviceName: 'S1'
        },
        installedCount: 1,
        installFailedCount: 2,
        initCount: 3,
        unknownCount: 4,
        startedCount: 5,
        totalCount: 15
      });
      expect(App.componentsStateMapper.componentStateToJSON(model)).to.be.eql({
        ServiceComponentInfo: {
          component_name: 'C1',
          service_name: 'S1',
          installed_count: 1,
          install_failed_count: 2,
          init_count: 3,
          unknown_count: 4,
          started_count: 5,
          total_count: 15
        }
      })
    });
  });

  describe('#updateComponentCountOnStateChange', function() {
    var model;
    beforeEach(function() {
      model = Em.Object.create({
        isLoaded: true,
        initCount: 1,
        installFailedCount: 0,
        totalCount: 2,
        installedCount: 1,
        startedCount: 0
      });
      sinon.stub(App.ClientComponent, 'getModelByComponentName').returns(model);
      sinon.stub(App.componentsStateMapper, 'mapExtendedModelComponents');
      sinon.stub(App.componentsStateMapper, 'componentStateToJSON');
    });
    afterEach(function() {
      App.ClientComponent.getModelByComponentName.restore();
      App.componentsStateMapper.mapExtendedModelComponents.restore();
      App.componentsStateMapper.componentStateToJSON.restore();
    });

    it('mapExtendedModelComponents should be called', function() {
      App.componentsStateMapper.updateComponentCountOnStateChange({
        componentName: 'C1',
        previousState: 'INIT',
        currentState: 'INSTALL_FAILED'
      });

      expect(App.componentsStateMapper.mapExtendedModelComponents.calledOnce).to.be.true;
    });

    it('state from INIT to INSTALL_FAILED', function() {
      App.componentsStateMapper.updateComponentCountOnStateChange({
        componentName: 'C1',
        previousState: 'INIT',
        currentState: 'INSTALL_FAILED'
      });

      expect(model.get('initCount')).to.be.equal(0);
      expect(model.get('installFailedCount')).to.be.equal(1);
    });

    it('state from INSTALLED to STARTED', function() {
      App.componentsStateMapper.updateComponentCountOnStateChange({
        componentName: 'C1',
        previousState: 'INSTALLED',
        currentState: 'STARTED'
      });

      expect(model.get('installedCount')).to.be.equal(0);
      expect(model.get('startedCount')).to.be.equal(1);
    });
  });


  describe('#updateComponentCountOnDelete', function() {
    var model;
    beforeEach(function() {
      model = Em.Object.create({
        installedCount: 1,
        totalCount: 1
      });
      sinon.stub(App.ClientComponent, 'getModelByComponentName').returns(model);
      sinon.stub(App.componentsStateMapper, 'mapExtendedModelComponents');
      sinon.stub(App.componentsStateMapper, 'componentStateToJSON');
    });
    afterEach(function() {
      App.ClientComponent.getModelByComponentName.restore();
      App.componentsStateMapper.mapExtendedModelComponents.restore();
      App.componentsStateMapper.componentStateToJSON.restore();
    });

    it('mapExtendedModelComponents should be called', function() {
      App.componentsStateMapper.updateComponentCountOnDelete({
        componentName: 'C1',
        lastComponentState: 'INSTALLED'
      });

      expect(App.componentsStateMapper.mapExtendedModelComponents.calledOnce).to.be.true;
    });

    it('update counters when component deleted', function() {
      App.componentsStateMapper.updateComponentCountOnDelete({
        componentName: 'C1',
        lastComponentState: 'INSTALLED'
      });

      expect(model.get('totalCount')).to.be.equal(0);
      expect(model.get('installedCount')).to.be.equal(0);
    });
  });

  describe('#updateComponentCountOnCreate', function() {
    var model;
    beforeEach(function() {
      model = Em.Object.create({
        isLoaded: true,
        initCount: 0,
        totalCount: 0
      });
      sinon.stub(App.ClientComponent, 'getModelByComponentName').returns(model);
      sinon.stub(App.componentsStateMapper, 'mapExtendedModelComponents');
      sinon.stub(App.componentsStateMapper, 'componentStateToJSON');
    });
    afterEach(function() {
      App.ClientComponent.getModelByComponentName.restore();
      App.componentsStateMapper.mapExtendedModelComponents.restore();
      App.componentsStateMapper.componentStateToJSON.restore();
    });

    it('mapExtendedModelComponents should be called', function() {
      App.componentsStateMapper.updateComponentCountOnCreate({
        componentName: 'C1'
      });

      expect(App.componentsStateMapper.mapExtendedModelComponents.calledOnce).to.be.true;
    });

    it('update counters when component created', function() {
      App.componentsStateMapper.updateComponentCountOnCreate({
        componentName: 'C1'
      });

      expect(model.get('totalCount')).to.be.equal(1);
      expect(model.get('initCount')).to.be.equal(1);
    });
  });

  describe('#statusToProperty', function() {

    it('INIT to initCount', function() {
      expect(App.componentsStateMapper.statusToProperty('INIT')).to.be.equal('initCount');
    });

    it('INSTALL_FAILED to installFailedCount', function() {
      expect(App.componentsStateMapper.statusToProperty('INSTALL_FAILED')).to.be.equal('installFailedCount');
    });

  });
});
