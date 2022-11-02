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

module.exports = {

  isValidEmail: function(value) {
    var emailRegex = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))$/i;
    return emailRegex.test(value);
  },

  isValidInt: function(value) {
    var intRegex = /^-?\d+$/;
    return intRegex.test(value);
  },

  isValidUNIXUser: function(value){
    var regex = /^[a-z_][a-z0-9_-]{0,31}$/;
    return regex.test(value);
  },

  isValidFloat: function(value) {
    if (typeof value === 'string' && value.trim() === '') {
      return false;
    }
    var floatRegex = /^-?(?:\d+|\d{1,3}(?:,\d{3})+)?(?:\.\d+)?$/;
    return floatRegex.test(value);
  },
  /**
   * validate directory with slash or drive at the start
   * @param value
   * @return {Boolean}
   */
  isValidDir: function(value){
    var floatRegex = /^\/[0-9a-z]*/;
    var winRegex = /^[a-z]:\\[0-9a-zA-Z]*/;
    var winUrlRegex = /^file:\/\/\/[a-zA-Z]:\/[0-9a-zA-Z]*/;
    var dirs = value.split(',');
    if (dirs.some(function(i) { return i.startsWith(' '); })) {
      return false;
    }
    for(var i = 0; i < dirs.length; i++){
      if(!floatRegex.test(dirs[i]) && !winRegex.test(dirs[i]) && !winUrlRegex.test(dirs[i])){
        return false;
      }
    }
    return true;
  },

  /**
   * validate filename
   */
  isValidFileName: function(value){
    var filenameRegex = /^[0-9a-zA-Z_-]+\.[a-zA-Z]+$/;
    return filenameRegex.test(value);
  },

  /**
   * defines if config value looks like link to other config
   * @param value
   * @returns {boolean}
   */
  isConfigValueLink: function(value) {
    return /^\${.+}$/.test(value);
  },

  /**
   * validate directory with slash at the start
   * @param value
   * @returns {boolean}
   */
  isValidDataNodeDir: function(value) {
    var dirRegex = /^(\[[0-9a-zA-Z]+_?[0-9a-zA-Z]+\])?(file:\/\/)?(\/[0-9a-z]*)/;
    var winRegex = /^(\[[0-9a-zA-Z]+_?[0-9a-zA-Z]+\])?[a-zA-Z]:\\[0-9a-zA-Z]*/;
    var winUrlRegex = /^(\[[0-9a-zA-Z]+_?[0-9a-zA-Z]+\])?file:\/\/\/[a-zA-Z]:\/[0-9a-zA-Z]*/;
    var dirs = value.split(',');
    if (dirs.some(function (i) {return i.startsWith(' '); })) {
      return false;
    }
    for(var i = 0; i < dirs.length; i++){
      if(!dirRegex.test(dirs[i]) && !winRegex.test(dirs[i]) && !winUrlRegex.test(dirs[i])){
        return false;
      }
    }
    return true;
  },

  /**
   * validate directory doesn't start "home" or "homes"
   * @param value
   * @returns {boolean}
   */
  isAllowedDir: function(value) {
    var dirs = value.replace(/,/g,' ').trim().split(new RegExp("\\s+", "g"));
    for(var i = 0; i < dirs.length; i++){
      if(dirs[i].startsWith('/home') || dirs[i].startsWith('/homes')) {
        return false;
      }
    }
    return true;
  },

  /**
   * validate ip address with port
   * @param value
   * @return {Boolean}
   */
  isIpAddress: function(value) {
    var ipRegex = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)($|\:[0-9]{1,5})$/;
    return ipRegex.test(value);
  },

  /**
   * validate hostname
   * @param value
   * @return {Boolean}
   */
  isHostname: function(value) {
    var regex = /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/;
    return value === 'localhost' || regex.test(value);
  },

  hasSpaces: function(value) {
    var regex = /(\s+)/;
    return regex.test(value);
  },

  isNotTrimmed: function(value) {
    var regex = /(^\s+|\s+$)/;
    return regex.test(value);
  },

  /**
   * Check if string ends with spaces.
   * For multiline content only last line will be checked.
   *
   * @method isNotTrimmedLeft
   * @param {String} value
   * @returns {Boolean} - <code>true</code> if ends with spaces
   */
  isNotTrimmedRight: function(value) {
    return value !== ' ' && /\s+$/.test(("" + value).split(/\n/).slice(-1)[0]);
  },

  /**
   * validate domain name with port
   * @param value
   * @return {Boolean}
   */
  isDomainName: function(value) {
    var domainRegex = /^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,6}$/;
    return domainRegex.test(value);
  },

  /**
   * validate username
   * @param value
   * @return {Boolean}
   */
  isValidUserName: function(value) {
    var usernameRegex = /^[a-z]([-a-z0-9]{0,30})$/;
    return usernameRegex.test(value);
  },

  /**
   * validate db name
   * @param value
   * @returns {boolean}
   */
  isValidDbName: function(value) {
    var dbPattern = /^\S+$/;
    return dbPattern.test(value);
  },

  /**
   * validate key of configurations
   * allow spaces as prefix and suffix
   *
   * @param value
   * @return {Boolean}
   */
  isValidConfigKey: function(value) {
    var configKeyRegex = /^\s*[0-9a-z_\-\.\/\*]+\s*$/i;
    return configKeyRegex.test(value);
  },

  /**
   * validate configuration group name
   * @param value
   * @return {Boolean}
   */
  isValidConfigGroupName: function(value) {
    var configKeyRegex = /^[\s0-9a-z_\-]+$/i;
    return configKeyRegex.test(value);
  },

  /**
   * validate alert notification name
   * @param value
   * @return {Boolean}
   */
  isValidAlertNotificationName: function(value) {
    var configKeyRegex = /^[\s0-9a-z_\-]+$/i;
    return configKeyRegex.test(value);
  },
  
  /**
   * validate alert group name
   * @param value
   * @return {Boolean}
   */
  isValidAlertGroupName: function(value) {
    var configKeyRegex = /^[\s0-9a-z_\-]+$/i;
    return configKeyRegex.test(value);
  },

  empty:function (e) {
    switch (e) {
      case "":
      case 0:
      case "0":
      case null:
      case false:
      case undefined:
      case typeof this == "undefined":
        return true;
      default :
        return false;
    }
  },
  /**
   * Validate string that will pass as parameter to .matches() url param.
   * Try to prevent invalid regexp.
   * For example: /api/v1/clusters/c1/hosts?Hosts/host_name.matches(.*localhost.)
   *
   * @param {String} value - string to validate
   * @return {Boolean}
   * @method isValidMatchesRegexp
   */
  isValidMatchesRegexp: function(value) {
    var checkPair = function(chars) {
      chars = chars.map(function(c) { return '\\' + c; });
      var charsReg = new RegExp(chars.join('|'), 'g');
      if (charsReg.test(value)) {
        var pairContentReg = new RegExp(chars.join('.*'), 'g');
        if (!pairContentReg.test(value)) return false;
        var pairCounts = chars.map(function(c) { return value.match(new RegExp(c, 'g')).length; });
        if (pairCounts[0] != pairCounts[1] ) return false;
      }
      return true;
    };
    if (/^[\?\|\*\!,]/.test(value)) return false;
    return /^((\.\*?)?([\w\s\[\]\/\?\-_,\|\*\!\{\}\(\)]*)?)+(\.\*?)?$/g.test(value) && (checkPair(['[',']'])) && (checkPair(['{','}']));
  },

  /**
  * Remove validation messages for components which are already installed
  */
  filterNotInstalledComponents: function(validationData) {
    var hostComponents = App.HostComponent.find();
    return validationData.resources[0].items.filter(function(item) {
      // true is there is no host with this component
      return hostComponents.filterProperty("componentName", item["component-name"]).filterProperty("hostName", item.host).length === 0;
    });
  },

  isValidRackId: function(path) {
    var _path = '' + path;
    // See app/message.js:hostPopup.setRackId.invalid
    return /^\/[/.\w-]+$/.test(_path) && _path.length < 255;
  },

  /**
   * Validate url
   * @param value
   * @return {Boolean}
   */
  isValidURL: function(value) {
    var urlRegex = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i;
    return urlRegex.test(value);
  },

  /**
   * Validate base URL
   * @param {string} value
   * @returns {boolean}
   */
  isValidBaseUrl: function (value) {
    var remotePattern = /^$|^(?:(?:https?|ftp):\/{2})(?:\S+(?::\S*)?@)?(?:(?:(?:[\w\-.]))*)(?::[0-9]+)?(?:\/\S*)?$/,
      localPattern = /^$|^file:\/{2,3}([a-zA-Z][:|]\/){0,1}[\w~!*'();@&=\/\\\-+$,?%#.\[\]]+$/;
    return remotePattern.test(value) || localPattern.test(value);
  },

  /**
   * Validate widget name
   * @param {string} value
   * @returns {boolean}
   */
  isValidWidgetName: function(value) {
    var widgetNameRegex = /^[\s0-9a-z_\-%]+$/i;
    return widgetNameRegex.test(value);
  },

  /**
   * Validate widget description
   * @param {string} value
   * @returns {boolean}
   */
  isValidWidgetDescription: function(value) {
    var widgetDescriptionRegex = /^[\s0-9a-z_\-%]+$/i;
    return widgetDescriptionRegex.test(value);
  },

  /**
   * Validate alert name
   * @param {string} value
   * @returns {boolean}
   */
  isValidAlertName: function(value) {
    var alertNameRegex = /^[\s0-9a-z_\-%\(\)]+$/i;
    return alertNameRegex.test(value);
  },

  /**
   * Validate ldaps URL
   * @param {string} value
   * @returns {boolean}
   */
  isValidLdapsURL: function(value) {
    var ldapsUrlRegex = /^(ldaps):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i;
    return ldapsUrlRegex.test(value);
  },

  isValidNameServiceId: function (value) {
    var nameSarviceIdRegex = /^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])$/;
    return nameSarviceIdRegex.test(value);
  },

};
