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
require('views/main/dashboard/widgets/uptime_text_widget');

var date = require('utils/date/date');
var uptimeTextDashboardWidgetView;
describe('App.UptimeTextDashboardWidgetView', function() {

  beforeEach(function () {
    uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({
      thresholdMin:40,
      thresholdMax:70,
      modelValue: Em.computed.alias('model.field'),
      model: Em.Object.create({})
    });
  });

  describe('#timeConverter', function() {
    var ts1 = 1358245370553, ts2 = 0;
    var timestamps = [
      {
        t: ts1,
        e: {
          l: 2,
          f: new Date(ts1)
        }
      },
      {
        t: ts2,
        e: {
          l: 2,
          f: new Date(ts2)
        }
      }
    ];
    timestamps.forEach(function(timestamp) {
      it('timestamp ' + timestamp.t, function() {
        var result = uptimeTextDashboardWidgetView.timeConverter(timestamp.t);
        expect(result.length).to.equal(timestamp.e.l);
        assert.include(timestamp.e.f.toString(), result[0].toString(), timestamp.e.f + ' contains string ' + result[0]);
      });
    });
  });

  describe('#calculate', function() {
    beforeEach(function() {
      sinon.stub(App, 'dateTimeWithTimeZone', function(arg) {return arg ? arg : 100000});
      sinon.stub(date, 'timingFormat', function(arg) {return String(arg);});
      sinon.stub(uptimeTextDashboardWidgetView, 'timeConverter').returns([1, 2]);
    });
    afterEach(function() {
      App.dateTimeWithTimeZone.restore();
      date.timingFormat.restore();
      uptimeTextDashboardWidgetView.timeConverter.restore();
    });

    it("should be 'n/a' when uptime is null", function() {
      uptimeTextDashboardWidgetView.get('model').set('field', null);

      uptimeTextDashboardWidgetView.calculate();

      expect(uptimeTextDashboardWidgetView.get('data')).to.be.null;
      expect(uptimeTextDashboardWidgetView.get('hiddenInfo')).to.be.eql([
        null, Em.I18n.t('services.service.summary.notRunning')
      ]);
    });

    it("should display formatted duration when uptime is number", function() {
      uptimeTextDashboardWidgetView.get('model').set('field', 10000);

      uptimeTextDashboardWidgetView.calculate();

      expect(uptimeTextDashboardWidgetView.get('data')).to.be.equal('90000');
      expect(uptimeTextDashboardWidgetView.get('hiddenInfo')).to.be.eql(['90000', 1, 2]);
    });
  });

});
