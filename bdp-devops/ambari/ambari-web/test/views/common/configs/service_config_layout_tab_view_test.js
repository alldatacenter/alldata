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
var lazyLoading = require('utils/lazy_loading');

describe('App.ServiceConfigLayoutTabView', function () {
  var view;

  beforeEach(function () {
    view = App.ServiceConfigLayoutTabView.create();
  });

  describe('#templateName', function () {
    it('should use custom template if it provided', function () {
      view.set('customTemplate', 'test');
      expect(view.get('templateName')).to.be.equal('test');
    });

    it('should use custom template if it provided', function () {
      view.set('customTemplate', null);
      expect(view.get('templateName')).to.be.equal(require('templates/common/configs/service_config_layout_tab'));
    });
  });

  describe('#prepareConfigProperties', function () {
    it('should call setActiveSubTab for each tab', function () {
      var sectionRows = [
        [Em.Object.create({subsectionRows:[
            [Em.Object.create({context: Em.Object.create({isVisible: true}), subSectionTabs: []})],
            [Em.Object.create({context: Em.Object.create({isVisible: false}), subSectionTabs: []})]
          ]})],
        [Em.Object.create({subsectionRows: [
            [Em.Object.create({context: Em.Object.create({isVisible: true}), subSectionTabs: []})]
          ]})]
      ];
      view.setConfigsToContainer = function(){};
      sinon.stub(view, 'setConfigsToContainer');
      view.set('content', Em.Object.create({sectionRows: sectionRows}));
      view.prepareConfigProperties.call(view);
      expect(view.setConfigsToContainer.calledThrice).to.be.true;
      view.setConfigsToContainer.restore();
    });
  });

  describe('#setActiveSubTab', function () {
    it('should do nothing if no context or isVisible property is false', function () {
      var event = {context: Em.Object.create({isVisible: false, isActive: false})};
      view.setActiveSubTab(event);
      expect(event.context.get('isActive')).to.be.false;
    });

    it('should set active tab and call setEach method of active tab with false params', function () {
      var subSection = Em.Object.create({subSectionTabs: {setEach: function () {}}});
      sinon.stub(subSection.get('subSectionTabs'), 'setEach');
      var event = {context: Em.Object.create({isVisible: true, isActive: false, subSection: subSection})};
      view.setActiveSubTab(event);
      expect(event.context.get('isActive')).to.be.true;
      expect(event.context.get('subSection.subSectionTabs').setEach.called).to.be.true;
      subSection.get('subSectionTabs').setEach.restore();
    });
  });
});