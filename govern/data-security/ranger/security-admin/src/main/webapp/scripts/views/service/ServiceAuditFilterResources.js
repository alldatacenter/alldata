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
    var XAEnums = require('utils/XAEnums');
    var localization = require('utils/XALangSupport');
    var XAUtil = require('utils/XAUtils');
    var RangerPolicyResource = require('models/RangerPolicyResource');
    var BackboneFormDataType = require('models/BackboneFormDataType');
    require('backbone-forms.list');
    require('backbone-forms.templates');
    require('backbone-forms');
    require('backbone-forms.XAOverrides');
    require('jquery-ui');
    require('tag-it');

    var ServiceAuditFilterResources = Backbone.Form.extend(
        /** @lends ServiceAuditFilterResources */
        {
            _viewName: 'ServiceAuditFilterResources',

            /**
             * intialize a new ServiceAuditFilterResources Form View
             * @constructs
             */

            templateData: function() {
                var policyType = XAUtil.enumElementByValue(XAEnums.RangerPolicyType, this.model.get('policyType'));
                return {
                    'id': this.model.id,
                    'policyType': policyType.label
                };
            },

            initialize: function(options) {
                console.log("initialized a ServiceAuditFilterResources Form View");
                _.extend(this, _.pick(options, 'rangerServiceDefModel', 'serviceName'));
                this.setupForm();
                Backbone.Form.prototype.initialize.call(this, options);
                this.bindEvents();
                this.defaultValidator = {}
            },

            /** all events binding here */
            bindEvents: function() {
                this.on('policyForm:parentChildHideShow', this.renderParentChildHideShow);
            },

            ui: {},

            /** fields for the form
             */
            fields: [],

            schema: function() {
                return this.getSchema();
            },

            getSchema: function() {
                var attrs = {},
                    that = this;
                var formDataType = new BackboneFormDataType();
                attrs = formDataType.getFormElements(this.rangerServiceDefModel.get('resources'), this.rangerServiceDefModel.get('enums'), attrs, this, true);
                return attrs;
            },

            /** on render callback */
            render: function(options) {
                var that = this;
                Backbone.Form.prototype.render.call(this, options);
                //initialize path plugin for hdfs component : resourcePath
                if (!_.isUndefined(this.initilializePathPlugin) && this.initilializePathPlugin) {
                    this.initializePathPlugins(this.pathPluginOpts);
                }
                //checkParent
                this.renderParentChildHideShow();
            },

            setupForm: function() {
                if (!this.model.isNew()) {
                    this.selectedResourceTypes = {};
                    var resourceDefList = this.rangerServiceDefModel.get('resources');
                    _.each(this.model.get('resources'), function(obj, key) {
                        var resourceDef = _.findWhere(resourceDefList, {
                                'name': key
                        }),
                        sameLevelResourceDef = [],
                        parentResource;
                        sameLevelResourceDef = _.filter(resourceDefList, function(objRsc){
                            if (objRsc.level === resourceDef.level && objRsc.parent === resourceDef.parent) {
                                return objRsc
                            }
                        });
                        //for parent leftnode status
                        if (resourceDef.parent) {
                            parentResource = _.findWhere(resourceDefList, {
                                'name': resourceDef.parent
                            });
                        }
                        if (sameLevelResourceDef.length == 1 && !_.isUndefined(sameLevelResourceDef[0].parent) &&
                            !_.isEmpty(sameLevelResourceDef[0].parent) &&
                            parentResource.isValidLeaf) {
                            sameLevelResourceDef.push({
                                'level': sameLevelResourceDef[0].level,
                                name: 'none',
                                label: 'none'
                            });
                        }
                        if (sameLevelResourceDef.length > 1) {
                            obj['resourceType'] = key;
                            if (_.isUndefined(resourceDef.parent)) {
                                this.model.set('sameLevel' + resourceDef.level, obj);
                                //parentShowHide
                                this.selectedResourceTypes['sameLevel' + resourceDef.level] = key;
                            } else {
                                this.model.set('sameLevel' + resourceDef.level + resourceDef.parent, obj);
                                this.selectedResourceTypes['sameLevel' + resourceDef.level + resourceDef.parent] = key;
                            }

                        } else {
                            this.model.set(resourceDef.name, obj)
                        }
                    }, this);
                }
            },

            /** all custom field rendering */
            renderParentChildHideShow: function(onChangeOfSameLevelType, val, e) {
                var formDiv = this.$el.find('.form-resources');
                if (!this.model.isNew() && !onChangeOfSameLevelType) {
                    _.each(this.selectedResourceTypes, function(val, sameLevelName) {
                        if (formDiv.find('.field-' + sameLevelName).length > 0) {
                            formDiv.find('.field-' + sameLevelName).attr('data-name', 'field-' + val)
                        }
                    });
                }
                //hide form fields if it's parent is hidden
                var resources = formDiv.find('.form-group');
                _.each(resources, function(rsrc, key) {
                    var parent = $(rsrc).attr('parent');
                    var label = $(rsrc).find('label').html();
                    $(rsrc).removeClass('error');
                    //remove validation and Required msg
                    $(rsrc).find('.help-inline').empty();
                    $(rsrc).find('.help-block').empty();
                    if (!_.isUndefined(parent) && !_.isEmpty(parent)) {
                        var selector = "div[data-name='field-" + parent + "']";
                        var kclass = formDiv.find(selector).attr('class');
                        var label = $(rsrc).find('label').html();
                        if (_.isUndefined(kclass) || (kclass.indexOf("field-sameLevel") >= 0 &&
                                formDiv.find(selector).find('select').val() != parent) ||
                            formDiv.find(selector).hasClass('hideResource')) {
                            $(rsrc).addClass('hideResource');
                            $(rsrc).removeClass('error');
                            //reset input field to "none" if it is hide
                            var resorceFieldName = _.pick(this.schema, this.selectedFields[key]);
                            if (resorceFieldName[this.selectedFields[key]].sameLevelOpts && _.contains(resorceFieldName[this.selectedFields[key]].sameLevelOpts, 'none') &&
                                formDiv.find(selector).find('select').val() != 'none' && onChangeOfSameLevelType) {
                                //change trigger and set value to none
                                $(rsrc).find('select').val("none").trigger('change', "onChangeResources");
                            }
                        } else {
                            if ($(rsrc).find('select').val() == 'none') {
                                $(rsrc).find('input[data-js="resource"]').select2('disable');
                                $(rsrc).removeClass('error');
                                $(rsrc).find('label').html(label.split('*').join(''));
                            } else {
                                $(rsrc).find('input[data-js="resource"]').select2('enable');
                                if (label.slice(-2) == '**') {
                                    $(rsrc).find('label').html(label.slice(-1) == '*' ? label : label + "*");
                                }
                            }
                            $(rsrc).removeClass('hideResource');
                        }
                    }
                }, this);
                //remove validation of fields if it's hidden
                //remove validation if fields is not empty
                _.each(this.fields, function(field, key) {
                    if ((key.substring(0, key.length - 2) === "sameLevel") && field.$el.find('[data-js="resource"]').val() != "" && field.$el.hasClass('error')) {
                        field.$el.removeClass('error');
                        field.$el.find('.help-inline').empty();
                    }
                    if (field.$el.hasClass('hideResource')) {
                        if ($.inArray('required', field.editor.validators) >= 0) {
                            this.defaultValidator[key] = field.editor.validators;
                            field.editor.validators = [];
                            var label = field.$el.find('label').html();
                            field.$el.find('label').html(label.replace('*', ''));
                            field.$el.removeClass('error');
                            field.$el.find('.help-inline').empty();
                        }
                    } else {
                        if (field.$el.find('select').val() == 'none') {
                            field.editor.validators = [];
                            this.defaultValidator[key] = field.editor.validators;
                            field.$el.removeClass('error');
                            field.$el.find('.help-inline').empty();
                        } else {
                            if (!_.isUndefined(this.defaultValidator[key])) {
                                field.editor.validators.push('required');
                                if ($.inArray('required', field.editor.validators) >= 0) {
                                    var label = field.$el.find('label').html();
                                    field.$el.find('label').html(label.slice(-1) == '*' ? label : label + "*");
                                }
                            }
                        }
                    }
                }, this);
            },

            /** all post render plugin initialization */
            initializePathPlugins: function(options) {
                var that = this,
                    defaultValue = [];
                if (!this.model.isNew() && !_.isUndefined(this.model.get('path'))) {
                    defaultValue = this.model.get('path').values;
                }
                var tagitOpts = {}
                if (!_.isUndefined(options.lookupURL) && options.lookupURL) {
                    tagitOpts["autocomplete"] = {
                        cache: false,
                        source: function(request, response) {
                            var url = "service/plugins/services/lookupResource/" + that.serviceName;
                            var context = {
                                'userInput': request.term,
                                'resourceName': that.pathFieldName,
                                'resources': {}
                            };
                            var val = that.fields[that.pathFieldName].editor.getValue('pathField');
                            context.resources[that.pathFieldName] = _.isNull(val) || _.isEmpty(val) ? [] : val.resource;
                            var p = $.ajax({
                                url: url,
                                type: "POST",
                                data: JSON.stringify(context),
                                dataType: 'json',
                                contentType: "application/json; charset=utf-8",
                            }).done(function(data) {
                                if (data) {
                                    response(data);
                                } else {
                                    response();
                                }
                            }).fail(function(responses) {
                                if (responses && responses.responseJSON && responses.responseJSON.msgDesc) {
                                    XAUtil.notifyError('Error', responses.responseJSON.msgDesc);
                                } else {
                                    XAUtil.notifyError('Error', localization.tt('msg.resourcesLookup'));
                                }
                                response();
                            });
                            setTimeout(function() {
                                p.abort();
                                console.log('connection timeout for resource path request...!!');
                            }, 10000);
                        },
                        open: function() {
                            $(this).removeClass('working');
                        },
                        search: function() {
                            $('.tagit-autocomplete').addClass('tagit-position');
                            if(!_.isUndefined(this.value) && this.value.includes('||')) {
                                var values = this.value.trim().split('||');
                                if (values.length > 1) {
                                    for (var i = 0; i < values.length; i++) {
                                        that.fields[that.pathFieldName].editor.$el.find('[data-js="resource"]').tagit("createTag", values[i]);
                                    }
                                    return ''
                                } else {
                                    return val
                                }
                            }
                        },
                    }
                }
                tagitOpts['beforeTagAdded'] = function(event, ui) {
                    // do something special
                    that.fields[that.pathFieldName].$el.removeClass('error');
                    that.fields[that.pathFieldName].$el.find('.help-inline').html('');
                    var tags = [];
                    if (!_.isUndefined(options.regExpValidation) && !options.regExpValidation.regexp.test(ui.tagLabel)) {
                        that.fields[that.pathFieldName].$el.addClass('error');
                        that.fields[that.pathFieldName].$el.find('.help-inline').html(options.regExpValidation.message);
                        return false;
                    }
                }
                tagitOpts['singleFieldDelimiter'] = '||';
                tagitOpts['singleField'] = false;
                this.fields[that.pathFieldName].editor.$el.find('[data-js="resource"]').tagit(tagitOpts).on('change', function(e) {
                    //check dirty field for tagit input type : `path`
                    XAUtil.checkDirtyField($(e.currentTarget).val(), defaultValue.toString(), $(e.currentTarget));
                });
            },

            getPlugginAttr: function(autocomplete, options) {
                var that = this,
                    type = options.containerCssClass,
                    validRegExpString = true,
                    select2Opts = [];
                if (!autocomplete)
                    return {
                        tags: true,
                        width: '220px',
                        multiple: true,
                        minimumInputLength: 1,
                        'containerCssClass': type
                    };
                else {
                    select2Opts = {
                        containerCssClass: options.type,
                        closeOnSelect: true,
                        tags: true,
                        multiple: true,
                        width: '220px',
                        tokenSeparators: [" "],
                        initSelection: function(element, callback) {
                            var data = [];
                            //this is form multi-select value
                            if(_.isArray(JSON.parse(element.val()))) {
                                $(JSON.parse(element.val())).each(function () {
                                    data.push({id: this, text: this});
                                })
                            }
                            callback(data);
                        },
                        createSearchChoice: function(term, data) {
                            term = _.escape(term);
                            if ($(data).filter(function() {
                                    return this.text.localeCompare(term) === 0;
                                }).length === 0) {
                                if (!_.isUndefined(options.regExpValidation) && !options.regExpValidation.regexp.test(term)) {
                                    validRegExpString = false;
                                } else if ($.inArray(term, this.val()) >= 0) {
                                    return null;
                                } else {
                                    return {
                                        id: "<b><i class='text-muted-select2'>Create</i></b> " + term,
                                        text: term
                                    };
                                }
                            }
                        },
                        ajax: {
                            url: options.lookupURL,
                            type: 'POST',
                            params: {
                                timeout: 10000,
                                contentType: "application/json; charset=utf-8",
                            },
                            cache: false,
                            data: function(term, page) {
                                return that.getDataParams(term, options);
                            },
                            results: function(data, page) {
                                var results = [];
                                if (!_.isUndefined(data)) {
                                    if (_.isArray(data) && data.length > 0) {
                                        results = data.map(function(m, i) {
                                            return {
                                                id: m,
                                                text: m
                                            };
                                        });
                                    }
                                    if (!_.isUndefined(data.resultSize) && data.resultSize != "0") {
                                        results = data.vXStrings.map(function(m, i) {
                                            return {
                                                id: m.value,
                                                text: m.value
                                            };
                                        });
                                    }
                                }
                                return {
                                    results: results
                                };
                            },
                            transport: function(options) {
                                $.ajax(options).fail(function(response) {
                                    if (response && response.responseJSON && response.responseJSON.msgDesc) {
                                        XAUtil.notifyError('Error', response.responseJSON.msgDesc);
                                    } else {
                                        XAUtil.notifyError('Error', localization.tt('msg.resourcesLookup'));
                                    }
                                    this.success({
                                        resultSize: 0
                                    });
                                });
                            }

                        },
                        formatResult: function(result) {
                            return result.id;
                        },
                        formatSelection: function(result) {
                            return result.text;
                        },
                        formatNoMatches: function(term) {
                            if (!validRegExpString && !_.isUndefined(options.regExpValidation)) {
                                return options.regExpValidation.message;
                            }
                            return "No Matches found";
                        }
                    };
                    //To support single value input
                    if (!_.isUndefined(options.singleValueInput) && options.singleValueInput) {
                        select2Opts['maximumSelectionSize'] = 1;
                    }
                    return select2Opts;
                }
            },

            getDataParams: function(term, options) {
                var resources = {},
                    resourceName = options.type,
                    dataResources = {};
                var isParent = true,
                    name = options.type,
                    val = null,
                    isCurrentSameLevelField = true;
                while (isParent) {
                    var currentResource = _.findWhere(this.rangerServiceDefModel.get('resources'), {
                        'name': name
                    });
                    //same level type
                    if (_.isUndefined(this.fields[currentResource.name])) {
                        if (!_.isUndefined(currentResource.parent)) {
                            var sameLevelName = 'sameLevel' + currentResource.level + currentResource.parent;
                        } else {
                            var sameLevelName = 'sameLevel' + currentResource.level;
                        }

                        name = this.fields[sameLevelName].editor.$resourceType.val()
                        val = this.fields[sameLevelName].getValue();
                        if (isCurrentSameLevelField) {
                            resourceName = name;
                        }
                    } else {
                        val = this.fields[name].getValue();
                    }
                    resources[name] = _.isNull(val) ? [] : val.resource;
                    if (!_.isEmpty(currentResource.parent)) {
                        name = currentResource.parent;
                    } else {
                        isParent = false;
                    }
                    isCurrentSameLevelField = false;
                }
                if(resources && !_.isEmpty(resources)) {
                    _.each(resources, function(val, key) {
                        dataResources[key] = _.map(val, function(obj){
                            return obj.text
                        })
                    })
                }
                var context = {
                    'userInput': term,
                    'resourceName': resourceName,
                    'resources': dataResources
                };
                return JSON.stringify(context);
            },
        });
    return ServiceAuditFilterResources;
});