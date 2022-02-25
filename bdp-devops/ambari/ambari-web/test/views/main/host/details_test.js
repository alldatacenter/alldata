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
require('views/main/host/details');

var view,
  activeCases = [
    {
      passiveState: 'ON',
      isActive: false,
      label: 'Off'
    },
    {
      passiveState: 'OFF',
      isActive: true,
      label: 'On'
    }
  ];

describe('App.MainHostDetailsView', function () {

  beforeEach(function () {
    view = App.MainHostDetailsView.create({
       content: Em.Object.create({
          hostComponents: []
       })
    });
  });

  describe('#content', function () {
    it('should take content from controller', function () {
      view.set('content', {
        property: 'value'
      });
      expect(view.get('content.property')).to.equal('value');
    });
  });

  describe('#clients', function () {
    it('should take clients from content', function () {
      view.set('content', {
        hostComponents: [
          {
            isClient: true
          },
          {
            isClient: false
          }
        ]
      });
      expect(view.get('clients')).to.have.length(1);
      view.get('content.hostComponents').pushObject({
        isClient: true
      });
      expect(view.get('clients')).to.have.length(2);
    });
  });

  describe('#maintenance', function () {
    activeCases.forEach(function (item) {
      it('passive state label should contain ' + item.label, function () {
        view.set('controller', {
          content: {
            passiveState: item.passiveState,
            isActive: item.isActive
          }
        });
        expect(view.get('maintenance').findProperty('action', 'onOffPassiveModeForHost').label).to.contain(item.label);
      });
    });
  });

  describe('#clientsWithConfigs', function() {
    beforeEach(function () {
      view.set('content', {
        hostComponents: [
          Em.Object.create({
            isClient: true,
            service: Em.Object.create({
              serviceName: 'WITH_CONFIGS'
            })
          }),
          Em.Object.create({
            isClient: true,
            service: Em.Object.create({
              serviceName: 'WITHOUT_CONFIGS'
            })
          }),
          Em.Object.create({
            isClient: false,
            service: Em.Object.create({
              serviceName: 'SAMPLE_SERVICE'
            })
          })
        ]
      });

      App.set('services', {
        noConfigTypes: ['WITHOUT_CONFIGS', 'WITHOUT_CONFIGS_2']
      });
    });

    afterEach(function () {
      App.set('services', Em.K);
    });

    it('should get only clients with configs (1)', function() {
      expect(view.get('clientsWithConfigs')).to.have.length(1);
    });

    it('should get only clients with configs (2)', function() {
      view.get('content.hostComponents').pushObject(Em.Object.create({
        isClient: true,
        service: Em.Object.create({
          serviceName: 'WITHOUT_CONFIGS_2'
        })
      }));
      expect(view.get('clientsWithConfigs')).to.have.length(1);
    });

    it('should get only clients with configs (3)', function() {
      view.get('content.hostComponents').pushObject(Em.Object.create({
        isClient: true,
        service: Em.Object.create({
          serviceName: 'WITH_CONFIGS_2'
        })
      }));
      expect(view.get('clientsWithConfigs')).to.have.length(2);
    });
  });

  describe('#didInsertElement()', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get', function () {
        return {
          updateHost: function (callback) {
            callback();
          }
        }
      });
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(App, 'tooltip', Em.K);
    });
    afterEach(function () {
      App.router.transitionTo.restore();
      App.router.get.restore();
      App.tooltip.restore();
    });
    it('host loaded', function () {
      view.set('content.isLoaded', true);
      view.didInsertElement();
      expect(App.router.get.calledWith('updateController')).to.be.true;
      expect(App.tooltip.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('main.hosts.index')).to.be.false;
    });
    it('host is not loaded', function () {
      view.set('content.isLoaded', false);
      view.didInsertElement();
      expect(App.router.get.calledWith('updateController')).to.be.true;
      expect(App.tooltip.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('main.hosts.index')).to.be.true;
    });
  });
});
