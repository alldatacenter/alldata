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

const stepConfigsConfigArray = Em.A([
  Em.Object.create({name: 'hive_database', serviceName: 'Hive', displayType: 'user', value: 'New MySQL Database', editDone: false}),
  Em.Object.create({name: 'tez_database', serviceName: 'Tez', displayType: 'user', value: 'Tez', editDone: false}),
]);
var App = require('app');
var view, controller = Em.Object.create({
  stepConfigs: Em.A([Em.Object.create({
    serviceName: 'MISC', configs:stepConfigsConfigArray
  }),]),
  stepConfigsCreated: true,
});

function getView() {
  return App.AccountsTabOnStep7View.create({
    'controller': controller
  });
}

describe('App.AccountsTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });
  describe('#property subscription', function () {
    it('should return properties', function () {
      expect(JSON.stringify(view.get('properties'))).to.be.equal(JSON.stringify(stepConfigsConfigArray));

    })
  });
  describe('#propertyChanged', function () {
    beforeEach(function () {
      view.reopen({
        properties: stepConfigsConfigArray
      });
      sinon.stub(view, 'showConfirmationDialogIfShouldChangeProps').returns(true);
    });

    afterEach(function () {
      view.showConfirmationDialogIfShouldChangeProps.restore();
    });
    it('showConfirmationDialogIfShouldChangeProps shouldn\'t be cleed', function () {
      view.propertyChanged();
      expect(view.showConfirmationDialogIfShouldChangeProps.called).to.be.equal(false);
    });
    it('should call showConfirmationDialogIfShouldChangeProps with certain properties', function () {
      var updatedConfig = Em.Object.create({name: 'tez_database', serviceName: 'Tez', displayType: 'user', value: 'Tez', editDone: true});
      controller.set('stepConfigs.configs', updatedConfig);
      view.set('properties', [updatedConfig]);
      expect(view.showConfirmationDialogIfShouldChangeProps.calledOnce).to.be.equal(true);
      expect(view.showConfirmationDialogIfShouldChangeProps.calledWith(updatedConfig, controller.get('stepConfigs'), updatedConfig.get('serviceName'))).to.be.equal(true);
    });
  });

  describe('#checkboxes subscription', function () {
    it('should return empty checkbox array', function () {
      expect(view.get('checkboxes').length).to.be.equal(0);
    });
    it('should checkbox array with items which name is in miscProperties array', function () {
      const miscProperties = ['sysprep_skip_create_users_and_groups', 'ignore_groupsusers_create', 'override_uid'];
      const updatedConfigs = Em.A([]);
      miscProperties.forEach(function(item) {
        updatedConfigs.pushObject(Em.Object.create({name: item}));
      });
      controller.get('stepConfigs')[0]['configs']= updatedConfigs;
      expect(view.get('checkboxes').length).to.be.equal(3);
    })
  });
});

