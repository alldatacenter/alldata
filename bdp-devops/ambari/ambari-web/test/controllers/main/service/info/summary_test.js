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
require('controllers/main/service/info/summary');
function getController() {
  return App.MainServiceInfoSummaryController.create();
}

describe('App.MainServiceInfoSummaryController', function () {

  var controller;

  beforeEach(function () {
    controller = App.MainServiceInfoSummaryController.create();
  });

  describe('#setRangerPlugins', function () {

    var cases = [
      {
        isLoaded: true,
        isRangerPluginsArraySet: false,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster loaded, ranger plugins array not set'
      },
      {
        isLoaded: false,
        isRangerPluginsArraySet: false,
        expectedIsRangerPluginsArraySet: false,
        title: 'cluster not loaded, ranger plugins array not set'
      },
      {
        isLoaded: false,
        isRangerPluginsArraySet: true,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster not loaded, ranger plugins array set'
      },
      {
        isLoaded: true,
        isRangerPluginsArraySet: true,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster loaded, ranger plugins array set'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS'
        }),
        Em.Object.create({
          serviceName: 'YARN'
        }),
        Em.Object.create({
          serviceName: 'HIVE'
        })
      ]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS',
          configTypes: {
            'ranger-hdfs-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'HIVE',
          displayName: 'Hive',
          configTypes: {
            'hive-env': {}
          }
        }),
        Em.Object.create({
          serviceName: 'HBASE',
          displayName: 'HBase',
          configTypes: {
            'ranger-hbase-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'KNOX',
          displayName: 'Knox',
          configTypes: {
            'ranger-knox-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'STORM',
          displayName: 'Storm',
          configTypes: {
            'ranger-storm-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'YARN',
          displayName: 'YARN',
          configTypes: {}
        })
      ]);
    });

    afterEach(function () {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isRangerPluginsArraySet', item.isRangerPluginsArraySet);
        App.set('router.clusterController.isLoaded', item.isLoaded);
        expect(controller.get('isRangerPluginsArraySet')).to.equal(item.expectedIsRangerPluginsArraySet);
        expect(controller.get('rangerPlugins').filterProperty('isDisplayed').mapProperty('serviceName').sort()).to.eql(['HDFS', 'HIVE']);
      });
    });

  });

  describe('#getRangerPluginsStatusSuccess', function () {

    beforeEach(function () {
      controller.getRangerPluginsStatusSuccess({
        'items': [
          {
            'type': 'ranger-hdfs-plugin-properties',
            'properties': {
              'ranger-hdfs-plugin-enabled': 'Yes'
            }
          },
          {
            'type': 'hive-env',
            'properties': {
              'hive_security_authorization': 'Ranger'
            }
          },
          {
            'type': 'ranger-hbase-plugin-properties',
            'properties': {
              'ranger-hbase-plugin-enabled': ''
            }
          }
        ]
      });
    });

    it('isPreviousRangerConfigsCallFailed is false', function () {
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.false;
    });
    it('rangerPlugins.HDFS status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HDFS').status).to.equal(Em.I18n.t('alerts.table.state.enabled'));
    });
    it('rangerPlugins.HIVE status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HIVE').status).to.equal(Em.I18n.t('alerts.table.state.enabled'));
    });
    it('rangerPlugins.HBASE status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HBASE').status).to.equal(Em.I18n.t('common.unknown'));
    });
  });

  describe('#getRangerPluginsStatusError', function () {

    it('should set isPreviousRangerConfigsCallFailed to true', function () {
      controller.getRangerPluginsStatusError();
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.true;
    });

  });

});