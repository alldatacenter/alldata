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
  stepConfigs: Em.A([])
});

function getView() {
  return App.CredentialsTabOnStep7View.create({
    'controller': controller
  });
}

describe('App.CredentialsTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });

  describe('#setRows()', function () {
    const configPropertiesArray = Em.A(["users.admin__activity-zeppelin-shiro", "metrics_grafana_username__ams-grafana-env", "metrics_grafana_password__ams-grafana-env"]);
    beforeEach(function () {
      sinon.stub(App.configsCollection, 'getConfig').returns(
        Em.Object.create({
          id: "metrics_grafana_username__ams-grafana-env",
          serviceName: "AMBARI_METRICS",
          name: "metrics_grafana_username",
        })
      );
      sinon.stub(App.Tab, 'find').returns(Em.A([
        Em.Object.create({
          id: "AMBARI_METRICS_credentials",
          name: 'credentials',
          isCategorized: true,
          sections: Em.A([
            Em.Object.create({
              name: 'credentials',
              subSections: Em.A([
                Em.Object.create({
                  configProperties: configPropertiesArray.slice(),
                })
              ])
            })
          ])
        }),
      ]));
    });
    afterEach(function () {
      controller.set('stepConfigs', Em.A([]));
      App.Tab.find.restore();
      App.configsCollection.getConfig.restore();
      controller.set('stepConfigsCreated', false);
    });

    it('should set only properties', function () {
      controller.set('stepConfigs', Em.A([
        Em.Object.create({
          serviceName: 'AMBARI_METRICS', configs: Em.A([
            Em.Object.create({name: "metrics_grafana_username", displayName: "Grafana Admin Password",})
          ])
        })
      ]));
      controller.set('stepConfigsCreated', true);
      expect(view.get('rows').length).to.be.equal(0);
      expect(view.get('properties').length).to.be.equal(configPropertiesArray.length);
    });

    it('should set rows and properties', function () {
      const stepConfigsArr = Em.A([
        Em.Object.create({
          serviceName: 'AMBARI_METRICS', configs: Em.A([
            Em.Object.create({name: "metrics_grafana_username", displayType: 'password'})
          ])
        })
      ]);
      controller.set('stepConfigs', stepConfigsArr);
      controller.set('stepConfigsCreated', true);
      expect(view.get('rows').length).to.be.equal(stepConfigsArr.length);
      expect(view.get('properties').length).to.be.equal(configPropertiesArray.length);
    });
  });

  describe('#updateNextDisabled()', function () {
    beforeEach(function () {

    });
    it('set controller.credentialsTabNextEnabled to false because of passwordProperty error', function () {
      view.set('rows', Em.A([
        Em.Object.create({passwordProperty: {error: true}, usernameProperty: {error: null}}),
      ]));
      view.set('properties', Em.A([Em.Object.create({error: "This field is required"})]));
      expect(controller.get('credentialsTabNextEnabled')).to.be.equal(false);
    });
    it('set controller.credentialsTabNextEnabled to false because of usernameProperty error', function () {
      view.set('rows', Em.A([
        Em.Object.create({passwordProperty: {error: null}, usernameProperty: {error: true}}),
      ]));
      view.set('properties', Em.A([Em.Object.create({error: "This field is required"})]));
      expect(controller.get('credentialsTabNextEnabled')).to.be.equal(false);
    });
    it('set controller.credentialsTabNextEnabled to true', function () {
      view.set('rows', Em.A([
        Em.Object.create({passwordProperty: {error: null}, usernameProperty: {error: null}}),
      ]));
      view.set('properties', Em.A([Em.Object.create({error: "This is required"})]));
      expect(controller.get('credentialsTabNextEnabled')).to.be.equal(true);
    });
  });

});

