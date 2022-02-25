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
.controller('ViewUrlCtrl',['$scope', 'View', 'Alert', 'Cluster', '$routeParams', '$location', 'UnsavedDialog', '$translate', 'Settings', function($scope, View, Alert, Cluster, $routeParams, $location, UnsavedDialog, $translate, Settings) {
  var $t = $translate.instant;
  $scope.form = {};
  $scope.constants = {
    props: $t('views.properties')
  };
  var targetUrl = '/viewUrls';

  $scope.url={};
  $scope.formHolder = {};
  $scope.stepOneNotCompleted = true;
  $scope.stepTwoNotCompleted = true;

  View.getAllVisibleInstance().then(function(views) {
    var names = [];
    var instances=[];
    views.map(function(view){
      var nameVersion = view.view_name+" {"+view.version+"}";
        names.push(nameVersion);
      instances.push({nameV:nameVersion,instance:view.instance_name,cname:view.view_name,version:view.version});
    });

    var output = [],
        keys = [];

    angular.forEach(names, function(item) {
      var key = item;
      if(keys.indexOf(key) === -1) {
        keys.push(key);
        output.push(item);
      }
    });

    $scope.viewsVersions =  output;
    $scope.viewInstances =  instances;

    if($routeParams.viewName && $routeParams.viewVersion && $routeParams.viewInstanceName){
      var selectedView = $routeParams.viewName+" {"+$routeParams.viewVersion+"}";
      $scope.url.selectedView = selectedView;
      $scope.url.selectedInstance = instances.find(function(inst){
         return inst.nameV === selectedView && inst.instance === $routeParams.viewInstanceName && inst.version === $routeParams.viewVersion && inst.cname === $routeParams.viewName;
      });
      $scope.stepOneNotCompleted = false;
      $scope.stepTwoNotCompleted = false;
    }

  }).catch(function(data) {
    Alert.error($t('views.alerts.cannotLoadViews'), data.data.message);
  });

  $scope.filterByName = function(nameV){
    return function (item) {
      if (item.nameV === nameV)
      {
        return true;
      }
      return false;
    };
  };

  $scope.chomp = function(viewNameVersion){
    if(viewNameVersion) {
      return viewNameVersion.substr(0, viewNameVersion.indexOf("{")).trim();
    }
  };


  $scope.doStepOne = function () {
    $scope.stepOneNotCompleted = false;
  };


  $scope.doStepTwo = function () {
    $scope.stepTwoNotCompleted = false;

  };

  $scope.cancelForm = function () {
    $scope.stepOneNotCompleted = true;
    $scope.stepTwoNotCompleted = true;
  };

  $scope.saveUrl = function() {
    $scope.formHolder.form.submitted = true;

    if($scope.formHolder.form.$valid){

      var payload = {ViewUrlInfo:{
        url_name:$scope.url.urlName,
        url_suffix:$scope.url.suffix,
        view_instance_version:$scope.url.selectedInstance.version,
        view_instance_name:$scope.url.selectedInstance.instance,
        view_instance_common_name:$scope.url.selectedInstance.cname
      }};

      View.updateShortUrl(payload).then(function(urlStatus) {
        Alert.success($t('urls.urlCreated', {
          siteRoot: Settings.siteRoot,
          viewName:$scope.url.selectedInstance.cname ,
          shortUrl:$scope.url.suffix,
          urlName:$scope.url.urlName
        }));
        $scope.formHolder.form.$setPristine();
        $scope.url={};
        $scope.formHolder = {};
        $scope.stepOneNotCompleted = true;
        $scope.stepTwoNotCompleted = true;
        $location.path(targetUrl);
      }).catch(function(data) {
        Alert.error($t('views.alerts.cannotLoadViewUrls'), data.message);
      });

    }
  };

}]);
