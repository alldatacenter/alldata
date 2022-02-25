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

require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/text_widget_single_threshold');

function getView() {
  return App.TextDashboardSingleThresholdWidgetView.create({thresholdMin:0});
}

describe('App.TextDashboardSingleThresholdWidgetView', function() {

  var tests = [
    {
      data: 1,
      e: {
        isNA: false
      }
    },
    {
      data: null,
      e: {
        isNA: true
      }
    }
  ];

  tests.forEach(function(test) {
    describe('data - ' + test.data + ' | thresholdMin - 0', function() {
      var textDashboardWidgetSingleThresholdView = App.TextDashboardSingleThresholdWidgetView.create({thresholdMin:0});
      textDashboardWidgetSingleThresholdView.set('data', test.data);
      it('isNA', function() {
        expect(textDashboardWidgetSingleThresholdView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

  App.TestAliases.testAsComputedGtProperties(getView(), 'isRed', 'data', 'thresholdMin');
  App.TestAliases.testAsComputedLteProperties(getView(), 'isGreen', 'data', 'thresholdMin');
});
