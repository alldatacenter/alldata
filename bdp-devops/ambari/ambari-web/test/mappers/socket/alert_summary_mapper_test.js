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

require('mappers/socket/alert_summary_mapper');

describe('App.alertSummaryMapper', function () {

  describe('#map', function() {
    beforeEach(function() {
      sinon.stub(App.alertDefinitionSummaryMapper, 'map');
    });
    afterEach(function() {
      App.alertDefinitionSummaryMapper.map.restore();
    });

    it('App.alertDefinitionSummaryMapper.map should be called', function() {
      const event = {
        summaries: {
          "1": {
            d1: {
              definition_name: 'd1'
            }
          }
        }
      };
      App.alertSummaryMapper.map(event);
      expect(App.alertDefinitionSummaryMapper.map.calledWith({
        alerts_summary_grouped: [
          {
            definition_name: 'd1'
          }
        ]
      })).to.be.true;
    });
  });
});
