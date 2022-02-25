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
var validator = require('utils/validator');
var lazyloading = require('utils/lazy_loading');

App.WizardStep2Controller = Em.Controller.extend({

  name: 'wizardStep2Controller',

  /**
   * List of not installed hostnames
   * @type {string[]}
   */
  hostNameArr: [],

  /**
   * Does pattern-expression for hostnames contains some errors
   * @type {bool}
   */
  isPattern: false,

  /**
   * Don't know if it used any more
   */
  bootRequestId: null,

  /**
   * Is step submitted
   * @type {bool}
   */
  hasSubmitted: false,

  /**
   * @type {string[]}
   */
  inputtedAgainHostNames: [],

  /**
   * Is Installer Controller used
   * @type {bool}
   */
  isInstaller: Em.computed.equal('content.controllerName', 'installerController'),

  /**
   * "Shortcut" to <code>content.installOptions.hostNames</code>
   * @type {string}
   */
  hostNames: function () {
    return this.get('content.installOptions.hostNames').toLowerCase();
  }.property('content.installOptions.hostNames'),

  /**
   * Is manual install selected
   * "Shortcut" to <code>content.installOptions.manualInstall</code>
   * @type {bool}
   */
  manualInstall: Em.computed.alias('content.installOptions.manualInstall'),

  /**
   * "Shortcut" to <code>content.installOptions.sshKey</code>
   * @type {string}
   */
  sshKey: Em.computed.alias('content.installOptions.sshKey'),

  /**
   * "Shortcut" to <code>content.installOptions.sshUser</code>
   * @type {string}
   */
  sshUser: Em.computed.alias('content.installOptions.sshUser'),

  /**
   * "Shortcut" to <code>content.installOptions.sshPort</code>
   * @type {string}
   */
  sshPort: Em.computed.alias('content.installOptions.sshPort'),

  /**
   * "Shortcut" to <code>content.installOptions.agentUser</code>
   * @type {string}
   */
  agentUser: Em.computed.alias('content.installOptions.agentUser'),

  /**
   * Installed type based on <code>manualInstall</code>
   * @type {string}
   */
  installType: Em.computed.ifThenElse('manualInstall', 'manualDriven', 'ambariDriven'),

  /**
   * List of invalid hostnames
   * @type {string[]}
   */
  invalidHostNames: [],

  /**
   * Error-message if <code>hostNames</code> is empty, null otherwise
   * @type {string|null}
   */
  hostsError: null,

  useSSH: function () {
    return !App.get('isHadoopWindowsStack');
  }.property('App.isHadoopWindowsStack'),

  /**
   * Error-message if <code>sshKey</code> is empty, null otherwise
   * @type {string|null}
   */
  sshKeyError: function () {
    if (this.get('hasSubmitted') && this.get('manualInstall') === false && this.get('useSSH') && Em.isBlank(this.get('sshKey'))) {
      return Em.I18n.t('installer.step2.sshKey.error.required');
    }
    return null;
  }.property('sshKey', 'useSSH', 'manualInstall', 'hasSubmitted'),

  /**
   * Error-message if <code>sshUser</code> is empty, null otherwise
   * @type {string|null}
   */
  sshUserError: function () {
    if (this.get('manualInstall') === false && this.get('useSSH') && Em.isBlank(this.get('sshUser'))) {
      return Em.I18n.t('installer.step2.sshUser.required');
    }
    return null;
  }.property('sshUser', 'useSSH', 'hasSubmitted', 'manualInstall'),

  /**
   * Error-message if <code>sshPort</code> is empty, null otherwise
   * @type {string|null}
   */
  sshPortError: function () {
    if (this.get('manualInstall') === false && this.get('useSSH') && Em.isBlank(this.get('sshPort')))  {
      return Em.I18n.t('installer.step2.sshPort.required');
    }
    return null;
  }.property('sshPort', 'useSSH', 'hasSubmitted', 'manualInstall'),

  /**
   * Error-message if <code>agentUser</code> is empty, null otherwise
   * @type {string|null}
   */
  agentUserError: function () {
    if (App.get('supports.customizeAgentUserAccount') && this.get('manualInstall') === false && Em.isBlank(this.get('agentUser'))) {
      return Em.I18n.t('installer.step2.sshUser.required');
    }
    return null;
  }.property('agentUser', 'hasSubmitted', 'manualInstall'),

  /**
   * is Submit button disabled
   * @type {bool}
   */
  isSubmitDisabled: Em.computed.or('hostsError', 'sshKeyError', 'sshUserError', 'sshPortError', 'agentUserError', 'App.router.btnClickInProgress'),

  installedHostNames: function () {
    var installedHostsName = [];
    var hosts = this.get('content.hosts');

    for (var hostName in hosts) {
      if (hosts[hostName].isInstalled) {
        installedHostsName.push(hostName);
      }
    }
    return installedHostsName;
  }.property('content.hosts'),

  /**
   * Set not installed hosts to the hostNameArr
   * @method updateHostNameArr
   */
  updateHostNameArr: function () {
    this.set('hostNameArr', this.get('hostNames').trim().split(new RegExp("\\s+", "g")));
    this.parseHostNamesAsPatternExpression();
    this.get('inputtedAgainHostNames').clear();
    var tempArr = [],
        hostNameArr = this.get('hostNameArr');
    for (var i = 0; i < hostNameArr.length; i++) {
      if (!this.get('installedHostNames').contains(hostNameArr[i])) {
        tempArr.push(hostNameArr[i]);
      }
      else {
        this.get('inputtedAgainHostNames').push(hostNameArr[i]);
      }
    }
    this.set('hostNameArr', tempArr);
  },

  /**
   * Validate host names
   * @method isAllHostNamesValid
   * @return {bool}
   */
  isAllHostNamesValid: function () {
    var result = true;
    this.updateHostNameArr();
    this.get('invalidHostNames').clear();
    this.get('hostNameArr').forEach(function (hostName) {
      if (!validator.isHostname(hostName)) {
        this.get('invalidHostNames').push(hostName);
        result = false;
      }
    }, this);

    return result;
  },

  /**
   * Set hostsError if host names don't pass validation
   * @method checkHostError
   */
  checkHostError: function () {
    if (Em.isEmpty(this.get('hostNames').trim())) {
      this.set('hostsError', Em.I18n.t('installer.step2.hostName.error.required'));
    }
    else {
      this.set('hostsError', null);
    }
  },

  /**
   * Check hostnames after Submit was clicked or <code>hostNames</code> were changed
   * @method checkHostAfterSubmitHandler
   */
  checkHostAfterSubmitHandler: function () {
    if (this.get('hasSubmitted')) {
      this.checkHostError();
    }
  }.observes('hasSubmitted', 'hostNames'),

  /**
   * Get host info, which will be saved in parent controller
   * @method getHostInfo
   */
  getHostInfo: function () {

    var hostNameArr = this.get('hostNameArr');
    var hostInfo = {};
    for (var i = 0; i < hostNameArr.length; i++) {
      hostInfo[hostNameArr[i]] = {
        name: hostNameArr[i],
        installType: this.get('installType'),
        bootStatus: 'PENDING',
        isInstalled: false
      };
    }

    return hostInfo;
  },

  /**
   * Used to set sshKey from FileUploader
   * @method setSshKey
   * @param {string} sshKey
   */
  setSshKey: function (sshKey) {
    this.set("content.installOptions.sshKey", sshKey);
  },

  /**
   * Onclick handler for <code>next button</code>. Do all UI work except data saving.
   * This work is doing by router.
   * @method evaluateStep
   * @return {bool}
   */
  evaluateStep: function () {

    if (this.get('isSubmitDisabled')) {
      return false;
    }

    this.set('hasSubmitted', true);

    this.checkHostError();
    this.updateHostNameArr();

    if (!this.get('hostNameArr.length')) {
      this.set('hostsError', Em.I18n.t('installer.step2.hostName.error.already_installed'));
    }

    if (this.get('hostsError') || this.get('sshUserError') || this.get('sshPortError') || this.get('agentUserError') || this.get('sshKeyError')) {
      return false;
    }

    if (this.get('isPattern')) {
      this.hostNamePatternPopup(this.get('hostNameArr'));
      return false;
    }
    if (this.get('inputtedAgainHostNames.length')) {
      this.installedHostsPopup();
    }
    else {
      this.proceedNext();
    }
    return true;
  },

  /**
   * check is there a pattern expression in host name textarea
   * push hosts that match pattern in hostNamesArr
   * @method parseHostNamesAsPatternExpression
   */
  parseHostNamesAsPatternExpression: function () {
    this.set('isPattern', false);
    var hostNames = [];

    this.get('hostNameArr').forEach(function (a) {
      var hn,
          allPatterns = a.match(/\[\d*\-\d*\]/g),
          patternsNumber = allPatterns ? allPatterns.length : 0;

      if (patternsNumber) {
        hn = [a];
        for (var i = 0; i < patternsNumber; i++) {
          hn = this._replacePatternInHosts(hn);
        }
        hostNames = hostNames.concat(hn);
      } else {
        hostNames.push(a);
      }
    }, this);

    this.set('hostNameArr', hostNames.uniq());
  },

  /**
   * return an array of results with pattern replacement for each host
   * replace only first pattern in each host
   * designed to be called recursively in <code>parseHostNamesAsPatternExpression</code>
   *
   * @param {Array} rawHostNames
   * @private
   * @return {Array}
   */
  _replacePatternInHosts: function (rawHostNames) {
    var start, end, extra, allHostNames = [];
    rawHostNames.forEach(function (rawHostName) {
      var hostNames = [];
      start = rawHostName.match(/\[\d*/);
      end = rawHostName.match(/\-\d*]/);
      extra = {0: ""};

      start = start[0].substr(1);
      end = end[0].substr(1);

      if (parseInt(start) <= parseInt(end, 10) && parseInt(start, 10) >= 0) {
        this.set('isPattern', true);

        if (start[0] == "0" && start.length > 1) {
          extra = start.match(/0*/);
        }

        for (var i = parseInt(start, 10); i < parseInt(end, 10) + 1; i++) {
          hostNames.push(rawHostName.replace(/\[\d*\-\d*\]/, extra[0].substring(0, start.length - i.toString().length) + i))
        }

      } else {
        hostNames.push(rawHostName);
      }
      allHostNames = allHostNames.concat(hostNames);
    }, this);

    return allHostNames;
  },

  /**
   * launch hosts to bootstrap
   * and save already registered hosts
   * @method proceedNext
   * @return {bool}
   */
  proceedNext: function (warningConfirmed) {
    if (this.isAllHostNamesValid() !== true && !warningConfirmed) {
      this.warningPopup();
      return false;
    }

    if (this.get('manualInstall') === true) {
      this.manualInstallPopup();
      return false;
    }

    this.saveHosts();
    App.router.send('next');
    return true;
  },

  /**
   * show warning for host names without dots or IP addresses
   * @return {App.ModalPopup}
   * @method warningPopup
   */
  warningPopup: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      primary: Em.I18n.t('common.continue'),
      primaryClass: 'btn-warning',
      onPrimary: function () {
        this.hide();
        self.proceedNext(true);
      },
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('installer.step2.warning.popup.body').format(self.get('invalidHostNames').join(', ')))
      })
    });
  },

  /**
   * show popup with the list of hosts that are already part of the cluster
   * @return {App.ModalPopup}
   * @method installedHostsPopup
   */
  installedHostsPopup: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      onPrimary: function () {
        self.proceedNext();
        this.hide();
      },
      bodyClass: Em.View.extend({
        inputtedAgainHostNames: function () {
          return self.get('inputtedAgainHostNames').join(', ');
        }.property(),
        templateName: require('templates/wizard/step2_installed_hosts_popup')
      })
    });
  },

  /**
   * Show popup with hosts generated by pattern
   * @method hostNamePatternPopup
   * @param {string[]} hostNames
   * @return {App.ModalPopup}
   */
  hostNamePatternPopup: function (hostNames) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.hostName.pattern.header'),
      onPrimary: function () {
        self.proceedNext();
        this.hide();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/common/items_list_popup'),
        items: hostNames,
        insertedItems: [],
        didInsertElement: function () {
          lazyloading.run({
            destination: this.get('insertedItems'),
            source: this.get('items'),
            context: this,
            initSize: 100,
            chunkSize: 500,
            delay: 100
          });
        }
      })
    });
  },

  /**
   * Show notify that installation is manual
   * save hosts
   * @return {App.ModalPopup}
   * @method manualInstallPopup
   */
  manualInstallPopup: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      onPrimary: function () {
        this.hide();
        self.saveHosts();
        App.router.send('next');
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step2ManualInstallPopup')
      })
    });
  },

  /**
   * Warn to manually install ambari-agent on each host
   * @method manualInstallWarningPopup
   */
  manualInstallWarningPopup: function () {
    if (!this.get('content.installOptions.useSsh')) {
      App.ModalPopup.show({
        header: Em.I18n.t('common.warning'),
        body: Em.I18n.t('installer.step2.manualInstall.info'),
        encodeBody: false,
        secondary: null
      });
    }
    this.set('content.installOptions.manualInstall', !this.get('content.installOptions.useSsh'));
  }.observes('content.installOptions.useSsh'),

  /**
   * Load java.home value frin server
   * @method setAmbariJavaHome
   */
  setAmbariJavaHome: function () {
    App.ajax.send({
      name: 'ambari.service',
      sender: this,
      success: 'onGetAmbariJavaHomeSuccess',
      error: 'onGetAmbariJavaHomeError'
    });
  },

  /**
   * Set received java.home value
   * @method onGetAmbariJavaHomeSuccess
   * @param {Object} data
   */
  onGetAmbariJavaHomeSuccess: function (data) {
    this.set('content.installOptions.javaHome', data.RootServiceComponents.properties['java.home']);
  },

  /**
   * Set default java.home value
   * @method onGetAmbariJavaHomeError
   */
  onGetAmbariJavaHomeError: function () {
    this.set('content.installOptions.javaHome', App.get('defaultJavaHome'));
  },

  /**
   * Save hosts info and proceed to the next step
   * @method saveHosts
   */
  saveHosts: function () {
    var hosts = this.get('content.hosts');

    //add previously installed hosts
    for (var hostName in hosts) {
      if (!hosts[hostName].isInstalled) {
        delete hosts[hostName];
      }
    }

    this.set('content.hosts', $.extend(hosts, this.getHostInfo()));
    this.setAmbariJavaHome();
  },

  isAddHostWizard: Em.computed.equal('content.controllerName', 'addHostController')

});
