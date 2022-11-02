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
.controller('StackVersionsEditCtrl', ['$scope', '$location', 'Cluster', 'Stack', '$routeParams', 'ConfirmationModal', 'Alert', '$translate', 'AddRepositoryModal', function($scope, $location, Cluster, Stack, $routeParams, ConfirmationModal, Alert, $translate, AddRepositoryModal) {
  var $t = $translate.instant;
  $scope.constants = {
    os: $t('versions.os')
  };
  $scope.editController = true;
  $scope.osList = []; // view modal for display repo urls of various OSes
  $scope.skipValidation = false;
  $scope.useRedhatSatellite = false;
  $scope.upgradeStack = {
    stack_name: '',
    stack_version: '',
    display_name: ''
  };
  $scope.defaulfOSRepos = {}; // a copy of initial loaded repo info for "changed" check later
  $scope.isGPLAccepted = false;

  $scope.isGPLRepo = function (repository) {
    return repository.Repositories.tags.indexOf('GPL') >= 0;
  };

  $scope.showRepo = function (repository) {
    return $scope.isGPLAccepted || !$scope.isGPLRepo(repository);
  };

  $scope.loadStackVersionInfo = function () {
    return Stack.getRepo($routeParams.versionId, $routeParams.stackName).then(function (response) {
      var stackVersion = response.updateObj.RepositoryVersions || response.updateObj.VersionDefinition;
      $scope.activeStackVersion = response;
      $scope.id = response.id;
      $scope.isPatch = stackVersion.type === 'PATCH';
      $scope.isMaint = stackVersion.type === 'MAINT';
      $scope.stackNameVersion = response.stackNameVersion || $t('common.NA');
      $scope.displayName = response.displayName || $t('common.NA');
      $scope.version = response.version || $t('common.NA');
      $scope.actualVersion = response.actualVersion || $t('common.NA');
      $scope.useRedhatSatellite = !response.ambari_managed_repositories;
      $scope.updateObj = response.updateObj;
      $scope.upgradeStack = {
        stack_name: response.stackName,
        stack_version: response.stackVersion,
        display_name: response.displayName
      };
      $scope.activeStackVersion.services = Stack.filterAvailableServices(response);
      response.updateObj.operating_systems.forEach(function(os) {
        $scope.defaulfOSRepos[os.OperatingSystems.os_type] = {};
        os.repositories.forEach(function(repo) {
          $scope.defaulfOSRepos[os.OperatingSystems.os_type][repo.Repositories.repo_id] = repo.Repositories.base_url;
          repo.Repositories.initial_repo_id = repo.Repositories.repo_id;
        });
      });
      $scope.repoVersionFullName = response.repoVersionFullName;
      angular.forEach(response.osList, function (os) {
        os.selected = true;
      });
      $scope.osList = response.osList;
      // load supported os type base on stack version
      $scope.afterStackVersionRead();

      // Load GPL license accepted value
      $scope.fetchGPLLicenseAccepted();

      // if user reach here from UI click, repo status should be cached
      // otherwise re-fetch repo status from cluster end point.
      $scope.repoStatus = Cluster.repoStatusCache[$scope.id];
      if (!$scope.repoStatus) {
        $scope.fetchClusters()
        .then(function () {
          return $scope.fetchRepoClusterStatus();
        })
        .then(function () {
          $scope.deleteEnabled = $scope.isDeletable();
        });
      } else {
        $scope.deleteEnabled = $scope.isDeletable();
      }
    });
  };

  /**
   * Load GPL License Accepted value
   */
  $scope.fetchGPLLicenseAccepted = function () {
    Stack.getGPLLicenseAccepted().then(function (data) {
      $scope.isGPLAccepted = data === 'true';
    })
  };

  /**
   * Load supported OS list
   */
  $scope.afterStackVersionRead = function () {
    Stack.getSupportedOSList($scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version)
      .then(function (data) {
        var operatingSystems = data.operating_systems;
        operatingSystems.map(function (os) {
          var existingOSHash = {};
          angular.forEach($scope.osList, function (os) {
            os.repositories.forEach(function(repo) {
              repo.Repositories.initial_base_url = repo.Repositories.base_url;
            });
            existingOSHash[os.OperatingSystems.os_type] = os;
          });
          // if os not in the list, mark as un-selected, add this to the osList
          if (!existingOSHash[os.OperatingSystems.os_type]) {
            os.selected = false;
            os.repositories.forEach(function(repo) {
              repo.Repositories.base_url = '';
            });
            $scope.osList.push(os);
          }
        });
      })
      .catch(function (data) {
        Alert.error($t('versions.alerts.osListError'), data.message);
      });
  };

  $scope.isDeletable = function() {
    return !($scope.repoStatus === 'CURRENT' || $scope.repoStatus === 'INSTALLED');
  };

  $scope.disableUnusedOS = function() {
    Cluster.getClusterOS().then(function(usedOS){
      angular.forEach($scope.osList, function (os) {
        if (os.OperatingSystems.os_type !== usedOS) {
          os.disabled = true;
        }
      });
    });
  };

  $scope.save = function () {
    $scope.editVersionDisabled = true;
    delete $scope.updateObj.href;
    $scope.updateObj.operating_systems = [];
    // check if there is any change in repo list
    var changed = false;
    angular.forEach($scope.osList, function (os) {
      var savedUrls = $scope.defaulfOSRepos[os.OperatingSystems.os_type];
      if (os.selected) { // currently shown?
        if (savedUrls) { // initially loaded?
          angular.forEach(os.repositories, function (repo) {
            if (repo.Repositories.base_url != savedUrls[repo.Repositories.repo_id]) {
              changed = true; // modified
            }
          });
        } else {
          changed = true; // added
        }
        os.OperatingSystems.ambari_managed_repositories = !$scope.useRedhatSatellite;
        $scope.updateObj.operating_systems.push(os);
      } else {
        if (savedUrls) {
          changed = true; // removed
        }
      }
    });
    // show confirmation when making changes to current/installed repo
    if (changed && !$scope.deleteEnabled) {
      ConfirmationModal.show(
          $t('versions.changeBaseURLConfirmation.title'),
          $t('versions.changeBaseURLConfirmation.message'),
          $t('common.controls.confirmChange')
      ).then(function() {
        $scope.updateRepoVersions();
      });
    } else {
      $scope.updateRepoVersions();
    }
  };

  $scope.updateRepoVersions = function () {
    var skip = $scope.skipValidation || $scope.useRedhatSatellite;
    // Filter out repositories that are not shown in the UI
    var osList = Object.assign([], $scope.osList).map(function(os) {
      return Object.assign({}, os, {repositories: os.repositories.filter(function(repo) { return $scope.showRepo(repo); })});
    });
    return Stack.validateBaseUrls(skip, osList, $scope.upgradeStack).then(function (invalidUrls) {
      if (invalidUrls.length === 0) {
        Stack.updateRepo($scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id, $scope.updateObj).then(function () {
          Alert.success($t('versions.alerts.versionEdited', {
            stackName: $scope.upgradeStack.stack_name,
            versionName: $scope.actualVersion,
            displayName: $scope.repoVersionFullName
          }));
          $location.path('/stackVersions');
        }).catch(function (data) {
          Alert.error($t('versions.alerts.versionUpdateError'), data.message);
        });
      } else {
        Stack.highlightInvalidUrls(invalidUrls);
      }
    });
  };

  $scope.fetchRepoClusterStatus = function () {
    var clusterName = ($scope.clusters && $scope.clusters.length > 0)
      ? $scope.clusters[0].Clusters.cluster_name : null; // only support one cluster at the moment
    if (!clusterName) {
      return null;
    }
    return Cluster.getRepoVersionStatus(clusterName, $scope.id).then(function (response) {
      $scope.repoStatus = response.status;
    });
  };

  $scope.fetchClusters = function () {
    return Cluster.getAllClusters().then(function (clusters) {
      $scope.clusters = clusters;
    });
  };

  $scope.delete = function () {
    ConfirmationModal.show(
      $t('versions.deregister'),
      {
        "url": 'views/modals/BodyForDeregisterVersion.html',
        "scope": {"displayName": $scope.repoVersionFullName }
      }
    ).then(function() {
        Stack.deleteRepo($scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id).then( function () {
          $location.path('/stackVersions');
        }).catch(function (data) {
            Alert.error($t('versions.alerts.versionDeleteError'), data.message);
          });
      });
  };

  /**
   * On click handler for removing OS
   */
  $scope.removeOS = function() {
    if ($scope.useRedhatSatellite) {
      return;
    }
    this.os.selected = false;
    if (this.os.repositories) {
      this.os.repositories.forEach(function(repo) {
        repo.hasError = false;
      });
    }
  };
  /**
   * On click handler for adding new OS
   */
  $scope.addOS = function() {
    this.os.selected = true;
    if (this.os.repositories) {
      this.os.repositories.forEach(function(repo) {
        repo.hasError = false;
      });
    }
  };

  $scope.isAddOsButtonDisabled = function () {
    var selectedCnt = 0;
    angular.forEach($scope.osList, function (os) {
      if (os.selected) {
        selectedCnt ++;
      }
    });
    return $scope.osList.length == selectedCnt || $scope.useRedhatSatellite;
  };

  $scope.hasNotDeletedRepo = function () {
    //check if any repository has been selected for deleting
    //if yes, drop down should be displayed
    var repoNotDeleted = true;
    for(var i=0;i<$scope.osList.length;i++) {
      if (!$scope.osList[i].selected) {
        repoNotDeleted=false;
        break; 
      }
    }
    return repoNotDeleted;
  };

  /**
   * On click handler for adding a new repository
   */
  $scope.addRepository = function() {
    AddRepositoryModal.show($scope.osList, $scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id);
  };

  $scope.isSaveButtonDisabled = function() {
    var enabled = false;
    $scope.osList.forEach(function(os) {
      if (os.selected) {
        enabled = true
      }
    });
    return !enabled;
  };

  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/stackVersions');
  };

  $scope.undoChange = function(repo) {
    repo.Repositories.base_url = repo.Repositories.initial_base_url;
  };

  $scope.clearErrors = function() {
    if ($scope.osList) {
      $scope.osList.forEach(function(os) {
        if (os.repositories) {
          os.repositories.forEach(function(repo) {
            repo.hasError = false;
          })
        }
      });
    }
  };

  $scope.useRedHatCheckbox = function() {
    if ($scope.useRedhatSatellite) {
      ConfirmationModal.show(
        $t('versions.useRedhatSatellite.title'),
        {
          "url": 'views/modals/BodyForUseRedhatSatellite.html'
        }
      ).catch(function () {
        $scope.useRedhatSatellite = !$scope.useRedhatSatellite;
      });
    } else {
      if ($scope.osList) {
        $scope.osList.forEach(function(os) {
          if (os.repositories) {
            os.repositories.forEach(function(repo) {
              repo.isEditing = false;
            })
          }
        });
      }
    }
  };

  $scope.clearError = function () {
    this.repository.hasError = false;
  };

  $scope.hasValidationErrors = function () {
    var hasErrors = false;
    if ($scope.osList) {
      $scope.osList.forEach(function (os) {
        if (os.repositories) {
          os.repositories.forEach(function (repo) {
            if (repo.hasError) {
              hasErrors = true;
            }
          })
        }
      });
    }
    return hasErrors;
  };

  $scope.loadStackVersionInfo();
}]);
