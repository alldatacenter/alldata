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
require('mappers/configs/config_groups_mapper');


describe('App.configGroupsMapper', function () {

  var allHosts = App.get('allHostNames');
  var defaultAllHosts = ['host1', 'host2', 'host3'];
  beforeEach(function () {
    App.set('allHostNames', defaultAllHosts);
  });
  afterEach(function(){
    App.set('allHostNames', allHosts);
  });

  describe("#map", function() {

    var json = {
      "items" : [
        {
          "ConfigGroup" : {
            "cluster_name" : "1",
            "description" : "1",
            "desired_configs" : [
              {
                "tag" : "version1426088081862",
                "type" : "hadoop-env"
              }
            ],
            "group_name" : "1",
            "hosts" : [
              {
                "host_name" : "host1"
              }
            ],
            "id" : 2,
            "tag" : "Service1"
          }
        }
      ]
    };

    beforeEach(function () {
      sinon.stub(App.store, 'safeLoadMany', Em.K);
      sinon.stub(App.configGroupsMapper, 'generateDefaultGroup').returns({id: 'default', is_default: true});
    });
    afterEach(function(){
      App.store.safeLoadMany.restore();
      App.configGroupsMapper.generateDefaultGroup.restore();
    });

    it('should not call safeLoadMany when no service provided', function() {
      App.configGroupsMapper.map(json);
      expect(App.store.safeLoadMany.called).to.be.false;
    });

    it('should call safeLoadMany when service provided', function() {
      App.configGroupsMapper.map(null, null, ['S1']);
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([{id: 'default', is_default: true}]);
    });

    it('should call safeLoadMany when json provided', function() {
      App.configGroupsMapper.map(json, null, ['S1']);
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([
        {
          "description": "1",
          "desired_configs": [
            {
              "tag": "version1426088081862",
              "type": "hadoop-env"
            }
          ],
          "hosts": [
            "host1"
          ],
          "id": 2,
          "name": "1",
          "parent_config_group_id": "Service1_Default",
          "service_id": "Service1",
          "service_name": "Service1"
        },
        {
          id: 'default',
          is_default: true
        }
      ]);
    });
  });

  describe("generateDefaultGroup", function() {
    var tests = [
      {
        service: 's1',
        hosts: ['host1'],
        res: {
          id: 's1_Default',
          name: 'Default',
          service_name: 's1',
          description: 'Default cluster level S1 configuration',
          hosts: ['host1'],
          child_config_groups: [],
          service_id: 's1',
          desired_configs: [],
          properties: [],
          is_default: true
        },
        m: 'with hosts'
      },
      {
        service: 's1',
        childConfigGroups: [{}],
        res: {
          id: 's1_Default',
          name: 'Default',
          service_name: 's1',
          description: 'Default cluster level S1 configuration',
          hosts: ['host2', 'host3', 'host1'],
          child_config_groups: [{}],
          service_id: 's1',
          desired_configs: [],
          properties: [],
          is_default: true
        },
        m: 'without hosts, with childConfigGroups'
      }
    ];

    beforeEach(function() {
      sinon.stub(App.configGroupsMapper, '_getAllHosts').returns(['host2', 'host3', 'host1']);
    });
    afterEach(function() {
      App.configGroupsMapper._getAllHosts.restore();
    });

    tests.forEach(function(t) {
      it('generates default config group mock object ' + t.m, function() {
        expect(App.configGroupsMapper.generateDefaultGroup(t.service, t.hosts, t.childConfigGroups)).to.be.eql(t.res);
      });
    });
  });
});
