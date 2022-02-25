/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * License); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var modelSetup = require('test/init_model_test');
require('models/service/yarn');

var yarnService,
  yarnServiceData = {
    id: 'yarn'
  },
  configs = [
    {
      properties: {
        'yarn.timeline-service.webapp.address': '0.0.0.0:0000'
      },
      tag: 'version2',
      type: 'yarn-site'
    }
  ];

describe('App.YARNService', function () {

  beforeEach(function () {
    yarnService = App.YARNService.createRecord(yarnServiceData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(yarnService);
  });

  describe('#ahsWebPort', function () {

    afterEach(function () {
      App.db.setConfigs([]);
    });

    it('should be 8188 as default', function () {
      App.db.setConfigs([]);
      expect(yarnService.get('ahsWebPort')).to.equal('8188');
    });

    it('should get value from configs', function () {
      App.db.setConfigs(configs);
      expect(yarnService.get('ahsWebPort')).to.equal('0000');
    });

  });

  describe('#queueFormatted', function () {
    it('should return formatted string', function () {
      yarnService.set('queue', '{"root":{"default":{}}}');
      expect(yarnService.get('queueFormatted')).to.equal('default (/root)<br/>');
    });
  });

  describe('#queuesCount', function () {
    it('should be 1', function () {
      yarnService.set('queue', '{"root":{"default":{}}}');
      expect(yarnService.get('queuesCount')).to.equal(1);
    });
  });

  describe('#maxMemory', function () {
    it('should add availableMemory to allocatedMemory', function () {
      yarnService.set('allocatedMemory', 1024);
      yarnService.set('availableMemory', 2048);
      expect(yarnService.get('maxMemory')).to.equal(3072);
    });
  });

  describe('#allQueueNames', function () {
    it('should list all queue names as array', function () {
      yarnService.set('queue', '{"root":{"default":{}}}');
      expect(yarnService.get('allQueueNames')).to.eql(['root', 'root/default']);
    });
  });

  describe('#childQueueNames', function () {
    it('should list child queue names as array', function () {
      yarnService.set('queue', '{"root":{"default":{}}}');
      expect(yarnService.get('childQueueNames')).to.eql(['root/default']);
    });
  });
/*
  describe('#nodeManagersCountLost', function () {
    nodeCountCases.forEach(function (item) {
      it('should be ' + item.nodeManagersCountLost, function () {
        setHostComponents();
        for (var prop in item.assets) {
          yarnService.set(prop, item.assets[prop]);
        };
        expect(yarnService.get('nodeManagersCountLost')).to.equal(item.nodeManagersCountLost);
      });
    });
  });
*/
});
