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
  // credentialsTabNextEnabled: false,
  // stepConfigsCreated: false,
  // stepConfigs: Em.A([])
});

function getView() {
  return App.DatabasesTabOnStep7View.create({
    'controller': controller
  });
}

describe('App.DatabasesTabOnStep7View', function () {
  beforeEach(function () {
    view = getView();
  });

  describe('#tabs()', function () {
    beforeEach(function () {
      sinon.stub(App.Tab, 'find').returns(Em.A([
        Em.Object.create({"id": "OOZIE_oozie_database", "themeName": "database",})
      ]));
    });
    afterEach(function () {
      App.Tab.find.restore();
    });
    it('should return tabs', function () {
      expect(view.get('tabs').length).to.be.equal(1);
    });
  });
  describe('#didInsertElement()', function () {
    beforeEach(function () {
      sinon.stub(view, 'setLocalProperties').returns(true);
      controller.set('name', 'wizardStep7Controller');
      view.set('configsView', null);
    });
    afterEach(function () {
      view.setLocalProperties.restore();
    });
    it('should set configsView', function () {
      view.didInsertElement();
      expect(view.get('configsView')).to.not.be.empty;
    });
    it('should call setLocalProperties', function () {
      view.didInsertElement();
      expect(view.setLocalProperties.calledOnce).to.be.equal(true);
    });
  });
  describe('#setLocalProperties()', function () {
    const configPropertiesArray = Em.A(["oozie_database__oozie-env"]);

    beforeEach(function () {
      sinon.stub(App.configsCollection, 'getConfig').returns(
        Em.Object.create({
          id: "oozie_database__oozie-env",
          serviceName: "OOZIE",
          name: "oozie_database",
        })
      );
      sinon.stub(App.Tab, 'find').returns(Em.A([
        Em.Object.create(Em.Object.create({
          id: "oozie_database__oozie-env",
          themeName: "database",
          isCategorized: true,
          sections: Em.A([
            Em.Object.create({
              subSections: Em.A([
                Em.Object.create({
                  configProperties: configPropertiesArray
                })])
            })])
        }))
      ]));
    });
    afterEach(function () {
      controller.set('stepConfigs', Em.A([]));
      App.configsCollection.getConfig.restore();
      App.Tab.find.restore();
    });
    it('should return empty properties', function () {
      controller.set('stepConfigsCreated', false);
      view.setLocalProperties();
      expect(view.get('properties').length).to.be.equal(0);
    });
    it('should set properties array', function () {
      controller.set('stepConfigsCreated', true);
      const stepConfigsArr = Em.A([
        Em.Object.create({
          serviceName: 'OOZIE', configs: Em.A([
            Em.Object.create({name: "oozie_database"})
          ])
        })
      ]);
      controller.set('stepConfigs', stepConfigsArr);
      view.setLocalProperties();
      expect(view.get('properties').length).to.be.equal(configPropertiesArray.length);
    });
  });
  describe('#updateNextDisabled()', function () {

    it('should set controller databasesTabNextEnabled to true', function () {
      view.set('properties', Em.A([
        Em.Object.create({id: "oozie_database__oozie-env", isActive: true, error: false}),
        Em.Object.create({id: "metrics_grafana_username-env", isActive: false, error: false}),
      ]));
      expect(controller.get('databasesTabNextEnabled')).to.be.equal(true);
    });
    it('should set controller databasesTabNextEnabled to false', function () {
      view.set('properties', Em.A([
        Em.Object.create({id: "oozie_database__oozie-env", isActive: true, error: true}),
        Em.Object.create({id: "metrics_grafana_username-env", isActive: false, error: false}),
      ]));
      expect(controller.get('databasesTabNextEnabled')).to.be.equal(false);
    });
    it('should set controller databasesTabNextEnabled to true if no property from properties has field isActive', function () {
      view.set('properties', Em.A([
        Em.Object.create({id: "oozie_database__oozie-env",  error: true}),
        Em.Object.create({id: "metrics_grafana_username-env",  error: true}),
      ]));
      expect(controller.get('databasesTabNextEnabled')).to.be.equal(true);
    });

  });
});
