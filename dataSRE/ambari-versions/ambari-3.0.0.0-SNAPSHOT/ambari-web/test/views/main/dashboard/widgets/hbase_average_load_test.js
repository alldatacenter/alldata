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

require('messages');
require('views/main/dashboard/widgets/hbase_average_load');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

function getView() {
  return App.HBaseAverageLoadView.create({model_type: null});
}

describe('App.HBaseAverageLoadView', function() {

  var tests = [
    {
      model: {
        averageLoad: 1
      },
      e: {
        isNA: false,
        content: '1'
      }
    },
    {
      model: {
        averageLoad: 10
      },
      e: {
        isNA: false,
        content: '10'
      }
    },
    {
      model: {
        averageLoad: 0
      },
      e: {
        isNA: false,
        content: '0'
      }
    },
    {
      model: {
        averageLoad: null
      },
      e: {
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable')
      }
    }
  ];

  tests.forEach(function(test) {
    describe('averageLoad - ' + test.model.averageLoad, function() {
      var hBaseAverageLoadView = App.HBaseAverageLoadView.create({model_type:null, model: test.model});
      it('content', function() {
        expect(hBaseAverageLoadView.get('content')).to.equal(test.e.content);
      });
      it('isNA', function() {
        expect(hBaseAverageLoadView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

  App.TestAliases.testAsComputedAlias(getView(), 'data', 'model.averageLoad', 'number');

  App.TestAliases.testAsComputedGtProperties(getView(), 'isRed', 'data', 'thresholdMax');

  App.TestAliases.testAsComputedLteProperties(getView(), 'isGreen', 'data', 'thresholdMin');

});
