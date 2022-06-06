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
  .controller('ViewsEditCtrl', ['$scope','$route', '$templateCache', '$routeParams', 'RemoteCluster', 'Cluster', 'View', 'Alert', 'PermissionLoader', 'PermissionSaver', 'ConfirmationModal', '$location', 'UnsavedDialog', '$translate', function($scope, $route, $templateCache , $routeParams, RemoteCluster, Cluster, View, Alert, PermissionLoader, PermissionSaver, ConfirmationModal, $location, UnsavedDialog, $translate) {
    var $t = $translate.instant;
    $scope.identity = angular.identity;
    $scope.isConfigurationEmpty = true;
    $scope.isSettingsEmpty = true;
    $scope.permissionRoles = View.permissionRoles;
    $scope.constants = {
      instance: $t('views.instance'),
      props: $t('views.properties'),
      perms: $t('views.permissions').toLowerCase()
    };

    function reloadViewInfo(section){
      // Load instance data, after View permissions meta loaded
      View.getInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId)
        .then(function(instance) {
          $scope.instance = instance;
          $scope.viewUrl = instance.ViewInstanceInfo.view_name + '/' + instance.ViewInstanceInfo.version + '/' + instance.ViewInstanceInfo.instance_name;
          $scope.settings = {
            'visible': $scope.instance.ViewInstanceInfo.visible,
            'label': $scope.instance.ViewInstanceInfo.label,
            'description': $scope.instance.ViewInstanceInfo.description,
            'shortUrl': $scope.instance.ViewInstanceInfo.short_url,
            'shortUrlName': $scope.instance.ViewInstanceInfo.short_url_name
          };
          switch (section) {
            case "details" :
              initConfigurations();
              initCtrlVariables(instance);
              break;
            case "settings" :
              initConfigurations(true);
              break;
            case "cluster" :
              initCtrlVariables(instance);
              break;
          }
        })
        .catch(function(data) {
          Alert.error($t('views.alerts.cannotLoadInstanceInfo'), data.data.message);
        });

    }

    function initCtrlVariables(instance) {
       $scope.data.clusterType = instance.ViewInstanceInfo.cluster_type;
       var clusterId = instance.ViewInstanceInfo.cluster_handle;
       if (!clusterId) $scope.data.clusterType = 'NONE';
       switch($scope.data.clusterType) {
          case 'LOCAL_AMBARI':
            $scope.cluster = null;
            $scope.clusters.forEach(function(cluster){
              if(cluster.id == clusterId){
                $scope.cluster = cluster;
              }
            })
            break;
          case 'REMOTE_AMBARI':
            $scope.data.remoteCluster = null;
            $scope.remoteClusters.forEach(function(cluster){
              if(cluster.id == clusterId){
                $scope.data.remoteCluster = cluster;
              }
            })
            break;
       }

      $scope.originalClusterType = $scope.data.clusterType;
      $scope.isConfigurationEmpty = !$scope.numberOfClusterConfigs;
      $scope.isSettingsEmpty = !$scope.numberOfSettingsConfigs;
    }

    function isClusterConfig(name) {
      var configurationMeta = $scope.configurationMeta;
      var clusterConfigs = configurationMeta.filter(function(el) {
        return el.clusterConfig;
      }).map(function(el) {
        return el.name;
      });
      return clusterConfigs.indexOf(name) !== -1;
    }

    function initConfigurations(initClusterConfig) {
      var initAllConfigs = !initClusterConfig;
      var configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
      if (initAllConfigs) {
        $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
      }
      for (var confName in configuration) {
        if (configuration.hasOwnProperty(confName)) {
          if (!isClusterConfig(confName) || initAllConfigs) {
            $scope.configuration[confName] = configuration[confName] === 'null' ? '' : configuration[confName];
          }
        }
      }
    }

    function filterClusterConfigs() {
      $scope.configurationMeta.forEach(function (element) {
        if (element.masked && !$scope.editConfigurationDisabled && element.clusterConfig && $scope.data.clusterType == 'NONE') {
          $scope.configuration[element.name] = '';
        }
        if(!element.clusterConfig) {
          delete $scope.configurationBeforeEdit[element.name];
        }
      });
    }

    // Get META for properties
    View.getMeta($routeParams.viewId, $routeParams.version).then(function(data) {
      $scope.configurationMeta = data.data.ViewVersionInfo.parameters;
      $scope.clusterConfigurable = data.data.ViewVersionInfo.cluster_configurable;
      $scope.clusterConfigurableErrorMsg = $scope.clusterConfigurable ? "" : $t('views.alerts.cannotUseOption');
      angular.forEach($scope.configurationMeta, function (item) {
        item.displayName = item.name.replace(/\./g, '\.\u200B');
        item.clusterConfig = !!item.clusterConfig;
        if (!item.clusterConfig) {
          $scope.numberOfSettingsConfigs++;
        }
        $scope.numberOfClusterConfigs = $scope.numberOfClusterConfigs + !!item.clusterConfig;
      });
      reloadViewInfo("details");
    });

    function reloadViewPrivileges(){
      PermissionLoader.getViewPermissions({
          viewName: $routeParams.viewId,
          version: $routeParams.version,
          instanceId: $routeParams.instanceId
        })
        .then(function(permissions) {
          // Refresh data for rendering
          $scope.permissionsEdit = permissions;
          $scope.permissions = angular.copy(permissions);
          $scope.isPermissionsEmpty = angular.equals({}, $scope.permissions);
        })
        .catch(function(data) {
          Alert.error($t('views.alerts.cannotLoadPermissions'), data.data.message);
        });
    }

    $scope.permissions = [];

    reloadViewPrivileges();

    $scope.clusterConfigurable = false;
    $scope.clusterConfigurableErrorMsg = "";
    $scope.clusters = [];
    $scope.remoteClusters = [];
    $scope.cluster = null;
    $scope.noLocalClusterAvailible = true;
    $scope.noRemoteClusterAvailible = true;
    $scope.data = {};
    $scope.data.remoteCluster = null;
    $scope.data.clusterType = 'NONE';

    $scope.editSettingsDisabled = true;
    $scope.editDetailsSettingsDisabled = true;
    $scope.numberOfClusterConfigs = 0;
    $scope.numberOfSettingsConfigs = 0;

    $scope.enableLocalCluster = function() {
      angular.extend($scope.configuration, $scope.configurationBeforeEdit);
      $scope.propertiesForm.$setPristine();
    };

    $scope.disableLocalCluster = function() {
      filterClusterConfigs();
    };

    $scope.toggleSettingsEdit = function() {
      $scope.editSettingsDisabled = !$scope.editSettingsDisabled;
      $scope.settingsBeforeEdit = angular.copy($scope.configuration);
      $scope.configurationMeta.forEach(function (element) {
        if (element.masked && !$scope.editSettingsDisabled && !element.clusterConfig) {
          $scope.configuration[element.name] = '';
        }
        if(element.clusterConfig) {
          delete $scope.settingsBeforeEdit[element.name];
        }
      });
    };

    $scope.toggleDetailsSettingsEdit = function() {
      $scope.editDetailsSettingsDisabled = !$scope.editDetailsSettingsDisabled;
      $scope.settingsBeforeEdit = angular.copy($scope.configuration);
      $scope.configurationMeta.forEach(function (element) {
        if (element.masked && !$scope.editDetailsSettingsDisabled && !element.clusterConfig) {
          $scope.configuration[element.name] = '';
        }
        if(element.clusterConfig) {
          delete $scope.settingsBeforeEdit[element.name];
        }
      });
    };

    Cluster.getAllClusters().then(function (clusters) {
      if(clusters.length >0){
        clusters.forEach(function(cluster) {
          $scope.clusters.push({
            "name" : cluster.Clusters.cluster_name,
            "id" : cluster.Clusters.cluster_id
          })
        });
        $scope.noLocalClusterAvailible = false;
      }else{
        $scope.clusters.push($t('common.noClusters'));
      }
      $scope.cluster = $scope.clusters[0];
    });

    loadRemoteClusters();

    function loadRemoteClusters() {
      RemoteCluster.listAll().then(function (clusters) {
        if(clusters.length >0){
          clusters.forEach(function(cluster) {
            $scope.remoteClusters.push({
              "name" : cluster.ClusterInfo.name,
              "id" : cluster.ClusterInfo.cluster_id
            })
          });
          $scope.noRemoteClusterAvailible = false;
          }else{
            $scope.remoteClusters.push($t('common.noClusters'));
          }
          $scope.data.remoteCluster = $scope.remoteClusters[0];
       });
     }


    $scope.saveSettings = function(callback) {
      if( $scope.settingsForm.$valid ){
        var data = {
          'ViewInstanceInfo':{
            'properties':{}
          }
        };
        $scope.configurationMeta.forEach(function (element) {
          if(!element.clusterConfig) {
            data.ViewInstanceInfo.properties[element.name] = $scope.configuration[element.name];
          }
        });
        return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
          .then(function() {
            if( callback ){
              callback();
            } else {
              reloadViewInfo("settings");
              $scope.editSettingsDisabled = true;
              $scope.settingsForm.$setPristine();
            }
          })
          .catch(function(data) {
            Alert.error($t('views.alerts.cannotSaveSettings'), data.data.message);
          });
      }
    };
    $scope.cancelSettings = function() {
      angular.extend($scope.configuration, $scope.settingsBeforeEdit);

      $scope.editSettingsDisabled = true;
      $scope.settingsForm.$setPristine();
    };

    $scope.saveDetails = function(callback) {
      if( $scope.detailsForm.$valid ){
        var data = {
          'ViewInstanceInfo':{
            'visible': $scope.settings.visible,
            'label': $scope.settings.label,
            'description': $scope.settings.description
          }
        };
        return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
          .then(function() {
            $scope.$root.$emit('instancesUpdate');
            if( callback ){
              callback();
            } else {
              reloadViewInfo("cluster");
              $scope.editDetailsSettingsDisabled = true;
              $scope.settingsForm.$setPristine();
            }
          })
          .catch(function(data) {
            Alert.error($t('views.alerts.cannotSaveSettings'), data.data.message);
          });
      }
    };
    $scope.cancelDetails = function() {
      $scope.settings = {
        'visible': $scope.instance.ViewInstanceInfo.visible,
        'label': $scope.instance.ViewInstanceInfo.label,
        'description': $scope.instance.ViewInstanceInfo.description,
        'shortUrl': $scope.instance.ViewInstanceInfo.short_url,
        'shortUrlName': $scope.instance.ViewInstanceInfo.short_url_name
      };
      $scope.editDetailsSettingsDisabled = true;
      $scope.settingsForm.$setPristine();
    };


    $scope.editConfigurationDisabled = true;
    $scope.togglePropertiesEditing = function () {
      $scope.editConfigurationDisabled = !$scope.editConfigurationDisabled;
      $scope.configurationBeforeEdit = angular.copy($scope.configuration);
      filterClusterConfigs();
    };
    $scope.saveConfiguration = function() {
      if( $scope.propertiesForm.$valid ){
        var data = {
          'ViewInstanceInfo':{
            'properties':{}
          }
        };

        data.ViewInstanceInfo.cluster_type = $scope.data.clusterType;

        switch($scope.data.clusterType) {
          case 'LOCAL_AMBARI':
            data.ViewInstanceInfo.cluster_handle = $scope.cluster.id;
            break;
          case 'REMOTE_AMBARI':
            data.ViewInstanceInfo.cluster_handle = $scope.data.remoteCluster.id;
            break;
            break;
          default :
            data.ViewInstanceInfo.cluster_handle = null;
            $scope.configurationMeta.forEach(function (element) {
              if(element.clusterConfig) {
                data.ViewInstanceInfo.properties[element.name] = $scope.configuration[element.name];
              }
            });
            $scope.removeAllRolePermissions();

          }

        $scope.originalClusterType = $scope.data.clusterType;
        return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
          .then(function() {
            $scope.editConfigurationDisabled = true;
            $scope.propertiesForm.$setPristine();
          })
          .catch(function(data) {
            var errorMessage = data.data.message;

            //TODO: maybe the BackEnd should sanitize the string beforehand?
            errorMessage = errorMessage.substr(errorMessage.indexOf("\{"));

            if (data.status >= 400 && $scope.data.clusterType == 'NONE') {
              try {
                var errorObject = JSON.parse(errorMessage);
                errorMessage = errorObject.detail;
                angular.forEach(errorObject.propertyResults, function (item, key) {
                  $scope.propertiesForm[key].validationError = !item.valid;
                  if (!item.valid) {
                    $scope.propertiesForm[key].validationMessage = item.detail;
                  }
                });
              } catch (e) {
                console.error($t('views.alerts.unableToParseError', {message: data.message}));
              }
            }
            Alert.error($t('views.alerts.cannotSaveProperties'), errorMessage);
          });
      }
    };
    $scope.cancelConfiguration = function() {
      angular.extend($scope.configuration, $scope.configurationBeforeEdit);
      $scope.data.clusterType = $scope.originalClusterType;
      $scope.editConfigurationDisabled = true;
      $scope.propertiesForm.$setPristine();
    };

    // Permissions edit
    $scope.editPermissionDisabled = true;
    $scope.cancelPermissions = function() {
      $scope.permissionsEdit = angular.copy($scope.permissions); // Reset textedit areaes
      $scope.editPermissionDisabled = true;
    };

    $scope.savePermissions = function() {
      $scope.editPermissionDisabled = true;
      return PermissionSaver.saveViewPermissions(
        $scope.permissionsEdit,
        {
          view_name: $routeParams.viewId,
          version: $routeParams.version,
          instance_name: $routeParams.instanceId
        }
        )
        .then(reloadViewPrivileges)
        .catch(function(data) {
          reloadViewPrivileges();
          Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
        });
    };

    $scope.removeAllRolePermissions = function() {
      angular.forEach(View.permissionRoles, function(key) {
        $scope.permissionsEdit["VIEW.USER"]["ROLE"][key] = false;
      })
    };

    $scope.$watch(function() {
      return $scope.permissionsEdit;
    }, function(newValue, oldValue) {
      if(newValue && oldValue != undefined) {
        $scope.savePermissions();
      }
    }, true);



    $scope.deleteInstance = function(instance) {
      ConfirmationModal.show(
        $t('common.delete', {
          term: $t('views.viewInstance')
        }),
        $t('common.deleteConfirmation', {
          instanceType: $t('views.viewInstance'),
          instanceName: instance.ViewInstanceInfo.label
        }),
        null,
        null,
        {
          primaryClass: 'btn-danger'
        }
      ).then(function() {
        View.deleteInstance(instance.ViewInstanceInfo.view_name, instance.ViewInstanceInfo.version, instance.ViewInstanceInfo.instance_name)
          .then(function() {
            $location.path('/views');
          })
          .catch(function(data) {
            Alert.error($t('views.alerts.cannotDeleteInstance'), data.data.message);
          });
      });
    };

    $scope.deleteShortURL = function(shortUrlName) {
      ConfirmationModal.show(
        $t('common.delete', {
          term: $t('urls.url')
        }),
        $t('common.deleteConfirmation', {
          instanceType: $t('urls.url').toLowerCase(),
          instanceName: '"' + shortUrlName + '"'
        })
      ).then(function() {
        View.deleteUrl(shortUrlName).then(function() {
          var currentPageTemplate = $route.current.templateUrl;
          $templateCache.remove(currentPageTemplate);
          $route.reload();
        });
      });
    };

    $scope.$on('$locationChangeStart', function(event, targetUrl) {
      if( $scope.settingsForm.$dirty || $scope.propertiesForm.$dirty){
        UnsavedDialog().then(function(action) {
          targetUrl = targetUrl.split('#').pop();
          switch(action){
            case 'save':
              if($scope.settingsForm.$valid &&  $scope.propertiesForm.$valid ){
                $scope.saveSettings(function() {
                  $scope.saveConfiguration().then(function() {
                    $scope.propertiesForm.$setPristine();
                    $scope.settingsForm.$setPristine();
                    $location.path(targetUrl);
                  });
                });
              }
              break;
            case 'discard':
              $scope.propertiesForm.$setPristine();
              $scope.settingsForm.$setPristine();
              $location.path(targetUrl);
              break;
            case 'cancel':
              targetUrl = '';
              break;
          }
        });
        event.preventDefault();
      }
    });

    $scope.checkAllRoles = function () {
      setAllViewRoles(true);
    };

    $scope.clearAllRoles = function () {
      setAllViewRoles(false);
    };

    function setAllViewRoles(value) {
      var viewRoles = $scope.permissionsEdit["VIEW.USER"]["ROLE"];
      for (var role in viewRoles) {
        $scope.permissionsEdit["VIEW.USER"]["ROLE"][role] = value;
      }
    }
  }]);
