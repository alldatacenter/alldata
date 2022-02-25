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

module.exports = Em.Route.extend(App.RouterRedirections, {

  breadcrumbs: {
    label: '<span class="glyphicon glyphicon-home"></span>',
    route: 'dashboard'
  },

  route: '/main',
  enter: function (router) {
    App.db.updateStorage();
    var self = this;
    var location = router.location.location.hash;
    var clusterController = App.router.get('clusterController');

    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = App.router.get('applicationController');
        App.router.get('experimentalController').loadSupports().complete(function () {
          applicationController.startKeepAlivePoller();
          clusterController.loadAmbariProperties().complete(function () {
            App.router.get('mainViewsController').loadAmbariViews();
            clusterController.loadClusterName(false).done(function () {
              $('#main').removeClass('install-wizard-content');
              if (App.get('testMode')) {
                router.get('mainController').initialize();
              } else {
                if (router.get('clusterInstallCompleted')) {
                  if (!App.get('isOnlyViewUser')) {
                    clusterController.checkDetailedRepoVersion().done(function () {
                      router.get('mainController').initialize();
                    });
                  } else {
                    // Don't transit to Views when user already on View page
                    if (App.router.currentState.name !== 'viewDetails' && App.router.currentState.name !== 'shortViewDetails') {
                      App.router.transitionTo('main.views.index');
                    }
                    clusterController.set('isLoaded', true); // hide loading bar
                  }
                }
                else {
                  Em.run.next(function () {
                    App.clusterStatus.updateFromServer().complete(function () {
                      var currentClusterStatus = App.clusterStatus.get('value');
                      if (router.get('currentState.parentState.name') !== 'views' && router.get('currentState.parentState.name') !== 'view'
                          && currentClusterStatus && self.get('installerStatuses').contains(currentClusterStatus.clusterState)) {
                        if (App.isAuthorized('AMBARI.ADD_DELETE_CLUSTERS')) {
                          self.redirectToInstaller(router, currentClusterStatus, false);
                        } else {
                          clusterController.set('isLoaded', true);
                          Em.run.next(function () {
                            App.router.transitionTo('main.views.index');
                          });
                        }
                      } else {
                        clusterController.set('isLoaded', true);
                      }
                    });
                  });
                }
              }
            });
          });
          // TODO: redirect to last known state
        });
      } else {
        router.savePreferedPath(location);
        Em.run.next(function () {
          router.transitionTo('login');
        });
      }
    });
  },
  /*
   routePath: function(router,event) {
   if (router.getAuthenticated()) {
   App.router.get('clusterController').loadClusterName(false);
   router.get('mainController').initialize();
   // TODO: redirect to last known state
   } else {
   Ember.run.next(function () {
   router.transitionTo('login');
   });
   }
   }, */

  index: Ember.Route.extend({
    route: '/',
    redirectsTo: 'dashboard.index'
  }),

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('main');
  },

  test: Em.Route.extend({
    route: '/test',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainTest');
    }
  }),

  dashboard: Em.Route.extend({

    breadcrumbs: {
      label: Em.I18n.t('menu.item.dashboard'),
      route: 'dashboard'
    },

    route: '/dashboard',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainDashboard');
    },
    index: Em.Route.extend({
      route: '/',
      enter: function (router) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.widgets');
        });
      }
    }),
    goToDashboardView: function (router, event) {
      router.transitionTo(event.context);
    },
    widgets: Em.Route.extend({
      route: '/metrics',
      breadcrumbs: {
        label: Em.I18n.t('common.metrics')
      },
      connectOutlets: function (router, context) {
        App.loadTimer.start('Dashboard Metrics Page');
        router.set('mainDashboardController.selectedCategory', 'widgets');
        router.get('mainDashboardController').connectOutlet('mainDashboardWidgets');
      }
    }),
    charts: Em.Route.extend({
      route: '/charts',
      breadcrumbs: null,
      connectOutlets: function (router, context) {
        App.loadTimer.start('Heatmaps Page');
        router.set('mainDashboardController.selectedCategory', 'charts');
        router.get('mainDashboardController').connectOutlet('mainCharts');
      },
      index: Ember.Route.extend({
        route: '/',
        enter: function (router) {
          Em.run.next(function () {
            router.transitionTo('heatmap');
          });
        }
      }),
      heatmap: Em.Route.extend({
        route: '/heatmap',
        connectOutlets: function (router, context) {
          router.get('mainController').dataLoading().done(function () {
            router.get('mainChartsController').connectOutlet('mainChartsHeatmap');
          });
        }
      }),
      horizon_chart: Em.Route.extend({
        route: '/horizon_chart',
        connectOutlets: function (router, context) {
          router.get('mainChartsController').connectOutlet('mainChartsHorizon');
        }
      }),
      showChart: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),
    configHistory: Em.Route.extend({
      route: '/config_history',
      connectOutlets: function (router, context) {
        App.loadTimer.start('Config History Page');
        router.set('mainDashboardController.selectedCategory', 'configHistory');
        router.get('mainDashboardController').connectOutlet('mainConfigHistory');
      }
    }),
    goToServiceConfigs: function (router, event) {
      router.get('mainServiceItemController').set('routeToConfigs', true);
      router.get('mainServiceInfoConfigsController').set('preSelectedConfigVersion', event.context);
      router.transitionTo('main.services.service.configs', App.Service.find(event.context.get('serviceName')));
      router.get('mainServiceItemController').set('routeToConfigs', false);
    }
  }),

  views: require('routes/views'),
  view: require('routes/view'),


  hosts: Em.Route.extend({

    breadcrumbs: {
      label: Em.I18n.t('menu.item.hosts'),
      route: 'hosts',
      beforeTransition() {
        App.router.set('mainHostController.showFilterConditionsFirstLoad', true);
        App.router.set('mainHostController.saveSelection', true);
      }
    },

    route: '/hosts',
    index: Ember.Route.extend({
      route: '/',
      connectOutlets: function (router, context) {
        App.loadTimer.start('Hosts Page');
        router.get('mainController').connectOutlet('mainHost');
      }
    }),

    hostDetails: Em.Route.extend({

      breadcrumbs: {
        itemView: Em.View.extend({
          tagName: "a",
          contentBinding: 'App.router.mainHostDetailsController.content',
          isActive: Em.computed.equal('content.passiveState', 'OFF'),
          click: function() {
            App.router.transitionTo('hosts.hostDetails.summary', this.get('content'));
          },
          template: Em.Handlebars.compile('<span class="host-breadcrumb">{{view.content.hostName}}</span>' +
            '<span rel="HealthTooltip" {{bindAttr class="view.content.healthClass view.content.healthIconClass :icon"}} ' +
            'data-placement="bottom" {{bindAttr data-original-title="view.content.healthToolTip" }}></span>')
        })
      },

      route: '/:host_id',
      connectOutlets: function (router, host) {
        router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
        router.get('mainController').connectOutlet('mainHostDetails', host);
      },

      index: Ember.Route.extend({
        route: '/',
        redirectsTo: 'summary'
      }),

      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          router.get('mainController').dataLoading().done(function() {
            var controller = router.get('mainHostDetailsController');
            var tags = ['hive-env'];
            if ( App.Service.find().mapProperty('serviceName').contains('OOZIE')) {
              controller.loadConfigs('loadOozieConfigs');
              controller.isOozieConfigLoaded.always(function () {
                if(App.Service.find().mapProperty('serviceName').contains('HIVE')){
                  App.router.get('configurationController').getCurrentConfigsBySites(tags).always(function () {
            	    controller.connectOutlet('mainHostSummary');
            	  });
            	} else
              controller.connectOutlet('mainHostSummary');
              });
            } else if(App.Service.find().mapProperty('serviceName').contains('HIVE')) {
              App.router.get('configurationController').getCurrentConfigsBySites(tags).always(function () {
                controller.connectOutlet('mainHostSummary');
              });
            } else {
              controller.connectOutlet('mainHostSummary');
            }
          });
        }
      }),

      configs: Em.Route.extend({
        route: '/configs',
        connectOutlets: function (router, context) {
          router.get('mainController').isLoading.call(router.get('clusterController'), 'isConfigsPropertiesLoaded').done(function () {
            router.get('mainHostDetailsController').connectOutlet('mainHostConfigs');
          });
        },
        exitRoute: function (router, context, callback) {
          callback();
        }
      }),

      alerts: Em.Route.extend({
        route: '/alerts',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostAlerts');
        },
        exit: function (router) {
          router.set('mainAlertInstancesController.isUpdating', false);
        }
      }),

      metrics: Em.Route.extend({
        route: '/metrics',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostMetrics');
        }
      }),

      stackVersions: Em.Route.extend({
        breadcrumbs: {
          label: Em.I18n.t('common.versions')
        },
        route: '/stackVersions',
        connectOutlets: function (router, context) {
          if (App.get('stackVersionsAvailable')) {
            router.get('mainHostDetailsController').connectOutlet('mainHostStackVersions');
          }
          else {
            router.transitionTo('summary');
          }
        }
      }),

      logs: Em.Route.extend({
        route: '/logs:query',
        connectOutlets: function (router, context) {
          if (App.get('supports.logSearch')) {
            router.get('mainHostDetailsController').connectOutlet('mainHostLogs')
          } else {
            router.transitionTo('summary');
          }
        },
        serialize: function(router, params) {
          return this.serializeQueryParams(router, params, 'mainHostDetailsController');
        }
      }),

      hostNavigate: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),

    back: function (router, event) {
      var referer = router.get('mainHostDetailsController.referer');
      if (referer) {
        router.route(referer);
      }
      else {
        window.history.back();
      }
    },

    addHost: function (router) {
      router.transitionTo('hostAdd');
    },

    exit: function (router) {
      router.set('mainHostController.saveSelection', false);
    }

  }),

  hostAdd: require('routes/add_host_routes'),

  alerts: Em.Route.extend({

    breadcrumbs: {
      label: Em.I18n.t('menu.item.alerts'),
      route: 'alerts',
      beforeTransition() {
        App.router.set('mainAlertDefinitionsController.showFilterConditionsFirstLoad', false);
      }
    },

    route: '/alerts',
    index: Em.Route.extend({
      route: '/',
      connectOutlets: function (router, context) {
        router.get('mainController').connectOutlet('mainAlertDefinitions');
      }
    }),

    alertDetails: Em.Route.extend({

      breadcrumbs: {
        labelBindingPath: 'App.router.mainAlertDefinitionDetailsController.content.label',
        disabled: true
      },

      route: '/:alert_definition_id',

      connectOutlets: function (router, alertDefinition) {
        App.router.set('mainAlertDefinitionsController.showFilterConditionsFirstLoad', true);
        router.get('mainController').connectOutlet('mainAlertDefinitionDetails', alertDefinition);
      },

      exit: function (router) {
        router.set('mainAlertInstancesController.isUpdating', false);
      },

      exitRoute: function (router, context, callback) {
        var controller = router.get('mainAlertDefinitionDetailsController');
        if (App.router.get('clusterController.isLoaded') && controller.get('isEditing')) {
          controller.showSavePopup(callback);
        } else {
          callback();
        }
      }
    }),

    back: function (router, event) {
      window.history.back();
    }
  }),

  alertAdd: require('routes/add_alert_definition_routes'),

  admin: Em.Route.extend({
    route: '/admin',
    breadcrumbs: {
      disabled: true
    },
    enter: function (router, transition) {
      if (router.get('loggedIn') && !App.isAuthorized('CLUSTER.TOGGLE_KERBEROS, SERVICE.SET_SERVICE_USERS_GROUPS, CLUSTER.UPGRADE_DOWNGRADE_STACK, CLUSTER.VIEW_STACK_DETAILS')
        && !(App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.index');
        });
      }
    },

    routePath: function (router, event) {
      if (!App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK') && !(App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
        Em.run.next(function () {
          App.router.transitionTo('main.dashboard.index');
        });
      } else {
        this._super(router, event);
      }
    },
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainAdmin');
    },

    index: Em.Route.extend({
      route: '/',
      redirectsTo: 'stackAndUpgrade.index'
    }),

    adminAuthentication: Em.Route.extend({
      route: '/authentication',
      connectOutlets: function (router, context) {
        router.set('mainAdminController.category', "authentication");
        router.get('mainAdminController').connectOutlet('mainAdminAuthentication');
      }
    }),

    adminKerberos: Em.Route.extend({

      breadcrumbs: {
        label: Em.I18n.t('common.kerberos')
      },

      route: '/kerberos',
      enter: function (router, transition) {
        if (router.get('loggedIn') && (!App.isAuthorized('CLUSTER.TOGGLE_KERBEROS') || !App.supports.enableToggleKerberos)) {
          router.transitionTo('main.dashboard.index');
        }
      },
      index: Em.Route.extend({
        route: '/',
        connectOutlets: function (router, context) {
          router.set('mainAdminController.category', "kerberos");
          router.set('mainAdminController.categoryLabel', Em.I18n.t('common.kerberos'));
          router.get('mainAdminController').connectOutlet('mainAdminKerberos');
        }
      }),
      adminAddKerberos: require('routes/add_kerberos_routes'),

      disableSecurity: Em.Route.extend({
        route: '/disableSecurity',
        enter: function (router) {
          App.router.get('updateController').set('isWorking', false);
          router.get('mainController').dataLoading().done(function () {
            App.ModalPopup.show({
              classNames: ['wizard-modal-wrapper', 'disable-security-modal'],
              modalDialogClasses: ['modal-xlg'],
              header: Em.I18n.t('admin.removeSecurity.header'),
              bodyClass: App.KerberosDisableView.extend({
                controllerBinding: 'App.router.kerberosDisableController'
              }),
              primary: Em.I18n.t('common.complete'),
              secondary: null,
              disablePrimary: Em.computed.alias('App.router.kerberosDisableController.isSubmitDisabled'),

              onPrimary() {
                this.onClose();
              },

              onClose: function () {
                var self = this;
                var controller = router.get('kerberosDisableController');
                if (!controller.get('isSubmitDisabled')) {
                  self.proceedOnClose();
                  return;
                }
                var unkerberizeCommand = controller.get('tasks').findProperty('command', 'unkerberize') || Em.Object.create();
                var isUnkerberizeInProgress = unkerberizeCommand.get('status') === 'IN_PROGRESS';
                if (controller.get('tasks').everyProperty('status', 'COMPLETED')) {
                  self.proceedOnClose();
                  return;
                }
                // user cannot exit wizard during removing kerberos
                if (isUnkerberizeInProgress) {
                  App.showAlertPopup(Em.I18n.t('admin.kerberos.disable.unkerberize.header'), Em.I18n.t('admin.kerberos.disable.unkerberize.message'));
                  return;
                }
                App.showConfirmationPopup(function () {
                  self.proceedOnClose();
                }, Em.I18n.t('admin.security.disable.onClose'));
              },
              proceedOnClose: function () {
                var self = this;
                var disableController = router.get('kerberosDisableController');
                disableController.clearStep();
                disableController.resetDbNamespace();
                App.db.setSecurityDeployCommands(undefined);
                App.router.get('updateController').set('isWorking', true);
                router.get('mainAdminKerberosController').setDisableSecurityStatus(undefined);
                router.get('addServiceController').finish();
                App.clusterStatus.setClusterStatus({
                  clusterName: router.get('content.cluster.name'),
                  clusterState: 'DEFAULT',
                  localdb: App.db.data
                }, {
                  alwaysCallback: function () {
                    self.hide();
                    router.transitionTo('adminKerberos.index');
                    Em.run.next(function() {
                      location.reload();
                    });
                  }
                });
              },
              didInsertElement: function () {
                this._super();
                this.fitHeight();
              }
            });
          });
        },

        unroutePath: function () {
          return false;
        },
        next: function (router, context) {
          $("#modal").find(".close").trigger('click');
        },
        done: function (router, context) {
          var controller = router.get('kerberosDisableController');
          if (!controller.get('isSubmitDisabled')) {
            $(context.currentTarget).parents("#modal").find(".close").trigger('click');
          }
        }
      })
    }),

    stackAndUpgrade: Em.Route.extend({
      route: '/stack',
      breadcrumbs: null,
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "stackAndUpgrade");
        router.set('mainAdminController.categoryLabel', Em.I18n.t('admin.stackUpgrade.title'));
        router.get('mainAdminController').connectOutlet('mainAdminStackAndUpgrade');
      },

      index: Em.Route.extend({
        route: '/',
        redirectsTo: 'services'
      }),

      services: Em.Route.extend({

        breadcrumbs: {
          label: Em.I18n.t('common.stack')
        },

        route: '/services',
        connectOutlets: function (router, context) {
          router.get('mainAdminStackAndUpgradeController').connectOutlet('mainAdminStackServices');
        }
      }),

      versions: Em.Route.extend({
        breadcrumbs: {
          label: Em.I18n.t('common.versions')
        },
        route: '/versions',
        connectOutlets: function (router, context) {
          router.get('mainAdminStackAndUpgradeController').connectOutlet('MainAdminStackVersions');
        }
      }),

      upgradeHistory: Em.Route.extend({

        breadcrumbs: {
          label: Em.I18n.t('common.upgrade.history')
        },

        route: '/history',
        connectOutlets: function (router, context) {
          router.get('mainAdminStackAndUpgradeController').connectOutlet('mainAdminStackUpgradeHistory');
        },
      }),

      stackNavigate: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),
    stackUpgrade: require('routes/stack_upgrade_routes'),

    adminAdvanced: Em.Route.extend({
      route: '/advanced',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "advanced");
        router.get('mainAdminController').connectOutlet('mainAdminAdvanced');
      }
    }),
    adminServiceAccounts: Em.Route.extend({

      breadcrumbs: {
        label: Em.I18n.t('common.serviceAccounts')
      },

      route: '/serviceAccounts',
      enter: function (router, transition) {
        if (router.get('loggedIn') && !App.isAuthorized('SERVICE.SET_SERVICE_USERS_GROUPS')) {
          router.transitionTo('main.dashboard.index');
        }
      },
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "adminServiceAccounts");
        router.set('mainAdminController.categoryLabel', Em.I18n.t('common.serviceAccounts'));
        router.get('mainAdminController').connectOutlet('mainAdminServiceAccounts');
      }
    }),

    adminServiceAutoStart: Em.Route.extend({

      breadcrumbs: {
        label: Em.I18n.t('admin.serviceAutoStart.title')
      },

      route: '/serviceAutoStart',
      enter: function(router, transition) {
        if (router.get('loggedIn') && !App.isAuthorized('CLUSTER.MANAGE_AUTO_START') && !App.isAuthorized('SERVICE.MANAGE_AUTO_START')) {
          router.transitionTo('main.dashboard.index');
        }
      },
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "serviceAutoStart");
        router.set('mainAdminController.categoryLabel', Em.I18n.t('admin.serviceAutoStart.title'));
        router.get('mainAdminController').connectOutlet('mainAdminServiceAutoStart');
      },
      exitRoute: function (router, context, callback) {
        var controller = router.get('mainAdminServiceAutoStartController');
        if (controller.get('isModified')) {
          controller.showSavePopup(callback);
        } else {
          callback();
        }
      }
    }),

    adminAudit: Em.Route.extend({
      route: '/audit',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "audit");
        router.get('mainAdminController').connectOutlet('mainAdminAudit');
      }
    }),
    upgradeStack: function (router, event) {
      if (!$(event.currentTarget).hasClass('inactive')) {
        router.transitionTo('stackUpgrade');
      }
    },


    adminNavigate: function (router, object) {
      router.transitionTo('admin' + object.context.capitalize());
    },

    //events
    goToAdmin: function (router, event) {
      var isDisabled = !!event.context.disabled;
      if(!isDisabled){
        router.transitionTo(event.context.url);
      }
    }

  }),

  createServiceWidget: function (router, context) {
    if (context) {
      var widgetController = router.get('widgetWizardController');
      widgetController.save('widgetService', context.get('serviceName'));
      var layout = JSON.parse(JSON.stringify(context.get('layout')));
      layout.widgets = context.get('layout.widgets').mapProperty('id');
      widgetController.save('layout', layout);
    }
    router.transitionTo('createWidget');
  },

  createWidget: require('routes/create_widget'),

  editServiceWidget: function (router, context) {
    if (context) {
      var widgetController = router.get('widgetEditController');
      widgetController.save('widgetService', context.get('serviceName'));
      widgetController.save('widgetType', context.get('widgetType'));
      widgetController.save('widgetProperties', context.get('properties'));
      widgetController.save('widgetMetrics', context.get('metrics'));
      widgetController.save('widgetValues', context.get('values'));
      widgetController.save('widgetName', context.get('widgetName'));
      widgetController.save('widgetDescription', context.get('description'));
      widgetController.save('widgetScope', context.get('scope'));
      widgetController.save('widgetAuthor', context.get('author'));
      widgetController.save('widgetId', context.get('id'));
      widgetController.save('allMetrics', []);
    }
    router.transitionTo('editWidget');
  },

  editWidget: require('routes/edit_widget'),

  services: Em.Route.extend({

    breadcrumbs: {
      disabled: true
    },

    route: '/services',
    index: Em.Route.extend({
      route: '/',
      enter: function (router) {
        Em.run.next(function () {
          var controller = router.get('mainController');
          controller.dataLoading().done(function () {
            if (router.currentState.parentState.name === 'services' && router.currentState.name === 'index') {
              var service = router.get('mainServiceItemController.content');
              if (!service || !service.get('isLoaded')) {
                service = App.Service.find().objectAt(0); // getting the first service to display
              }
              if (router.get('mainServiceItemController').get('routeToConfigs')) {
                router.transitionTo('service.configs', service);
              } else if (router.get('mainServiceItemController.routeToHeatmaps')) {
                router.transitionTo('service.heatmaps', service);
              } else {
                router.transitionTo('service.summary', service);
              }
            }
          });
        });
      }
    }),
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainService');
    },
    service: Em.Route.extend({
      route: '/:service_id',
      breadcrumbs: {
        labelBindingPath: 'App.router.mainServiceItemController.content.displayName',
        disabled: true
      },
      connectOutlets: function (router, service) {
        router.get('mainServiceController').connectOutlet('mainServiceItem', service);
        if (service.get('isLoaded')) {
          if (router.get('mainServiceItemController').get('routeToConfigs')) {
            router.transitionTo('configs');
          } else if (router.get('mainServiceItemController.routeToHeatmaps')) {
            router.transitionTo('heatmaps');
          } else {
            router.transitionTo('summary');
          }
        } else {
          router.transitionTo('index');
        }
      },
      index: Ember.Route.extend({
        route: '/'
      }),
      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          App.loadTimer.start('Service Summary Page');
          var item = router.get('mainServiceItemController.content');
          if (router.get('clusterController.isServiceMetricsLoaded')) router.get('updateController').updateServiceMetric(Em.K);
          //if service is not existed then route to default service
          if (item.get('isLoaded')) {
            router.get('mainServiceItemController').connectOutlet('mainServiceInfoSummary', item);
          } else {
            router.transitionTo('services.index');
          }
        }
      }),
      metrics: Em.Route.extend({
        route: '/metrics',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoMetrics', item);
        }
      }),
      configs: Em.Route.extend({
        route: '/configs',
        connectOutlets: function (router, context) {
          App.loadTimer.start('Service Configs Page');
          router.get('mainController').dataLoading().done(function () {
            var item = router.get('mainServiceItemController.content');
            //if service is not existed then route to default service
            if (item.get('isLoaded')) {
              if (router.get('mainServiceItemController.isConfigurable')) {
                router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
              }
              else {
                // if service doesn't have configs redirect to summary
                router.transitionTo('summary');
              }
            } else {
              item.set('routeToConfigs', true);
              router.transitionTo('services.index');
            }
          });
        },
        exitRoute: function (router, nextRoute, callback) {
          var controller = router.get('mainServiceInfoConfigsController');
          // If another user is running some wizard, current user can't save configs
          if (controller.hasUnsavedChanges() && !router.get('wizardWatcherController.isWizardRunning')) {
            controller.showSavePopup(callback);
          } else {
            callback();
          }
        }
      }),
      heatmaps: Em.Route.extend({
        route: '/heatmaps',
        connectOutlets: function (router, context) {
          App.loadTimer.start('Service Heatmaps Page');
          router.get('mainController').dataLoading().done(function () {
            var item = router.get('mainServiceItemController.content');
            if (item.get('isLoaded')) {
              router.get('mainServiceItemController').connectOutlet('mainServiceInfoHeatmap', item);
            } else {
              item.set('routeToHeatmaps', true);
              router.transitionTo('services.index');
            }
          });
        }
      }),
      audit: Em.Route.extend({
        route: '/audit',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoAudit', item);
        }
      }),
      showInfo: function (router, event) {
        router.transitionTo(event.context);
      }
    }),
    showService: Em.Router.transitionTo('service'),
    addService: Em.Router.transitionTo('serviceAdd'),
    reassign: Em.Router.transitionTo('reassign'),

    enableHighAvailability: require('routes/high_availability_routes'),

    manageJournalNode: require('routes/manage_journalnode_routes'),

    enableRMHighAvailability: require('routes/rm_high_availability_routes'),

    enableRAHighAvailability: require('routes/ra_high_availability_routes'),

    enableNameNodeFederation: require('routes/namenode_federation_routes'),

    addHawqStandby: require('routes/add_hawq_standby_routes'),

    removeHawqStandby: require('routes/remove_hawq_standby_routes'),

    activateHawqStandby: require('routes/activate_hawq_standby_routes'),

    rollbackHighAvailability: require('routes/rollbackHA_routes')
  }),

  reassign: require('routes/reassign_master_routes'),

  serviceAdd: require('routes/add_service_routes'),

  selectService: Em.Route.transitionTo('services.service.summary'),
  selectHost: function (router, event) {
    router.get('mainHostDetailsController').set('isFromHosts', false);
    router.transitionTo('hosts.hostDetails.index', event.context);
  },
  filterHosts: function (router, component) {
    if (!component.context)
      return;
    router.get('mainHostController').filterByComponent(component.context);
    router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    router.get('mainHostController').set('filterChangeHappened', true);
    router.transitionTo('hosts.index');
  },
  showDetails: function (router, event) {
    router.get('mainHostDetailsController').set('referer', router.location.lastSetURL);
    router.get('mainHostDetailsController').set('isFromHosts', true);
    router.transitionTo('hosts.hostDetails.summary', event.context);
  },
  gotoAlertDetails: function (router, event) {
    router.transitionTo('alerts.alertDetails', event.context);
  },

  /**
   * Open summary page of the selected service
   * @param {object} event
   * @method routeToService
   */
  routeToService: function (router, event) {
    var service = event.context;
    router.transitionTo('main.services.service.summary', service);
  }
});
