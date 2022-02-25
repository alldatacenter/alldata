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

const date = require('utils/date/date');

describe('App.HDFSSummaryWidgetsView', function () {
  var view;

  beforeEach(function () {
    view = App.HDFSSummaryWidgetsView.create({
      hostName: 'test'
    });
  });

  describe('#nodeUptime', function () {
    it('should display notRunning text if time is equal to 0', function () {
      view.set('model.nameNodeStartTimeValues', Em.Object.create({test: 0}));
      expect(view.get('nodeUptime')).to.be.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('should display uptime text if time is more than 0', function () {
      const now = +(new Date());
      view.set('model.nameNodeStartTimeValues', Em.Object.create({test: now}));
      sinon.stub(App, 'dateTime').returns(now);
      expect(view.get('nodeUptime')).to.be.equal('0s');
      App.dateTime.restore();
    });
  });

  describe('#upgradeStatus', function () {
    it('should return not available status if upgrade status and health status are null', function () {
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return not pending status if upgrade status is true', function () {
      view.set('model.upgradeStatusValues', {'test': true});
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });

    it('should return not finalized status if health status is true', function () {
      view.set('model.healthStatusValues', {'test': true});
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });
  });

  describe('#safeModeStatus', function () {
    it('should return not available if safe mode is none', function () {
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return notInSafeMode if safe mode length is 0', function () {
      view.set('model.safeModeStatusValues', {'test': []});
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t('services.service.summary.safeModeStatus.notInSafeMode'));
    });

    it('should return inSafeMode if safe mode length is more than 1', function () {
      view.set('model.safeModeStatusValues', {'test': [{}]});
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t('services.service.summary.safeModeStatus.inSafeMode'));
    });
  });
});