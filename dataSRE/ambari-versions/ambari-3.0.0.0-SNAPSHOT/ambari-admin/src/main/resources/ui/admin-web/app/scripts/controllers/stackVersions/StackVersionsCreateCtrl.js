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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', 'Utility', '$routeParams', '$location', '$timeout' ,'Alert', '$translate', 'Cluster', 'AddRepositoryModal', 'AddVersionModal', 'ConfirmationModal',
    function($scope, Stack, Utility, $routeParams, $location, $timeout, Alert, $translate, Cluster, AddRepositoryModal, AddVersionModal, ConfirmationModal) {
  var $t = $translate.instant;
  $scope.constants = {
    os: $t('versions.os')
  };
  $scope.createController = true;
  $scope.osList = [];
  $scope.stackIds = [];
  $scope.allVersions = [];
  $scope.networkLost = false;
  $scope.stackRepoUpdateLinkExists = true;
  $scope.skipValidation = false;
  $scope.useRedhatSatellite = false;

  $scope.clusterName = $routeParams.clusterName;
  $scope.upgradeStack = {
    stack_name: '',
    stack_version: '',
    display_name: ''
  };

  $scope.isGPLAccepted = false;

  $scope.isGPLRepo = function (repository) {
    return  repository.Repositories.tags && repository.Repositories.tags.indexOf('GPL') >= 0;
  };

  $scope.showRepo = function (repository) {
    return $scope.isGPLAccepted || !$scope.isGPLRepo(repository);
  };

  $scope.publicOption = {
    index: 1,
    hasError: false
  };
  $scope.localOption = {
    index: 2,
    hasError: false
  };
  $scope.option1 = {
    index: 3,
    displayName: $t('versions.uploadFile'),
    file: '',
    hasError: false
  };
  $scope.option2 = {
    index: 4,
    displayName: $t('versions.enterURL'),
    url: $t('versions.defaultURL'),
    hasError: false
  };
  $scope.selectedOption = {
    index: 1
  };
  $scope.selectedLocalOption = {
    index: 3
  };

  /**
   * User can select ONLY one option to upload version definition file
   */
  $scope.toggleOptionSelect = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.isPublicRepoSelected = function () {
    if ($scope.selectedOption.index == $scope.publicOption.index) return true;
  };
  $scope.togglePublicLocalOptionSelect = function () {
    if ($scope.selectedOption.index == $scope.publicOption.index) {
      $scope.setInitialPublicRepoVersions();
    } else {
      $scope.clearRepoVersions();
    }
    $scope.validateRepoUrl();
  };
  $scope.setInitialPublicRepoVersions = function () {
    angular.forEach($scope.osList, function (os) {
      os.repositories.forEach(function(repo) {
        repo.Repositories.base_url = repo.Repositories.initial_base_url;
      });
    });
  };
  $scope.clearRepoVersions = function () {
    angular.forEach($scope.osList, function (os) {
      os.repositories.forEach(function(repo) {
        repo.Repositories.base_url = '';
      });
    });
  };
  $scope.clearOptionsError = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.readInfoButtonDisabled = function () {
    if ($scope.selectedOption.index == $scope.publicOption.index) return true;
    return $scope.option1.index == $scope.selectedLocalOption.index ? !$scope.option1.file : !$scope.option2.url;
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

  $scope.allInfoCategoriesBlank = function () {
    return !$scope.upgradeStack.stack_name;
  };

  $scope.onFileSelect = function(e){
    if (e.files && e.files.length == 1) {
      var file = e.files[0];
      var reader = new FileReader();
      reader.onload = (function () {
        return function (e) {
          $scope.option1.file = e.target.result;
        };
      })(file);
      reader.readAsText(file);
    } else {
      $scope.option1.file = '';
    }
  };

  /**
   * On click handler for adding a new version
   */
  $scope.addVersion = function() {
    AddVersionModal.show($scope);
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

    return Stack.postVersionDefinitionFile(isXMLdata, data).then(function (versionInfo) {
      if (versionInfo.id && versionInfo.stackName && versionInfo.stackVersion) {
        Stack.getRepo(versionInfo.id, versionInfo.stackName, versionInfo.stackVersion)
          .then(function (response) {
            $scope.setVersionSelected(response);
        });
      }
    })
    .catch(function (data) {
      Alert.error($t('versions.alerts.readVersionInfoError'), data.message);
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
        var existingOSHash = {};
        angular.forEach($scope.osList, function (os) {
          if (angular.isUndefined(os.selected)) {
            os.selected = true;
          }
          existingOSHash[os.OperatingSystems.os_type] = os;

        });
        var operatingSystems = data.operating_systems;
        angular.forEach(operatingSystems, function (stackOs) {
          // if os not in the list, mark as un-selected, add this to the osList
          if (!existingOSHash[stackOs.OperatingSystems.os_type]) {
            stackOs.selected = false;
            stackOs.repositories.forEach(function(repo) {
              repo.Repositories.initial_base_url = repo.Repositories.default_base_url;
              repo.Repositories.initial_repo_id = repo.Repositories.repo_id;
            });
            $scope.osList.push(stackOs);
          }
        });
        if ($scope.selectedOption.index == $scope.localOption.index) {
          $scope.clearRepoVersions();
          $scope.validateRepoUrl();
        }
      })
      .catch(function (data) {
        Alert.error($t('versions.alerts.osListError'), data.message);
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
  $scope.addOS = function($event) {
    var dropdownEl = $event.target.parentElement.parentElement;
    // close the dopdown when an OS is added.
    $timeout(function () {
      dropdownEl.click();
    });

    this.os.selected = true;
    if (this.os.repositories) {
      this.os.repositories.forEach(function(repo) {
        repo.hasError = false;
      });
    }
  };

  /**
   * On click handler for adding a new repository
   */
  $scope.addRepository = function() {
    AddRepositoryModal.show($scope.osList, $scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id);
  };

  $scope.validBaseUrlsExist = function () {
    var validBaseUrlsExist = true;
    if ($scope.osList) {
      $scope.osList.forEach(function(os) {
        if (os.repositories && os.selected) {
          os.repositories.forEach(function(repo) {
            if (repo.invalidBaseUrl && $scope.showRepo(repo)) {
              validBaseUrlsExist = false;
            }
          })
        }
      });
    }
    return validBaseUrlsExist;
  };


  $scope.isSaveButtonDisabled = function() {
    var enabled = false;
    $scope.osList.forEach(function(os) {
      if (os.selected) {
        enabled = true
      }
    });
    return !($scope.useRedhatSatellite || (enabled && $scope.validBaseUrlsExist()));
  };

  $scope.defaulfOSRepos = {};

  $scope.save = function () {
    $scope.editVersionDisabled = true;
    delete $scope.updateObj.href;
    $scope.updateObj.operating_systems = [];
    angular.forEach($scope.osList, function (os) {
      os.OperatingSystems.ambari_managed_repositories = !$scope.useRedhatSatellite;
      if (os.selected) {
        $scope.updateObj.operating_systems.push(os);
      }
    });

    var skip = $scope.skipValidation || $scope.useRedhatSatellite;
    return Stack.validateBaseUrls(skip, $scope.osList, $scope.upgradeStack).then(function (invalidUrls) {
      if (invalidUrls.length === 0) {
        if ($scope.isPublicVersion) {
          var data = {
            "VersionDefinition": {
              "available": $scope.id
            }
          };
          var isXMLdata = false;
        } else {
          var data = $scope.data;
          var isXMLdata = $scope.isXMLdata;
        }

        if (!isXMLdata) {
          data.VersionDefinition.display_name = $scope.activeStackVersion.displayName;
        }

        var repoUpdate = {
          operating_systems: $scope.updateObj.operating_systems
        };
        Stack.postVersionDefinitionFile(isXMLdata, data, false).then(function (response) {
          var versionInfo = response.resources[0].VersionDefinition;
          if (versionInfo.id && versionInfo.stack_name && versionInfo.stack_version) {
            Stack.updateRepo(versionInfo.stack_name, versionInfo.stack_version, versionInfo.id, repoUpdate).then(function () {
              Alert.success($t('versions.alerts.versionCreated', {
                stackName: $scope.upgradeStack.stack_name,
                versionName: $scope.actualVersion
              }));
              $location.path('/stackVersions');
            }).catch(function (data) {
              Stack.deleteRepo(versionInfo.stack_name, versionInfo.stack_version, versionInfo.id);
              ConfirmationModal.show(
                $t('versions.register.error.header'),
                $t('versions.register.error.body'),
                null,
                null,
                {hideCancelButton: true}
              )
            });
          }
        })
        .catch(function (data) {
          Alert.error($t('versions.alerts.readVersionInfoError'), data.message);
        });
      } else {
        Stack.highlightInvalidUrls(invalidUrls);
      }
    });
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

  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/stackVersions');
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

  $scope.showPublicRepoDisabledDialog = function() {
    ConfirmationModal.show(
      $t('versions.networkIssues.publicDisabledHeader'),
      {
        "url": 'views/modals/publicRepoDisabled.html'
      },
      $t('common.controls.ok'),
      $t('common.controls.cancel'),
      {hideCancelButton: true}
    )
  };

  $scope.onRepoUrlChange = function (repository) {
    $scope.clearError(repository);
    $scope.setInvalidUrlError(repository);
  };

  $scope.undoChange = function(repo) {
    if ($scope.selectedOption.index == 1) {
      repo.Repositories.base_url = repo.Repositories.initial_base_url;
    } else {
      repo.Repositories.base_url = '';
    }
  };

  $scope.clearError = function(repository) {
    repository.hasError = false;
  };

  $scope.setInvalidUrlError = function (repository) {
    repository.invalidBaseUrl =  !$scope.isValidRepoBaseUrl(repository.Repositories.base_url);
  };
  /**
   * Validate base URL
   * @param {string} value
   * @returns {boolean}
   */
  $scope.isValidRepoBaseUrl = function (value) {
    var remotePattern = /^(?:(?:https?|ftp):\/{2})(?:\S+(?::\S*)?@)?(?:(?:(?:[\w\-.]))*)(?::[0-9]+)?(?:\/\S*)?$/,
      localPattern = /^file:\/{2,3}([a-zA-Z][:|]\/){0,1}[\w~!*'();@&=\/\\\-+$,?%#.\[\]]+$/;
    return remotePattern.test(value) || localPattern.test(value);
  };

  $scope.hasValidationErrors = function() {
    var hasErrors = false;
    if ($scope.osList) {
      $scope.osList.forEach(function(os) {
        if (os.repositories) {
          os.repositories.forEach(function(repo) {
            if (repo.hasError) {
              hasErrors = true;
            }
          })
        }
      });
    }
    return hasErrors;
  };


  $scope.setVersionSelected = function (version) {
    var response = version;
    var stackVersion = response.updateObj.RepositoryVersions || response.updateObj.VersionDefinition;
    $scope.id = response.id;
    $scope.isPatch = stackVersion.type === 'PATCH';
    $scope.isMaint = stackVersion.type === 'MAINT';
    $scope.stackNameVersion = response.stackNameVersion || $t('common.NA');
    $scope.displayName = response.displayName || $t('common.NA');
    $scope.actualVersion = response.repositoryVersion || response.actualVersion || $t('common.NA');
    $scope.isPublicVersion = response.showAvailable == true;
    $scope.updateObj = response.updateObj;
    $scope.upgradeStack = {
      stack_name: response.stackName,
      stack_version: response.stackVersion,
      display_name: response.displayName || $t('common.NA')
    };
    $scope.activeStackVersion.services = Stack.filterAvailableServices(response);
    $scope.repoVersionFullName = response.repoVersionFullName;
    $scope.osList = response.osList;

    // load supported os type base on stack version
    $scope.afterStackVersionRead();

    // Load GPL license accepted value
    $scope.fetchGPLLicenseAccepted();
  };

  $scope.selectRepoInList = function() {
    $scope.selectedPublicRepoVersion = this.version;
    $scope.setVersionSelected(this.version);
  };

  $scope.onStackIdChange = function () {
    $scope.setStackIdActive(this.stack);
    $scope.setVisibleStackVersions($scope.allVersions);
    $scope.setVersionSelected($scope.activeStackVersion);
  };

  $scope.setStackIdActive =  function (stack) {
    angular.forEach($scope.stackIds, function(_stack){
      _stack.isSelected = false;
    });
    stack.isSelected = true;
  };

  $scope.setStackIds = function(stacks) {
    var stackIds = [];
    // sort stacks as per per {stack_name}-{stack_version}
    stacks.sort(function(a,b){
      if (a.stackName === b.stackName) {
        var aStackVersion = parseFloat(a.stackVersion);
        var bStackVersion = parseFloat(b.stackVersion);
        if (aStackVersion === bStackVersion) {
          // sort numerically as per per {repository_version}
          return Utility.compareVersions(a.repositoryVersion, b.repositoryVersion);
        } else {
          //sort numerically as per per {stack_version}
          return aStackVersion > bStackVersion;
        }
      } else {
        //sort lexicographically as per per {stack_name}
        return  (a.stackName > b.stackName);
      }
    }).reverse();
    angular.forEach(stacks, function (stack) {
      stackIds.push(stack.stackNameVersion);
    });
    $scope.stackIds = stackIds.filter(function(item, index, self){
      return self.indexOf(item) === index;
    }).map(function(item){
      return {
        stackNameVersion: item,
        isSelected: false
      };
    });
    $scope.stackIds[0].isSelected = true;
  };

  $scope.setActiveVersion = function () {
    $scope.activeStackVersion = this.version;
    $scope.setVersionSelected($scope.activeStackVersion);
  };

  $scope.setVisibleStackVersions = function (versions) {
    var activeStackId = $scope.stackIds.find(function(item){
      return item.isSelected === true;
    });
    angular.forEach(versions, function (item, index) {
      item.visible = (item.stackNameVersion === activeStackId.stackNameVersion);
    });
    $scope.activeStackVersion = versions.filter(function(item){
      return item.visible;
    })[0];
  };

  /**
   * Return true if at least one stacks have the repo URL link in the repoinfo.xml
   * @return boolean
   * */
  $scope.setStackRepoUpdateLinkExists = function (versions) {
    var stackRepoUpdateLinkExists = versions.find(function(_version){
      return _version.stackRepoUpdateLinkExists;
    });

    //Found at least one version with the stack repo update link
    if (stackRepoUpdateLinkExists){
      $scope.stackRepoUpdateLinkExists = true;
    } else {
      $scope.stackRepoUpdateLinkExists = false;
    }
  };

  $scope.setNetworkIssues = function (versions) {
   $scope.networkLost = !versions.find(function(_version){
     return !_version.stackDefault;
   });
    if ($scope.networkLost) {
      $scope.selectedOption.index = 2;
      $scope.clearRepoVersions();
    }
  };

  $scope.validateRepoUrl = function () {
    angular.forEach($scope.osList,function(os){
      if (os.repositories) {
        os.repositories.forEach(function(repo) {
          $scope.onRepoUrlChange(repo);
        });
      }
    });
  };

  $scope.updateCurrentVersionInput = function () {
    $scope.activeStackVersion.displayName = $scope.activeStackVersion.stackNameVersion + "." + angular.element('[name="version"]')[0].value;
  };

  $scope.fetchPublicVersions = function () {
    return Stack.allPublicStackVersions().then(function (versions) {
      if (versions && versions.length) {
        $scope.setStackIds(versions);
        $scope.setVisibleStackVersions(versions);
        $scope.allVersions = versions;
        $scope.selectedPublicRepoVersion = $scope.activeStackVersion;
        $scope.setVersionSelected($scope.activeStackVersion);
        $scope.setNetworkIssues(versions);
        $scope.setStackRepoUpdateLinkExists(versions);
        $scope.validateRepoUrl();
        $scope.availableStackRepoList = versions.length == 1 ? [] : versions;
      }
    });
  };

  $scope.fetchPublicVersions();
}]);
