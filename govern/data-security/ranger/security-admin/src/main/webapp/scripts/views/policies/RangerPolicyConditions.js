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
    var XAUtil          = require('utils/XAUtils');
    var XAEnums         = require('utils/XAEnums');
    var XALinks         = require('modules/XALinks');
    var PolicyConditionsItem = require('hbs!tmpl/policies/PolicyConditionsItem_tmpl');

    var PolicyConditionsItem = Backbone.Marionette.ItemView.extend({
        _msvName : 'PolicyConditionsItem',
        template : require('hbs!tmpl/policies/PolicyConditionsItem_tmpl'),
        templateHelpers : function(){
            return {
                policyConditions : this.rangerServiceDefModel.get('policyConditions')
            }
        },

        ui : {},

        events : function(){
            var events = {};
            return events;
        },

        initialize : function(options) {
            _.extend(this, _.pick(options,'rangerServiceDefModel','model'));
        },

        onRender : function() {
            var that = this;
            if(this.model && this.model.get('conditions') && !_.isEmpty(this.model.get('conditions'))){
                var that = this;
                _.each(this.$el.find('[data-id="textAreaContainer"]'), function(e, context){
                    var inputFieldName = e.name;
                    that.model.get('conditions').find(function(m){
                        if(m.type == inputFieldName){
                            e.value = m.values;
                        }
                    })
                });
                that.$el.find('[data-id="inputField"]').val([" "]);
            }
            _.each(this.$el.find('[data-id="inputField"]'), function(e, context){
                var inputFieldName = e.name , tag= [], option = {};
                var isSingleValueInput = XAUtil.isSinglevValueInput(that.rangerServiceDefModel.get('policyConditions').find(function(e)
                    {return e.name == inputFieldName}));
                if(that.model && that.model.get('conditions') && !_.isEmpty(that.model.get('conditions'))){
                    that.model.get('conditions').find(function(m){
                        if(m.type == inputFieldName){
                            tag = _.map(m.values.filter(Boolean), function(val){
                                if(!_.isEmpty(val)){
                                    return{'id':_.escape(val), 'text':_.escape(val)}
                                }
                            });
                        }
                    })
                }
                option = {
                    closeOnSelect : true,
                    data:[],
                    width :'450px',
                    allowClear: true,
                    tokenSeparators: ["," , " "],
                    minimumInputLength: 1,
                    multiple: true,
                    initSelection : function (element, callback) {
                        callback(tag);
                    },
                    createSearchChoice: function(term, data) {
                        term = _.escape(term);
                        if ($(data).filter(function() {
                            return this.text.localeCompare(term) === 0;
                        }).length === 0) {
                            if($.inArray(term, this.val()) >= 0){
                                return null;
                            }else{
                                return {
                                    id : term,
                                    text: term
                                };
                            }
                        }
                    },
                }
                if(isSingleValueInput){
                    option['maximumSelectionSize'] = 1;
                }
                that.$el.find('[name='+inputFieldName+']').select2(option);
            });
        }
    });
    return PolicyConditionsItem;
});