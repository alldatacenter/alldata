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


define(function(require){
    'use strict';

    var Backbone        = require('backbone');
    var App             = require('App');
    var XALinks         = require('modules/XALinks');
    var XAUtil          = require('utils/XAUtils');
    var XAEnums         = require('utils/XAEnums');
    var localization    = require('utils/XALangSupport');

    var UserTableLayout = require('views/users/UserTableLayout');
    var VXRoleList     = require('collections/VXRoleList');
    var RoleForm       = require('views/users/RoleForm');
    var RolecreateTmpl = require('hbs!tmpl/users/RoleCreate_tmpl');
    var SessionMgr      = require('mgrs/SessionMgr');

    var RoleCreate = Backbone.Marionette.Layout.extend(
    /** @lends RoleCreate */
    {
        _viewName : 'RoleCreate',

        template: RolecreateTmpl,
        breadCrumbs :function(){
            return this.model.isNew() ? [XALinks.get('Roles'),XALinks.get('RoleCreate')]
                : [XALinks.get('Roles'),XALinks.get('RoleEdit')];
        },

        /** Layout sub regions */
        regions: {
            'rForm' :'div[data-id="r_form"]'
        },

        /** ui selector cache */
        ui: {
            'btnSave'   : '[data-id="save"]',
            'btnCancel' : '[data-id="cancel"]'
        },

        /** ui events hash */
        events: function() {
            var events = {};
            //events['change ' + this.ui.input]  = 'onInputChange';
            events['click ' + this.ui.btnSave]      = 'onSave';
            events['click ' + this.ui.btnCancel]    = 'onCancel';
            return events;
        },

        /**
        * intialize a new RoleCreate Layout
        * @constructs
        */
        initialize: function(options) {
            console.log("initialized a RoleCreate Layout");

            _.extend(this, _.pick(options, ''));
            this.editRole = this.model.has('id') ? true : false;
            this.bindEvents();
            this.form = new RoleForm({
                template : require('hbs!tmpl/users/RoleForm_tmpl'),
                model : this.model
            });
        },

        /** all events binding here */
        bindEvents : function(){
            /*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
            /*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
        },

        /** on render callback */
        onRender: function() {
            var that = this
            this.initializePlugins();
            this.rForm.show(this.form);
            // this.rForm.$el.dirtyFields();
            // XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavGroupForm'),this.rForm.$el);
        },

        /** all post render plugin initialization */
        initializePlugins: function(){
        },
        onSave: function(){
            var that = this, usersDetails = [], groupsDetails = [], rolesDetails = [] ;
            var errors = this.form.commit({validate : false});
            if(! _.isEmpty(errors)){
                return;
            }
            XAUtil.blockUI();
            if(!this.form.beforeSave()){
                XAUtil.blockUI('unblock');
                return
            }
            this.form.usersColl.models.filter(function(m){
                usersDetails.push ({'name' : m.get('name') , 'isAdmin' : m.get('isAdmin')});
            })
            this.form.groupsColl.models.filter(function(m){
                groupsDetails.push ({'name' : m.get('name') , 'isAdmin' : m.get('isAdmin')});
            })
            this.form.rolesColl.models.filter(function(m){
                rolesDetails.push ({'name' : m.get('name') , 'isAdmin' : m.get('isAdmin')});
            })
            this.model.set('users', usersDetails);
            this.model.set('groups', groupsDetails);
            this.model.set('roles', rolesDetails);
            this.model.save({},{
                success: function () {
                    XAUtil.blockUI('unblock');
                    XAUtil.allowNavigation();
                    Backbone.fetchCache._cache = {}
                    var msg = that.editRole ? 'Role updated successfully' :'Role created successfully';
                    XAUtil.notifySuccess('Success', msg);
                    App.usersGroupsListing = {'showLastPage' : true}
                    App.appRouter.navigate("#!/users/Roletab",{trigger: true});
                },
                error : function (model, response, options) {
                    XAUtil.blockUI('unblock');
                    if ( response && response.responseJSON && response.responseJSON.msgDesc){
                        if(response.responseJSON.msgDesc == "XRole already exists"){
                            XAUtil.notifyError('Error', "Role name already exists");
                        } else {
                            XAUtil.notifyError('Error', response.responseJSON.msgDesc);
                        }
                    }else {
                        XAUtil.notifyError('Error', 'Error occurred while creating/updating Role!');
                    }
                }
            });
        },
        onCancel : function(){
            XAUtil.allowNavigation();
            App.appRouter.navigate("#!/users/roletab",{trigger: true});
        },

        /** on close */
        onClose: function(){
        }

    });

    return RoleCreate;
});