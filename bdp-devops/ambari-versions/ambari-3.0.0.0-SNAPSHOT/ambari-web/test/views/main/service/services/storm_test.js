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
require('/views/main/service/services/storm');

function getView(options) {
  return App.MainDashboardServiceStormView.create(options || {});
}

describe('App.MainDashboardServiceStormView', function () {
  var view;

  beforeEach(function() {
    view = getView();
  });

  App.TestAliases.testAsComputedPercents(getView(), 'freeSlotsPercentage', 'service.freeSlots', 'service.totalSlots');

  App.TestAliases.testAsComputedAlias(getView(), 'superVisorsLive', 'service.superVisorsStarted');

  App.TestAliases.testAsComputedAlias(getView(), 'superVisorsTotal', 'service.superVisorsTotal');

  describe("#nimbusUptimeFormatted", function () {

    it("nimbusUptime is set", function () {
      view.set('service', Em.Object.create({
        nimbusUptime: '1'
      }));
      view.propertyDidChange('nimbusUptimeFormatted');
      expect(view.get('nimbusUptimeFormatted')).to.be.equal('1');
    });

    it("nimbusUptime is not set", function () {
      view.set('service', null);
      view.propertyDidChange('nimbusUptimeFormatted');
      expect(view.get('nimbusUptimeFormatted')).to.be.equal(Em.I18n.t('services.service.summary.notRunning'));
    });
  });
});