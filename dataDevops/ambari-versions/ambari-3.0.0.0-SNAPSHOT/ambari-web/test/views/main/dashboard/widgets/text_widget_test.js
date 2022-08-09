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
require('views/main/dashboard/widgets/text_widget');

function getView() {
  return App.TextDashboardWidgetView.create({thresholdMin:40, thresholdMax:70});
}

describe('App.TextDashboardWidgetView', function() {

  var tests = [
    {
      data: 100,
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
    describe('data - ' + test.data + ' | thresholdMin - 40 | thresholdMax - 70', function() {
      var textDashboardWidgetView = App.TextDashboardWidgetView.create({thresholdMin:40, thresholdMax:70});
      textDashboardWidgetView.set('data', test.data);
      it('isNA', function() {
        expect(textDashboardWidgetView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

  App.TestAliases.testAsComputedGtProperties(getView(), 'isGreen', 'data', 'thresholdMax');

  App.TestAliases.testAsComputedLteProperties(getView(), 'isRed', 'data', 'thresholdMin');

  App.TestAliases.testAsComputedAnd(getView(), 'isOrange', ['!isGreen', '!isRed']);

});
