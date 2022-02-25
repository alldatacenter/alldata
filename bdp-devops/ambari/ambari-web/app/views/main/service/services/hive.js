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

App.MainDashboardServiceHiveView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/hive'),
  serviceName: 'HIVE',

  viewsToShow: {},

  viewLinks: function() {
    var viewsToShow = this.get('viewsToShow');
    var links = [];
    App.router.get('mainViewsController.ambariViews').forEach(function(viewInstance) {
      var viewMeta = viewsToShow[viewInstance.get('instanceName')];
      if (viewMeta) {
        var link = {
          viewInstance: viewInstance,
          label: viewInstance.get('label')
        };
        if (viewMeta.overwriteLabel) {
          link.label = Em.I18n.t(viewMeta.overwriteLabel);
        }
        links.push(link);
      }
    });
    return links;
  }.property('App.router.mainViewController.ambariViews'),

  didInsertElement: function () {
    var controller = this.get('controller');
    this._super();
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isComponentsStateLoaded').done(function () {
      controller.setHiveEndPointsValue();
    });
  },

  willDestroyElement: function () {
    this.get('controller.hiveServerEndPoints').clear();
  },

  /**
   * View for clipboard image that copies JDBC connection string
   */
  clipBoardView: Em.View.extend({
    tagName: 'a',
    classNames: ['clip-board'],
    href: "javascript:void(null)",
    attributeBindings: ['data-clipboard-text', 'data-clipboard-action', "href"],
    didInsertElement: function() {
      var $this = this.$();
      var id = "#" + $this.attr('id');
      var clipboard = new Clipboard(id);
      var options = {
        trigger: "click"
      };
      Em.run.next(function () {
        $("[rel=clipboard-tooltip]").tooltip(options);
      });
    },
    mouseLeave: function() {
      Em.run.next(function () {
        $("[rel=clipboard-tooltip]").tooltip("hide");
      });
    }
  })
});