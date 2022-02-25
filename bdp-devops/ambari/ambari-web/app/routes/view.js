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

module.exports = Em.Route.extend({

  breadcrumbs: {
    labelBindingPath: 'App.router.mainViewsDetailsController.content.label'
  },

  route: '/view',
  enter: function (router) {
    Em.$('body').addClass('contribview');
    router.get('mainViewsController').loadAmbariViews();
  },

  exit:function (router) {
    this._super();
    Em.$('body').removeClass('contribview');
  },

  index: Em.Route.extend({
    route: '/',
    connectOutlets: function (router) {
      router.get('mainViewsController').dataLoading().done(function() {
        router.get('mainController').connectOutlet('mainViews');
      });
    }
  }),

  shortViewDetails: Em.Route.extend({
    route: '/:viewName/:shortName',
    breadcrumbs: null,
    connectOutlets: function (router, params) {
      var viewPath = this.parseViewPath(window.location.href.slice(window.location.href.indexOf('?')));
      var slicedShortName = params.shortName;
      if (viewPath) {
        slicedShortName = this._getSlicedShortName(params.shortName);
        if (slicedShortName === params.shortName) {
          viewPath = '';
        }

        if (viewPath.charAt(0) === '/') viewPath = viewPath.slice(1);
      }

      router.get('mainViewsController').dataLoading().done(function() {
        var content = App.router.get('mainViewsController.ambariViews').filterProperty('viewName', params.viewName).findProperty('shortUrl', slicedShortName)
        if (content) content.set('viewPath', viewPath);
        router.get('mainController').connectOutlet('mainViewsDetails', content);
      });

    },

    /**
     * parse the short name and slice if needed
     *
     * @param {string}
     * @returns {string}
     * @private
     */
    _getSlicedShortName: function (instanceName) {
      if (instanceName.lastIndexOf('?') > -1) {
        return instanceName.slice(0, instanceName.lastIndexOf('?'));
      }

      return instanceName;
    },

    /**
     * parse internal view path
     * "viewPath" - used as a key of additional path
     * Example:
     *   origin URL: viewName?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1
     * should be translated to
     *   view path: /user/admin/address?foo=bar&count=1
     *
     * @param {string} instanceName
     * @returns {string}
     */
    parseViewPath: function (instanceName) {
      var path = '';
      if (instanceName.contains('?')) {
        path = instanceName.slice(instanceName.indexOf('?'));
        if (path.contains('viewPath')) {
          path = decodeURIComponent(path.slice((path.lastIndexOf('?viewPath=') + 10))).replace('&', '?');
        }
      }
      return path;
    }

  })
});
