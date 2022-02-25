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

var timezoneUtils = require('utils/date/timezone');

/**
 * Controller for user settings
 * Allows to get them from persist and update them to the persist
 *
 * @class UserSettingsController
 */
App.UserSettingsController = Em.Controller.extend(App.Persist, {

  name: 'userSettingsController',

  /**
   * @type {object}
   */
  userSettings: {},

  /**
   * Each property's type is {name: string, defaultValue: *, formatter: function}
   *
   * @type {object}
   */
  userSettingsKeys: function () {
    var loginName = App.router.get('loginName');
    var prefix = 'admin-settings-';
    return {
      show_bg: {
        name: prefix +'show-bg-' + loginName,
        defaultValue: true
      },
      timezone: {
        name: prefix + 'timezone-' + loginName,
        defaultValue: timezoneUtils.detectUserTimezone(),
        formatter: function (v) {
          return timezoneUtils.get('timezonesMappedByValue')[v];
        }
      }
    };
  }.property('App.router.loginName'),

  /**
   * Load some user's setting from the persist
   * If <code>persistKey</code> is not provided, all settings are loaded
   *
   * @param {string} [persistKey]
   * @method dataLoading
   * @returns {$.Deferred.promise}
   */
  dataLoading: function (persistKey) {
    var key = persistKey ? this.get('userSettingsKeys.' + persistKey + '.name') : '';
    var dfd = $.Deferred();
    var self = this;
    this.getUserPref(key).complete(function () {
      var curPref = self.get('currentPrefObject');
      self.set('currentPrefObject', null);
      dfd.resolve(curPref);
    });
    return dfd.promise();
  },

  /**
   * Success-callback for user pref
   *
   * @param {?object} response
   * @param {object} opt
   * @returns {?object}
   * @method getUserPrefSuccessCallback
   */
  getUserPrefSuccessCallback: function (response, opt) {
    var getAllRequest = opt.url.endsWith('persist/');
    if (Em.isNone(response)) {
      this.updateUserPrefWithDefaultValues(response, getAllRequest);
    }
    this.set('currentPrefObject', response);
    return response;
  },

  /**
   * Error-callback for user-pref request
   * Update user pref with default values if user firstly login
   *
   * @param {object} request
   * @method getUserPrefErrorCallback
   */
  getUserPrefErrorCallback: function (request) {
    // this user is first time login
    if (404 === request.status) {
      this.updateUserPrefWithDefaultValues();
    }
  },

  /**
   * Load all current user's settings to the <code>userSettings</code>
   *
   * @method getAllUserSettings
   */
  getAllUserSettings: function () {
    var userSettingsKeys = this.get('userSettingsKeys');
    var userSettings = {};
    var self = this;
    this.dataLoading().done(function (json) {
      if (!json) {
        return;
      }
      Object.keys(userSettingsKeys).forEach(function (k) {
        var value = userSettingsKeys[k].defaultValue;
        if (undefined === json[userSettingsKeys[k].name]) {
          self.postUserPref(k, userSettingsKeys[k].defaultValue);
        }
        else {
          value = JSON.parse(json[userSettingsKeys[k].name]);
        }
        if ('function' === Em.typeOf(userSettingsKeys[k].formatter)) {
          value = userSettingsKeys[k].formatter(value);
        }
        userSettings[k] = value;
      });
    });
    this.set('userSettings', userSettings);
  },

  /**
   * If user doesn't have any settings stored in the persist,
   * default values should be populated there
   *
   * @param {object} [response]
   * @param {boolean} [getAllRequest] determines, if user tried to get one field or all fields
   * @method updateUserPrefWithDefaultValues
   */
  updateUserPrefWithDefaultValues: function (response, getAllRequest) {
    var r = response || {};
    var keys = this.get('userSettingsKeys');
    var self = this;
    if (getAllRequest) {
      Object.keys(keys).forEach(function (key) {
        if (Em.isNone(r[keys[key].name])) {
          self.postUserPref(key, keys[key].defaultValue);
        }
      });
    }
  },

  /**
   * "Short"-key method for post user settings to the persist
   * Example:
   *  real key is something like 'userSettingsKeys.timezone.name'
   *  but user should call this method with 'timezone'
   *
   * @method postUserPref
   * @param {string} key
   * @param {*} value
   * @returns {*}
   */
  postUserPref: function (key, value) {
    var normalizedKey = key.startsWith('userSettingsKeys.') ? key : 'userSettingsKeys.' + key + '.name';
    var shortKey = normalizedKey.replace('userSettingsKeys.', '').replace('.name', '');
    this.set('userSettings.' + shortKey, value);
    return this._super(this.get(normalizedKey), value);
  },

  /**
   * Open popup with user settings after settings-request is complete
   *
   * @method showSettingsPopup
   */
  showSettingsPopup: function() {
    var self = this;
    // Settings only for admins
    if (!App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK')) {
      return;
    }

    this.dataLoading().done(function(response) {
      self.loadPrivileges().complete(function() {
        self._showSettingsPopup(response);
      });
    });
  },

  loadPrivileges: function() {
    return App.ajax.send({
      name: 'router.user.privileges',
      sender: this,
      data: {
        userName: App.db.getLoginName()
      },
      success: 'loadPrivilegesSuccessCallback'
    });
  },

  loadPrivilegesSuccessCallback: function(data) {
    var key,
        privileges = this.parsePrivileges(data),
        clusters = [],
        views = [];

    for (key in privileges.clusters) {
      clusters.push({
        name: key,
        privileges: privileges.clusters[key]
      });
    }
    for (key in privileges.views) {
      views.push({
        instance_name: key,
        privileges: privileges.views[key].privileges,
        version: privileges.views[key].version,
        view_name: privileges.views[key].view_name
      });
    }
    privileges.clusters = clusters;
    privileges.views = views;
    this.set('privileges', data.items.length ? privileges : null);
    this.set('noClusterPriv', Em.isEmpty(clusters));
    this.set('noViewPriv', Em.isEmpty(views));
    this.set('hidePrivileges', this.get('noClusterPriv') && this.get('noViewPriv'));
  },

  /**
   *
   * @param {?object} data
   * @returns {{clusters: {}, views: {}}}
   */
  parsePrivileges: function (data) {
    var privileges = {
      clusters: {},
      views: {}
    };
    data.items.forEach(function (privilege) {
      privilege = privilege.PrivilegeInfo;
      if (privilege.type === 'CLUSTER') {
        // This is cluster
        privileges.clusters[privilege.cluster_name] = privileges.clusters[privilege.cluster_name] || [];
        privileges.clusters[privilege.cluster_name].push(privilege.permission_label);
      } else if (privilege.type === 'VIEW') {
        privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || {privileges: []};
        privileges.views[privilege.instance_name].version = privilege.version;
        privileges.views[privilege.instance_name].view_name = privilege.view_name;
        privileges.views[privilege.instance_name].privileges.push(privilege.permission_label);
      }
    });
    return privileges;
  },

  /**
   * Show popup with settings for user
   * Don't call this method directly! Use <code>showSettingsPopup</code>
   *
   * @param {object} response
   * @returns {App.ModalPopup}
   * @method _showSettingsPopup
   * @private
   */
  _showSettingsPopup: function (response) {
    var keys = this.get('userSettingsKeys');
    var curValue, self, timezonesFormatted, initValue, initTimezone;
    if (response[keys.show_bg.name]) {
      curValue = null;
      self = this;
      timezonesFormatted = timezoneUtils.get('timezones');
      initValue = JSON.parse(response[keys.show_bg.name]);
      initTimezone = timezonesFormatted.findProperty('value', JSON.parse(response[keys.timezone.name]));
      return App.ModalPopup.show({

        header: Em.I18n.t('common.userSettings'),

        bodyClass: Em.View.extend({

          templateName: require('templates/common/settings'),

          isNotShowBgChecked: !initValue,

          updateValue: function () {
            curValue = !this.get('isNotShowBgChecked');
          }.observes('isNotShowBgChecked'),

          timezonesList: timezonesFormatted,

          privileges: self.get('privileges'),

          isAdmin: App.get('isAdmin'),

          noClusterPriv: self.get('noClusterPriv'),

          noViewPriv: self.get('noViewPriv'),

          hidePrivileges: self.get('hidePrivileges') || App.get('isAdmin')
        }),

        /**
         * @type {string}
         */
        selectedTimezone: initTimezone,

        primary: Em.I18n.t('common.save'),

        onPrimary: function () {
          if (Em.isNone(curValue)) {
            curValue = initValue;
          }
          var tz = this.get('selectedTimezone.value');
          var popup = this;
          if (!App.get('testMode')) {
            self.postUserPref('show_bg', curValue).always(function () {
              self.postUserPref('timezone', tz).always(function () {
                if (popup.needsPageRefresh()) {
                  location.reload();
                }
              });
            });
          }
          this._super();
        },

        /**
         * Determines if page should be refreshed after user click "Save"
         *
         * @returns {boolean}
         */
        needsPageRefresh: function () {
          return initTimezone !== this.get('selectedTimezone');
        }
      });
    } else {
      App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('app.settings.noData'));
    }
  }

});
