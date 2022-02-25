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
require('views/common/rolling_restart_view');

describe('App.RollingRestartView', function () {

  var view = App.RollingRestartView.create({
    restartHostComponents: []
  });

  describe('#initialize', function () {
    var testCases = [
      {
        restartHostComponents: [],
        result: {
          batchSize: 1,
          tolerateSize: 1
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(10),
        result: {
          batchSize: 1,
          tolerateSize: 1
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(11),
        result: {
          batchSize: 2,
          tolerateSize: 2
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(20),
        result: {
          batchSize: 2,
          tolerateSize: 2
        }
      },
      {
        hostComponentName: 'DATANODE',
        restartHostComponents: new Array(20),
        result: {
          batchSize: 1,
          tolerateSize: 1
        }
      }
    ];

    testCases.forEach(function (test) {
      describe(test.restartHostComponents.length + ' components to restart', function () {

        beforeEach(function () {
          view.set('batchSize', -1);
          view.set('interBatchWaitTimeSeconds', -1);
          view.set('tolerateSize', -1);
          view.set('hostComponentName', test.hostComponentName);
          view.set('restartHostComponents', test.restartHostComponents);
          view.initialize();
        });

        it('batchSize is ' + test.result.batchSize, function() {
          expect(view.get('batchSize')).to.equal(test.result.batchSize);
        });

        it('tolerateSize is ' + test.result.tolerateSize, function() {
          expect(view.get('tolerateSize')).to.equal(test.result.tolerateSize);
        });
      })
    }, this);
  });
});
