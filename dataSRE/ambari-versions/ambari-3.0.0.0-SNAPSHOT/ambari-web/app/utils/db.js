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
App.db = {};
var InitialData = {
  'app': {
    'loginName': '',
    'authenticated': false,
    'configs': [],
    'tags': [],
    'tables': {
      'filterConditions': {},
      'displayLength': {},
      'startIndex': {},
      'sortingConditions': {},
      'selectedItems': {}
    }
  },

  'Installer': {},
  'AddHost': {},
  'AddService': {},
  'WidgetWizard': {},
  'KerberosWizard': {},
  'ReassignMaster': {},
  'AddSecurity': {},
  'AddAlertDefinition': {
    content: {}
  },
  'HighAvailabilityWizard': {},
  'RMHighAvailabilityWizard': {},
  'AddHawqStandbyWizard': {},
  'RemoveHawqStandbyWizard': {},
  'ActivateHawqStandbyWizard': {},
  'RAHighAvailabilityWizard': {},
  'NameNodeFederationWizard': {},
  'RollbackHighAvailabilityWizard': {},
  'MainAdminStackAndUpgrade': {},
  'KerberosDisable': {},
  'tmp': {}

};

function checkNamespace(namespace) {
  if (!namespace) {
    return false;
  }
  if (Em.isNone(Em.get(App.db.data, namespace))) {
    Em.setFullPath(App.db.data, namespace, {});
  }
  return true;
}

if (typeof Storage === 'undefined') {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  };
  localStorage.getItem = function (key) {
    return this[key];
  };
  window.localStorage.setObject = function (key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function (key) {
    return this[key];
  };
}
else {
  Storage.prototype.setObject = function (key, value) {
    this.setItem(key, JSON.stringify(value));
  };

  Storage.prototype.getObject = function (key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  };
}

App.db.cleanUp = function () {
  App.db.data = InitialData;
  localStorage.setObject('ambari', App.db.data);
};

App.db.cleanTmp = function () {
  App.db.data.tmp = {};
  localStorage.setObject('ambari', App.db.data);
};

App.db.updateStorage = function () {
  App.db.data = localStorage.getObject('ambari');
  if (Em.get(App, 'db.data.app.tables') && Em.get(App, 'db.data.app.configs')) {
    return true;
  }
  App.db.cleanUp();
  return false;
};

/*
 Initialize wizard namespaces if they are not initialized on login.
 This will be required during upgrade.
 */
App.db.mergeStorage = function () {
  if (localStorage.getObject('ambari') == null) {
    App.db.cleanUp();
  } else {
    localStorage.setObject('ambari', $.extend(true, {}, InitialData, App.db.data));
  }
};

// called whenever user logs in
if (localStorage.getObject('ambari') == null) {
  App.db.cleanUp();
}

/**
 *
 * @param {string} namespace
 * @param {string} key
 * @returns {*}
 */
App.db.get = function (namespace, key) {
  App.db.data = localStorage.getObject('ambari');
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  if (key.contains('user-pref')) {
    // username may contain "." which is the part of "user-pref-*" key so Em.set should be avoided
    return Em.get(App.db.data, namespace)[key];
  }
  return Em.get(Em.get(App.db.data, namespace), key);
};

/**
 *
 * @param {string} namespace
 * @param {string[]} listOfProperties
 * @returns {object}
 */
App.db.getProperties = function (namespace, listOfProperties) {
  App.db.data = localStorage.getObject('ambari');
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  return Em.getProperties(Em.get(App.db.data, namespace), listOfProperties);
};

/**
 *
 * @param {string} namespace
 * @param {string} key
 * @param {*} value
 */
App.db.set = function (namespace, key, value) {
  App.db.data = localStorage.getObject('ambari');
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  if (key.contains('user-pref')) {
    // username may contain "." which is the part of "user-pref-*" key so Em.set should be avoided
    Em.get(App.db.data, namespace)[key] = value;
  } else {
    Em.set(Em.get(App.db.data, namespace), key, value);
  }
  localStorage.setObject('ambari', App.db.data);
};

/**
 *
 * @param {string} namespace
 * @param {{key: value}} hash
 */
App.db.setProperties = function (namespace, hash) {
  App.db.data = localStorage.getObject('ambari');
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  Em.setProperties(Em.get(App.db.data, namespace), hash);
  localStorage.setObject('ambari', App.db.data);
};

App.db.setLoginName = function (name) {
  App.db.set('app', 'loginName', name);
};

/**
 * Set user model to db
 * @param user
 */
App.db.setUser = function (user) {
  App.db.set('app', 'user', user);
};

App.db.setAuth = function (auth) {
  App.db.set('app', 'auth', auth);
};

App.db.setAuthenticated = function (authenticated) {
  App.db.set('app', 'authenticated', authenticated);
  App.db.data = localStorage.getObject('ambari');
};

App.db.setFilterConditions = function (name, filterConditions) {
  App.db.set('app.tables.filterConditions', name, filterConditions);
};

