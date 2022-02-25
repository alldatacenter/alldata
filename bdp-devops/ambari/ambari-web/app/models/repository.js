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

App.Repository = DS.Model.extend({
  id:  DS.attr('string'), // This is ${osType}-${repoId}.
  repoId: DS.attr('string'),
  originalRepoId: DS.attr('string'),
  osType: DS.attr('string'),
  baseUrl: DS.attr('string'),
  baseUrlInit: DS.attr('string'),
  defaultBaseUrl: DS.attr('string'),
  latestBaseUrl: DS.attr('string'),
  repoName: DS.attr('string'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  operatingSystem: DS.belongsTo('App.OperatingSystem'),
  components: DS.attr('string'),
  distribution: DS.attr('string'),
  tags: DS.attr('array'),
  applicable_services: DS.attr('array'),

  validation: DS.attr('string', {defaultValue: ''}),
  validationClassName: Em.computed.getByKey('validationClassNameMap', 'validation', ''),
  validationClassNameMap: {
    INVALID: 'glyphicon glyphicon-exclamation-sign',
    OK: 'glyphicon glyphicon-ok',
    INPROGRESS: 'glyphicon glyphicon-repeat'
  },
  errorContent: DS.attr('string', {defaultValue: ''}),
  errorTitle: DS.attr('string', {defaultValue: ''}),

  isSelected: Em.computed.alias('operatingSystem.isSelected'),

  invalidFormatError: function() {
    return !validator.isValidBaseUrl(this.get('baseUrl'));
  }.property('baseUrl'),

  isEmpty: function() {
    return this.get('showRepo') && this.get('baseUrl') === '';
  }.property('baseUrl'),

  invalidError: function() {
    return this.get('validation') === 'INVALID';
  }.property('validation'),

  /**
   * @type {boolean}
   */
  isUtils: function () {
    return this.get('repoName').contains('UTILS');
  }.property('repoName'),

  /**
   * @type {boolean}
   */
  isGPL: function () {
    var tags = this.get('tags');
    return tags && tags.contains('GPL');
  }.property('tags'),

  /**
   * Determines whether a repo needs to be displayed in the UI or not
   * @type {boolean}
   */
  showRepo: function () {
    const isGPLAccepted = App.router.get('clusterController.ambariProperties')['gpl.license.accepted'] === 'true';
    return isGPLAccepted || !this.get('isGPL');
  }.property('isGPL'),

  undo: Em.computed.notEqualProperties('baseUrl', 'baseUrlInit'),

  notEmpty: Em.computed.notEqual('baseUrl', ''),

  clearAll: Em.computed.alias('baseUrl'),

  /**
   * @type {string}
   */
  placeholder: Em.computed.ifThenElse('isUtils', '', Em.I18n.t('installer.step1.advancedRepo.localRepo.placeholder')),

});


App.Repository.FIXTURES = [];
