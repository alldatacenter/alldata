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

  breadcrumbs: null,

  route: '/views',
  enter: function (router) {
    router.get('mainViewsController').loadAmbariViews();
  },
  index: Em.Route.extend({
    route: '/',
    connectOutlets: function (router) {
      router.get('mainViewsController').dataLoading().done(function() {
        router.get('mainController').connectOutlet('mainViews');
      });
    }
  }),


  viewDetails: Em.Route.extend({

    route: '/:viewName/:version/:instanceName',

    enter: function (router) {
      Em.$('body').addClass('contribview');
    },

    breadcrumbs: {
      labelBindingPath: 'App.router.mainViewsDetailsController.content.label'
    },

    exit:function (router) {
      this._super();
      Em.$('body').removeClass('contribview');
    },

    connectOutlets: function (router, params) {
      // find and set content for `mainViewsDetails` and associated controller
      var href = ['/views', params.viewName, params.version, params.instanceName + "/"].join('/');
      var viewPath = this.parseViewPath(window.location.href.slice(window.location.href.indexOf('?')));
      if (viewPath) {
        var slicedInstanceName = this._getSlicedInstanceName(params.instanceName);
        if (slicedInstanceName === params.instanceName) {
          viewPath = '';
        }
        href = ['/views', params.viewName, params.version, slicedInstanceName + "/"].join('/');
        //remove slash from viewPath since href already contains it at the end
        if (viewPath.charAt(0) === '/') viewPath = viewPath.slice(1);
      }

      router.get('mainViewsController').dataLoading().done(function() {
        var content = App.router.get('mainViewsController.ambariViews').filter(function(i) {
          return Em.get(i, 'href').endsWith(href);
        })[0];
        if (content) content.set('viewPath', viewPath);
        router.get('mainController').connectOutlet('mainViewsDetails', content);
      });
    },

    /**
     * parse the instance name and slice if needed
     *
     * @param {string}
     * @returns {string}
     * @private
     */
    _getSlicedInstanceName: function (instanceName) {
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
