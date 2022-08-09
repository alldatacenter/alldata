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
require('views/main/service/menu');

function getItemViewClass() {
  return App.MainServiceMenuView.create().get('itemViewClass').create({
    content: Em.Object.create(),
    parentView: Em.Object.create()
  });
}

describe('App.MainServiceMenuView', function () {

  var view;

  beforeEach(function () {
    view = App.MainServiceMenuView.create({
      $: sinon.stub().returns({tooltip: Em.K})
    });
  });

  describe('#content', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns([
        Em.Object.create({id: 'S1'}),
        Em.Object.create({id: 'S2'})
      ]);
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('should return content', function() {
      view.set('disabledServices', ['S2']);
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('id')).to.be.eql(['S1']);
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(App.router.location, 'addObserver');
      sinon.stub(view, 'renderOnRoute');
      sinon.stub(App, 'tooltip');
      view.didInsertElement();
    });
    afterEach(function() {
      App.router.location.addObserver.restore();
      view.renderOnRoute.restore();
      App.tooltip.restore();
    });

    it('addObserver should be called', function() {
      expect(App.router.location.addObserver.calledOnce).to.be.true;
    });
    it('renderOnRoute should be called', function() {
      expect(view.renderOnRoute.calledOnce).to.be.true;
    });
    it('App.tooltip should be called', function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe('#willDestroyElement', function() {
    beforeEach(function() {
      sinon.stub(App.router.location, 'removeObserver');
    });
    afterEach(function() {
      App.router.location.removeObserver.restore();
    });

    it('removeObserver should be called', function() {
      view.willDestroyElement();
      expect(App.router.location.removeObserver.calledOnce).to.be.true;
    });
  });

  describe('#itemViewClass', function () {
    var itemView;

    beforeEach(function () {
      itemView = getItemViewClass();
    });

    App.TestAliases.testAsComputedNotExistsInByKey(getItemViewClass(), 'isConfigurable', 'content.serviceName', 'App.services.noConfigTypes', ['HDFS', 'ZOOKEEPER']);

    describe('#active', function() {

      it('should return "active" ', function() {
        itemView.set('content.id', 'S1');
        itemView.set('parentView.activeServiceId', 'S1');
        expect(itemView.get('active')).to.be.equal('active');
      });
      it('should not return "active" ', function() {
        itemView.set('content.id', 'S1');
        itemView.set('parentView.activeServiceId', 'S2');
        expect(itemView.get('active')).to.be.empty;
      });
    });

    describe('#link', function() {
      beforeEach(function() {
        this.mock = sinon.stub(App.router, 'get');
        itemView.set('content.id', 'S1');
        itemView.set('isConfigurable', true);
      });
      afterEach(function() {
        this.mock.restore();
      });

      it('should link to summary when current state is unknown', function() {
        this.mock.returns('');
        expect(itemView.get('link')).to.be.equal('#/main/services/S1/summary');
      });
      it('should link to summary when current state is configs', function() {
        this.mock.returns('configs');
        itemView.set('parentView.activeServiceId', 'S2');
        expect(itemView.get('link')).to.be.equal('#/main/services/S1/configs');
      });
      it('should link to summary when current state is summary', function() {
        this.mock.returns('configs');
        itemView.set('parentView.activeServiceId', 'S1');
        expect(itemView.get('link')).to.be.equal('#/main/services/S1/summary');
      });
    });

    describe('#goToConfigs', function() {
      beforeEach(function() {
        sinon.stub(App.router, 'set');
        sinon.stub(App.router, 'transitionTo');
        itemView.goToConfigs();
      });
      afterEach(function() {
        App.router.set.restore();
        App.router.transitionTo.restore();
      });

      it('App.router.set should be called (routeToConfigs true)', function() {
        expect(App.router.set.calledWith('mainServiceItemController.routeToConfigs', true)).to.be.true;
      });
      it('App.router.transitionTo should be called', function() {
        expect(App.router.transitionTo.calledWith('services.service.configs', Em.Object.create())).to.be.true;
      });
      it('App.router.set should be called (routeToConfigs false)', function() {
        expect(App.router.set.calledWith('mainServiceItemController.routeToConfigs', false)).to.be.true;
      });
    });

    describe('#refreshRestartRequiredMessage', function() {

      it('no component require restart', function() {
        var expected = 0 + ' ' + Em.I18n.t('common.component') + ' ' + Em.I18n.t('on') + ' ' +
          0 + ' ' + Em.I18n.t('common.host') + ' ' + Em.I18n.t('services.service.config.restartService.needToRestartEnd');
        itemView.set('content.restartRequiredHostsAndComponents', {});
        expect(itemView.get('restartRequiredMessage')).to.be.equal(expected);
      });

      it('one component on one host require restart', function() {
        var expected = 1 + ' ' + Em.I18n.t('common.component') + ' ' + Em.I18n.t('on') + ' ' +
          1 + ' ' + Em.I18n.t('common.host') + ' ' + Em.I18n.t('services.service.config.restartService.needToRestartEnd');
        itemView.set('content.restartRequiredHostsAndComponents', {
          'host1': [{}]
        });
        expect(itemView.get('restartRequiredMessage')).to.be.equal(expected);
      });

      it('3 components on two hosts require restart', function() {
        var expected = 3 + ' ' + Em.I18n.t('common.components') + ' ' + Em.I18n.t('on') + ' ' +
          2 + ' ' + Em.I18n.t('common.hosts') + ' ' + Em.I18n.t('services.service.config.restartService.needToRestartEnd');
        itemView.set('content.restartRequiredHostsAndComponents', {
          'host1': [{}],
          'host2': [{}, {}]
        });
        expect(itemView.get('restartRequiredMessage')).to.be.equal(expected);
      });
    });
  });

});