App.db.setComboSearchQuery = function (name, query) {
  App.db.set('app.tables.comboSearchQuery', name, query);
};

App.db.setDisplayLength = function (name, displayLength) {
  App.db.set('app.tables.displayLength', name, displayLength);
};

App.db.setStartIndex = function (name, startIndex) {
  App.db.set('app.tables.startIndex', name, startIndex);
};

App.db.setSortingStatuses = function (name, sortingConditions) {
  App.db.set('app.tables.sortingConditions', name, sortingConditions);
};

App.db.setSelectedHosts = function (selectedHosts) {
  App.db.set('app.tables.selectedItems', 'mainHostController', selectedHosts);
};

App.db.unselectHosts = function (hostsToUnselect = []) {
  let selectedHosts = App.db.getSelectedHosts();
  selectedHosts = selectedHosts.filter(host => hostsToUnselect.indexOf(host) === -1);
  App.db.setSelectedHosts(selectedHosts);
};

App.db.setMasterComponentHosts = function (masterComponentHosts) {
  App.db.set('Installer', 'masterComponentHosts', masterComponentHosts);
};

App.db.setMasterToReassign = function (masterComponent) {
  App.db.set('ReassignMaster', 'masterComponent', masterComponent);
};

App.db.setReassignTasksStatuses = function (tasksStatuses) {
  App.db.set('ReassignMaster', 'tasksStatuses', tasksStatuses);
};

App.db.setReassignTasksRequestIds = function (requestIds) {
  App.db.set('ReassignMaster', 'tasksRequestIds', requestIds);
};

App.db.setStacks = function (stacks) {
  App.db.set('Installer', 'stacksVersions', stacks);
};

App.db.setOses = function (oses) {
  App.db.set('Installer', 'operatingSystems', oses);
};

App.db.setRepos = function (repos) {
  App.db.set('Installer', 'repositories', repos);
};

App.db.setLocalRepoVDFData = function (data) {
  App.db.set('Installer', 'localRepoVDFData', data);
};

App.db.setConfigs = function (configs) {
  App.db.set('app', 'configs', configs);
};

App.db.setTags = function (tags) {
  App.db.set('app', 'tags', tags);
};

/**
 * Set current step value for specified Wizard Type
 * @param wizardType
 * @param currentStep
 */
App.db.setWizardCurrentStep = function (wizardType, currentStep) {
  App.db.set(wizardType.capitalize(), 'currentStep', currentStep);
};

/**
 * Set localStorage with data from server
 */
App.db.setLocalStorage = function () {
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSecurityWizardStatus = function (status) {
  App.db.set('AddSecurity', 'status', status);
};

App.db.setDisableSecurityStatus = function (status) {
  App.db.set('AddSecurity', 'disableSecurityStatus', status);
};

App.db.setSecurityDeployCommands = function (commands) {
  App.db.set('AddSecurity', 'securityDeployCommands', commands);
};

App.db.setHighAvailabilityWizardConfigTag = function (tag) {
  App.db.set('HighAvailabilityWizard', tag.name, tag.value);
};

App.db.setHighAvailabilityWizardHdfsClientHosts = function (hostNames) {
  App.db.set('HighAvailabilityWizard', 'hdfsClientHostNames', hostNames);
};

App.db.setHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.set('HighAvailabilityWizard', 'tasksStatuses', tasksStatuses);
};

App.db.setHighAvailabilityWizardTasksRequestIds = function (requestIds) {
  App.db.set('HighAvailabilityWizard', 'tasksRequestIds', requestIds);
};

App.db.setHighAvailabilityWizardHdfsUser = function (hdfsUser) {
  App.db.set('HighAvailabilityWizard', 'hdfsUser', hdfsUser);
};

App.db.setHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.set('HighAvailabilityWizard', 'requestIds', requestIds);
};

App.db.setHighAvailabilityWizardNameServiceId = function (nameServiceId) {
  App.db.set('HighAvailabilityWizard', 'nameServiceId', nameServiceId);
};

App.db.setRollBackHighAvailabilityWizardAddNNHost = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'addNNHost', host);
};

App.db.setRollBackHighAvailabilityWizardSNNHost = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'sNNHost', host);
};

App.db.setRollBackHighAvailabilityWizardSelectedAddNN = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'selectedAddNN', host);
};

App.db.setRollBackHighAvailabilityWizardSelectedSNN = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'selectedSNNH', host);
};

App.db.setRollbackHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.set('RollbackHighAvailabilityWizard', 'tasksStatuses', tasksStatuses);
};

App.db.setRollbackHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.set('RollbackHighAvailabilityWizard', 'requestIds', requestIds);
};

App.db.setReassignMasterWizardRequestIds = function (requestIds) {
  App.db.set('ReassignMaster', 'requestIds', requestIds);
};

App.db.setReassignMasterWizardComponentDir = function (componentDir) {
  App.db.set('ReassignMaster', 'componentDir', componentDir);
};

App.db.setReassignMasterWizardReassignHosts = function (reassignHosts) {
  App.db.set('ReassignMaster', 'reassignHosts', reassignHosts);
};

