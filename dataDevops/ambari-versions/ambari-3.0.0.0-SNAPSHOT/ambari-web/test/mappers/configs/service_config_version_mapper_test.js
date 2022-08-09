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
require('mappers/configs/service_config_version_mapper');

describe('App.serviceConfigVersionsMapper', function () {

  describe('#map', function() {
    var json = {
      itemTotal: 2,
      items: [
        {
          service_name: 'S1',
          service_config_version: 'v1',
          is_current: false,
          createtime: 1,
          group_id: 1,
          user: 'admin',
          is_cluster_compatible: true,
          group_name: 'g1',
          service_config_version_note: 'notes...',
          stack_id: 'HDP-1',
          hosts: ['host1']
        },
        {
          service_name: 'S1',
          service_config_version: 'v1',
          is_current: true,
          createtime: 1,
          group_id: -1,
          user: 'admin',
          is_cluster_compatible: true,
          group_name: 'g2',
          service_config_version_note: 'notes...',
          stack_id: 'HDP-1',
          hosts: []
        }
      ]
    };
    var currentVersionMap;

    beforeEach(function() {
      currentVersionMap = {
        S1_g2: Em.Object.create({
          isCurrent: true
        })
      };
      sinon.stub(App.router, 'set');
      sinon.stub(App.serviceConfigVersionsMapper, 'getCurrentVersionMap').returns(currentVersionMap);
      sinon.stub(App, 'dateTimeWithTimeZone').returns(1);
      sinon.stub(App.serviceConfigVersionsMapper, 'setHostsForDefaultCG');
      sinon.stub(App.ServiceConfigVersion, 'find').returns({clear: Em.K});
      sinon.stub(App.store, 'safeLoadMany');
      sinon.stub(App.router, 'get').returns('configHistory');
      App.serviceConfigVersionsMapper.map(json);
    });
    afterEach(function() {
      App.router.set.restore();
      App.serviceConfigVersionsMapper.getCurrentVersionMap.restore();
      App.dateTimeWithTimeZone.restore();
      App.serviceConfigVersionsMapper.setHostsForDefaultCG.restore();
      App.ServiceConfigVersion.find.restore();
      App.store.safeLoadMany.restore();
      App.router.get.restore();
    });

    it('should call safeLoadMany', function() {
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([
        {
          "author": "admin",
          "create_time": 1,
          "group_id": 1,
          "group_name": "g1",
          "hosts": [
            "host1"
          ],
          "id": "S1_v1",
          "index": 0,
          "is_compatible": true,
          "is_current": false,
          "is_requested": true,
          "notes": "notes...",
          "raw_create_time": 1,
          "service_id": "S1",
          "service_name": "S1",
          "stack_version": "HDP-1",
          "version": "v1"
        },
        {
          "author": "admin",
          "create_time": 1,
          "group_id": 'S1_default',
          "group_name": "g2",
          "hosts": [],
          "id": "S1_v1",
          "index": 1,
          "is_compatible": true,
          "is_current": true,
          "is_requested": true,
          "notes": "notes...",
          "raw_create_time": 1,
          "service_id": "S1",
          "service_name": "S1",
          "stack_version": "HDP-1",
          "version": "v1"
        }
      ]);
    });

    it('currentVersionMap should have not current version', function() {
      expect(currentVersionMap.S1_g2.get('isCurrent')).to.be.false;
    });

    it('App.router.set should be called', function() {
      expect(App.router.set.calledWith('mainConfigHistoryController.filteredCount', 2)).to.be.true;
    });

    it('setHostsForDefaultCG should be called', function() {
      expect(App.serviceConfigVersionsMapper.setHostsForDefaultCG.calledOnce).to.be.true;
    });

    it('App.ServiceConfigVersion.find should be called', function() {
      expect(App.ServiceConfigVersion.find.calledOnce).to.be.true;
    });
  });

  describe('#setHostsForDefaultCG', function() {
    var serviceToHostMap = {
      'S1': ['host2']
    };
    var result = [
      {
        is_current: true,
        group_name: 'Default',
        service_name: 'S1'
      }
    ];

    beforeEach(function() {
      sinon.stub(App, 'get').returns(['host1', 'host2']);
    });
    afterEach(function() {
      App.get.restore();
    });

    it('should set hosts to default group', function() {
      App.serviceConfigVersionsMapper.setHostsForDefaultCG(serviceToHostMap, result);
      expect(result[0].hosts).to.be.eql(['host1']);
    });
  });

  describe('#getCurrentVersionMap', function() {
    beforeEach(function() {
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        Em.Object.create({
          isCurrent: true,
          serviceName: 'S1',
          groupName: 'g1'
        })
      ]);
    });
    afterEach(function() {
      App.ServiceConfigVersion.find.restore();
    });

    it('should return current version map', function() {
      expect(App.serviceConfigVersionsMapper.getCurrentVersionMap()).to.be.eql({
        'S1_g1': Em.Object.create({
          isCurrent: true,
          serviceName: 'S1',
          groupName: 'g1'
        })
      });
    });
  });

  describe('#makeId', function() {

    it('should return id', function() {
      expect(App.serviceConfigVersionsMapper.makeId('S1', 'v1')).to.be.equal('S1_v1');
    });
  });

});

