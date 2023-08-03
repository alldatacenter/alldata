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

    var RangerZoneBase = require('model_bases/RangerZoneBase');
    var XAUtil = require('utils/XAUtils');
    var XAEnums = require('utils/XAEnums');
    var localization = require('utils/XALangSupport');


    var RangerZone = RangerZoneBase.extend(
        /** @lends RangerZone.prototype */
        {
            /**
             * RangerZone initialize method
             * @augments RangerZoneBase
             * @constructs
             */
            initialize: function() {
                this.modelName = 'RangerZone';
                this.bindErrorEvents();
            },
            /**
             * @function schema
             * This method is meant to be used by UI,
             * by default we will remove the unrequired attributes from serverSchema
             */

            schemaBase: function() {
                var attrs = {}
                return _.extend(attrs, {
                    name: {
                        type: 'Text',
                        title: 'Zone Name *',
                        validators: ['required',{type:'regexp',regexp:/^[a-zA-Z0-9_-][a-zA-Z0-9\s_-]{0,254}$/, message : localization.tt("validationMessages.nameValidationMsg")}],
                        editorAttrs: {
                            maxlength: 255
                        },
                    },
                    description: {
                        type: 'TextArea',
                        title: 'Description',
                        validators: []
                    },
                    tagServices : {
                        type : 'Select2Remote',
                        title : 'Select Tag Services',
                        pluginAttr : XAUtil.getTagBaseServices(),
                        options    : function(callback, editor){
                            callback();
                        },
                        onFocusOpen : false
                    },
                    adminUsers: {
                        type: 'Select2Remote',
                        title: 'Admin Users',
                        pluginAttr: this.getPluginAttr(false, 'adminUsers'),
                        options: function(callback, editor) {
                            callback();
                        },
                        validators: [
                            function validate(value, formValues) {
                                var err = {
                                    type: 'adminUsers',
                                    message: ''
                                };
                                if(_.isEmpty(value) && _.isEmpty(formValues.adminUserGroups)) {
                                    return err;
                                } 
                            }
                        ] 
                    },
                    adminUserGroups: {
                        type: 'Select2Remote',
                        title: 'Admin Usergroups',
                        pluginAttr: this.getPluginAttr(true, 'adminUserGroups'),
                        options: function(callback, editor) {
                            callback();
                        },
                        validators: [
                            function validate(value, formValues) {
                                var err = {
                                    type: 'adminUsers',
                                    message: 'Please provide atleast one admin user or group!'
                                };
                                if(_.isEmpty(value) && _.isEmpty(formValues.adminUsers)) {
                                  return err;  
                                } 
                            }
                        ]
                    },
                    auditUsers: {
                        type: 'Select2Remote',
                        title: 'Audit Users',
                        pluginAttr: this.getPluginAttr(false, 'auditUsers'),
                        options: function(callback, editor) {
                            callback();
                        },
                        validators: [
                            function validate(value, formValues) {
                                var err = {
                                    type: 'auditUsers',
                                    message: ''
                                };
                                if(_.isEmpty(value) && _.isEmpty(formValues.auditUserGroups)) {
                                    return err;
                                } 
                            }
                        ]
                    },
                    auditUserGroups: {
                        type: 'Select2Remote',
                        title: 'Audit Usergroups',
                        pluginAttr: this.getPluginAttr(true, 'auditUserGroups'),
                        options: function(callback, editor) {
                            callback();
                        },
                        validators: [
                            function validate(value, formValues) {
                                var err = {
                                    type: 'auditUserGroups',
                                    message: 'Please provide atleast one audit user or group!'
                                };
                                if(_.isEmpty(value) && _.isEmpty(formValues.auditUsers)) {
                                    return err;
                                } 
                            }
                        ]
                    },
                })

            },
            getPluginAttr: function(typeGroup, $select) {
                var that = this,
                    tags = [],
                    placeholder = (typeGroup) ? 'Select Group' : 'Select User',
                    searchUrl = (typeGroup) ? "service/xusers/lookup/groups" : "service/xusers/lookup/users";
                _.each(this.get($select), function(name) {
                    tags.push({
                        'id': _.escape(name),
                        'text': _.escape(name)
                    });
                });
                return {
                    closeOnSelect: true,
                    placeholder: placeholder,
                    // width: '500px',
                    tokenSeparators: [",", " "],
                    tags: true,
                    initSelection: function(element, callback) {
                        callback(tags);
                    },
                    ajax: {
                        url: searchUrl,
                        dataType: 'json',
                        data: function(term, page) {
                            return {
                                name: term,
                                isVisible : XAEnums.VisibilityStatus.STATUS_VISIBLE.value,
                            };
                        },
                        results: function(data, page) {
                            var results = [],
                                selectedVals = [];
                            //Get selected values of groups/users dropdown
                            // selectedVals = that.getSelectedValues(typeGroup);
                            if (data.totalCount != "0") {
                                if (typeGroup) {
                                    results = data.vXStrings.map(function(m) {
                                        return {
                                            id: _.escape(m.value),
                                            text: _.escape(m.value)
                                        };
                                    });
                                } else {
                                    results = data.vXStrings.map(function(m) {
                                        return {
                                            id: _.escape(m.value),
                                            text: _.escape(m.value)
                                        };
                                    });
                                }
                                if (!_.isEmpty(selectedVals)) {
                                    results = XAUtil.filterResultByText(results, selectedVals);
                                }
                                return {
                                    results: results
                                };
                            }
                            return {
                                results: results
                            };
                        },
                        transport: function(options) {
                            $.ajax(options).fail(function(respones) {
                                XAUtil.defaultErrorHandler('error', respones);
                                this.success({
                                    resultSize: 0
                                });
                            });
                        }
                    },
                    formatResult: function(result) {
                        return result.text;
                    },
                    formatSelection: function(result) {
                        return result.text;
                    },
                    formatNoMatches: function(result) {
                        return typeGroup ? 'No group found.' : 'No user found.';
                    }

                }
            },
            toString : function(){
                return this.get('name');
            },
        }, {
            // static class members
        });

    return RangerZone;

});