App.db.setKerberosWizardConfigTag = function (tag) {
  App.db.set('KerberosWizard', tag.name, tag.value);
};

App.db.setManageJournalNodeWizardConfigTag = function (tag) {
  App.db.set('ManageJournalNodeWizard', tag.name, tag.value);
};

/**
 * Get user model from db
 * @return {*}
 */
App.db.getUser = function () {
  return App.db.get('app', 'user');
};

App.db.getAuth = function () {
  return App.db.get('app', 'auth');
};

App.db.getLoginName = function () {
  return App.db.get('app', 'loginName');
};

App.db.getAuthenticated = function () {
  return Boolean(App.db.get('app', 'authenticated'));
};

App.db.getFilterConditions = function (name) {
  return name ? App.db.get('app.tables.filterConditions', name) : null;
};

App.db.getComboSearchQuery = function (name) {
  return name ? App.db.get('app.tables.comboSearchQuery', name) : null;
};

App.db.getDisplayLength = function (name) {
  return name ? App.db.get('app.tables.displayLength', name) : null;
};

App.db.getStartIndex = function (name) {
  return name ? App.db.get('app.tables.startIndex', name): null;
};

App.db.getSortingStatuses = function (name) {
  return name ? App.db.get('app.tables.sortingConditions', name): null;
};

App.db.getSelectedHosts = function () {
  return App.db.get('app.tables.selectedItems', 'mainHostController') || [];
};

/**
 * Return current step for specified Wizard Type
 * @param wizardType
 * @return {*}
 */
App.db.getWizardCurrentStep = function (wizardType) {
  return App.db.get(wizardType.capitalize(), 'currentStep') || 0;
};

App.db.getAllHostNames = function () {
  return App.db.get('Installer', 'hostNames');
};

App.db.getMasterToReassign = function () {
  return App.db.get('ReassignMaster', 'masterComponent');
};

App.db.getReassignTasksStatuses = function () {
  return App.db.get('ReassignMaster', 'tasksStatuses');
};

App.db.getReassignTasksRequestIds = function () {
  return App.db.get('ReassignMaster', 'tasksRequestIds');
};

App.db.getSecurityWizardStatus = function () {
  return App.db.get('AddSecurity', 'status');
};

App.db.getDisableSecurityStatus = function () {
  return App.db.get('AddSecurity', 'disableSecurityStatus');
};

App.db.getStacks = function () {
  return App.db.get('Installer', 'stacksVersions');
};

App.db.getOses = function () {
  return App.db.get('Installer', 'operatingSystems');
};

App.db.getRepos = function () {
  return App.db.get('Installer', 'repositories');
};

App.db.getLocalRepoVDFData = function () {
  return App.db.get('Installer', 'localRepoVDFData');
};

App.db.getHighAvailabilityWizardHdfsUser = function () {
  return App.db.get('HighAvailabilityWizard', 'hdfsUser');
};

App.db.getHighAvailabilityWizardTasksStatuses = function () {
  return App.db.get('HighAvailabilityWizard', 'tasksStatuses');
};

App.db.getHighAvailabilityWizardTasksRequestIds = function () {
  return App.db.get('HighAvailabilityWizard', 'tasksRequestIds');
};

App.db.getHighAvailabilityWizardFailedTask = function () {
  return App.db.get('HighAvailabilityWizard', 'failedTask');
};

App.db.getHighAvailabilityWizardHdfsClientHosts = function () {
  return App.db.get('HighAvailabilityWizard', 'hdfsClientHostNames');
};

App.db.getHighAvailabilityWizardConfigTag = function (tag) {
  return App.db.get('HighAvailabilityWizard', tag);
};

App.db.getHighAvailabilityWizardRequestIds = function () {
  return App.db.get('HighAvailabilityWizard', 'requestIds');
};

App.db.getHighAvailabilityWizardNameServiceId = function () {
  return App.db.get('HighAvailabilityWizard', 'nameServiceId');
};

App.db.getRollbackHighAvailabilityWizardTasksStatuses = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'tasksStatuses');
};

App.db.getRollbackHighAvailabilityWizardRequestIds = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'requestIds');
};

App.db.getRollBackHighAvailabilityWizardAddNNHost = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'addNNHost');
};

App.db.getRollBackHighAvailabilityWizardSNNHost = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'sNNHost');
};

App.db.getReassignMasterWizardRequestIds = function () {
  return App.db.get('ReassignMaster', 'requestIds');
};

App.db.getReassignMasterWizardComponentDir = function () {
  return App.db.get('ReassignMaster', 'componentDir');
};

App.db.getManageJournalNodeWizardConfigTag = function (tag) {
  return App.db.get('ManageJournalNodeWizard', tag);
};

App.db.getConfigs = function () {
  return App.db.get('app', 'configs');
};

App.db.getTags = function () {
  return App.db.get('app', 'tags');
};

App.db.getReassignMasterWizardReassignHosts = function () {
  return App.db.get('ReassignMaster', 'reassignHosts');
};

module.exports = App.db;
