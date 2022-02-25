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
require('views/common/modal_popups/hosts_table_list_popup');


describe('App.DependentConfigsTableView', function () {
  var view;
 
  
  beforeEach(function () {
    view = App.DependentConfigsTableView.create({
      controller: Em.Object.create({
        undoRedoRecommended: sinon.spy()
      }),
      parentView: Em.Object.create(),
      elementsWithPopover: {
        popover: sinon.spy()
      },
      recommendations: [
        {
          saveRecommended: false
        },
        {
          saveRecommended: true
        }
      ]
    });
  });

  describe('#updateRecommendedDefault', function() {
    
    it('undoRedoRecommended should be called with applyRecommendations', function() {
      view.set('controller.isInstallWizard', true);
      
      view.updateRecommendedDefault();
      expect(view.get('controller').undoRedoRecommended.calledWith(
        [{saveRecommended: true}], true
      )).to.be.true;
    });
  
    it('undoRedoRecommended should be called with dontApplyRecommendations', function() {
      view.set('controller.isInstallWizard', true);
    
      view.updateRecommendedDefault();
      expect(view.get('controller').undoRedoRecommended.calledWith(
        [{saveRecommended: false}], false
      )).to.be.true;
    });
  });
  
  describe('#message', function() {
    
    it('should return required message', function() {
      view.set('isEditable', false);
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.equal(Em.I18n.t('popup.dependent.configs.title.required'));
    });
  
    it('should return values message', function() {
      view.set('isEditable', true);
      view.set('parentView.isAfterRecommendation', false);
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.equal(Em.I18n.t('popup.dependent.configs.title.values'));
    });
  
    it('should return recommendation message', function() {
      view.set('isEditable', true);
      view.set('parentView.isAfterRecommendation', true);
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.equal(
        Em.I18n.t('popup.dependent.configs.title.recommendation') +
        '<br>' +Em.I18n.t('popup.dependent.configs.title.values')
      );
    });
  });
  
  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(App, 'popover');
    });
    afterEach(function() {
      App.popover.restore();
    });
    
    it('App.popover should be called', function() {
      view.set('showPopovers', true);
      view.didInsertElement();
      expect(App.popover.calledOnce).to.be.true;
    });
  });
  
  describe('#willDestroyElement', function() {
    
    it('popover should be called', function() {
      view.set('showPopovers', true);
      view.willDestroyElement();
      expect(view.get('elementsWithPopover').popover.calledOnce).to.be.true;
    });
  });
  
});

describe('App.DependentConfigsListView', function () {
  var view;
  
  
  beforeEach(function () {
    view = App.DependentConfigsListView.create({
      controller: Em.Object.create(),
      parentView: Em.Object.create()
    });
  });
  
  describe('#setAllConfigsWithErrors', function() {
    
    it('no stepConfigs', function() {
      view.set('controller.stepConfigs', null);
      view.setAllConfigsWithErrors();
      expect(view.get('allConfigsWithErrors')).to.be.empty;
    });
  
    it('should copy configWithErrors to allConfigsWithErrors', function() {
      view.set('controller.stepConfigs', [Em.Object.create({
        configsWithErrors: [{}]
      })]);
      view.setAllConfigsWithErrors();
      expect(view.get('allConfigsWithErrors')).to.be.eql([{}]);
    });
  });
  
  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'setAllConfigsWithErrors');
    });
    afterEach(function() {
      view.setAllConfigsWithErrors.restore();
    });
    
    it('setAllConfigsWithErrors should be called', function() {
      view.didInsertElement();
      expect(view.setAllConfigsWithErrors.calledOnce).to.be.true;
    });
  });
});

describe('App.showDependentConfigsPopup', function () {
  var view;
  
  describe('#saveChanges', function () {
    
    beforeEach(function () {
      this.recommendations = [{saveRecommended: true}];
      view = App.showDependentConfigsPopup(this.recommendations, [], this.primary);
    });
    
    it('should save saveRecommendedDefault', function () {
      view.saveChanges();
      expect(this.recommendations[0].saveRecommendedDefault).to.be.true;
    });
  });
  
  describe('#discardChanges', function () {
    
    beforeEach(function () {
      this.recommendations = [{saveRecommendedDefault: true}];
      view = App.showDependentConfigsPopup(this.recommendations, [], this.primary);
    });
    
    it('should discard saveRecommended', function () {
      view.discardChanges();
      expect(this.recommendations[0].saveRecommended).to.be.true;
    });
  });
  
  describe('#onPrimary', function () {
    
    beforeEach(function () {
      this.primary = sinon.spy();
      view = App.showDependentConfigsPopup([], [], this.primary);
      sinon.stub(view, 'saveChanges');
    });
    
    afterEach(function () {
      view.saveChanges.restore();
    });
    
    it('should call primary-callback', function () {
      view.onPrimary();
      expect(this.primary.calledOnce).to.be.true;
    });
    
    it('saveChanges should be called', function () {
      view.onPrimary();
      expect(view.saveChanges.calledOnce).to.be.true;
    });
  });
  
  describe('#onSecondary', function () {
    
    beforeEach(function () {
      this.secondary = sinon.spy();
      view = App.showDependentConfigsPopup([], [], Em.K, this.secondary);
      sinon.stub(view, 'discardChanges');
    });
  
    afterEach(function () {
      view.discardChanges.restore();
    });
    
    it('should call secondary-callback', function () {
      view.onSecondary();
      expect(this.secondary.calledOnce).to.be.true;
    });
  
    it('discardChanges should be called', function () {
      view.onSecondary();
      expect(view.discardChanges.calledOnce).to.be.true;
    });
  });
  
  describe('#onClose', function () {
    
    beforeEach(function () {
      view = App.showDependentConfigsPopup([], [], Em.K);
      sinon.stub(view, 'onSecondary');
    });
    
    afterEach(function () {
      view.onSecondary.restore();
    });
    
    it('onSecondary should be called', function () {
      view.onSecondary();
      expect(view.onSecondary.calledOnce).to.be.true;
    });
  });
});
