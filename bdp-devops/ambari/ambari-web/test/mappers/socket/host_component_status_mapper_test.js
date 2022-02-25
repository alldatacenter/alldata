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

require('mappers/socket/host_component_status_mapper');

describe('App.hostComponentStatusMapper', function () {

  describe('#map', function() {
    var hc = Em.Object.create({
      workStatus: 'INSTALLED',
      staleConfigs: false,
      passiveState: 'ON',
      isLoaded: true
    });
    const event = {
      hostComponents: [
        {
          componentName: 'C1',
          hostName: 'host1',
          currentState: 'STARTED',
          staleConfigs: false,
          maintenanceState: 'OFF'
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns(hc);
      sinon.stub(App.hostComponentStatusMapper, 'updateComponentsWithStaleConfigs');
      sinon.stub(App.componentsStateMapper, 'updateComponentCountOnStateChange');
    });
    afterEach(function() {
      App.HostComponent.find.restore();
      App.hostComponentStatusMapper.updateComponentsWithStaleConfigs.restore();
      App.componentsStateMapper.updateComponentCountOnStateChange.restore();
    });

    it('host-component should have STARTED status', function() {
      App.hostComponentStatusMapper.map(event);
      expect(hc.get('workStatus')).to.be.equal('STARTED');
    });

    it('host-component should have staleConfigs false', function() {
      App.hostComponentStatusMapper.map(event);
      expect(hc.get('staleConfigs')).to.be.false;
    });

    it('host-component should have maintenanceState OFF', function() {
      App.hostComponentStatusMapper.map(event);
      expect(hc.get('passiveState')).to.be.equal('OFF');
    });

    it('updateComponentsWithStaleConfigs should be called', function() {
      App.hostComponentStatusMapper.map(event);
      expect(App.hostComponentStatusMapper.updateComponentsWithStaleConfigs.calledWith(
        {
          componentName: 'C1',
          hostName: 'host1',
          currentState: 'STARTED',
          staleConfigs: false,
          maintenanceState: 'OFF'
        }
      )).to.be.true;
    });

    it('updateComponentCountOnStateChange should be called', function() {
      App.hostComponentStatusMapper.map(event);
      expect(App.componentsStateMapper.updateComponentCountOnStateChange.calledWith(
        {
          componentName: 'C1',
          hostName: 'host1',
          currentState: 'STARTED',
          staleConfigs: false,
          maintenanceState: 'OFF'
        }
      )).to.be.true;
    });
  });

  describe('#updateComponentsWithStaleConfigs', function() {
    beforeEach(function() {
      sinon.stub(App.componentsStateMapper, 'updateStaleConfigsHosts');
    });
    afterEach(function() {
      App.componentsStateMapper.updateStaleConfigsHosts.restore();
    });

    it('updateStaleConfigsHosts should be called', function() {
      var state = {
        staleConfigs: true,
        componentName: 'C1',
        hostName: 'host1'
      };
      App.hostComponentStatusMapper.updateComponentsWithStaleConfigs(state);
      expect(App.componentsStateMapper.updateStaleConfigsHosts.calledWith('C1', ['host1'])).to.be.true;
    });
  });
});
