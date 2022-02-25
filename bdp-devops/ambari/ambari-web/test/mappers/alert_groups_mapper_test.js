/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('mappers/alert_groups_mapper');
var testHelpers = require('test/helpers');

describe('App.alertGroupsMapper', function () {

  describe('#map', function () {

    var json = {
      items: [
        {
          "AlertGroup" : {
            "default" : true,
            "definitions" : [
              {
                "id" : 8,
                "source_type" : "PORT"
              },
              {
                "id" : 9,
                "source_type" : "AGGREGATE"
              }
            ],
            "id" : 3,
            "name" : "ZOOKEEPER",
            "targets": [{id: 1}, {id: 2}]
          }
        },
        {
          "AlertGroup" : {
            "default" : true,
            "definitions" : [
              {
                "id" : 1,
                "source_type" : "METRIC"
              },
              {
                "id" : 2,
                "source_type" : "WEB"
              },
              {
                "id" : 3,
                "source_type" : "WEB"
              },
              {
                "id" : 4,
                "source_type" : "AGGREGATE"
              },
              {
                "id" : 5,
                "source_type" : "METRIC"
              },
              {
                "id" : 6,
                "source_type" : "SCRIPT"
              },
              {
                "id" : 7,
                "source_type" : "WEB"
              }
            ],
            "id" : 2,
            "name" : "YARN",
            "targets": [{id: 2}, {id: 3}]
          }
        }
      ]
    };

    beforeEach(function () {

      sinon.stub(App.store, 'fastCommit', Em.K);
      sinon.stub(App.store, 'loadMany', function (type, content) {
        type.content = content;
      });

      App.alertGroupsMapper.set('model', {});
      App.cache.previousAlertGroupsMap = {};

    });

    afterEach(function () {

      App.store.fastCommit.restore();
      App.store.loadMany.restore();
      App.alertGroupsMapper.set('model', App.AlertGroup);
      App.cache.previousAlertGroupsMap = {};
      App.cache.alertNotificationsGroupsMap = {};

    });

    /*eslint-disable mocha-cleanup/asserts-limit */
    it('should parse alert groups', function() {

      var expected = [
        {
          id: 3,
          name: 'ZOOKEEPER',
          default: true,
          definitions: [8,9],
          targets: [1, 2]
        },
        {
          id: 2,
          name: 'YARN',
          default: true,
          definitions: [1, 2, 3, 4, 5, 6, 7],
          targets: [2, 3]
        }
      ];

      App.alertGroupsMapper.map(json);
      var mapped = App.alertGroupsMapper.get('model.content');
      testHelpers.nestedExpect(expected, mapped);
    });
    /*eslint-enable mocha-cleanup/asserts-limit */

    it('should set App.cache.previousAlertGroupsMap', function () {

      var expected = {
        8: [3],
        9: [3],
        1: [2],
        2: [2],
        3: [2],
        4: [2],
        5: [2],
        6: [2],
        7: [2]
      };

      App.alertGroupsMapper.map(json);

      expect(App.cache.previousAlertGroupsMap).to.eql(expected);

    });

    describe('should delete not existing groups', function () {

      var groups = [
        {id: 1},
        {id: 2},
        {id: 3},
        {id: 4}
      ];

      beforeEach(function () {

        sinon.stub(App.AlertGroup, 'find', function() {
          if (arguments.length) {
            return groups.findProperty('id', arguments[0]);
          }
          return groups;
        });

        sinon.stub(App.alertGroupsMapper, 'deleteRecord', Em.K);

      });

      afterEach(function () {
        App.AlertGroup.find.restore();
        App.alertGroupsMapper.deleteRecord.restore();
      });

      it('should call deleteRecord with not existing groups', function () {

        App.alertGroupsMapper.map(json);
        expect(App.alertGroupsMapper.deleteRecord.calledTwice).to.be.true;
        // first call
        expect(App.alertGroupsMapper.deleteRecord.args[0][0].id).to.equal(1);
        // second call
        expect(App.alertGroupsMapper.deleteRecord.args[1][0].id).to.equal(4);

      });

    });

  });

});