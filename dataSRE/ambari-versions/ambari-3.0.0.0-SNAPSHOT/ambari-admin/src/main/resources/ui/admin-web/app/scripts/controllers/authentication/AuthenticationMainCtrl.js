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
'use strict';

angular.module('ambariAdminConsole')
  .controller('AuthenticationMainCtrl', ['$scope', '$translate', 'Alert', 'Settings', function ($scope, $translate, $Alert, Settings) {
    $scope.t = $translate.instant;
    $scope.settings = Settings;

    $scope.isLDAPEnabled = false;
    $scope.connectivity = {
      trustStore: 'default',
      trustStoreOptions: ['default', 'custom'],
      trustStoreType: 'jks',
      trustStoreTypeOptions: ['jks', 'jceks', 'pkcs12']
    };
    $scope.attributes = {
      detection: 'auto'
    };

    $scope.isConnectivityFormInvalid = true;
    $scope.isAutoDetectFormInvalid = true;
    $scope.isAttributesFormInvalid = true;
    $scope.isTestAttributesFormInvalid = false;

    $scope.isRequestRunning = false;

    $scope.isConnectionTestRunning = false;
    $scope.isConnectionTestComplete = false;
    $scope.hasConnectionTestPassed = false;

    $scope.isAttributeDetectionRunning = false;
    $scope.isAttributeDetectionComplete = false;
    $scope.isAttributeDetectionSuccessful = false;

    $scope.isTestAttributesRunning = false;
    $scope.isTestAttributesComplete = false;
    $scope.isTestAttributesSuccessful = false;

    $scope.isSaving = false;
    $scope.isSavingComplete = false;
    $scope.isSavingSuccessful = false;

    $scope.isTestAttributesFormShown = false;

    $scope.toggleAuthentication = function () {
      $scope.isConnectionTestRunning = false;
      $scope.isConnectionTestComplete = false;
      $scope.hasConnectionTestPassed = false;
    };

    $scope.testConnection = function () {
      $scope.isConnectionTestRunning = true;
      $scope.isConnectionTestComplete = false;
      $scope.isAttributeDetectionRunning = false;
      $scope.isAttributeDetectionComplete = false;
      $scope.isAttributeDetectionSuccessful = false;

      // TODO replace mock with test connection request when API is available
      setTimeout(function (prevValue) {
        $scope.isConnectionTestRunning = false;
        $scope.isConnectionTestComplete = true;
        $scope.hasConnectionTestPassed = !prevValue;
      }, 1000, $scope.hasConnectionTestPassed);
      $scope.hasConnectionTestPassed = false;
    };

    $scope.detectAttributes = function () {
      $scope.isAttributeDetectionRunning = true;
      $scope.isAttributeDetectionComplete = false;

      // TODO replace mock with attributes detection request when API is available
      setTimeout(function (prevValue) {
        $scope.isAttributeDetectionRunning = false;
        $scope.isAttributeDetectionComplete = true;
        $scope.isAttributeDetectionSuccessful = !prevValue;
        if ($scope.isAttributeDetectionSuccessful) {
          var form = $scope.attributes;
          form.userObjClass = 'person';
          form.userNameAttr = 'sAMAccountName';
          form.groupObjClass = 'group';
          form.groupNameAttr = 'cn';
          form.groupMemberAttr = 'member';
          form.distinguishedNameAttr = 'distinguishedName';
        }
      }, 1000, $scope.isAttributeDetectionSuccessful);

      $scope.isAttributeDetectionSuccessful = false;
    };

    $scope.showTestAttributesForm = function () {
      $scope.isTestAttributesFormShown = true;
    };

    $scope.testAttributes = function () {
      $scope.isTestAttributesRunning = true;
      $scope.isTestAttributesComplete = false;

      // TODO replace mock with test attributes request when API is available
      setTimeout(function (prevValue) {
        $scope.isTestAttributesRunning = false;
        $scope.isTestAttributesComplete = true;
        $scope.isTestAttributesSuccessful = !prevValue;
        if ($scope.isTestAttributesSuccessful) {
          $scope.attributes.availableGroups = ['HadoopOps', 'HadoopOpsDFW', 'AmbariAdmins', 'ExchangeAdmins', 'AmbariUsers', 'ExchangeUsers'];
        }
      }, 1000, $scope.isTestAttributesSuccessful);
      $scope.isTestAttributesSuccessful = false;
    };

    $scope.save = function () {
      $scope.isSaving = true;
      $scope.isSavingComplete = false;
      // TODO replace mock with save request when API is available
      setTimeout(function (prevValue) {
        $scope.isSaving = false;
        $scope.isSavingComplete = true;
        $scope.isSavingSuccessful = !prevValue;
        if ($scope.isSavingSuccessful) {
          $Alert.success('Settings saved');
        } else {
          $Alert.error('Saving failed', '500 Error');
        }
      }, 1000, $scope.isSavingSuccessful);
      $scope.isSavingSuccessful = false;
    };

    $scope.$watch('connectivity', function (form, oldForm, scope) {
      scope.isConnectivityFormInvalid = !(form.host && form.port
        && (form.trustStore === 'default' || form.trustStorePath && form.trustStorePassword)
        && form.dn && form.bindPassword);
    }, true);

    $scope.$watch('attributes', function (form, oldForm, scope) {
      scope.isAutoDetectFormInvalid = !(form.userSearch && form.groupSearch);
      scope.isAttributesFormInvalid = !(form.userObjClass && form.userNameAttr && form.groupObjClass
        && form.groupNameAttr && form.groupMemberAttr && form.distinguishedNameAttr
        && (form.detection === 'auto' || form.userSearchManual && form.groupSearchManual));
      scope.isTestAttributesFormInvalid = !(form.username && form.password);
    }, true);

    $scope.$watch('attributes.detection', function (newValue, oldValue, scope) {
      scope.isTestAttributesFormShown = false;
      scope.isAttributeDetectionComplete = false;
      scope.isAttributeDetectionSuccessful = false;
    });

    $scope.$watch(function (scope) {
      return scope.isConnectionTestRunning || scope.isAttributeDetectionRunning || scope.isTestAttributesRunning || scope.isSaving;
    }, function (newValue, oldValue, scope) {
      scope.isRequestRunning = newValue;
    });
}]);
