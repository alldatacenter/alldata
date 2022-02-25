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
  .factory('AddRepositoryModal', ['$modal', '$q', function($modal, $q) {
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

    modalObject.show = function (osList, stackName, stackVersion, repositoryVersionId) {
      var deferred = $q.defer();
      var modalInstance = $modal.open({
        templateUrl: 'views/modals/AddRepositoryModal.html',
        controller: ['$scope', '$modalInstance', function ($scope, $modalInstance) {
          $scope.osTypes = osList.map(function (os) {
            return os.OperatingSystems.os_type;
          });
          $scope.repo = {
            selectedOS: $scope.osTypes[0]
          };

          $scope.add = function (newRepo) {
            var repositoriesForOS = modalObject.getRepositoriesForOS(osList, newRepo.selectedOS);

            // If repo with the same id exists for the selectedOS, show an alert and do not take any action
            $scope.showAlert = modalObject.repoExists(repositoriesForOS, newRepo.id);
            if ($scope.showAlert) {
              return;
            }

            // If no duplicate repository is found on the selectedOS, add the new repository
            repositoriesForOS.push({
              Repositories: {
                repo_id: newRepo.id,
                repo_name: newRepo.name,
                os_type: newRepo.selectedOS,
                base_url: newRepo.baseUrl,
                stack_name: stackName,
                stack_version: stackVersion,
                repository_version_id: repositoryVersionId
              }
            });

            $modalInstance.close();
            deferred.resolve();
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