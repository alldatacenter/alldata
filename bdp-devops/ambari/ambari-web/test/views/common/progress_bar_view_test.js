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

describe('App.ProgressBarView', function () {
  var view = App.ProgressBarView.create();

  describe("#progressWidth", function () {
    it("depends on `progress`", function () {
      view.set('progress', 1);
      view.propertyDidChange('progressWidth');
      expect(view.get('progressWidth')).to.equal('width:1%;');
    });
  });

  describe("#barClass", function () {
    var testCases = [
      {
        status: 'FAILED',
        result: 'progress-bar-danger'
      },
      {
        status: 'ABORTED',
        result: 'progress-bar-warning'
      },
      {
        status: 'TIMED_OUT',
        result: 'progress-bar-warning'
      },
      {
        status: 'COMPLETED',
        result: 'progress-bar-success'
      },
      {
        status: 'QUEUED',
        result: 'progress-bar-info active progress-bar-striped'
      },
      {
        status: 'PENDING',
        result: 'progress-bar-info active progress-bar-striped'
      },
      {
        status: 'IN_PROGRESS',
        result: 'progress-bar-info active progress-bar-striped'
      },
      {
        status: null,
        result: 'progress-bar-info'
      }
    ];
    testCases.forEach(function (test) {
      it("status is " + test.status, function () {
        view.set('status', test.status);
        view.propertyDidChange('barClass');
        expect(view.get('barClass')).to.equal(test.result);
      });
    });
  });
});