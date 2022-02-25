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
  .controller('LoginMessageMainCtrl',['$scope', 'Alert', '$timeout', '$location', '$http', '$translate', 'UnsavedDialog', function($scope, Alert, $timeout, $location, $http, $translate, UnsavedDialog) {
    var $t = $translate.instant,
      targetUrl = '/loginActivities';

    $scope.getMOTD = function() {
      $http.get('/api/v1/settings/motd').then(function (res) {
        $scope.motdExists = true;
        var
          response = JSON.parse(res.data.Settings.content.replace(/\n/g, "\\n")),
          lt = /&lt;/g,
          gt = /&gt;/g,
          ap = /&#39;/g,
          ic = /&#34;/g;

        $scope.text = response.text ? response.text.toString().replace(lt, "<").replace(gt, ">").replace(ap, "'").replace(ic, '"') : "";
        $scope.buttonText = response.button ? response.button.toString().replace(lt, "<").replace(gt, ">").replace(ap, "'").replace(ic, '"') : "OK";
        $scope.status = response.status && response.status == "true" ? true : false;
      }, function(response) {
        $scope.status = false;
        $scope.motdExists = false;
        $scope.text = "";
        $scope.buttonText = $t('common.controls.ok');
      });
      $scope.submitDisabled = true;
    };

    $scope.inputChangeEvent = function(){
      $scope.submitDisabled = false;
    };
    $scope.changeStatus = function(){
      $scope.submitDisabled = false;
    };

    $scope.cancel = function() {
      $scope.getMOTD();
    };

    $scope.$watch(function(scope) {
      return scope.submitDisabled;
    }, function(submitDisabled) {
      $scope.form.$dirty = !submitDisabled
    });

    $scope.saveLoginMsg = function(targetUrl) {
      var
        method = $scope.motdExists ? 'PUT' : 'POST',
        text = "",
        buttonText = "",
        lt = /</g,
        gt = />/g,
        ap = /'/g,
        ic = /"/g;

      text = $scope.text.toString().replace(lt, "&lt;").replace(gt, "&gt;").replace(ap, "&#39;").replace(ic, "&#34;");
      buttonText = $scope.buttonText ? $scope.buttonText.toString().replace(lt, "&lt;").replace(gt, "&gt;").replace(ap, "&#39;").replace(ic, "&#34;") : $scope.buttonText;

      var data = {
        'Settings' : {
          'content' : '{"text":"' + text + '", "button":"' + buttonText + '", "status":"' + $scope.status + '"}',
          'name' : 'motd',
          'setting_type' : 'ambari-server'
        }
      };
      $scope.form.submitted = true;
      if ($scope.form.$valid){
        $scope.submitDisabled = true;
        return $http({
          method: method,
          url: '/api/v1/settings/' + ($scope.motdExists ? 'motd' : ''),
          data: data
        }).then(function successCallback() {
          $scope.motdExists = true;
          targetUrl ? $location.path(targetUrl) : "";
        }, function errorCallback(data) {
          $scope.submitDisabled = false;
          Alert.error($t('common.loginActivities.saveError'), data.data.message);
        });
      }
    };

    $scope.$on('$locationChangeStart', function(event, __targetUrl) {
      if( $scope.form.$dirty ){
        UnsavedDialog().then(function(action) {
          targetUrl = __targetUrl.split('#').pop();
          switch(action){
            case 'save':
              $scope.saveLoginMsg(targetUrl);
              break;
            case 'discard':
              $scope.form.$setPristine();
              $location.path(targetUrl);
              break;
            case 'cancel':
              targetUrl = '/loginActivities';
              break;
          }
        });
        event.preventDefault();
      }
    });

  }]);
