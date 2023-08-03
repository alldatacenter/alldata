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

 /*
 *
 */
define(function(require) {
    'use strict';
    
	var Backbone		= require('backbone');
	var Vent = require('modules/Vent');
	var XAUtils		= require('utils/XAUtils');

	var ZoneResourceForm = require('views/security_zone/ZoneResourceForm');

	var ResourceItem = Backbone.Marionette.ItemView.extend({
		_msvName : 'ResourceItem',
		template : require('hbs!tmpl/policies/ResourceItem_tmpl'),
		ui : {
			'deleteBtn' : '[data-action="delete"]'
		},
		events : {
			'click [data-action="delete"]'	: 'evDelete',

		},

		initialize : function(options) {
			_.extend(this, _.pick(options,'rangerPolicyModel', 'rangerServiceDefModel','rangerService','storeResourceRef'));
		},
 
		onRender : function() {
			this.renderPolicyResource();
			if(this.model.collection.length === 1){
				this.ui.deleteBtn.hide();
			}else{
				this.ui.deleteBtn.show();
			}

			
		},
		renderPolicyResource : function (){
			this.resourceForm = new ZoneResourceForm({
				model: this.model,
				rangerServiceDefModel: this.rangerServiceDefModel,
				rangerService: this.rangerService,
				isPolicyResource: true
			});
			this.resourceForm.render();
			this.$('[data-customfields="resource-item"]').html(this.resourceForm.el);
			this.model.attributes.resourceForm = this.resourceForm;
			var resourceItemIndex = this.model && this.model.collection.indexOf(this.model);
			var resourceName = this.getLastResourceName()
			Vent.trigger('resourceType:change', { changeType : 'resourceType', value : resourceName, resourceName : resourceName, resourceItemIndex : resourceItemIndex, action : 'added' });

		},
		evDelete : function(){
			var resourceItemIndex = this.model && this.model.collection.indexOf(this.model);
			Vent.trigger('resourceType:change', { resourceItemIndex : resourceItemIndex, action : 'removed' });
			this.collection.remove(this.model);
		},
		getResourceName : function (resource, resourceDef){
			var found = _.findWhere(resourceDef,{'parent': resource.name });
			if(found){
				return this.getResourceName(found, resourceDef);
			}else {
				return resource.name;
			}
		},
		getLastResourceName : function (){
			var resourceDef = XAUtils.policyTypeResources(this.rangerServiceDefModel, this.model.get('policyType'));
			return this.getResourceName(resourceDef[0], resourceDef);
		}

	});

	return Backbone.Marionette.CompositeView.extend({
		_msvName : 'PermissionItemList',
		template : require('hbs!tmpl/policies/ResourceList_tmpl'),
		templateHelpers: function() {
			return { resourceCnt : this.collection.length };
		},
		getItemView : function(item){
			if(!item){
				return;
			}
			return ResourceItem;
		},
		itemViewContainer : ".js-formInput",
		itemViewOptions : function() {
			return {
				'collection' 	: this.collection,
				'rangerPolicyModel' : this.rangerPolicyModel,
				'rangerServiceDefModel' : this.rangerServiceDefModel,
                'rangerService' : this.rangerService,
			};
		},
		ui : {
			'showAllResourceChbox' : '[data-js="showAllResourceChbox"]',
		},
		events : {
			'click [data-action="addGroup"]' : 'addNew',
			'click [data-js="showAllResourceChbox"]' : 'onShowAllResource'
		},
		collectionEvents: {
			'add': 'onAdd',
			'remove': 'onRemove'
		},
		initialize : function(options) {
			_.extend(this, _.pick(options, 'rangerPolicyModel', 'rangerServiceDefModel','rangerService'));
			if(this.collection.length == 0){
				this.collection.add(new Backbone.Model({ policyType: this.rangerPolicyModel.get('policyType') }))
			}
			this.storeResourceRef = {};
			this.hideFirstResource = false;
		},
		onRender : function(){
			if(this.hideFirstResource){
				this.$el.find('.additional-resource:not(:first)').hide();
				this.hideFirstResource = false;
			}

		},

		addNew : function(){
			if (!this.ui.showAllResourceChbox.prop("checked")) {
				this.ui.showAllResourceChbox.trigger('click');
			}
			this.collection.add(new Backbone.Model({ policyType: this.rangerPolicyModel.get('policyType') }));
		},
		onShowAllResource : function(){

			if (this.ui.showAllResourceChbox.prop("checked")) {
				this.$el.find('.additional-resource').show();
			}else {
				this.$el.find('.additional-resource:not(:first)').hide()
			}
		},
		onAdd : function (){
			this.$el.find('[data-js="resource-count"]').html(this.collection.length);
			this.toggleDeleteBtn();
		},
		onRemove : function (){
			this.onAdd();
			this.toggleDeleteBtn();
		},
		toggleDeleteBtn : function (){
			if(this.collection.length === 1){
				this.$el.find('.additional-resource .btn-danger').hide();
			}else {
				this.$el.find('.additional-resource .btn-danger').show();
			}
		}
	});

});
