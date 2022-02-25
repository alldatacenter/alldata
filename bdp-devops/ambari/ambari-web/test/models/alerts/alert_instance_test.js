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

require('models/alerts/alert_instance');

var model;

function getModel() {
  return App.AlertInstance.createRecord();
}

describe('App.AlertInstance', function () {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedGetByKey(getModel(), 'shortStateMsg', 'shortState', 'state', {
    map: {
      CRITICAL: 'CRIT',
      WARNING: 'WARN',
      OK: 'OK',
      UNKNOWN: 'UNKWN',
      PENDING: 'NONE'
    }
  });

  describe('#serviceDisplayName', function () {

    it('should get name for non-existing service', function () {
      model.set('serviceName', 'FOOBAR');
      expect(model.get('serviceDisplayName')).to.equal('Foobar');
    });

  });

  describe('#statusChangedAndLastCheckedFormatted', function () {

    it('should Status Changed before Last Checked', function () {

      var lastCheckedFormatted = '123',
        lastTriggeredFormatted = '321';

      model.reopen({
        lastCheckedFormatted: lastCheckedFormatted,
        lastTriggeredFormatted: lastTriggeredFormatted
      });
      var status = model.get('statusChangedAndLastCheckedFormatted');
      expect(status.indexOf(lastCheckedFormatted)).to.be.above(status.indexOf(lastTriggeredFormatted));
    });

  });

  describe('#escapeSpecialCharactersFromTooltip', function () {
    it('it Should Display Alert Without special characters "<" and ">"', function () {

      model.set('text', '<urlopen error [Errno 111] Connection refused>');
      var resultedText = model.get('escapeSpecialCharactersFromTooltip');

      expect(resultedText).to.equal('urlopen error [Errno 111] Connection refused');
    });
  });

});
