/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ExperimentalController = Em.Controller.extend(App.Persist, {
  name: 'experimentalController',
  supports: function () {
    return Em.keys(App.get('supports')).map(function (sup) {
      return Ember.Object.create({
        name: sup,
        selected: App.get('supports')[sup]
      });
    });
  }.property('App.supports'),


  loadSupports: function () {
    return this.getUserPref('user-pref-' + App.router.get('loginName') + '-supports');
  },

  getUserPrefSuccessCallback: function (response) {
    if (response) {
      App.set('supports', $.extend({}, App.get('supports'), response));
    }
  },

  doSave: function () {
    var supports = this.get('supports');
    supports.forEach(function(s){
      var propName = 'App.supports.' + s.get('name');
      var propValue = s.get('selected');
      Ember.set(propName, propValue);
    });
    this.postUserPref('user-pref-' + App.router.get('loginName') + '-supports', App.get('supports')).complete(function(){
      App.router.transitionTo('root.index');
    });
  },

  doCancel: function () {
    App.router.transitionTo('root.index');
  },

  doResetUIStates: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('reset.ui.states'),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('reset.ui.states.body'))
      }),
      primary: Em.I18n.t('yes'),
      context: self,
      onPrimary: function () {
        var router = App.router;
        App.db.cleanUp();
        router.clearAllSteps();
        App.cache.clear();
        App.clusterStatus.setClusterStatus({});
        this.context.postUserPref('wizard-data', {});
        this.hide();
        router.transitionTo('root.index');
      }
    });
  }
});
