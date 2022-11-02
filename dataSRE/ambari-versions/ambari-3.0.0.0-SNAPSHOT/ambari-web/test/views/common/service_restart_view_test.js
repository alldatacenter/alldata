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

describe('App.ServiceRestartView', function() {
  var view;
  beforeEach(function () {
    view = App.ServiceRestartView.create({});
    view.initDefaultConfigs();
  });

  describe('#initDefaultConfigs', function () {
    it('should set correct default configs', function () {
      expect(view.get('useRolling')).to.be.true;
      expect(view.get('showAdvancedOptions')).to.be.false;
      expect(view.get('showBatchRackOptions')).to.be.false;
      expect(view.get('batchesOfHosts')).to.be.true;
      expect(view.get('noOfHostsInBatch')).to.be.equal(10);
      expect(view.get('batchIntervalHosts')).to.be.equal(120);
      expect(view.get('percentRackStarted')).to.be.equal(100);
      expect(view.get('batchIntervalRacks')).to.be.equal(120);
      expect(view.get('isRetryChecked')).to.be.true;
      expect(view.get('noOfRetriesPerHost')).to.be.equal(2);
      expect(view.get('maxFailuresTolerated')).to.be.equal(10);
      expect(view.get('maxFailuresBatch')).to.be.equal(2);
      expect(view.get('maxFailuresRack')).to.be.equal(2);
      expect(view.get('suppressAlerts')).to.be.true;
      expect(view.get('pauseAfterFirst')).to.be.false;
    });
  });

  describe('#getRestartConfigs', function () {
    it('should return batchInterval as batchIntervalHosts if batchesOfHosts', function () {
      var result = view.getRestartConfigs();
      expect(result).to.be.eql(Em.Object.create({
        batchInterval: view.get('batchIntervalHosts'),
        maxFailures: view.get('maxFailuresTolerated'),
        maxFailuresPerBatch: view.get('maxFailuresTolerated'),
      }));
    });

    it('should return batchInterval as batchIntervalRacks if no batchesOfHosts', function () {
      view.set('batchesOfHosts', false);
      view.set('batchIntervalRacks', 95);
      var result = view.getRestartConfigs();
      expect(result).to.be.eql(Em.Object.create({
        batchInterval: view.get('batchIntervalRacks'),
        maxFailures: view.get('maxFailuresTolerated'),
        maxFailuresPerBatch: view.get('maxFailuresTolerated'),
      }));
    });
  });

  describe('#getNoOfHosts', function () {
    it('should return noOfHostsInBatch if batchesOfHosts is truthly', function () {
      var result = view.getNoOfHosts();
      expect(result).to.be.equal(view.get('noOfHostsInBatch'));
    });

    it('should return correct value if batchesOfHosts is falsy', function () {
      view.set('batchesOfHosts', false);
      view.set('componentHostRackInfoMap', {
        test: {
          size: function () { return 200}
        }
      });
      var result = view.getNoOfHosts('test');
      expect(result).to.be.equal(50);
    });
  });

  describe('#toggleAdvancedOptions', function () {
    it('should toggle showAdvancedOptions', function () {
      view.set('showAdvancedOptions', true);
      view.toggleAdvancedOptions();
      expect(view.get('showAdvancedOptions')).to.be.false;
      view.toggleAdvancedOptions();
      expect(view.get('showAdvancedOptions')).to.be.true;
    });
  });
});