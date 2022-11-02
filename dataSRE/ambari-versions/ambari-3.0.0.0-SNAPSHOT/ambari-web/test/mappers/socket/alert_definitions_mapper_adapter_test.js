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

require('mappers/socket/alert_definitions_mapper_adapter');

describe('App.alertDefinitionsMapperAdapter', function () {

  describe('#map', function() {
    beforeEach(function() {
      sinon.stub(App.alertDefinitionsMapper, 'map');
      sinon.stub(App.AlertDefinition, 'find').returns({});
      sinon.stub(App.alertDefinitionsMapperAdapter, 'deleteRecord');
      App.set('clusterId', 1);
    });
    afterEach(function() {
      App.alertDefinitionsMapper.map.restore();
      App.AlertDefinition.find.restore();
      App.alertDefinitionsMapperAdapter.deleteRecord.restore();
    });

    it('should call App.alertDefinitionsMapper.map on UPDATE event', function() {
      var event = {
        eventType: 'UPDATE',
        clusters: {
          1: {
            alertDefinitions: [
              {
                definitionId: 1,
                componentName: 'C1',
                serviceName: 'S1'
              }
            ]
          }
        }
      };
      App.alertDefinitionsMapperAdapter.map(event);
      expect(App.alertDefinitionsMapper.map.getCall(0).args[0]).to.be.eql({
        items: [{
          AlertDefinition: {
            id: 1,
            component_name: 'C1',
            service_name: 'S1',
            definitionId: 1,
            componentName: 'C1',
            serviceName: 'S1'
          }
        }]
      });
    });

    it('should call deleteRecord on DELETE event', function() {
      var event = {
        eventType: 'DELETE',
        clusters: {
          1: {
            alertDefinitions: [
              {
                definitionId: 1
              }
            ]
          }
        }
      };
      App.alertDefinitionsMapperAdapter.map(event);
      expect(App.alertDefinitionsMapperAdapter.deleteRecord.calledWith({})).to.be.true;
    });
  });
});
