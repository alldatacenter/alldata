/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


define(function(require) {
    'use strict';
    var Backbone = require('backbone');
    var App = require('App');
    var MAppState = require('models/VAppState');
    var XAGlobals = require('utils/XAGlobals');
    var XAUtil    = require('utils/XAUtils');
    var moment    = require('moment');

    return Backbone.Marionette.Controller.extend({

        initialize: function(options) {
            console.log("initialize a Controller Controller");
            if (App.userProfile && App.userProfile.get('configProperties') && App.userProfile.get('configProperties').inactivityTimeout
                && App.userProfile.get('configProperties').inactivityTimeout > 0) {
                XAUtil.setIdealActivityTime()
                $('#contentBody').on("click mousemove keyup mousedown scroll keypress", function(e){
                    // do preload here
                    XAUtil.setIdealActivityTime()
                })
            }
            var vTopNav = require('views/common/TopNav');
            var vProfileBar = require('views/common/ProfileBar');
            var vFooter = require('views/common/Footer');
            var rSidebar = require('views/policymanager/ServiceLayoutSidebar');
            App.rTopNav.show(new vTopNav({
                model: App.userProfile,
                appState: MAppState
            }));
            var type = "resource"
            $('#r_sidebar').addClass('sidebar-list')
            App.rSideBar.show(new rSidebar({
                type: type,
            }))
            App.rTopProfileBar.show(new vProfileBar({}));
            App.rFooter.show(new vFooter({}));
            $('#contentBody').addClass("service-layout");
            App.rSideBar.$el.addClass('expanded');
            App.rSideBar.$el.removeClass('collapsed');
        },

        dashboardAction: function(action) {
            console.log('dashboard action called..');
            var vDashboardLayout = require('views/common/DashboardLayout');
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Dashboard.value
            });
            App.rContent.show(new vDashboardLayout({}));
        },

        //************** Analytics(reports)  Related *********************/
        userAccessReportAction: function(tab) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/reports/UserAccessLayout');
            var RangerPolicyList = require('collections/RangerPolicyList');
            var VXGroupList = require('collections/VXGroupList');
            var VXUserList = require('collections/VXUserList');
            this.rSidebarContentHideAndShow('ReportManager');
            if (App.rContent.currentView)
                App.rContent.currentView.close();
            App.rContent.show(new view({
                collection: new RangerPolicyList(),
                groupList: new VXGroupList(),
                userList: new VXUserList(),
            }));
        },
        auditReportAction: function(tab) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Audit.value
            });
            var view = require('views/reports/AuditLayout');
            var VXAccessAuditList = require('collections/VXAccessAuditList');
            var accessAuditList = new VXAccessAuditList();
            var XAUtil = require('utils/XAUtils');
            var localization = require('utils/XALangSupport');
            _.extend(accessAuditList.queryParams, {
                'sortBy': 'eventTime'
            });
            this.rSidebarContentHideAndShow('AuditManager');
            App.rSideBar.currentView.selectedList(tab.split('?')[0]);
            App.rContent.show(new view({
                accessAuditList: accessAuditList,
                tab: tab
            }));
        },
        loginSessionDetail: function(type, id) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Audit.value
            });
            var view = require('views/reports/LoginSessionDetail');
            var VXAuthSessionList = require('collections/VXAuthSessionList');
            var authSessionList = new VXAuthSessionList();
            this.rSidebarContentHideAndShow('AuditManager');
            authSessionList.fetch({
                data: {
                    id: id
                }
            }).done(function() {
                App.rContent.show(new view({
                    model: authSessionList.first()
                }));
            });
        },
        auditEventDetail: function(eventID) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/reports/AuditAccessLogDetailView');
            var VXAccessAuditList = require('collections/VXAccessAuditList');
            var RangerServiceDefList = require('collections/RangerServiceDefList');
            var serviceDefList = new RangerServiceDefList();
            serviceDefList.fetch({
                cache : false,
                async:false,
                data :{'pageSource':'Audit'}
            });
            var auditList = new VXAccessAuditList();
            auditList.url = 'service/assets/accessAudit?eventId='+eventID
            auditList.fetch({
               cache : false,
               async : false
            }).done(function() {
                App.rContent.show(new view({
                    auditaccessDetail : auditList.models[0].attributes,
                    auditAccessView : true,
                    serviceDefList : serviceDefList
                }));
            })
        },
        //************** UserProfile Related *********************/
        userProfileAction: function() {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.None.value
            });
            var view = require('views/user/UserProfile');
            this.rSidebarContentHideAndShow('ProfileManager');
            App.rContent.show(new view({
                model: App.userProfile.clone()
            }));

        },

        /************** UserORGroups Related *********************/
        userManagerAction: function(tab) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var XAUtil = require('utils/XAUtils');
            var view = require('views/users/UserTableLayout');
            var VXUserList = require('collections/VXUserList');
            var userList = new VXUserList();
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            App.rSideBar.currentView.selectedList(tab.split('?')[0]);
            App.rContent.show(new view({
                collection: userList,
                tab: tab.split('?')[0],
            }));
        },
        userCreateAction: function() {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/UserCreate');
            var VXUser = require('models/VXUser');
            var VXUserList = require('collections/VXUserList');
            var VXGroupList = require('collections/VXGroupList');
            var groupList = new VXGroupList();
            var user = new VXUser();
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            user.collection = new VXUserList();
            groupList.fetch({
                cache: true,
                async: false
            }).done(function() {
                App.rContent.show(new view({
                    model: user,
                    groupList: groupList
                }));
            });
        },
        userEditAction: function(userId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/UserCreate');
            var VXUser = require('models/VXUser');
            var VXUserList = require('collections/VXUserList');
            var user = new VXUser({
                id: userId
            });
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            user.collection = new VXUserList();
            user.fetch({
                cache: true
            }).done(function() {
                App.rContent.show(new view({
                    model: user,
                }));
            });
        },
        groupCreateAction: function() {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/GroupCreate');
            var VXGroup = require('models/VXGroup');
            var VXGroupList = require('collections/VXGroupList');
            var group = new VXGroup();
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            group.collection = new VXGroupList();
            App.rContent.show(new view({
                model: group
            }));
        },
        groupEditAction: function(groupId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/GroupCreate');
            var VXGroup = require('models/VXGroup');
            var VXGroupList = require('collections/VXGroupList');
            var group = new VXGroup({
                id: groupId
            });
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            group.collection = new VXGroupList();
            group.fetch({
                cache: true
            }).done(function() {
                App.rContent.show(new view({
                    model: group
                }));
            });
        },

        roleCreateAction: function() {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/RoleCreate');
            var VXRole = require('models/VXRole');
            var VXRoleList = require('collections/VXRoleList');
            var role = new VXRole();
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            role.collection = new VXRoleList();
            App.rContent.show(new view({
                model: role
            }));
        },
        roleEditAction: function(roleId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/users/RoleCreate');
            var VXRole = require('models/VXRole');
            var VXRoleList = require('collections/VXRoleList');

            var role = new VXRole({
                id: roleId
            });
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            role.collection = new VXRoleList();
            role.fetch({
                cache: true
            }).done(function() {
                App.rContent.show(new view({
                    model: role
                }));
            });
        },

        /************************************************************/
        //************** Generic design Related *********************/
        /************************************************************/

        serviceManagerAction: function(type) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });     
            var view1 = require('views/policymanager/NServiceLayout');
            this.rSidebarContentHideAndShow('AccessManager');
            App.rSideBar.currentView.componetList(type);
            App.rSideBar.currentView.initializePlugins()
            if(App.rSideBar.currentView.ui.serviceActive.length <= 0) {
                App.rContent.show(new view1({
                    type    : type,
                }));
            }
        },

        serviceCreateAction: function(serviceTypeId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/service/ServiceCreate');
            var RangerServiceDef = require('models/RangerServiceDef');
            var RangerService = require('models/RangerService');

            var rangerServiceDefModel = new RangerServiceDef({
                id: serviceTypeId
            });
            var rangerServiceModel = new RangerService();
            this.rSidebarContentHideAndShow('AccessManager');
            App.rContent.show(new view({
                model: rangerServiceModel,
                serviceTypeId: serviceTypeId
            }));
        },
        serviceEditAction: function(serviceTypeId, serviceId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/service/ServiceCreate');
            var RangerServiceDef = require('models/RangerServiceDef');
            var RangerService = require('models/RangerService');
            var XAUtil = require('utils/XAUtils');
            var rangerServiceDefModel = new RangerServiceDef({
                id: serviceTypeId
            });
            if (_.isNaN(parseInt(serviceId))) {
                var rangerService = new RangerService();
                rangerService.url = XAUtil.getServiceByName(serviceId);
            } else {
                var rangerService = new RangerService({
                    id: serviceId
                });
            }
            this.rSidebarContentHideAndShow('AccessManager');
            rangerService.fetch({
                cache: false
            }).done(function() {
                App.rContent.show(new view({
                    model: rangerService,
                    serviceTypeId: serviceTypeId
                }));
            });
        },

        policyManageAction: function(serviceId, policyType) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var XAUtil = require('utils/XAUtils');
            var view = require('views/policies/NRangerPolicyTableLayout');
            var RangerService = require('models/RangerService');
            var RangerPolicyList = require('collections/RangerPolicyList');
            var rangerPolicyList = new RangerPolicyList();
            this.rSidebarContentHideAndShow('AccessManager');
            App.rSideBar.currentView.selecttedService(serviceId);
            rangerPolicyList.queryParams['policyType'] = policyType.split("?")[0];
            if (_.isNaN(parseInt(serviceId))) {
                var rangerService = new RangerService();
                rangerService.url = XAUtil.getServiceByName(serviceId);
            } else {
                var rangerService = new RangerService({
                    id: serviceId
                });
            }
            rangerService.fetch({
                cache: false,
                async: false
            }).done(function() {
                App.rContent.show(new view({
                    rangerService: rangerService,
                    collection: rangerPolicyList,
                }));
            });
        },
        RangerPolicyCreateAction: function(serviceId, policyType) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var XAUtil = require('utils/XAUtils');
            var view = require('views/policies/RangerPolicyCreate');
            var RangerService = require('models/RangerService');
            var RangerPolicy = require('models/RangerPolicy');

            if (_.isNaN(parseInt(serviceId))) {
                var rangerService = new RangerService();
                rangerService.url = XAUtil.getServiceByName(serviceId);
            } else {
                var rangerService = new RangerService({
                    id: serviceId
                });
            }
            this.rSidebarContentHideAndShow('AccessManager');
            App.rSideBar.currentView.selecttedService(serviceId);
            rangerService.fetch({
                cache: false,
            }).done(function() {
                App.rContent.show(new view({
                    model: new RangerPolicy({
                        'policyType': policyType
                    }),
                    rangerService: rangerService,
                }));
            });
        },
        RangerPolicyEditAction: function(serviceId, policyId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/policies/RangerPolicyCreate');
            var RangerService = require('models/RangerService');
            var RangerPolicy = require('models/RangerPolicy');
            var RangerPolicyList = require('collections/RangerPolicyList');
            var XAUtil = require('utils/XAUtils');
            var rangerPolicy = new RangerPolicy({
                id: policyId
            });
            if (_.isNaN(parseInt(serviceId))) {
                var rangerService = new RangerService();
                rangerService.url = XAUtil.getServiceByName(serviceId);
            } else {
                var rangerService = new RangerService({
                    id: serviceId
                });
            }
            this.rSidebarContentHideAndShow('AccessManager');
            App.rSideBar.currentView.selecttedService(serviceId);
            rangerPolicy.collection = new RangerPolicyList();
            rangerPolicy.collection.url = XAUtil.getServicePoliciesURL(serviceId);
            rangerService.fetch({
                cache: false,
                async: false,
            }).done(function() {
                rangerPolicy.fetch({
                    cache: false,
                }).done(function() {
                    App.rContent.show(new view({
                        model: rangerPolicy,
                        rangerService: rangerService
                    }));
                });
            });
        },
        /************PERMISSIONS LISTING *****************************************/
        modulePermissionsAction: function(argument) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/permissions/ModulePermsTableLayout');
            var ModulePermissionList = require('collections/VXModuleDefList');
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            App.rSideBar.currentView.selectedList(argument.split('?')[0]);
            App.rContent.show(new view({
                collection: new ModulePermissionList(),
            }));

        },
        modulePermissionEditAction: function(moduleId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Settings.value
            });
            var view = require('views/permissions/ModulePermissionCreate');
            var ModulePermission = require('models/VXModuleDef');
            var ModulePermissionList = require('collections/VXModuleDefList');
            var modulePermission = new ModulePermission({
                id: moduleId
            });
            var that = this
            this.rSidebarContentHideAndShow('UserGroupRoleManager');
            modulePermission.collection = new ModulePermissionList();
            modulePermission.fetch({
                cache: false
            }).done(function() {
                App.rContent.show(new view({
                    model: modulePermission,
                    groupList: that.groupList,
                    userList: that.userList
                }));
            });
        },
        postLoginDefaultView: function() {
            var SessionMgr = require('mgrs/SessionMgr');
            var XAGlobals = require('utils/XAGlobals');
            var XAUtils = require('utils/XAUtils');
            var vXPortalUser = SessionMgr.getUserProfile();
            var userModuleNames = _.pluck(vXPortalUser.get('userPermList'), 'moduleName');
            XAUtils.setLocationHash(userModuleNames);
        },
        /************** KMS *********************/
        kmsManagerAction: function(kmsManagePage, kmsServiceName) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Encryption.value
            });
            var view = require('views/kms/KMSTableLayout');
            var KmsKeyList = require('collections/VXKmsKeyList');
            this.rSidebarContentHideAndShow('KeyManager');
            App.rContent.show(new view({
                collection: new KmsKeyList(),
                kmsServiceName: kmsServiceName.split("?")[0],
                kmsManagePage: kmsManagePage,
            }));
        },
        kmsKeyCreateAction: function(kmsServiceName) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.Encryption.value
            });
            var view = require('views/kms/KmsKeyCreate');
            var KmsKey = require('models/VXKmsKey');
            this.rSidebarContentHideAndShow('KeyManager');
            App.rContent.show(new view({
                model: new KmsKey({
                    'length': 128,
                    'cipher': 'AES/CTR/NoPadding'
                }),
                kmsServiceName: kmsServiceName
            }));
        },
        /**************** SECURITY ZONE ******************************/
        zoneManagmentAction: function(listId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.SecurityZone.value
            });
            var XAUtil = require('utils/XAUtils');
            var vSecurityZone = require('views/security_zone/SecurityZone');
            var RangerServiceList = require('collections/RangerServiceList');
            var RangerZoneList = require('collections/RangerZoneList');
            var rangerServiceList = new RangerServiceList();
            var rangerZoneList = new RangerZoneList();
            rangerServiceList.setPageSize(200);
            rangerServiceList.fetch({
                cache: false,
                async: false
            });
            rangerZoneList.fetch({
                cache: false,
                async: false,
            });
            if (App.rSideBar.currentView) {
                this.rSidebarContentHideAndShow('SecurityZone');
                if (!_.isNaN(parseInt(listId))) {
                    var zoneNameModel = rangerZoneList.find(function(m) {
                        return m.id == listId
                    });
                    App.rSideBar.currentView.selectedList(zoneNameModel.get('name'));
                }
                if (_.isNaN(parseInt(listId)) && rangerZoneList.length > 0) {
                    var zoneNameModel = rangerZoneList.find(function(m) {
                        return m.id == listId
                    });
                    App.rSideBar.currentView.selectedList(rangerZoneList.models[0].get('name'));
                }
            }
            App.rContent.show(new vSecurityZone({
                rangerService: rangerServiceList,
                collection: rangerZoneList,
                zoneId: listId
            }));
        },

        RangerZoneCreateAction: function() {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.SecurityZone.value
            });
            var view = require('views/security_zone/ZoneCreate');
            var RangerServiceList = require('collections/RangerServiceList');
            var RangerZone = require('models/RangerZone');
            var RangerZoneList = require('collections/RangerZoneList');
            var zoneSerivesColl = new RangerZoneList();
            var rangerServiceList = new RangerServiceList();
            this.rSidebarContentHideAndShow('SecurityZone');
            rangerServiceList.setPageSize(200);
            rangerServiceList.fetch({
                cache: false,
            }).done(function() {
                App.rContent.show(new view({
                    model: new RangerZone({
                        'policyType': 0
                    }),
                    rangerService: rangerServiceList,
                    zoneSerivesColl: zoneSerivesColl
                }));
            });
        },

        RangerZoneEditAction: function(zoneId) {
            MAppState.set({
                'currentTab': XAGlobals.AppTabs.AccessManager.value
            });
            var view = require('views/security_zone/ZoneCreate');
            var RangerServiceList = require('collections/RangerServiceList');
            var RangerZone = require('models/RangerZone');
            var RangerZoneList = require('collections/RangerZoneList');
            var XAUtil = require('utils/XAUtils');
            var rangerServiceList = new RangerServiceList();
            var rangerZone = new RangerZone({
                id: zoneId
            })
            var zoneSerivesColl = new RangerZoneList();
            this.rSidebarContentHideAndShow('SecurityZone');
            rangerServiceList.setPageSize(200);
            rangerServiceList.fetch({
                cache: false,
                async: false,
            });
            rangerZone.fetch({
                cache: false,
                async: false,
            }).done(function() {
                App.rContent.show(new view({
                    rangerService: rangerServiceList,
                    model: rangerZone,
                    zoneSerivesColl: zoneSerivesColl,
                }));
            })
        },

        /**************** ERROR PAGE ******************************/
        pageNotFoundAction: function() {
            var XAUtils = require('utils/XAUtils');
            XAUtils.defaultErrorHandler(undefined, {
                'status': 404
            });
        },

        /**************** Sidebar content hide and show ******************************/
        rSidebarContentHideAndShow: function(moduleName) {
            App.rSideBar.$el.find('.listOfModule').hide();
            App.rSideBar.$el.find("[data-name="+moduleName+"]").show();
        },
    });
});