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

var chartUtils = require('utils/chart_utils');

describe('chart utils', function () {

  describe('#getColorSchemeForChart', function () {
    it('should return the array of colors', function () {
      expect(chartUtils.getColorSchemeForChart(3)).to.eql(['#63c2e5', '#79e3d1', '#41bfae']);
    });
  });

  describe('#getColorSchemeForGaugeWidget', function () {
    it('default color', function () {
      expect(chartUtils.getColorSchemeForGaugeWidget()).to.eql(['#41bfae', '#DDDDDD']);
    });
    it('custom color', function () {
      expect(chartUtils.getColorSchemeForGaugeWidget('#000000')).to.eql(['#000000', '#DDDDDD']);
    });
  });

});
