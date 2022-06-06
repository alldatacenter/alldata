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
require('views/common/modal_popups/select_groups_popup');

describe('App.showSelectGroupsPopup', function () {
  var view;
  
  describe('#onSecondary', function () {
  
    beforeEach(function () {
      view = App.showSelectGroupsPopup('', Em.Object.create({
        dependentConfigGroups: {
          S1: 'g1'
        }
      }), []);
    });

    it('dependentConfigGroups should be set', function () {
      view.set('selectedGroups', {g1: {}});
      view.onSecondary();
      expect(view.get('selectedConfigGroup.dependentConfigGroups')).to.be.eql({
        g1: {}
      });
    });
  });
  
  
  describe('#onPrimary', function () {
    var configs = [
      {
        serviceName: 'S1',
        configGroup: 'g1'
      },
      {
        serviceName: 'S1',
        configGroup: 'g2'
      }
    ];
    beforeEach(function () {
      view = App.showSelectGroupsPopup('', Em.Object.create({
          dependentConfigGroups: {
            S1: 'g1'
          }
        }),
        [
          Em.Object.create({
            serviceName: 'S1',
            configGroups: [
              {
                name: 'g1'
              }
            ]
          })
        ],
        configs);
      sinon.stub(view, 'applyOverridesToConfigGroups');
    });
  
    afterEach(function() {
      view.applyOverridesToConfigGroups.restore();
    });
    
    it('configs should be modified', function () {
      view.set('selectedGroups', {
        S1: 'g2'
      });
      view.onPrimary();
      expect(JSON.stringify(configs)).to.be.eql(JSON.stringify([{
        serviceName: 'S1',
        configGroup: 'g1'
      }]));
    });
  
    it('applyOverridesToConfigGroups should be called', function () {
      view.set('selectedGroups', {
        S1: 'g2'
      });
      view.onPrimary();
      expect(view.applyOverridesToConfigGroups.calledOnce).to.be.true;
    });
  });
  
  describe('#applyOverridesToConfigGroups', function () {
    var configs = [
      {
        propertyName: 'c1',
        fileName: 'site',
        serviceName: 'S1',
        configGroup: 'g1',
        recommendedValue: 'val1'
      }
    ];
    var dependentStepConfigs = [
      Em.Object.create({
        serviceName: 'S1',
        configGroups: [
          {
            name: 'g1'
          }
        ],
        configs: [
          Em.Object.create({
            name: 'c1',
            filename: 'site'
          })
        ]
      })
    ];
    beforeEach(function () {
      sinon.stub(App.config, 'createOverride');
    });
    
    afterEach(function() {
      App.config.createOverride.restore();
    });
   
    it('App.config.createOverride should be called when no overrides', function () {
      view = App.showSelectGroupsPopup('', Em.Object.create(), dependentStepConfigs, configs);
      view.applyOverridesToConfigGroups('S1', {}, 'g2', 'g1');
      expect(App.config.createOverride.calledWith(
        Em.Object.create({
          name: 'c1',
          filename: 'site'
        }),
        {
          "value": 'val1',
          "recommendedValue": 'val1',
          "isEditable": true
        }, {}
      )).to.be.true;
    });
  
    it('should set value of override to recommended', function () {
      var override = Em.Object.create({
        group: {
          name: 'g2'
        },
        initialValue: 'val2',
        recommendedValue: 'val1'
      });
      dependentStepConfigs[0].get('configs')[0].set('overrides', [override]);
      view = App.showSelectGroupsPopup('', Em.Object.create(), dependentStepConfigs, configs);
      view.applyOverridesToConfigGroups('S1', {}, 'g2', 'g1');
      expect(override.get('value')).to.be.equal('val1');
    });
  
    it('App.config.createOverride should be called when no overrides for selected group', function () {
      var override = Em.Object.create({
        group: {
          name: 'g2'
        },
        initialValue: 'val1',
        recommendedValue: 'val1'
      });
      dependentStepConfigs[0].get('configs')[0].set('overrides', [override]);
      view = App.showSelectGroupsPopup('', Em.Object.create(), dependentStepConfigs, configs);
      view.applyOverridesToConfigGroups('S1', Em.Object.create({name: 'g1'}), 'g2', 'g1');
      expect(App.config.createOverride.calledWith(
        Em.Object.create({
          name: 'c1',
          filename: 'site',
          overrides: [override]
        }),
        {
          "value": 'val1',
          "recommendedValue": 'val1',
          "isEditable": true
        }, Em.Object.create({name: 'g1'})
      )).to.be.true;
    });
  
    it('should set value of override to recommended for selected group', function () {
      var override = Em.Object.create({
        group: {
          name: 'g2'
        },
        initialValue: 'val1',
        recommendedValue: 'val1'
      });
      dependentStepConfigs[0].get('configs')[0].set('overrides', [override]);
      view = App.showSelectGroupsPopup('', Em.Object.create(), dependentStepConfigs, configs);
      view.applyOverridesToConfigGroups('S1', Em.Object.create({name: 'g2'}), 'g2', 'g1');
      expect(override.get('value')).to.be.equal('val1');
    });
  });
});
