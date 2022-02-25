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
require('views/main/dashboard/widgets/namenode_rpc');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

function getView() {
  return App.NameNodeRpcView.create({model_type:null});
}

describe('App.NameNodeRpcView', function() {

  var tests = [
    {
      model: {
        nameNodeRpcValues: {
          c: 1
        }
      },
      e: {
        isNA: false,
        content: '1.00 ms',
        data: '1.00'
      }
    },
    {
      model: {
        nameNodeRpcValues: {
          c: 10
        }
      },
      e: {
        isNA: false,
        content: '10.00 ms',
        data: '10.00'
      }
    },
    {
      model: {
        nameNodeRpcValues: {
          c: 0
        }
      },
      e: {
        isNA: false,
        content: '0 ms',
        data: 0
      }
    },
    {
      model: {
        nameNodeRpcValues: {
          c: null
        }
      },
      e: {
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    }
  ];

  tests.forEach(function(test) {
    var hostName = 'c';
    describe('nameNodeRpc - ' + test.model.nameNodeRpcValues[hostName], function() {
      var nameNodeRpcView = App.NameNodeRpcView.create({
        hostName: hostName,
        model: test.model
      });
      it('content', function() {
        expect(nameNodeRpcView.get('content')).to.equal(test.e.content);
      });
      it('data', function() {
        expect(nameNodeRpcView.get('data')).to.equal(test.e.data);
      });
      it('isNA', function() {
        expect(nameNodeRpcView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

  App.TestAliases.testAsComputedGtProperties(getView(), 'isRed', 'data', 'thresholdMax');

  App.TestAliases.testAsComputedLteProperties(getView(), 'isGreen', 'data', 'thresholdMin');

});
