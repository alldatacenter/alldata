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
var view, controller = Em.Object.create({
  credentialsTabNextEnabled: false,
  stepConfigsCreated: false,
  stepConfigs: Em.A([]),
  selectedServiceObserver: function() {}
});

function getView() {
  return App.DirectoriesTabOnStep7View.create({
    'controller': controller
  });
}

describe('App.DirectoriesTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });
  describe('#setServices()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'selectedServiceObserver', Em.K);

        sinon.stub(App.Tab, 'find').returns(Em.A([
        Em.Object.create({
          "id": "AMBARI_METRICS_directories",
          "themeName": "directories",
          "serviceName": "AMBARI_METRICS"
        }),
      ]));
      controller.set('tabs', Em.A([
        Em.Object.create({isActive: true, selectedServiceName: 'AMBARI_METRICS_directories'})
      ]));
    });
    afterEach(function () {
      App.Tab.find.restore();
      controller.selectedServiceObserver.restore();
    });
    it('should set services', function () {
      controller.set('stepConfigs', Em.A([
        Em.Object.create({
          serviceName: 'AMBARI_METRICS', configs: Em.A([
            Em.Object.create({name: "metrics_grafana_username", displayName: "Grafana Admin Password",})
          ])
        })
      ]));
      expect(view.get('services').length).to.be.equal(1);
    });
    it('should set controller selectedService', function () {
      controller.set('stepConfigs', Em.A([
        Em.Object.create({
          serviceName: 'AMBARI_METRICS_directories', configs: Em.A([
            Em.Object.create({name: "metrics_grafana_username", displayName: "Grafana Admin Password",})
          ])
        })
      ]));
      expect(controller.get('selectedService')['serviceName']).to.be.equal(controller.get('tabs')[0]['selectedServiceName']);
    });
    it('should call controller selectedServiceObserver', function () {
        controller.set('stepConfigs', Em.A([
          Em.Object.create({
            serviceName: 'AMBARI_METRICS', configs: Em.A([
              Em.Object.create({name: "metrics_grafana_username", displayName: "Grafana Admin Password",})
            ])
          })
        ]));
      expect(controller.selectedServiceObserver.called).to.be.equal(true);
    });
  });
  describe('#selectService()', function () {
    var eventContext;
    beforeEach(function () {
      eventContext = Em.Object.create({context: Em.Object.create({'isActive': false, serviceName: 'AMBARI_METRICS'})});
      controller.set('filterColumns', Em.A([
        Em.Object.create({'selected': false}),
      ]));
      controller.set('tabs', Em.A([
        Em.Object.create({isActive: true, selectedServiceName: ''}),
        Em.Object.create({isActive: false, selectedServiceName: ''}),
      ]));
    });
    afterEach(function () {
    });
    it('should reset controller.filter', function () {
      view.selectService(eventContext);
      expect(controller.get('filter')).to.be.equal('');
    });
    it('should set selected to false in each controller.filterColumns', function () {
      view.selectService(eventContext);
      expect(controller.get('filterColumns').everyProperty('selected', false)).to.be.equal(true);
    });
    it('should set isActive to false in each view.services', function () {
      view.set('services', Em.A([
        Em.Object.create({'isActive': true}),
        Em.Object.create({'isActive': true}),
      ]));
      view.selectService(eventContext);
      expect(view.get('services').everyProperty('isActive', false)).to.be.equal(true);
    });
    it('should set isActive to true in event.context', function () {
      view.selectService(eventContext);
      expect(eventContext.get('context.isActive')).to.be.equal(true);
    });
    it('should set selectedServiceName in controller.tabs where isActive is true', function () {
      view.selectService(eventContext);
      expect(controller.get('tabs').findProperty('isActive', true).get('selectedServiceName')).to.be.equal(eventContext.get('context.serviceName'));
    });
  });
  describe('#didInsertElement()', function () {
    beforeEach(function () {
      sinon.stub(view, 'setServices').returns(true);
    });
    afterEach(function () {
      view.setServices.restore();
    });
    it('should call setServices', function () {
      view.didInsertElement();
      expect(view.setServices.calledOnce).to.be.equal(true);
    });
  });
  describe('#enableRightArrow()', function () {
    beforeEach(function () {
      sinon.stub(view, 'setServices').returns(true);
    });
    afterEach(function () {
      view.setServices.restore();
    });
    it('should call setServices', function () {
      view.didInsertElement();
      expect(view.setServices.calledOnce).to.be.equal(true);
    });
  });

});
