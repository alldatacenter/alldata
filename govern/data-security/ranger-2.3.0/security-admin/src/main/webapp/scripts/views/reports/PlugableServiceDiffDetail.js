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

	var Backbone						= require('backbone');
	var XAEnums					 		= require('utils/XAEnums');
	var XALinks							= require('modules/XALinks');
	var XAUtils					 		= require('utils/XAUtils');
	
	var RangerPolicy					= require('models/RangerPolicy');
	var RangerService					= require('models/RangerService');
	var RangerServiceDef				= require('models/RangerServiceDef');
	var PolicyOperationDiff_tmpl 		= require('hbs!tmpl/reports/PlugableServicePolicyDiff_tmpl');
	var PolicyUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/PlugableServicePolicyUpdateDiff_tmpl');
	var PolicyDeleteOperationDiff_tmpl 	= require('hbs!tmpl/reports/PlugableServicePolicyDeleteDiff_tmpl');
	
	var PlugableServiceDiffDetail = Backbone.Marionette.ItemView.extend(
	/** @lends PlugableServiceDiffDetail */
	{
		_viewName : 'PlugableServiceDiffDetail',
		
    	template: PolicyOperationDiff_tmpl,
        templateHelpers :function(){
                var zoneName = !_.isEmpty(this.zoneName) && !_.isUndefined(this.zoneName) ? this.zoneName : false;
        	return {
        			collection : this.collection.models,
        			action	   : this.action,
        			objectName : this.objectName,
        			objectId   : this.objectId,
        			objectCreatedDate : this.objectCreatedDate,
        			objectCreatedBy : this.objectCreatedBy,
					policyName	 : this.policyName,
					policyId	 : this.policyId,
					repositoryType : this.repositoryType,
					newPolicyItems : this.newPolicyItems,
					oldPolicyItems : this.oldPolicyItems,
					newAllowExceptionPolicyItems : this.newAllowExceptionPolicyItems,
					oldAllowExceptionPolicyItems : this.oldAllowExceptionPolicyItems,
					newDenyPolicyItems : this.newDenyPolicyItems,
					oldDenyPolicyItems : this.oldDenyPolicyItems,
					newDenyExceptionPolicyItems : this.newDenyExceptionPolicyItems,
					oldDenyExceptionPolicyItems : this.oldDenyExceptionPolicyItems,
					newMaskPolicyItems: this.newMaskPolicyItems,
					newRowFilterPolicyItems: this.newRowFilterPolicyItems,
					oldMaskPolicyItems: this.oldMaskPolicyItems,
					oldRowFilterPolicyItems: this.oldRowFilterPolicyItems,
					userName   : this.userName,
					newPolicyValidityPeriod: this.newValidityPeriod,
					oldPolicyValidityPeriod: this.oldValidityPeriod,
					zoneName: zoneName,
					newPolicyConditions: this.newConditions,
					oldPolicyCondition: this.oldConditions,
					repoName: this.repoName,

        		};
        },
    	/** ui selector cache */
    	ui: {
    		groupPerm : '.groupPerm',
    		userPerm  : '.userPerm',
    		oldValues : '[data-id="oldValues"]',
    		diff 	  : '[data-id="diff"]',
    		policyDiff: '[data-name="policyDiff"]',
    		policyDetail: '[class="policyDetail"]'
    		
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			return events;
		},

    	/**
		* intialize a new PlugableServiceDiffDetail ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a PlugableServiceDiffDetail ItemView");
                        _.extend(this, _.pick(options, 'classType','objectName','objectId','objectCreatedDate','action','userName','policyId','policyLabels','repoName'));
			this.bindEvents();
			this.initializeServiceDef();
			this.getTemplateForView();
			
		},
		initializeServiceDef : function(){
                        var url, policyName = this.collection.findWhere({'attributeName':'Policy Name'}),
                        zoneName = this.collection.findWhere({'attributeName':'Zone Name'});
                        if((this.action == 'create' || this.action == 'Import Create') && zoneName && !_.isEmpty(zoneName)){
                                this.zoneName = zoneName.get('newValue');
                                this.collection.remove(zoneName);
                        } else if((this.action == 'delete' || this.action == 'update' || this.action == 'Import Delete') && zoneName && !_.isEmpty(zoneName)){
                                this.zoneName = zoneName.get('previousValue');
                                this.collection.remove(zoneName);
                        }
			if(this.action == 'create' || this.action == 'Import Create'){
				this.policyName = policyName.get('newValue');
			} else if(this.action == 'delete'){
				this.policyName = policyName.get('previousValue');
			}
			if(!_.isUndefined(this.collection.models[0]) ){
				this.policyName = _.isUndefined(this.policyName) ? this.collection.models[0].get('objectName') : this.policyName;
//              get policy created/updated date/owner
				var model = this.collection.models[0];
				this.objectCreatedBy = model.get('updatedBy');
			}
		},
		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},
		/** on render callback */
		onRender: function() {
			this.initializePlugins();
			this.removeLastCommaFromSpans(this.$el.find('.policyDetail').find('ol li'));
			this.removeLastCommaFromSpans(this.ui.diff.find('ol li'));
			
			_.each(this.ui.policyDiff.find('ol li'),function(m){
				if(_.isEmpty($(m).text().trim()))
					$(m).removeClass('change-row').text('--');
			});
			//Remove last br from ol
			this.$el.find('.diff-perms').find('.diff-right').find('ol:last').next().remove()
			this.$el.find('.diff-perms').find('.diff-left').find('ol:last').next().remove()
			this.$el.find('.validityPeriod').find('.diff-right').find('ol:last').next().remove()
            this.$el.find('.validityPeriod').find('.diff-left').find('ol:last').next().remove()
			
			var newOl = this.$el.find('.diff-perms').find('.diff-right').find('ol');
			var oldOl = this.$el.find('.diff-perms').find('.diff-left').find('ol');
            var newTimeOl = this.$el.find('.validityPeriod').find('.diff-right').find('ol');
            var oldTimeOl = this.$el.find('.validityPeriod').find('.diff-left').find('ol');
			
			_.each(oldOl, function(ol, i) {
				this.highLightElement($(ol).find('.username'), $(newOl[i]).find('.username'));
                                this.highLightElement($(ol).find('.rolename'), $(newOl[i]).find('.rolename'));
				this.highLightElement($(ol).find('.groupname'), $(newOl[i]).find('.groupname'));
				this.highLightElement($(ol).find('.perm'), $(newOl[i]).find('.perm'));
				this.highLightElement($(ol).find('.condition'), $(newOl[i]).find('.condition'));
				this.highLightElement($(ol).find('.maskingAndRow'), $(newOl[i]).find('.maskingAndRow'));
			},this);

            _.each(oldTimeOl, function(ol, i){
                this.highLightElement($(ol).find('.startTime'), $(newTimeOl[i]).find('.startTime'));
                this.highLightElement($(ol).find('.endTime'), $(newTimeOl[i]).find('.endTime'));
                this.highLightElement($(ol).find('.timeZone'), $(newTimeOl[i]).find('.timeZone'));
			},this);
		},
		removeLastCommaFromSpans : function($el) {
			//remove last comma
			_.each($el,function(m){
				var text = $(m).text().replace(/,(?=[^,]*$)/, '');
				$(m).find('span').last().remove();
			});
		},
		highLightElement : function(oldOlList, newOlList) {
			var removedUsers = this.array_diff(oldOlList, newOlList)
			var addedUsers = this.array_diff(newOlList, oldOlList)
			_.each(removedUsers, function(userSpan) { $(userSpan).addClass('delete-text')});
			_.each(addedUsers, function(userSpan) { $(userSpan).addClass('add-text')});
		},
		array_diff :function(array1, array2){
			var difference = [];
			var tmpArr2 = _.map(array2,function(a){ return (a.innerHTML);})
			$.grep(array1, function(el) {
			        if ($.inArray(el.innerHTML, tmpArr2) == -1){
			        	difference.push(el);
			        } 
			        	
			});
			return difference;
		},
		getTemplateForView : function(){
			if(this.action == 'create' || this.action == 'Import Create'){
				this.template = PolicyOperationDiff_tmpl;
			}else if(this.action == 'update'){
				this.template = PolicyUpdateOperationDiff_tmpl;
			}else{
				this.template = PolicyDeleteOperationDiff_tmpl;
			}
			//prepare data for template
			this.newPolicyItems = null, this.oldPolicyItems = null,
			this.newAllowExceptionPolicyItems = null, this.oldAllowExceptionPolicyItems = null,
			this.newDenyPolicyItems = null, this.oldDenyPolicyItems = null,
			this.newDenyExceptionPolicyItems = null, this.oldDenyExceptionPolicyItems = null;
			this.newMaskPolicyItems = null, this.newRowFilterPolicyItems = null,
			this.oldMaskPolicyItems = null, this.oldRowFilterPolicyItems = null;
			var policyStatus = this.collection.findWhere({'attributeName':'Policy Status'})
			if(!_.isUndefined(policyStatus)){
				if(!_.isEmpty(policyStatus.get('previousValue'))){
					var tmp = this.collection.get(policyStatus.id)
					tmp.set("previousValue", policyStatus.get('previousValue') == "true" ? 'enabled' : 'disabled')
				}
				if(!_.isEmpty(policyStatus.get('newValue'))){
					var tmp = this.collection.get(policyStatus.id)
					tmp.set("newValue", policyStatus.get('newValue') ==  "true" ? 'enabled' : 'disabled')
				}
			}
                        var policyLabels = this.collection.findWhere({'attributeName':'Policy Labels'});
                        if(!_.isUndefined(policyLabels)){
                            if(!_.isEmpty(policyLabels.get('previousValue'))){
                                var resourcepreviousValue = JSON.parse(policyLabels.get('previousValue'));
                                policyLabels.set('previousValue' , resourcepreviousValue.join(', '));
                            }
                            if(!_.isEmpty(policyLabels.get('newValue'))){
                                var resourcenewValue = JSON.parse(policyLabels.get('newValue'));
                                policyLabels.set('newValue' , resourcenewValue.join(', '));
                            }
                        }
			var policyResource = this.collection.findWhere({'attributeName':'Policy Resources'})
			if(!_.isUndefined(policyResource)){
				this.getPolicyResources();
			}
            var policyValiditySchedules = this.collection.findWhere({'attributeName':'Validity Schedules'});
            if(!_.isUndefined(policyValiditySchedules)){
                var validityPeriod = this.getPolicyValiditySchedules('Validity Schedules');
                if(!_.isEmpty(validityPeriod)){
                    this.newValidityPeriod = validityPeriod.newPerms;
                    this.oldValidityPeriod = validityPeriod.oldPerms;
                }
            }
			var policyItems = this.collection.findWhere({'attributeName':'Policy Items'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('Policy Items');
				if(!_.isEmpty(perms)){
					this.newPolicyItems = perms.newPerms;
					this.oldPolicyItems = perms.oldPerms;
				}
			}
			var policyItems = this.collection.findWhere({'attributeName':'Allow Exceptions'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('Allow Exceptions');
				if(!_.isEmpty(perms)){
					this.newAllowExceptionPolicyItems = perms.newPerms;
					this.oldAllowExceptionPolicyItems = perms.oldPerms;
				}
			}
			var policyItems = this.collection.findWhere({'attributeName':'DenyPolicy Items'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('DenyPolicy Items');
				if(!_.isEmpty(perms)){
					this.newDenyPolicyItems = perms.newPerms;
					this.oldDenyPolicyItems = perms.oldPerms;
				}
			}
			var policyItems = this.collection.findWhere({'attributeName':'Deny Exceptions'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('Deny Exceptions');
				if(!_.isEmpty(perms)){
					this.newDenyExceptionPolicyItems = perms.newPerms;
					this.oldDenyExceptionPolicyItems = perms.oldPerms;
				}
			}
			var policyItems = this.collection.findWhere({'attributeName':'Masked Policy Items'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('Masked Policy Items');
				if(!_.isEmpty(perms)){
					this.newMaskPolicyItems = perms.newPerms;
					this.oldMaskPolicyItems = perms.oldPerms;
				}
			}
			var policyItems = this.collection.findWhere({'attributeName':'Row level filter Policy Items'});
			if(!_.isUndefined(policyItems)){
				var perms = this.getPolicyItems('Row level filter Policy Items');
				if(!_.isEmpty(perms)){
					this.newRowFilterPolicyItems = perms.newPerms;
					this.oldRowFilterPolicyItems = perms.oldPerms;
				}
			}
            var policyConditions = this.collection.findWhere({'attributeName':'Policy Conditions'});
            if(!_.isUndefined(policyConditions)){
                var conditions = this.getPolicyCondition(policyConditions);
                if(!_.isEmpty(conditions)){
                    this.newConditions = conditions.newPerms;
                    this.oldConditions = conditions.oldPerms;
                }
            }
		},
		getPolicyResources : function() {
			var policyResources = this.collection.findWhere({'attributeName':'Policy Resources'});
			this.collection.remove(policyResources);
			
			if(!_.isUndefined(policyResources.get('newValue')) && !_.isEmpty(policyResources.get('newValue'))){
				var resources = {} ;
				var resourceNewValues = JSON.parse(policyResources.get('newValue'));
				//for resource  new value
				_.each(resourceNewValues,function(val,key){ 
					resources[key] = val.values.toString();
					resources[key +' exclude'] = val.isExcludes.toString();
					resources[key +' recursive'] = val.isRecursive.toString();
				});
			}
			if(!_.isUndefined(policyResources.get('previousValue')) && !_.isEmpty(policyResources.get('previousValue'))){
				var oldResources = {} ;
				var resourceNewValues = JSON.parse(policyResources.get('previousValue'));
				////for resource  old value
				_.each(resourceNewValues,function(val,key){
					oldResources[key] = val.values.toString();
					oldResources[key +' exclude'] = val.isExcludes.toString();
					oldResources[key +' recursive'] = val.isRecursive.toString();
				});
			}
			if(this.action == "update"){
				//**Show diffview data for resource change at same level.
				var done = false;
				_.each(resources, function(val, key){
					if(_.isUndefined(oldResources[key] && !done)){
						_.each(resources,function(val,key){
							if(!oldResources.hasOwnProperty(key)){
								oldResources[key] = "";
							}
						});
						_.each(oldResources, function(val,key){
							if(!resources.hasOwnProperty(key)){
								resources[key] = "";
							}
						});
						done = true;
					}
				});
			_.each(resources, function(val, key){
				if(val != oldResources[key])
					this.collection.add({'attributeName':key, 'newValue':val.toString(),'previousValue': oldResources[key],type : "Policy Resources"});
			}, this);
			} else if(this.action == "create" || this.action == "Import Create"){
				_.each(resources,function(val, key){ this.collection.add({'attributeName':key, 'newValue':val.toString()}); }, this);
			} else{
				_.each(oldResources,function(val, key){ this.collection.add({'attributeName':key, 'previousValue':val.toString()}); }, this);
			}
		},
        getPolicyValiditySchedules : function(){
            var validityPeriod = {},that = this;
            var validityTime=[], oldValidityTime =[];
            var validitySchedules = this.collection.findWhere({'attributeName':'Validity Schedules'});
            this.collection.remove(validitySchedules);
            if(!_.isUndefined(validitySchedules.get('newValue')) && !_.isEmpty(validitySchedules.get('newValue'))){
                var validityTimeNewValues = JSON.parse(validitySchedules.get('newValue'));
            }
            if(!_.isUndefined(validitySchedules.get('previousValue')) && !_.isEmpty(validitySchedules.get('previousValue'))){
                var oldvalidityTime = {} ;
                var validityTimePreviousValue = JSON.parse(validitySchedules.get('previousValue'));
            }
            if(this.action == "update"){
                return this.setOldNewPermDiff(validityTimeNewValues, validityTimePreviousValue);
            } else {
                return {'oldPerms' : validityTimePreviousValue, 'newPerms' : validityTimeNewValues};
            }
        },

        getPolicyCondition : function(policyConditions) {
            var conditionNewValues = [], conditionOldValues = [] ;
            this.collection.remove(policyConditions);
        	if(!_.isUndefined(policyConditions.get('newValue')) && !_.isEmpty(policyConditions.get('newValue'))){
                conditionNewValues = JSON.parse(policyConditions.get('newValue'));
            }
            if(!_.isUndefined(policyConditions.get('previousValue')) && !_.isEmpty(policyConditions.get('previousValue'))){
                var conditionOldValues = JSON.parse(policyConditions.get('previousValue'));
            }
            if(this.action == "update"){
                return this.setOldNewPermDiff(conditionNewValues, conditionOldValues);
            } else {
                return {'oldPerms' : conditionOldValues, 'newPerms' : conditionNewValues};
            }
        },

		getPolicyItems : function(itemType) {
			var items = {},that = this;
			var newPolicyItems=[], oldPolicyItems =[];
			var policyItems = this.collection.findWhere({'attributeName': itemType });
			this.collection.remove(policyItems);
			if(!_.isUndefined(policyItems.get('newValue')) && !_.isEmpty(policyItems.get('newValue'))){
				newPolicyItems = JSON.parse(policyItems.get('newValue'));
				_.each(newPolicyItems, function(obj){
					if(!_.isUndefined(obj.accesses)){
						var permissions = _.map(_.where(obj.accesses,{'isAllowed':true}), function(t) { return t.type; });
						obj['permissions'] = permissions;
						obj['delegateAdmin'] = obj.delegateAdmin ? 'enabled' : 'disabled';
					}
				});
			}
			if(!_.isUndefined(policyItems.get('previousValue')) && !_.isEmpty(policyItems.get('previousValue'))){
				oldPolicyItems = JSON.parse(policyItems.get('previousValue'));
				_.each(oldPolicyItems, function(obj){
					if(!_.isUndefined(obj.accesses)){
						var permissions = _.map(_.where(obj.accesses,{'isAllowed':true}), function(t) { return t.type; });
						obj['permissions'] = permissions;
						obj['delegateAdmin'] = obj.delegateAdmin ? 'enabled' : 'disabled';
					}
				});
			}
			if(itemType === 'Masked Policy Items') {
		//   its for new created record  
					for(var i = 0; i < newPolicyItems.length ; i++){
						if(newPolicyItems[i].DataMasklabel && newPolicyItems[i].DataMasklabel == "Custom"){
						var maskingType = newPolicyItems[i].dataMaskInfo.dataMaskType;
						newPolicyItems[i].dataMaskInfo.dataMaskType = newPolicyItems[i].DataMasklabel +' : '+newPolicyItems[i].dataMaskInfo.valueExpr;
						}else if(newPolicyItems[i].DataMasklabel){
							var maskingType = newPolicyItems[i].dataMaskInfo.dataMaskType;
							newPolicyItems[i].dataMaskInfo.dataMaskType = newPolicyItems[i].DataMasklabel;
						}
					}
					
					for(var i = 0; i < oldPolicyItems.length ; i++){
						if(oldPolicyItems[i].DataMasklabel && oldPolicyItems[i].DataMasklabel == "Custom"){
							var maskingType = oldPolicyItems[i].dataMaskInfo.dataMaskType;
							oldPolicyItems[i].dataMaskInfo.dataMaskType = oldPolicyItems[i].DataMasklabel +' : '+oldPolicyItems[i].dataMaskInfo.valueExpr;
						}else if(oldPolicyItems[i].DataMasklabel){
							var maskingType = oldPolicyItems[i].dataMaskInfo.dataMaskType;
							oldPolicyItems[i].dataMaskInfo.dataMaskType = oldPolicyItems[i].DataMasklabel;
						}
					}
			}

			if(this.action == "update"){
				return this.setOldNewPermDiff(newPolicyItems, oldPolicyItems);
			} else {
				return {'oldPerms' : oldPolicyItems, 'newPerms' : newPolicyItems};
			}
		},
		setOldNewPermDiff: function(newPolicyItems, oldPolicyItems){
			var oldPerms = [], newPerms = [];
			var len = oldPolicyItems.length > newPolicyItems.length ? oldPolicyItems.length : newPolicyItems.length;
			for(var i = 0; i < len ; i++) {
				if (JSON.stringify(newPolicyItems[i]) != JSON.stringify(oldPolicyItems[i])) {
					oldPerms.push(oldPolicyItems[i]);
					newPerms.push(newPolicyItems[i]);
				}
			}
			return {'newPerms': newPerms, 'oldPerms': oldPerms};
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		/** on close */
		onClose: function(){
		}

	});

	return PlugableServiceDiffDetail;
});
