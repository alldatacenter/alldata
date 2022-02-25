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

require('mappers/socket/alert_groups_mapper_adapter');

describe('App.alertGroupsMapperAdapter', function () {

  describe('#map', function() {
    beforeEach(function() {
      sinon.stub(App.alertGroupsMapper, 'map');
      sinon.stub(App.AlertGroup, 'find').returns({});
      sinon.stub(App.alertGroupsMapperAdapter, 'deleteRecord');
      App.set('clusterId', 1);
    });
    afterEach(function() {
      App.alertGroupsMapper.map.restore();
      App.AlertGroup.find.restore();
      App.alertGroupsMapperAdapter.deleteRecord.restore();
    });

    it('should call App.alertGroupsMapper.map on UPDATE event', function() {
      var event = {
        updateType: 'UPDATE',
        groups: [
          {
            id: 1,
            definitions: [2]
          }
        ]
      };
      App.alertGroupsMapperAdapter.map(event);
      expect(App.alertGroupsMapper.map.getCall(0).args[0]).to.be.eql({
        items: [{
          AlertGroup: {
            id: 1,
            definitions: [
              {
                id: 2
              }
            ]
          }
        }]
      });
    });

    it('should call deleteRecord on DELETE event', function() {
      var event = {
        updateType: 'DELETE',
        groups: [
          {
            id: 1
          }
        ]
      };
      App.alertGroupsMapperAdapter.map(event);
      expect(App.alertGroupsMapperAdapter.deleteRecord.calledWith({})).to.be.true;
    });
  });
});
