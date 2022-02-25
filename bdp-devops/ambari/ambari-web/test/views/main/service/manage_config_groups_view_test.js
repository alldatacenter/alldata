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
require('views/main/service/manage_config_groups_view');

var view;

describe('App.MainServiceManageConfigGroupView', function () {
  beforeEach(function() {
    view = App.MainServiceManageConfigGroupView.create({
      controller: Em.Object.create({
        loadHosts: sinon.spy()
      })
    });
  });

  describe('#addHostTooltip', function() {

    it('should return addHost message when group is default', function() {
      view.set('controller.selectedConfigGroup', Em.Object.create({
        isDefault: true
      }));
      expect(view.get('addHostTooltip')).to.be.equal(Em.I18n.t('services.service.config_groups_popup.addHost'));
    });
    it('should return addHost message when add host is enabled', function() {
      view.set('controller.selectedConfigGroup', Em.Object.create({
        isDefault: false,
        isAddHostsDisabled: false
      }));
      expect(view.get('addHostTooltip')).to.be.equal(Em.I18n.t('services.service.config_groups_popup.addHost'));
    });
    it('should return addHostDisabled message when add host is disabled', function() {
      view.set('controller.selectedConfigGroup', Em.Object.create({
        isDefault: false,
        isAddHostsDisabled: true
      }));
      expect(view.get('addHostTooltip')).to.be.equal(Em.I18n.t('services.service.config_groups_popup.addHostDisabled'));
    });
  });

  describe('#willInsertElement', function() {

    it('loadHosts should be called', function() {
      view.willInsertElement();
      expect(view.get('controller').loadHosts.calledOnce).to.be.true;
    });
  });

  describe('#willDestroyElement', function() {

    it('should clean configGroups and originalConfigGroups', function() {
      view.set('controller.configGroups', [{}]);
      view.set('controller.originalConfigGroups', [{}]);
      view.willDestroyElement();
      expect(view.get('controller.configGroups')).to.be.empty;
      expect(view.get('controller.originalConfigGroups')).to.be.empty;
    });
  });

  describe('#showTooltip', function() {
    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(App, 'tooltip');
      sinon.stub(view, 'selectDefaultGroup');
    });
    afterEach(function() {
      Em.run.next.restore();
      App.tooltip.restore();
      view.selectDefaultGroup.restore();
    });

    it('Em.run.next should be called', function() {
      view.set('controller.isLoaded', true);
      expect(Em.run.next.calledOnce).to.be.true;
    });

    it('App.tooltip should be called', function() {
      view.set('controller.isLoaded', true);
      expect(App.tooltip.calledThrice).to.be.true;
    });
  });

  describe('#onGroupSelect', function() {

    it('should set selectedConfigGroup of controller', function() {
      view.set('selectedConfigGroup', [Em.Object.create({id: 1})]);
      expect(view.get('controller.selectedConfigGroup')).to.be.eql(Em.Object.create({id: 1}));
      expect(view.get('controller.selectedHosts')).to.be.empty;
    });
    it('should set selectedConfigGroup of view', function() {
      view.set('selectedConfigGroup', [Em.Object.create({id: 1}), Em.Object.create({id: 2})]);
      expect(view.get('controller.selectedConfigGroup')).to.be.eql(Em.Object.create({id: 2}));
      expect(view.get('selectedConfigGroup')).to.be.eql(Em.Object.create({id: 2}));
      expect(view.get('controller.selectedHosts')).to.be.empty;
    });
  });

  describe('#selectDefaultGroup', function() {

    it('should set selectedConfigGroup', function() {
      view.set('controller.configGroups', [
        Em.Object.create({isDefault: true})
      ]);
      view.set('controller.isLoaded', true);
      view.selectDefaultGroup();
      expect(view.get('selectedConfigGroup')).to.be.eql([Em.Object.create({isDefault: true})]);
    });
  });
});
