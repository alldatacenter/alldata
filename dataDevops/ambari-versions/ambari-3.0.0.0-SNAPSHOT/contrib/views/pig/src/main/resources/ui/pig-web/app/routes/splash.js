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
"use strict";
var App = require('app');

App.SplashRoute = Em.Route.extend({
  model: function() {
    return Ember.Object.create({
      storageTest: null,
      storageTestDone: null,
      webhcatTest: null,
      webhcatTestDone: null,
      hdfsTest: null,
      hdfsTestDone: null,
      userhomeTest: null,
      userhomeTestDone: null,
      percent: 0,
      numberOfChecks: null,
      serviceCheckPolicy: null,
    });
  },
  renderTemplate: function() {
    this.render('splash');
  },
  setupController: function (controller, model) {
    controller.set('model', model);
    var self = this;
    controller.fetchServiceCheckPolicy()
      .then(function(data){
        var numberOfChecks = 0;
        var serviceCheckPolicy = data.serviceCheckPolicy;
        for (var serviceCheck in serviceCheckPolicy) {
          if (serviceCheckPolicy[serviceCheck] === true) {
            numberOfChecks++;
          }
        }
        model.set("numberOfChecks", numberOfChecks);
        model.set("serviceCheckPolicy", serviceCheckPolicy);
        controller.startTests(model).then(function () {
          if (model.get("storageTest") && model.get("webhcatTest") && model.get("hdfsTest") && model.get("userhomeTest")) {
            Ember.run.later(this, function () {
              var previousTransition = App.get('previousTransition');
              if (previousTransition) {
                previousTransition.retry();
              } else {
                self.transitionTo('pig.scripts');
              }
            }, 2000);
          }
        });
      });
  },
});
