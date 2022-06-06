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
  .factory('AddVersionModal', ['$modal', '$q', function($modal, $q) {
    var modalObject = {};

    modalObject.repoExists = function(existingRepos, repoId) {
      for(var i = existingRepos.length - 1; i >= 0; --i) {
        if (existingRepos[i].Repositories.repo_id === repoId) {
          return true;
        }
      }
      return false;
    };

    modalObject.getRepositoriesForOS = function (osList, selectedOS) {
      // Get existing list of repositories for selectedOS
      for (var i = osList.length - 1; i >= 0; --i) {
        if (osList[i].OperatingSystems.os_type === selectedOS) {
          osList[i].repositories = osList[i].repositories || [];
          return osList[i].repositories;
        }
      }
      return null;
    };

    modalObject.show = function (parentScope) {
      var deferred = $q.defer();
      var modalInstance = $modal.open({
        templateUrl: 'views/modals/AddVersionModal.html',
        controller: ['$scope', '$modalInstance', '$translate', 'Stack', 'Alert', function ($scope, $modalInstance, $translate, Stack, Alert) {
          var $t = $translate.instant;
          $scope.selectedLocalOption = {
            index: 1
          };
          $scope.option1 = {
            index: 1,
            displayName: $t('versions.uploadFile'),
            file: ''
          };
          $scope.option2 = {
            index: 2,
            displayName: $t('versions.enterURL'),
            url: "",
            placeholder: $t('versions.URLPlaceholder')
          };
          $scope.readInfoButtonDisabled = function () {
            return $scope.option1.index == $scope.selectedLocalOption.index ? !$scope.option1.file : !$scope.option2.url;
          };
          $scope.onFileSelect = function(e){
            $scope.option1.file = '';
            if (e.files && e.files.length == 1) {
              var file = e.files[0];
              var reader = new FileReader();
              reader.onload = (function () {
                return function (e) {
                  $scope.option1.file = e.target.result;
                  $scope.$apply();
                };
              })(file);
              reader.readAsText(file);
            }
            $scope.$apply();
          };
          /**
           * Load selected file to current page content
           */
          $scope.readVersionInfo = function(){
            var data = {};
            var isXMLdata = false;
            if ($scope.option2.index == $scope.selectedLocalOption.index) {
              var url = $scope.option2.url;
              data = {
                "VersionDefinition": {
                  "version_url": url
                }
              };
            } else if ($scope.option1.index == $scope.selectedLocalOption.index) {
              isXMLdata = true;
              // load from file browser
              data = $scope.option1.file;
            }
            parentScope.isXMLdata = isXMLdata;
            parentScope.data = data;

            return Stack.postVersionDefinitionFile(isXMLdata, data, true).then(function (versionInfo) {
              var repo = versionInfo.resources[0];
              var response = {
                id : repo.VersionDefinition.id,
                stackVersion : repo.VersionDefinition.stack_version,
                stackName: repo.VersionDefinition.stack_name,
                type: repo.VersionDefinition.release? repo.VersionDefinition.release.type: null,
                stackNameVersion: repo.VersionDefinition.stack_name + '-' + repo.VersionDefinition.stack_version, /// HDP-2.3
                stackNameRepositoryVersion: repo.VersionDefinition.stack_name + '-' + repo.VersionDefinition.repository_version,
                actualVersion: repo.VersionDefinition.repository_version, /// 2.3.4.0-3846
                version: repo.VersionDefinition.release ? repo.VersionDefinition.release.version: null, /// 2.3.4.0
                releaseNotes: repo.VersionDefinition.release ? repo.VersionDefinition.release.release_notes: null,
                displayName: repo.VersionDefinition.stack_name + '-' + repo.VersionDefinition.repository_version, //HDP-2.3.4.0
                editableDisplayName: repo.VersionDefinition.repository_version.substring(4),
                isNonXMLdata: !isXMLdata,
                repoVersionFullName : repo.VersionDefinition.stack_name + '-' + repo.VersionDefinition.release ? repo.VersionDefinition.release.version: repo.VersionDefinition.repository_version,
                ambari_managed_repositories: repo.operating_systems[0].OperatingSystems.ambari_managed_repositories !== false,
                osList: repo.operating_systems,
                updateObj: repo
              };
              var services = [];
              angular.forEach(repo.VersionDefinition.stack_services, function (service) {
                var servicesToExclude = ['GANGLIA', 'KERBEROS', 'MAPREDUCE2'];
                if (servicesToExclude.indexOf(service.name) === -1) {
                  services.push({
                    name: service.name,
                    version: service.versions[0],
                    displayName: service.display_name
                  });
                }
              });
              response.services = services.sort(function(a, b){return a.name.localeCompare(b.name)});
              response.osList.forEach(function (os) {
                os.repositories.forEach(function(repo) {
                  repo.Repositories.initial_base_url = repo.Repositories.base_url;
                });
              });

              angular.forEach(parentScope.stackIds, function(stack){
                if (stack.stackNameVersion == response.stackNameVersion) {
                  parentScope.setStackIdActive(stack);
                }
              });
              parentScope.allVersions.push(response);
              angular.forEach(parentScope.allVersions, function(version) {
                var isPublicVersionsExist = false;
                // If public VDF exists for a stack then default base stack version should be hidden
                if (version.stackDefault) {
                  isPublicVersionsExist = parentScope.allVersions.find(function(_version){
                    return (version.stackNameVersion === _version.stackNameVersion && !_version.stackDefault);
                  });
                }
                version.visible = (version.stackNameVersion === response.stackNameVersion) && !isPublicVersionsExist;
              });
              parentScope.activeStackVersion = response;
              parentScope.selectedPublicRepoVersion = response;
              parentScope.setVersionSelected(response);
              $modalInstance.close();
              deferred.resolve();
            }).catch(function (data) {
              Alert.error($t('versions.alerts.readVersionInfoError'), data.message);
            });
          };
          $scope.cancel = function () {
            $modalInstance.dismiss();
            deferred.reject();
          };
        }]
      });
      modalInstance.result.then(function () {
        // Gets triggered on close
      }, function () {
        // Gets triggered on dismiss
        deferred.reject();
      });
      return deferred.promise;
    };

    return modalObject;
  }]);
