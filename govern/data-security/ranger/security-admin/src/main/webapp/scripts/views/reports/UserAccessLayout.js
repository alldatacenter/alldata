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

 
define(function(require) {'use strict';

	var Backbone 			= require('backbone');
	var XALinks 			= require('modules/XALinks');
	var XAEnums 			= require('utils/XAEnums');
	var XAUtil				= require('utils/XAUtils');
	var XABackgrid			= require('views/common/XABackgrid');
	var App    				= require('App');
	var XATableLayout		= require('views/common/XATableLayout');
	var localization		= require('utils/XALangSupport');
	var XAGlobals			= require('utils/XAGlobals');
	var CustomSubgrid 		= require('views/common/CustomSubgrid');
	var RangerService		= require('models/RangerService');
	var RangerServiceDefList= require('collections/RangerServiceDefList');
	var RangerPolicyList	= require('collections/RangerPolicyList');
	var UseraccesslayoutTmpl= require('hbs!tmpl/reports/UserAccessLayout_tmpl');
	var SessionMgr  	= require('mgrs/SessionMgr');
	var RangerZoneBase = require('model_bases/RangerZoneBase');
	var UserAccessLayout 	= Backbone.Marionette.Layout.extend(
	/** @lends UserAccessLayout */
	{
		_viewName : 'UserAccessLayout',

		template : UseraccesslayoutTmpl,
		breadCrumbs : [XALinks.get('UserAccessReport')],
		templateHelpers :function(){
			return {
				groupList : this.groupList,
				policyHeaderList : this.policyCollList,
				showExportJson : (XAUtil.isAuditorOrKMSAuditor(SessionMgr)) ? false : true,
				setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			};
		},

		/** Layout sub regions */
		regions :function(){
			var regions = {};
			this.initializeRequiredData();
			_.each(this.policyCollList, function(obj) {
				regions[obj.collName+'Table'] =  'div[data-id="'+obj.collName+'"]';
			},this)
			
			return regions;
		},

		/** ui selector cache */
		ui : {
			userGroup 			: '[data-js="selectGroups"]',
			searchBtn 			: '[data-js="searchBtn"]',
			userName 			: '[data-js="userName"]',
			resourceName 		: '[data-js="resourceName"]',
			policyName 			: '[data-js="policyName"]',
			gotoHive 			: '[data-js="gotoHive"]',
			gotoHbase 			: '[data-js="gotoHbase"]',
			gotoKnox 			: '[data-js="gotoKnox"]',
			gotoStorm 			: '[data-js="gotoStorm"]',
			btnShowMore 		: '[data-id="showMore"]',
			btnShowLess 		: '[data-id="showLess"]',
			btnShowMoreUsers 	: '[data-id="showMoreUsers"]',
			btnShowLessUsers 	: '[data-id="showLessUsers"]',
			componentType       : '[data-id="component"]',
			downloadReport      : '[data-id="downloadReport"]',
			policyType          : '[data-id="policyType"]',
			btnShowMoreAccess 	: '[data-id="showMoreAccess"]',
			btnShowLessAccess 	: '[data-id="showLessAccess"]',
			iconSearchInfo      : '[data-id="searchInfo"]',
			policyLabels		: '[data-id="policyLabels"]',
			zoneName			: '[data-id="zoneName"]',
            selectUserGroup		: '[data-id="btnUserGroup"]',
            roleName 			: '[data-js="roleName"]',

		},

		/** ui events hash */
		events : function() {
			var events = {};
			events['click ' + this.ui.searchBtn]  = 'onSearch';
			events['click .autoText']  = 'autocompleteFilter';
			events['click .gotoLink']  = 'gotoTable';
			events['click ' + this.ui.btnShowMore]  = 'onShowMore';
			events['click ' + this.ui.btnShowLess]  = 'onShowLess';
			events['click ' + this.ui.btnShowMoreUsers]  = 'onShowMoreUsers';
			events['click ' + this.ui.btnShowLessUsers]  = 'onShowLessUsers';
			events['click .downloadFormat'] = 'setDownloadFormatFilter';
			events['click ' + this.ui.btnShowMoreAccess] = 'onShowMorePermissions';
			events['click ' + this.ui.btnShowLessAccess] = 'onShowLessPermissions';
			return events;
		},

		/**
		 * intialize a new UserAccessLayout Layout
		 * @constructs
		 */
		initialize : function(options) {
			console.log("initialized a UserAccessLayout Layout");
			_.extend(this, _.pick(options, 'groupList','userList'));
			this.urlQueryParams = XAUtil.urlQueryParams();
			this.bindEvents();
			this.previousSearchUrl = '';
			this.searchedFlag = false;
			this.allowDownload = false;
		},
		initializeRequiredData : function() {
			this.policyCollList = [];
			this.initializeServiceDef();
			this.serviceDefList.each(function(servDef) {
				var serviceDefName = servDef.get('name')
				var collName = serviceDefName +'PolicyList';
				this[collName] = new RangerPolicyList();
				this.defaultPageState = this[collName].state;
				this.policyCollList.push({ 'collName' : collName, 'serviceDefName' : serviceDefName});
				//set subgrid coll for policy item on pagination
				this.listenTo(this[collName],'change',function(model){
					this.setSubgridCollForPolicyItems(model);			
				});
			},this);
			
		},
		initializeServiceDef : function() {
			var that = this;
			this.serviceDefList = new RangerServiceDefList();
			this.serviceDefList.fetch({
				cache : false,
				async:false
			});
			this.rangerZoneList = new RangerZoneBase();
			this.rangerZoneList.fetch({
				cache : false,
				async:false,
				url: "service/public/v2/api/zone-headers",
			})
		},

		/** all events binding here */
		bindEvents : function() {
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
//			this.listenTo(this.hiveResourceList, "change:foo", function(){alert();}, this);
		},

		onRender : function() {
			if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
				var sidebarUiElement = App.rSideBar.currentView.ui;
				this.ui.policyName = sidebarUiElement.policyName;
				this.ui.componentType = sidebarUiElement.componentType;
				this.ui.policyType = sidebarUiElement.policyType;
				this.ui.policyLabels = sidebarUiElement.policyLabels;
				this.ui.zoneName = sidebarUiElement.zoneName;
				this.ui.resourceName = sidebarUiElement.resourceName;
				this.ui.userGroup = sidebarUiElement.userGroup;
				this.ui.selectUserGroup = sidebarUiElement.selectUserGroup;
				this.ui.userName = sidebarUiElement.userName;
				this.ui.searchBtn = sidebarUiElement.searchBtn;
				this.ui.roleName = sidebarUiElement.roleName;
			}
			this.initializePlugins();
                        if( this.urlQueryParams) {
                                this.urlParam = XAUtil.changeUrlToSearchQuery(decodeURIComponent(this.urlQueryParams));
                                if (this.urlParam['polResource']) {
                                        this.ui.resourceName.val(this.urlParam['polResource']);
                                }
                                if(this.urlParam['policyNamePartial']) {
                                        this.ui.policyName.val(this.urlParam['policyNamePartial']);
                                }
                                if(!_.isUndefined(this.urlParam['user']) && !_.isEmpty(this.urlParam['user'])) {
                                        this.ui.userName.show();
                                        this.setupUserAutoComplete();
                                        this.ui.userGroup.select2('destroy');
                                        this.ui.userGroup.val('').hide();
                                        this.ui.roleName.select2('destroy');
                                        this.ui.roleName.val('').hide();
                                        this.ui.selectUserGroup.text('Username')
                                } else if (!_.isUndefined(this.urlParam['group']) && !_.isEmpty(this.urlParam['group'])) {
                                        this.ui.userGroup.show();
                                        this.setupGroupAutoComplete();
                                        this.ui.userName.select2('destroy');
                                        this.ui.userName.val('').hide();
                                        this.ui.roleName.select2('destroy');
                                        this.ui.roleName.val('').hide();
                                        this.ui.selectUserGroup.text('Group')
                                } else {
                                        this.ui.roleName.show();
                                        this.setupRoleAutoComplete();
                                        this.ui.userGroup.select2('destroy');
                                        this.ui.userGroup.val('').hide();
                                        this.ui.userName.select2('destroy');
                                        this.ui.userName.val('').hide();
                                        this.ui.selectUserGroup.text('Rolename')
                                }
                        } else {
                                this.setupGroupAutoComplete();
                        }
			this.renderComponentAndPolicyTypeSelect();
			if(!_.isUndefined(this.ui.policyType.val()) && _.isUndefined(this.urlQueryParams)) {
				this.urlQueryParams = {'policyType' : this.ui.policyType.val()}
			}
                        if(this.urlQueryParams) {
                                this.onSearch()
                        } else {
                                var policyType = this.ui.policyType.val();
        //			Show policies listing for each service and GET policies for each service
                                _.each(this.policyCollList, function(obj,i){
                                        this.renderTable(obj.collName, obj.serviceDefName);
                                        this.getResourceLists(obj.collName,obj.serviceDefName,policyType);
                                },this);
                        }
			this.$el.find('[data-js="policyName"]').focus()
			var urlString = XAUtil.getBaseUrl();
			if(urlString.slice(-1) == "/") {
				urlString = urlString.slice(0,-1);
			}
			this.previousSearchUrl = urlString+"/service/plugins/policies/downloadExcel?";
			XAUtil.searchInfoPopover(this.getSearchInfoArray() , this.ui.iconSearchInfo , 'left');
		},
		getSearchInfoArray: function(){
			return [{text :'Policy Name' , info :localization.tt('msg.policyNameMsg')},
					{text :'Policy Type' , info :localization.tt('msg.policyTypeMsg')},
					{text :'Component'   , info :localization.tt('msg.componentMsg')},
					{text :'Search By'   , info :localization.tt('msg.searchBy')},
					{text :'Resource'    , info :localization.tt('msg.resourceMsg')},
					{text :'Policy Label', info :localization.tt('msg.policyLabelsinfo')},
					{text :'Zone Name', info :localization.tt('lbl.zoneName')}]
		},
		getResourceLists: function(collName, serviceDefName , policyType){
			var that = this, coll = this[collName];
			that.allowDownload = false;
			coll.queryParams.serviceType = serviceDefName;
			if(policyType){
//				to set default value access type in policy type
				coll.queryParams.policyType = policyType;
			}
			coll.fetch({
				cache : false,
				reset: true,
//				async:false,
			}).done(function(){
				coll.trigger('sync')
				XAUtil.blockUI('unblock');
				if(coll.length >= 1 && !that.allowDownload)
					that.allowDownload = true;
				_.each(that[collName].models,function(model,ind){
					that.setSubgridCollForPolicyItems(model);
				});
			});
		},
		renderTable : function(collName,serviceDefName){
			var that = this, tableRegion  = this[collName+'Table'];
			tableRegion.show(new XATableLayout({
				columns: this.getSubgridColumns(this[collName],collName,serviceDefName),
				collection: this[collName],
				includeFilter : false,
				scrollToTop : false,
				paginationCache : false,
				gridOpts : {
					row : 	Backgrid.Row.extend({}),
					emptyText : 'No Policies found!'
				}
			}));
		
		},
		getSubgridColumns:function(coll,collName,serviceDefName){
			var that = this;

			var subcolumns = [
				{
					name: 'roles',
					cell: 'html',
					label: 'Roles',
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model, coll) {
							var role_str = '';
							if(_.isEmpty(model.get('roles'))){
								return '<center>--</center>';
							} else {
								_.each(model.get('roles'),function(role,index){
									if(index < 4) {
										role_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" role-policy-id="'+model.cid+'" style="">' + _.escape(role) + '</span>'  + " ";
									} else {
										role_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" role-policy-id="'+model.cid+'" style="display:none">' + _.escape(role) + '</span>'  + " ";
									}
								});
								if(model.get('roles').length > 4) {
									role_str += '<span class="pull-left float-left-margin-2">\
									<a href="javascript:void(0);" data-id="showMoreAccess" policy-id="'+
									model.cid+'"><code style=""> + More..</code></a></span>\
									<span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLessAccess" policy-id="'+
									model.cid+'" style="display:none;"><code style=""> - Less..</code></a></span>';}
									return role_str;
								}
						}
					}),
					editable: false,
					click: false,
					sortable: false
				},
				{
					name: 'groups',
					cell: 'html',
					label: 'Groups',
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model, coll) {
							var group_str = '';
							if(_.isEmpty(model.get('groups'))){
								return '<center>--</center>';
							} else {
								_.each(model.get('groups'),function(group,index){
									if(index < 4) {
										group_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" group-policy-id="'+model.cid+'" style="">' + _.escape(group) + '</span>'  + " ";
									} else {
										group_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" group-policy-id="'+model.cid+'" style="display:none">' + _.escape(group) + '</span>'  + " ";
									}
								});
								if(model.get('groups').length > 4) {
									group_str += '<span class="pull-left float-left-margin-2">\
									<a href="javascript:void(0);" data-id="showMoreAccess" policy-id="'+
									model.cid+'"><code style=""> + More..</code></a></span>\
									<span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLessAccess" policy-id="'+
									model.cid+'" style="display:none;"><code style=""> - Less..</code></a></span>';}
									return group_str;
								}
						}
					}),
					editable: false,
					click: false,
					sortable: false
				},
				{
					name: 'users',
					cell: 'html',
					label: 'Users',
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model) {
							var user_str = '';
							if(_.isEmpty(model.get('users'))){
								return '<center>--</center>';
							} else {
								_.each(model.get('users'),function(user,index){
									if(index < 4) {
										user_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" user-policy-id="'+model.cid+'" style="">' + _.escape(user) + '</span>'+ " ";
									} else {
										user_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" user-policy-id="'+model.cid+'" style="display:none">' + _.escape(user) + '</span>'+ " ";
									}
								});
								if(model.get('users').length > 4) {
									user_str += '<span class="pull-left float-left-margin-2">\
									<a href="javascript:void(0);" data-id="showMoreAccess" policy-id="'+
									model.cid+'"><code style=""> + More..</code></a></span>\
									<span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLessAccess" policy-id="'+
									model.cid+'" style="display:none;"><code style=""> - Less..</code></a></span>';}
									return user_str;}
								}
					}),
					editable: false,
					click: false,
					sortable: false
				},
				{ 
					name: 'accesses',
					cell: 'html',
					label: 'Accesses',
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model) {
							var access_str = '';
							_.each(model.get('accesses'),function(access,index){
								if(index < 4){
                                                                        access_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" access-policy-id="'+model.cid+'" style="">' + access.type+'</span>'  + " ";
								} else {
                                                                        access_str += '<span class="badge badge-info cellWidth-1 float-left-margin-2" access-policy-id="'+model.cid+'" style="display:none">' + access.type+'</span>'+ " ";
								}
							});
							if(model.get('accesses').length > 4) {
								access_str += '<span class="pull-left float-left-margin-2">\
								<a href="javascript:void(0);" data-id="showMoreAccess" policy-id="'+
								model.cid+'"><code style=""> + More..</code></a></span>\
								<span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLessAccess" policy-id="'+
				                model.cid+'" style="display:none;"><code style=""> - Less..</code></a></span>';}
								return access_str;
						}
					}),
					editable: false,
					click: false,
					sortable: false
				}
			];
               if(XAUtil.isMaskingPolicy(this.ui.policyType.val())){
                        subcolumns.push({
                                name: 'maskingCondition',
                                cell: Backgrid.HtmlCell.extend({
                                        className : 'subgridTable'
                        }),
                        label: 'Masking Condition',
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function (rawValue,model) {
                                var access_str = '';
                                var servicedef = _.find(that.serviceDefList.models ,function(m){
									return m.get('name') == model.get('type');
                                });
                                var maskValue = _.find(servicedef.get('dataMaskDef').maskTypes,function(m){
									return m.name == model.get('dataMaskInfo').dataMaskType
                                })
                                if(maskValue.label){
									if(maskValue.label == "Custom"){
										var title = model.attributes.dataMaskInfo.dataMaskType +' : '+model.attributes.dataMaskInfo.valueExpr;
										access_str = '<span class="badge badge-info trim-containt" title="'+_.escape(title)+'">'+_.escape(title)+'</span>';
									}else{
										access_str = '<span class="badge badge-info trim-containt" title="'+_.escape(maskValue.label)+'">'+_.escape(maskValue.label)+'</span>';
									}
                                }else {
                                  access_str ='<center>--</center>';
                                }
                                return access_str;
                            }
                        }),
                        editable: false,
                        click: false,
                        sortable: false

                })
            };
            if(XAUtil.isRowFilterPolicy(this.ui.policyType.val())){
                    subcolumns.push({
                            name: 'rowLevelFilter',
                            cell: Backgrid.HtmlCell.extend({
                                className : 'subgridTable'
                            }),
                            label: 'Row Level Filter',
                            formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                fromRaw: function (rawValue,model) {
                                    var access_str = '';
                                    if(model.get('rowFilterInfo').filterExpr){
                                           access_str = '<span class="badge badge-info trim-containt" title="'+_.escape(model.get('rowFilterInfo').filterExpr)+'">' + _.escape(model.get('rowFilterInfo').filterExpr)+ '</span>'  + " ";
                                    } else {
                                           access_str ='<center>--</center>';
                                    }
                                    return access_str;
                                }
                            }),
                            editable: false,
                            click: false,
                            sortable: false
                    })
            }
			var columns = {
				id : {
					cell : "uri",
					href: function(model){
						var rangerService = new RangerService();
						rangerService.urlRoot += '/name/'+model.get('service');
						rangerService.fetch({
							cache : false,
							async : false
						});
						return '#!/service/'+rangerService.get('id')+'/policies/'+model.id+'/edit';
					},
					label	: localization.tt("lbl.policyId"),
					editable: false,
					sortable : false
				},
				name : {
					cell : 'string',
					label	: localization.tt("lbl.policyName"),
					editable: false,
					sortable : false
				},
				policyLabels: {
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					label : localization.tt("lbl.policyLabels"),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							var labels ="";
							if(!_.isUndefined(rawValue) && rawValue.length != 0){
								return XAUtil.showMoreAndLessButton(rawValue, model)
							}else{
								return '--';
							}
						}
					}),
					editable : false,
                                        sortable : false,
				},
				resources: {
					label: 'Resources',
					cell: 'Html',
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model) {
							var strVal = '', names = '';
							var resource = model.get('resources');
							_.each(resource,function(resourceObj,key){
								strVal += "<b>"+key+":</b>";
								strVal += "<span title='";
								names = '';
								_.map(resourceObj.values,function(resourceVal){
									names += _.escape(resourceVal)+",";
								});
								names = names.slice(0,-1);
								strVal += names + "'>"+names +"</span>";
								strVal = strVal+ "<br />";
							});
							return strVal;
						}
					}),
					editable: false,
					sortable: false,
					click: false
					},
				policyType: {
					label: 'Policy Type',
					cell: Backgrid.HtmlCell.extend({className: 'html-cell, cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype,{
						fromRaw: function(rawValue,model){
							var policyType = model.get("policyType");
							var startLbl = '<label class="badge badge-primary">';
							if (XAUtil.isMaskingPolicy(policyType)) {
								return startLbl + XAEnums.RangerPolicyType.RANGER_MASKING_POLICY_TYPE.label + '</label>';
							} else if (XAUtil.isRowFilterPolicy(policyType)) {
								return startLbl + XAEnums.RangerPolicyType.RANGER_ROW_FILTER_POLICY_TYPE.label + '</label>';
							}else{// by default it is access
								return startLbl + XAEnums.RangerPolicyType.RANGER_ACCESS_POLICY_TYPE.label + '</label>';
							}
						}
					}),
					editable: false,
					sortable: false,
					click: false
				},
				isEnabled:{
					label:localization.tt('lbl.status'),
					cell :"html",
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue) {
							return rawValue ? '<label class="badge badge-success" style="float:inherit;">Enabled</label>' : '<label class="badge badge-danger" style="float:inherit;">Disabled</label>';
						}
					}),
					click : false,
					drag : false,
					sortable : false
				},
				zoneName: {
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					label : localization.tt("lbl.zoneName"),
					editable : false,
                                        sortable : false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							var labels ="";
							if(!_.isUndefined(rawValue) && rawValue.length != 0){
								return '<span class="badge badge-dark" style="float:inherit;">'+rawValue+'</span>'
							}else{
								return '<span style="float:inherit;">'+"--"+'</span>';
							}
						}
					}),
				},
			};
			var permissions = this.getPermissionColumns(this[collName],collName,serviceDefName,subcolumns);
			_.extend(columns,permissions);
                        if(XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                            columns.id.cell = 'string';
                        }
			return coll.constructor.getTableCols(columns, coll);
		},
		getPermissionColumns: function (coll,collName,serviceDefName,subcolumns){

			var that = this,permissions,cols,serviceDefOptions;

			var serviceDefOptions = {};
			_.each(this.serviceDefList.models, function(model){
				if(model.get('name') === serviceDefName)
					serviceDefOptions = model.get('options');
			});
			var enableDenyAndExceptionsInPolicies = false;
			if(!_.isUndefined(serviceDefOptions.enableDenyAndExceptionsInPolicies))
				 enableDenyAndExceptionsInPolicies = true;
			if(serviceDefName === XAEnums.ServiceType.SERVICE_TAG.label){
				enableDenyAndExceptionsInPolicies = true;
			}
            if(XAUtil.isAccessPolicy(this.ui.policyType.val())){
                permissions = {
                        allow:{
                                label: 'Allow Conditions',
                                cell: "subgrid-custom",
                                optionValues : subcolumns,
                                editable: false,
                                sortable: false,
                                click : false
                           }
                        };
            }else if(XAUtil.isMaskingPolicy(this.ui.policyType.val())){
                                permissions = {
                                        mask:{
                                                        label: 'Masking Conditions',
                                                        cell: "subgrid-custom",
                                                        optionValues : subcolumns,
                                                        editable: false,
                                                        sortable: false,
                                                        click : false
                                        }
                            };
                   }else if(XAUtil.isRowFilterPolicy(this.ui.policyType.val())){
                                permissions = {
                                                rowlvl:{
                                                                label: 'Row Level Conditions',
                                                                cell: "subgrid-custom",
                                                                optionValues : subcolumns,
                                                                editable: false,
                                                                sortable: false,
                                                                click : false
                                                }
                                    };
                        }


            if(enableDenyAndExceptionsInPolicies && that.ui.policyType.val() == 0) {
            	cols = {
            			allowExclude:{
            				label: 'Allow Exclude',
            				cell: "subgrid-custom",
            				optionValues : subcolumns,
            				editable: false,
            				sortable: false,
            				click : false
            			},	
            			deny:{
            				label: 'Deny Conditions',
            				cell: "subgrid-custom",
            				optionValues : subcolumns,
            				editable: false,
            				sortable: false,
            				click : false
            			},
            			denyExclude:{
            				label: 'Deny Exclude',
            				cell: "subgrid-custom",
            				optionValues : subcolumns,
            				editable: false,	
            				sortable: false,
            				click : false
            			}
            	};
            }
			return _.extend(permissions,cols);
		},

		/* add 'component' and 'policy type' select */
		renderComponentAndPolicyTypeSelect: function(){
			var that = this;
			var options = this.serviceDefList.map(function(m){ return { 'id' : m.get('name'), 'text' : m.get('name')}; });
			var policyTypes = _.map(XAEnums.RangerPolicyType,function(m){
				return {'id': m.value,'text': m.label};
			});
			var zoneListOptions = _.map(this.rangerZoneList.attributes, function(m){
				return { 'id':m.name, 'text':m.name}
			});
			zoneListOptions = _.sortBy(zoneListOptions, 'id')
                        var tags = [];
                        if (this.urlParam && this.urlParam['policyLabelsPartial'] && !_.isEmpty(this.urlParam['policyLabelsPartial'])) {
                                tags.push( { 'id' : _.escape( this.urlParam['policyLabelsPartial'] ), 'text' : _.escape( this.urlParam['policyLabelsPartial'] ) } );
                        }
			this.ui.componentType.select2({
				multiple: true,
				closeOnSelect: true,
				placeholder: 'Select Component',
				//maximumSelectionSize : 1,
                                value : (!_.isUndefined(that.urlParam) && !_.isUndefined(that.urlParam['serviceType']) && !_.isEmpty(that.urlParam['serviceType'])) ?
                                                this.ui.componentType.val(that.urlParam['serviceType']) : this.ui.componentType.val(""),
				allowClear: true,
				data: options
			});
			this.ui.policyType.select2({
				closeOnSelect: false,
				maximumSelectionSize : 1,
				value : (!_.isUndefined(that.urlParam) && !_.isUndefined(that.urlParam['policyType']) && !_.isEmpty(that.urlParam['policyType'])) ?
                                                this.ui.policyType.val(that.urlParam['policyType']) : this.ui.policyType.val("0"),
				allowClear: false,
				data: policyTypes
			});
			this.ui.policyLabels.select2({
                                multiple: true,
                closeOnSelect : true,
                placeholder : 'Policy Label',
                allowClear: true,
                tokenSeparators: ["," , " "],
                tags : true,
                maximumSelectionSize : 1,
                initSelection : function (element, callback) {
                    callback(tags);
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
                ajax: {
                    url: 'service/plugins/policyLabels',
                    dataType: 'json',
                    data: function (term, page) {
                        return {policyLabel : term};
                    },
                    results: function (data, page) {
                        var results = [] , selectedVals = [];
                        if(data.resultSize != "0"){
                            results = data.map(function(m){	return {id : _.escape(m), text: _.escape(m) };	});
                        }
                        return {results : results};
                    },
                },
                formatResult : function(result){
                    return result.text;
                },
                formatSelection : function(result){
                    return result.text;
                },
			});
                        if(this.urlParam && this.urlParam['policyLabelsPartial']) {
                                this.ui.policyLabels.val(this.urlParam['policyLabelsPartial']).trigger('change');
                        }
			this.ui.zoneName.select2({
				closeOnSelect: false,
				maximumSelectionSize : 1,
				allowClear: true,
				data: zoneListOptions,
				placeholder: 'Select Zone Name',
                                value : (!_.isUndefined(that.urlParam) && !_.isUndefined(that.urlParam['zoneName']) && !_.isEmpty(that.urlParam['zoneName'])) ?
                                                this.ui.zoneName.val(that.urlParam['zoneName']) : this.ui.zoneName.val(""),
			});
		},
		onDownload: function(e){
			var that = this, url = '';
			if (!_.isUndefined($('.latestResponse')) && $('.latestResponse').length > 0) {
				$('.latestResponse').html('<b>Last Response Time : </b>' + Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
			}
			if(!this.allowDownload){
				return XAUtil.alertBoxWithTimeSet(localization.tt('msg.noPolicytoExport'))
			}
			if(!this.searchedFlag) {
				url =  this.previousSearchUrl;
			} else if (this.searchedFlag && this.updatedUrl) {
				var urlString = XAUtil.getBaseUrl();
				if(urlString.slice(-1) === "/") {
					urlString = urlString.slice(0,-1);
				}
				url = url + urlString;
				if (e === "xlsFormat") {
					url = url + '/service/plugins/policies/downloadExcel?';
				}else if (e === "csvFormat") {
					url = url + '/service/plugins/policies/csv?';
				}else{
					url = url + '/service/plugins/policies/exportJson?';
				}
				url = url + this.searchedParamsString + this.searchedComponentString;
				this.previousSearchUrl = url;
				this.searchedFlag = true;
			}
			this.ui.downloadReport.attr("href",url)[0].click();
		},
		setDownloadReportUrl: function(that,component,params){

			var compString = '', url = '';
			if(!_.isUndefined(component)) {
				_.each(component,function(comp){
					compString = compString + comp + ',';
					});
			}
			if (!_.isEmpty(compString)) {
				compString = compString.slice(0,-1);
			}
			_.each(params, function(val, paramName){
				if(_.isUndefined(val) || _.isEmpty(val)) {
					delete params[paramName];
				}
			});
			var str = jQuery.param( params );
			this.searchedComponentString = "&serviceType=" + compString;
			this.searchedParamsString = str;
			this.updatedUrl = true;

		},	
		/** on render callback */
		setupGroupAutoComplete : function(){
                        var that = this;
			this.ui.userGroup.select2({
				closeOnSelect : true,
				placeholder : 'Select Group',
				maximumSelectionSize : 1,
				tokenSeparators: [",", " "],
				allowClear: true,
				// tags : this.groupArr,
				initSelection : function (element, callback) {
                                        var data = {};
                                        data = {id: element.val(), text: element.val()};
					callback(data);
				},
				ajax: { 
					url: "service/xusers/lookup/groups",
					dataType: 'json',
					data: function (term, page) {
						return {name : term};
					},
					results: function (data, page) { 
						var results = [],selectedVals = [];
						if(!_.isEmpty(that.ui.userGroup.val()))
							selectedVals = that.ui.userGroup.val().split(',');
						if(data.totalCount != "0"){
							results = data.vXStrings.map(function(m){	return {id : m.value, text: _.escape(m.value) };	});
							if(!_.isEmpty(selectedVals))
								results = XAUtil.filterResultByIds(results, selectedVals);
							return {results : results};
						}
						return {results : results};
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No group found.';
				}
			})//.on('select2-focus', XAUtil.select2Focus);
                        if(this.urlParam && this.urlParam['group'] && !_.isEmpty(this.urlParam['group'])) {
                                this.ui.userGroup.val(this.urlParam['group']).trigger('change');
                        }
                },
		setupUserAutoComplete : function(){
			var that = this;
			this.ui.userName.select2({
				closeOnSelect : true,
				placeholder : 'Select User',
				allowClear: true,
				initSelection : function (element, callback) {
                                        var data = {};
                                        data = {id: element.val(), text: element.val()};
					callback(data);
				},
				ajax: { 
					url: "service/xusers/lookup/users",
					dataType: 'json',
					data: function (term, page) {
						return {name : term};
					},
					results: function (data, page) { 
						var results = [],selectedVals=[];
						if(!_.isEmpty(that.ui.userName.select2('val')))
							selectedVals = that.ui.userName.select2('val');
						if(data.totalCount != "0"){
							results = data.vXStrings.map(function(m){	return {id : m.value, text: _.escape(m.value) };	});
							if(!_.isEmpty(selectedVals))
								results = XAUtil.filterResultByIds(results, selectedVals);
							return {results : results};
						}
						return {results : results};
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No user found.';
				}
				
			})//.on('select2-focus', XAUtil.select2Focus);
                        if(this.urlParam && this.urlParam['user'] && !_.isEmpty(this.urlParam['user'])) {
                                this.ui.userName.val(this.urlParam['user']).trigger('change');
                        }
		},

		setupRoleAutoComplete : function(){
			var that = this;
			this.ui.roleName.select2({
				closeOnSelect : true,
				placeholder : 'Select Role',
				allowClear: true,
				initSelection : function (element, callback) {
                                        var data = {};
                                        data = {id: element.val(), text: element.val()};
					callback(data);
				},
				ajax: {
					url: "service/roles/roles",
					dataType: 'json',
					data: function (term, page) {
						return {roleNamePartial : term};
					},
					results: function (data, page) {
						var results = [],selectedVals=[];
						if(!_.isEmpty(that.ui.roleName.select2('val')))
							selectedVals = that.ui.roleName.select2('val');
						if(data.totalCount != "0"){
							results = data.roles.map(function(m){	return {id : _.escape(m.name), text: _.escape(m.name) };});
							if(!_.isEmpty(selectedVals))
								results = XAUtil.filterResultByIds(results, selectedVals);
							return {results : results};
						}
						return {results : results};
					}
				},
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No user found.';
				}
			})//.on('select2-focus', XAUtil.select2Focus);
                        if(this.urlParam && this.urlParam['role'] && !_.isEmpty(this.urlParam['role'])) {
                                this.ui.roleName.val(this.urlParam['role']).trigger('change');
                        }
		},

		/** all post render plugin initialization */
		initializePlugins : function() {
			var that = this;
			this.$(".wrap-header").each(function() {
				var wrap = $(this).next();
				// If next element is a wrap and hasn't .non-collapsible class
				if (wrap.hasClass('wrap') && ! wrap.hasClass('non-collapsible'))
					$(this).prepend('<a href="#" class="wrap-collapse btn-right">hide&nbsp;&nbsp;<i class="fa-fw fa fa-caret-up"></i></a>').prepend('<a href="#" class="wrap-expand btn-right" style="display: none">show&nbsp;&nbsp;<i class="fa-fw fa fa-caret-down"></i></a>');
			});
			
			// Collapse wrap
			$(document).on("click", "a.wrap-collapse", function() {
				var self = $(this).hide(100, 'linear');
				self.parent('.wrap-header').next('.wrap').slideUp(500, function() {
					$('.wrap-expand', self.parent('.wrap-header')).show(100, 'linear');
				});
				return false;

				// Expand wrap
			}).on("click", "a.wrap-expand", function() {
				var self = $(this).hide(100, 'linear');
				self.parent('.wrap-header').next('.wrap').slideDown(500, function() {
					$('.wrap-collapse', self.parent('.wrap-header')).show(100, 'linear');
				});
				return false;
			});
			
			this.ui.resourceName.bind( "keydown", function( event ) {
				if ( event.keyCode === $.ui.keyCode.ENTER ) {
					that.onSearch();
				}
			});
			
		},
		onSearch : function(e){
                        var that = this, url = '', urlString = XAUtil.getBaseUrl(), urlParam = {};
			//Get search values
                        var groups = (this.ui.selectUserGroup.text().trim() == "Group" ) ? this.ui.userGroup.select2('val'):undefined;
                        var users = (this.ui.selectUserGroup.text().trim() == "Username") ? this.ui.userName.select2('val'):undefined;
                        var roles = (this.ui.selectUserGroup.text().trim() == "Rolename") ? this.ui.roleName.select2('val'):undefined;
			var rxName = this.ui.resourceName.val(), policyName = this.ui.policyName.val() , policyType = this.ui.policyType.val(),
			policyLabel = this.ui.policyLabels.val(), zoneName = this.ui.zoneName.val()
			var params = {group : groups, user : users, role : roles, polResource : rxName, policyNamePartial : policyName, policyType: policyType, policyLabelsPartial:policyLabel,
				zoneName : zoneName};
			var component = (this.ui.componentType.val() != "") ? this.ui.componentType.select2('val'):undefined;
                        urlParam = _.extend(params, {'serviceType': this.ui.componentType.val()});
                        _.each(urlParam, function(value, key, obj){
                                if (value === "" || value  === undefined) {
                                        delete obj[key];
                                }
                        })
                        XAUtil.changeParamToUrlFragment(urlParam);
                        that.initializeRequiredData();
            _.each(this.policyCollList, function(obj,i){
                    this.renderTable(obj.collName, obj.serviceDefName);
            },this);
			var compFlag = false;
			if(!_.isUndefined(component)) {
				compFlag = true;
			} else {
				compFlag = false;
			}
			that.$el.find('[data-compHeader]').show();
			that.$el.find('[data-comp]').show();
			if(compFlag) { //if components selected
				that.$el.find('[data-compHeader]').hide();
				that.$el.find('[data-comp]').hide();
				_.each(that.policyCollList, function(obj,i){
					_.each(component,function(comp){
						if(comp === obj.serviceDefName) {
							var coll = that[obj.collName];
							//clear previous query params
							_.each(params, function(val, attr){
								delete coll.queryParams[attr];
							});
							//Set default page state
							coll.state = that.defaultPageState;
							coll.queryParams = $.extend(coll.queryParams, params);
							that.getResourceLists(obj.collName, obj.serviceDefName);
							that.$el.find('[data-compHeader="'+comp+'"]').show();
							that.$el.find('[data-comp="'+comp+'"]').show();
						}
					});
				},this);
			} else {
				// show all tables if no search component values selected
				that.$el.find('[data-compHeader]').show();
				that.$el.find('[data-comp]').show();
				_.each(this.policyCollList, function(obj,i){
					var coll = this[obj.collName];
					//clear previous query params
					_.each(params, function(val, attr){
						delete coll.queryParams[attr];
					});
					//Set default page state
					coll.state = this.defaultPageState;
					coll.queryParams = $.extend(coll.queryParams, params);
					this.getResourceLists(obj.collName, obj.serviceDefName);
				},this);
			}
			params = {
				group : groups,
				user : users,
				role : roles,
				polResource : rxName,
				policyNamePartial : policyName,
				policyType: policyType,
				policyLabelsPartial:policyLabel,
				zoneName : zoneName
			};

			this.setDownloadReportUrl(this,component,params);
			this.searchedFlag = true;
        },
		autocompleteFilter	: function(e){
			var $el = $(e.currentTarget);
			var $button = $(e.currentTarget).parent().parent().find('button');
			if($el.data('id')== "grpSel"){
				$button.text('Group');
				this.ui.userGroup.show();
				this.setupGroupAutoComplete();
				this.ui.userName.select2('destroy');
				this.ui.userName.val('').hide();
				this.ui.roleName.select2('destroy');
				this.ui.roleName.val('').hide();
			} else if ($el.data('id')== "userSel") {
				this.ui.userGroup.select2('destroy');
				this.ui.userGroup.val('').hide();
				this.ui.userName.show();
				this.setupUserAutoComplete();
				$button.text('Username');
				this.ui.roleName.select2('destroy');
				this.ui.roleName.val('').hide();
			}else {
				this.ui.userGroup.select2('destroy');
				this.ui.userGroup.val('').hide();
				this.ui.roleName.show();
				this.setupRoleAutoComplete();
				$button.text('Rolename');
				this.ui.userName.select2('destroy');
				this.ui.userName.val('').hide();
			}
		},
		setDownloadFormatFilter : function(e){
			var that = this;
			var el = $(e.currentTarget);
			var urlString = XAUtil.getBaseUrl();
			if(urlString.slice(-1) === "/") {
				urlString = urlString.slice(0,-1);
			}
			if(el.data('id') === "xlsFormat") {
				if(!that.searchedFlag) {
					this.previousSearchUrl = urlString + "/service/plugins/policies/downloadExcel?";
				}
			}
			else if(el.data('id') === "csvFormat") {
				if(!that.searchedFlag) {
					this.previousSearchUrl = urlString + "/service/plugins/policies/csv?";
				}
			} else {
				if(!that.searchedFlag) {
					this.previousSearchUrl = urlString + "/service/plugins/policies/exportJson?";
				}
			}
			this.onDownload(el.data('id'));
		},
		gotoTable : function(e){
			var that = this, elem = $(e.currentTarget),pos;
			var scroll = false;
			if(elem.attr('data-js') == this.ui.gotoHive.attr('data-js')){
				if(that.rHiveTableList.$el.parent('.wrap').is(':visible')){
					pos = that.rHiveTableList.$el.offsetParent().position().top - 100;
					scroll =true;	
				}
			} else if(elem.attr('data-js') == this.ui.gotoHbase.attr('data-js')){
				if(that.rHbaseTableList.$el.parent('.wrap').is(':visible')){
					pos = that.rHbaseTableList.$el.offsetParent().position().top - 100;
					scroll = true;
				}
			} else if(elem.attr('data-js') == this.ui.gotoKnox.attr('data-js')){
				if(that.rKnoxTableList.$el.parent('.wrap').is(':visible')){
					pos = that.rKnoxTableList.$el.offsetParent().position().top - 100;
					scroll = true;
				}
			} else if(elem.attr('data-js') == this.ui.gotoStorm.attr('data-js')){
				if(that.rStormTableList.$el.parent('.wrap').is(':visible')){
					pos = that.rStormTableList.$el.offsetParent().position().top - 100;
					scroll = true;
				}
			}
			if(scroll){
				$("html, body").animate({
					scrollTop : pos
				}, 1100);
			}
		},
		
		setSubgridCollForPolicyItems: function(model){
			if (XAUtil.isMaskingPolicy(model.get('policyType'))) {
				//'<name>Collection' must be same as subgrid custom column name
                model.attributes.maskCollection = model.get('dataMaskPolicyItems');
                //Add service type in masking condition
                _.each(model.attributes.dataMaskPolicyItems , function(m){
                    m.type = model.collection.queryParams.serviceType;
                })
			} else if (XAUtil.isRowFilterPolicy(model.get('policyType'))) {
                model.attributes.rowlvlCollection = model.get('rowFilterPolicyItems');
			} else {
				model.attributes.allowCollection = model.get('policyItems');
			}
			model.attributes.denyCollection  = model.get('denyPolicyItems');
			model.attributes.denyExcludeCollection    = model.get('denyExceptions');
			model.attributes.allowExcludeCollection = model.get('allowExceptions');
		},
		onShowMorePermissions: function(e){
						var policyId = $(e.currentTarget).attr('policy-id');
						var $td = $(e.currentTarget).parents('td');
						$td.find('[access-policy-id="'+policyId+'"]').show();
						$td.find('[user-policy-id="'+policyId+'"]').show();
						$td.find('[group-policy-id="'+policyId+'"]').show();
						$td.find('[data-id="showLessAccess"][policy-id="'+policyId+'"]').show();
						$td.find('[data-id="showMoreAccess"][policy-id="'+policyId+'"]').hide();

					},
					onShowLessPermissions: function(e){
						var policyId = $(e.currentTarget).attr('policy-id');
						var $td = $(e.currentTarget).parents('td');
						$td.find('[access-policy-id="'+policyId+'"]').slice(4).hide();
						$td.find('[user-policy-id="'+policyId+'"]').slice(4).hide();
						$td.find('[group-policy-id="'+policyId+'"]').slice(4).hide();
						$td.find('[data-id="showMoreAccess"][policy-id="'+policyId+'"]').show();
						$td.find('[data-id="showLessAccess"][policy-id="'+policyId+'"]').hide();

					},

		onShowMore : function(e){
                    var attrName = this.attributName(e);
                    var id = $(e.currentTarget).attr(attrName[0]);
			var $td = $(e.currentTarget).parents('td');
			$td.find('['+attrName+'="'+id+'"]').show();
			$td.find('[data-id="showLess"]['+attrName+'="'+id+'"]').show();
			$td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').hide();
		},
		onShowLess : function(e){
                    var attrName = this.attributName(e);
                    var id = $(e.currentTarget).attr(attrName[0]);
			var $td = $(e.currentTarget).parents('td');
			$td.find('['+attrName+'="'+id+'"]').slice(4).hide();
			$td.find('[data-id="showLess"]['+attrName+'="'+id+'"]').hide();
			$td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').show();
		},
                attributName :function(e){
                    var attrName = ['policy-groups-id', 'policy-users-id', 'policy-label-id'], attributeName = "";
                    attributeName =_.filter(attrName, function(name){
                        if($(e.currentTarget).attr(name)){
                            return name;
                        }
                    });
                    return attributeName;
                },
		/** on close */
		onClose : function() {
            XAUtil.removeUnwantedDomElement();
		}
	});

	return UserAccessLayout;
});